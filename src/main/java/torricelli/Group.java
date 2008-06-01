package torricelli;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.kohsuke.scotland.xstream.XmlFile;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import torricelli.tasks.RemoteCloneTask;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inter-related group of {@link Repository}s.
 *
 * <p>
 * Persisted with {@link Torricelli}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Group extends AbstractModelObject {
    public transient final String name;
    public transient final File home;

    /**
     * Repository list.
     */
    private transient final ConcurrentHashMap<String,Repository> repositories = new ConcurrentHashMap<String,Repository>();

    public Group(String name) throws IOException {
        this.name = name;
        this.home = new File(Torricelli.INSTANCE.home,name);
        XmlFile xml = getXmlFile();
        if(xml.exists())
            xml.unmarshal(this);
    }

    public XmlFile getXmlFile() {
        return new XmlFile(new File(home,"group.xml"));
    }

    /**
     * Gets the repository.
     */
    public Repository getRepository(String name) throws IOException {
        Repository r = repositories.get(name);
        if(r==null) {
            File repoDir = new File(home,name);
            if(!repoDir.exists())
                return null;

            r = Torricelli.NEW ? new Repository2(this,repoDir) : new Repository(this,repoDir);
            Repository prev = repositories.putIfAbsent(name, r);
            if(prev!=null)  r=prev;
        } else {
            if(!r.home.exists()) {
                // no longer a valid repository. files might have been removed on the file system
                repositories.remove(name);
                r = null;
            }
        }
        return r;
    }

    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) throws IOException {
        return getRepository(name);
    }

    /**
     * List up all the repositories in the root
     */
    public List<Repository> listRepositories() throws IOException {
        File[] repos = home.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return new File(f, ".hg").exists();
            }
        });
        if (repos == null) return Collections.emptyList();

        List<Repository> r = new ArrayList<Repository>();
        for (File repo : repos) {
            r.add(getRepository(repo.getName()));
        }

        return r;
    }

    public void save() throws IOException {
        home.mkdirs();
        getXmlFile().write(this);
    }

    /**
     * Creates a new repository.
     */
    public void doCreate(StaplerResponse rsp, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        // create a new mercurial repository
        File repoHome = new File(home,name);
        repoHome.mkdirs();
        if(!repoHome.exists()) {
            sendError("Failed to create "+repoHome);
            return;
        }

        HgInvoker hgi = new HgInvoker(repoHome,"init");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = hgi.launch(baos).join();
        if(r!=0) {
            sendError("hg init failed: "+r+"\n<PRE>"+baos+"</PRE>");
            return;
        }

        rsp.sendRedirect2(name);
    }
    
    /**
     * Clones a repository to a new one.
     */
    public void doClone(StaplerResponse rsp, @QueryParameter("src") String src, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        Repository srcRepo = getRepository(src);
        if(srcRepo==null) {
            sendError("No such repository: "+src);
            return;
        }

        // create a new mercurial repository
        HgInvoker hgi = new HgInvoker(home,"clone","--quiet",src,name);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = hgi.launch(baos).join();
        if(r!=0) {
            sendError("hg clone failed: "+r+"\n<PRE>"+baos+"</PRE>");
            return;
        }

        rsp.sendRedirect(name);
    }

    /**
     * Clones a remote repository to a new one.
     */
    public void doRemoteClone(StaplerResponse rsp, @QueryParameter("src") String src, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        File newHome = new File(home,name);
        newHome.mkdirs();
        Repository r = getRepository(name);
        r.startTask(new RemoteCloneTask(r,src,newHome));

        rsp.sendRedirect(name);
    }

    /**
     * Deletes a repository.
     */
    public void doDoDelete(StaplerResponse rsp, @QueryParameter("src") String src) throws IOException, InterruptedException, ServletException {
        Repository srcRepo = getRepository(src);
        if(srcRepo==null) {
            sendError("No such repository: "+src);
            return;
        }

        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(srcRepo.home);
        del.execute();

        rsp.sendRedirect(".");
    }

    /**
     * Make sure that the name is usable as the repository name.
     */
    protected boolean checkName(String name) throws IOException, ServletException {
        Repository repo = getRepository(name);
        if(repo!=null) {
            sendError("Repository "+ name +" already exists");
            return false;
        }

        return super.checkName(name);
    }
}
