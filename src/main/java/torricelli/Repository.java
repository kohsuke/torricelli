package torricelli;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.IOException;

/**
 * Mercurial repository.
 *
 * @author Kohsuke Kawaguchi
 */
public class Repository {
    public final String name;

    /**
     * Root directory of the repository.
     */
    public final File home;


    public Repository(File home) throws IOException {
        this.name = home.getName();
        this.home = home;
    }

    /**
     * Delegate the processing to "hg serv".
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException {
        Torricelli.INSTANCE.getRunner().proxy(req, rsp);
    }

}
