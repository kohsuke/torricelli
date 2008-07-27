package torricelli;

import groovy.util.Node;
import org.kohsuke.scotland.xstream.XmlFile;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.QueryParameter;
import org.xml.sax.SAXException;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.Project;
import torricelli.tasks.TaskThread;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Mercurial repository.
 *
 * @author Kohsuke Kawaguchi
 */
public class Repository {
    public transient final String name;

    private String description;

    private String upstream;

    private ChangeDirection dir;

    /**
     * Root directory of the repository.
     */
    public transient final File home;

    /**
     * Currently running, or last completed task.
     */
    protected transient volatile TaskThread task;

    /**
     * Repository group that this belongs to.
     */
    public transient final Group group;

    public Repository(Group group, File home) throws IOException {
        this.name = home.getName();
        this.home = home;
        this.group = group;
        XmlFile xml = getXmlFile();
        if(xml.exists())
            xml.unmarshal(this);
    }

    public TaskThread getTask() {
        return task;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return Stapler.getCurrentRequest().findAncestor(this).getUrl();
    }

    public XmlFile getXmlFile() {
        return new XmlFile(new File(home.getParentFile(),name+".xml"));
    }

    public Repository getUpstream() throws IOException {
        if(upstream==null)  return null;
        return group.getRepository(upstream);
    }

    /**
     * How does the changes flow to the upstream?
     */
    public ChangeDirection getChangeDirection() {
        if(dir==null)
            return ChangeDirection.UP;  // compatibility with old data
        return dir;
    }

    public void setUpstream(Repository r) {
        this.upstream = r.name;
    }

    public void startTask(TaskThread t) {
        t.start();
        this.task = t;
    }

    public void clearTask() {
        this.task = null;
    }

    /**
     * Delegate the processing to "hg serve".
     *
     * This is mostly for helping development by looking at the backend raw output.
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        getRunner().proxy(req, rsp);
    }

    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        TaskThread t = task;
        if(t!=null && t.isProminent()) {
            req.getView(this,"executingProminentTask").forward(req,rsp);
        } else {
            // the default action is to forward to "hg serve"
            getRunner().proxy(req, rsp);

            if(req.getMethod().equals("POST"))
                notifyChanges();
        }
    }

    /**
     * Called after a change is submitted to the repository.
     */
    protected void notifyChanges() {
    }

    public void doConfigSubmit(StaplerRequest req, StaplerResponse rsp) throws IOException {
        String up = req.getParameter("upstream");
        if(up.equals("(none)"))
            up = null;  // no upstream
        this.upstream = up;
        this.description = req.getParameter("description");
        this.dir = ChangeDirection.valueOf(req.getParameter("dir"));

        save();
        rsp.sendRedirect2(".");
    }

    /**
     * Deletes a repository.
     */
    public void doDoDelete(StaplerResponse rsp, @QueryParameter("src") String src) throws IOException, InterruptedException, ServletException {
        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(home);
        del.execute();

        rsp.sendRedirect("..");
    }

    public void save() throws IOException {
        getXmlFile().write(this);
    }

    /**
     * Sends the request to the backend "hg serve" and parses the result into
     * the format suitable for processing by Groovy.
     */
    public Node parse(String relative) throws SAXException, ParserConfigurationException, IOException {
        return getRunner().parse('/'+group.name+'/'+name+relative);
    }

    protected final HgServeRunner getRunner() {
        return Torricelli.INSTANCE.getRunner();
    }
}
