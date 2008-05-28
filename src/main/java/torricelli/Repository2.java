package torricelli;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class Repository2 extends Repository {
    public Repository2(File home) throws IOException {
        super(home);
    }

    @Override
    public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        String qs = req.getQueryString();
        if(qs!=null && qs.startsWith("cmd=")) {
            // Mercurial commands. Forward to the backend
            getRunner().proxy(req, rsp);
        } else {
            TaskThread t = task;
            if(t!=null && t.isProminent()) {
                req.getView(this,"executingProminentTask").forward(req,rsp);
            } else {
                req.getView(this,"_index").forward(req,rsp);
            }
        }
    }

    public ChangeSet getRev(String id) {
        return new ChangeSet(this,id);
    }
}
