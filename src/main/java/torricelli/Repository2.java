package torricelli;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import torricelli.tasks.TaskThread;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Repository2 extends Repository {
    public Repository2(Group group, File home) throws IOException {
        super(group, home);
    }

    @Override
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        String qs = req.getQueryString();
        if(qs!=null && qs.startsWith("cmd=")) {
            // Mercurial commands. Forward to the backend
            getRunner().proxy(req, rsp);
            if(req.getMethod().equals("POST"))
                notifyChanges();
        } else {
            TaskThread t = task;
            if(t!=null && t.isProminent()) {
                req.getView(this,"executingProminentTask").forward(req,rsp);
            } else {
                rsp.sendRedirect2("browse/");
//                req.getView(this,"_index").forward(req,rsp);
            }
        }
    }

    /**
     * Binds the tip manifest under the browse/ URL.
     */
    public Dir getBrowse() {
        return getChangeSet("tip").manifest();
    }

    public ChangeSet getChangeSet(String id) {
        return new ChangeSet(this,id);
    }

    /**
     * URL to this repository, starting from context path in the '/abc/def' form.
     */
    public String getUrl() {
        return Stapler.getCurrentRequest().findAncestor(this).getUrl();
    }
}
