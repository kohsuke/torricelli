package torricelli;

import org.kohsuke.scotland.dir.DirectoryModel;

import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public class DirectoryModelImpl extends DirectoryModel<Dir> {
    public String getName(Dir node) {
        return node.getName();
    }

    public String getUrl(Dir node) {
        return node.getName();
    }

    public Collection<Dir> getChildren(Dir parent) {
        return parent.getChildren().values();
    }

    public static final DirectoryModel<Dir> INSTANCE = new DirectoryModelImpl();
}
