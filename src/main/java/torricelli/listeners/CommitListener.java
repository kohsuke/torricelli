package torricelli.listeners;

import org.kohsuke.scotland.extensibility.Describable;
import torricelli.Repository;

/**
 * Extension point.
 * 
 * @author Kohsuke Kawaguchi
 */
public abstract class CommitListener implements Describable {
    /**
     * Notifies that there was some commits to this repository.
     */
    public abstract void onChanged(Repository r);
}
