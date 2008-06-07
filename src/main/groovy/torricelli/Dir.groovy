package torricelli;

import org.kohsuke.stapler.WebMethod
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse

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
     * Repository.
     */
    /*final*/ Repository repository;
    /**
     * Child directories keyed by their names.
     */
    Map<String,Dir> children = new TreeMap<String,Dir>();
    /**
     * Does this directory have any files?
     */
    boolean hasFiles;

    Dir child(String name) {
        Dir child = children.get(name)
        if(child==null)
            children.put(name,child=new Dir(repository:repository, parent:this, name:name))
        return child;
    }

    /**
     * If this directory should be collapsed while rendering,
     * return the sole child node. Otherwise null
     */
    Dir collapse() {
        return !hasFiles && children.size()==1 ? children.values().asList()[0] : null;
    }

    String getPath(base) {
        return parent==null ? base : parent.getPath(base)+'/'+name;
    }

    @WebMethod(name=["*directoryModel*"])
    def getDirectoryModel() {
        return new DirectoryModelImpl(this);
    }

    def listDirs(/*DirectoryTags*/ tags) {
        tags.list(getDirectoryModel(),this);
    }

    def parseFileSummary() {
        return repository.parse("/filesummary/?path="+getPath('/'))
    }

    def parseChangelog() {
        // TODO: changelog limited to sub directories
        return repository.parse("/changelog/"+getPath(""))
    }

    /**
     * Map subdirectories to URL.
     */
    Dir getDynamic(String token, StaplerRequest req, StaplerResponse rsp) {
        return child(token);
    }
}
