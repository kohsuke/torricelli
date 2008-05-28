package torricelli.tasks;

import torricelli.HgInvoker;
import torricelli.tasks.TaskThread;

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
        HgInvoker hgi = new HgInvoker(local,"clone","-y",url,".");
        int r = hgi.launch(log).join();
        if(r !=0) {
            log.println("hg clone failed: "+r);
            throw new Failure();
        }
    }
}
