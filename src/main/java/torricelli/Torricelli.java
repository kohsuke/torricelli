package torricelli;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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
     * List up all the repositories in the root
     */
    public List<Repository> listRepositories() throws IOException {
        File[] repos = home.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return new File(f, ".hg").exists();
            }
        });
        if(repos==null) return Collections.emptyList();

        List<Repository> r = new ArrayList<Repository>();
        for (File repo : repos) {
            r.add(getRepository(repo.getName()));
        }

        return r;
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

        // TODO: monitor output
        HgInvoker hgi = new HgInvoker(repoHome,"init");
        int r = hgi.launch().waitFor();
        if(r!=0) {
            sendError("hg init failed: "+r);
            return;
        }

        rsp.sendRedirect(name);
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
        // TODO: monitor output
        HgInvoker hgi = new HgInvoker(home,"clone","--quiet",src,name);
        int r = hgi.launch().waitFor();
        if(r!=0) {
            sendError("hg clone failed: "+r);
            return;
        }

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
    private boolean checkName(String name) throws IOException, ServletException {
        Repository repo = getRepository(name);
        if(repo!=null) {
            sendError("Repository "+ name +" already exists");
            return false;
        }

        if(name==null || name.length()==0) {
            sendError("No name given");
            return false;
        }

        for( int i=0; i<name.length(); i++ ) {
            char ch = name.charAt(i);
            if(Character.isISOControl(ch)) {
                sendError("Control character is not allowed");
                return false;
            }
            if("?*/\\%!@#$^&|<>[]:;".indexOf(ch)!=-1) {
                sendError("Unsafe character '"+ch+"' is not allowed");
                return false;
            }
        }

        // looks good
        return true;
    }

    private void sendError(String msg) throws IOException, ServletException {
        StaplerRequest req = Stapler.getCurrentRequest();
        StaplerResponse rsp = Stapler.getCurrentResponse();
        req.setAttribute("text",msg);
        rsp.forward(this,"error",req);
    }

    public HgServeRunner getRunner() {
        // TODO: dispose every 100 requests or so
        return runner;
    }
}
