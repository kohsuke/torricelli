package torricelli

import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */
class ChangeSet {
    public final Repository parent;

    /**
     * Either hexadecimal node ID or the revision number.
     * This uniquely identifies a change set.
     */
    public final String key;

    public ChangeSet(parent,key) {
        this.parent = parent;
        this.key = key;
    }


    public Dir manifest() {
        Dir root = new Dir(repository:parent, name:"/")

        parent.parse("/dirtree/"+key).dir.each {
            Dir d = root;
            it.@name.split('/').each { token ->
                d = d.child(token)
            }
            d.hasFiles = true
        }
        return root
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) {
        rsp.forward(manifest(),req.getRestOfPath(),req);
    }

}
