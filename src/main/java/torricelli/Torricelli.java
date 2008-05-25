package torricelli;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletContext;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The root object of the web application.
 *
 * @author Kohsuke Kawaguchi
 */
public class Torricelli {
    public final File home;
    public final ServletContext context;
    public static Torricelli INSTANCE;
    /**
     * Repository list.
     */
    private final ConcurrentHashMap<String,Repository> repositories = new ConcurrentHashMap<String,Repository>();

    private volatile HgServeRunner runner;

    public Torricelli(File home, ServletContext context) throws IOException {
        INSTANCE = this;

        this.home = home;
        this.context = context;
        this.runner = new HgServeRunner(this);
    }

    public void cleanUp() {
        // no-op
    }

    public Repository getDynamic(String name, StaplerRequest req, StaplerResponse rsp) throws IOException {
        return getRepository(name);
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

            r = new Repository(repoDir);
            Repository prev = repositories.putIfAbsent(name, r);
            if(prev!=null)  r=prev;
        }
        return r;
    }

    public void doCreate(StaplerResponse rsp, @QueryParameter("name") String name) throws IOException, InterruptedException {
        Repository repo = getRepository(name);
        if(repo!=null) {
            sendError("Repository "+ name +" already exists");
            return;
        }

        // create a new mercurial repository
        File repoHome = new File(home,name);
        repoHome.mkdirs();
        if(!repoHome.exists()) {
            sendError("Failed to create "+repoHome);
            return;
        }

        // TODO: monitor output
        HgInvoker hgi = new HgInvoker(repoHome,"init");
        int r = hgi.launch().waitFor();
        if(r!=0) {
            sendError("hg init failed: "+r);
            return;
        }

        rsp.sendRedirect(name);
    }

    private void sendError(String msg) throws IOException {
        Stapler.getCurrentResponse().sendError(SC_INTERNAL_SERVER_ERROR, msg);
    }

    public HgServeRunner getRunner() {
        // TODO: dispose every 100 requests or so
        return runner;
    }
}
