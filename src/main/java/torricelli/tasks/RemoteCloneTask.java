package torricelli.tasks;

import torricelli.HgInvoker;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Task that performs a long clone operation.
 * 
 * @author Kohsuke Kawaguchi
 */
public class RemoteCloneTask extends TaskThread {
    private final String url;
    private final File local;

    public RemoteCloneTask(String url, File local) throws IOException {
        super("Cloning "+url+" to "+local.getName());
        this.url = url;
        this.local = local;
    }

    protected void execute(PrintStream log) throws IOException, InterruptedException {
        HgInvoker hgi = new HgInvoker(local,"init");
        int r = hgi.launch(log).join();
        if(r !=0) {
            log.println("hg init failed: "+r);
            throw new Failure();
        }

        hgi = new HgInvoker(local,"pull","-vyu",url);
        r = hgi.launch(log).join();
        if(r !=0) {
            log.println("hg update failed: "+r);
            throw new Failure();
        }
    }

    @Override
    public boolean isProminent() {
        return true;
    }
}
