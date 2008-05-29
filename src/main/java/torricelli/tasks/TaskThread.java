package torricelli.tasks;

import org.kohsuke.stapler.framework.io.LargeText;
import org.kohsuke.stapler.StaplerResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import torricelli.Torricelli;
import torricelli.Repository;

/**
 * Asynchronous activity that takes some time to execute.
 * @author Kohsuke Kawaguchi
 */
public abstract class TaskThread extends Thread {
    public final Repository owner;
    private final File logFile;

    /**
     * If running, null. If success, true, and if failed, false.
     */
    private volatile Boolean result;

    public TaskThread(Repository owner, String name) throws IOException {
        super(name);
        this.owner = owner;
        this.logFile =createLogFile();

    }

    private File createLogFile() throws IOException {
        File tasksHome = new File(Torricelli.INSTANCE.home,"tasks");
        tasksHome.mkdirs();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        Date dt = new Date();

        while(true) {
            File logFile = new File(tasksHome,df.format(dt)+".log");
            if(logFile.createNewFile()) return logFile;

            dt.setTime(dt.getTime()+1);
        }
    }

    /**
     * Returns true if the execution of this task should be
     * displayed in the repository top page.
     */
    public boolean isProminent() {
        return false;
    }

    /**
     * Exposes the log file data.
     */
    public LargeText getLog() {
        return new LargeText(logFile, isDone());
    }

    /**
     * Is the task completed?
     */
    public boolean isDone() {
        return !isAlive();
    }

    /**
     * Did the task completed successfully?
     */
    public boolean isSuccess() {
        return isDone() && result;
    }

    /**
     * Did the task failed?
     */
    public boolean isFailed() {
        return isDone() && !result;
    }

    public final void run() {
        PrintStream ps;
        
        try {
            ps = new PrintStream(new FileOutputStream(logFile),true);
        } catch (IOException e) {
            throw new Error(e);
        }

        try {
            execute(ps);
            result = true;
            ps.println("SUCCESS");
        } catch(Failure e) {
            result = false;
            ps.println("FAILED");
        } catch(InterruptedException e) {
            result = false;
            ps.println("ABORTED");
        } catch(Throwable t) {
            result = false;
            t.printStackTrace(ps);
        } finally {
            ps.close();
        }
    }

    /**
     * Clear the task from {@link Repository#task}.
     */
    public void doClear(StaplerResponse rsp) throws IOException {
        if(isAlive())
            throw new IllegalStateException();
        if(owner.getTask()==this)
            owner.clearTask();
        rsp.sendRedirect("..");
    }

    /**
     * This is where the actual task is executed asynchronously.
     *
     * <p>
     * Return normally and the task is considered successful.
     *
     * @throws InterruptedException
     *      If the thread is interrupted, that is a sign that the task
     *      was aborted. Just propagate that exception without catching it.
     * @throws Failure
     *      If tje tasl detected an error, printed the datails, and just
     *      wants to abort the {@link #execute(PrintStream)} method right there,
     *      throw this exception, and no stack trace will be printed.
     */
    protected abstract void execute(PrintStream log) throws Throwable;

    public final class Failure extends RuntimeException {}
}
