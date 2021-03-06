package torricelli

import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse
import java.text.DateFormat

/**
 * Represents a change set.
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

    // the information below is parsed lazily
    /**
     * Hexadecimal unique node number.
     */
    String node;
    /**
     * Revision number. Only unique within a repository.
     */
    String rev;

    String author;

    private Date date;

    String summary;
    String description;

    /**
     * Parent node names.
     */
    List<String> parents;
    /**
     * Child node names.
     */
    List<String> children;
    /**
     * File path names changed.
     */
    List<String> files;

    List<String> tags;

    List<String> diffLines;

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

    public void parse() {
        if(summary!=null)   return; // already parsed

        def dom = parent.parse("/changeset/"+key);
        node = dom.@node;
        rev = dom.@rev;
        author = dom.@author;
        date = Torricelli.parseDate(dom.@date);
        summary = dom.summary.text();
        description = dom.description.text();
        parents = dom.parent*.@node;
        children = dom.child*.@node;
        files = dom.file*.text();
        tags = dom.tag*.text();
        diffLines = dom.diff.line*.text();
    }

    public String getDateString() {
        return DateFormat.getInstance().format(date);
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) {
        rsp.forward(manifest(),req.getRestOfPath(),req);
    }

    public void doPatch(StaplerResponse rsp) {
        parse();

        rsp.contentType = "text/plain; charset=UTF-8";
        def w = rsp.writer;
        diffLines.each { w.print(it) }
    }

}
