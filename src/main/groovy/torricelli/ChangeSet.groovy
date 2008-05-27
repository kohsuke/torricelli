package torricelli
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


    public Dir dirTree() {
        Dir root = new Dir(name:"/")

        parent.parse("/dirtree/"+key).dir.each {
            Dir d = root;
            it.@name.split('/').each { token ->
                d = d.child(token)
            }
            d.hasFiles = true
        }
        return root
    }

}

class Dir {
    /**
     * Parent directory or null if this is the root
     */
    /*final*/ Dir parent;
    /**
     * Directory name.
     */
    /*final*/ String name;
    /**
     * Child directories keyed by their names.
     */
    def children = new TreeMap<String,Dir>();
    /**
     * Does this directory have any files?
     */
    boolean hasFiles;

    Dir child(String name) {
        Dir child = children.get(name)
        if(child==null)
            children.put(name,child=new Dir(parent:this,name:name))
        return child;
    }

    /**
     * If this directory should be collapsed while rendering,
     * return the sole child node. Otherwise null
     */
    Dir collapse() {
        return !hasFiles && children.size()==1 ? children.values().asList()[0] : null;
    }

    String getPath() {
        return parent==null ? "." : parent.path+'/'+name;
    }
}
