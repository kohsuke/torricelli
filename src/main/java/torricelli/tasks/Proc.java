package torricelli.tasks;

import torricelli.util.StreamCopyThread;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Process launched from Torricelli.
 *
 * @author Kohsuke Kawaguchi
 */
public final class Proc {
    private final Process proc;
    private final StreamCopyThread copier;

    public Proc(Process proc, OutputStream out) throws IOException {
        this.proc = proc;
        proc.getOutputStream().close();
        this.copier = new StreamCopyThread(proc+" stdout",proc.getInputStream(),out);
        copier.start();
    }

    /**
     * Waits for the completion of the process.
     */
    public int join() throws InterruptedException, IOException {
        try {
            int r = proc.waitFor();
            copier.join();
            return r;
        } catch (InterruptedException e) {
            // aborting. kill the process
            proc.destroy();
            throw e;
        }
    }
}
