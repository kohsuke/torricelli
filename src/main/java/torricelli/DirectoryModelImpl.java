package torricelli;

import org.kohsuke.scotland.dir.DirectoryModel;
import org.kohsuke.stapler.StaplerResponse;

import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public class DirectoryModelImpl extends DirectoryModel<Dir> {
    public DirectoryModelImpl() {
        super(".", "*directoryModel*");
    }

    public String getName(Dir node) {
        return node.getName();
    }

    public String getUrl(Dir node) {
        return node.getName();
    }

    public Collection<Dir> getChildren(Dir parent) {
        return parent.getChildren().values();
    }

    public void doAjax(StaplerResponse rsp) {
        rsp.setStatus(200);
    }
}
