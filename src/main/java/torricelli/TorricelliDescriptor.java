package torricelli;

import org.kohsuke.scotland.extensibility.Describable;
import org.kohsuke.scotland.extensibility.Descriptor;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class TorricelliDescriptor<T extends Describable> extends Descriptor<T> {
    protected TorricelliDescriptor(Class<? extends T> clazz) {
        super(Torricelli.INSTANCE.home, clazz);
    }
}
