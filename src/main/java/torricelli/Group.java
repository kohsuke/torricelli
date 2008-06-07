package torricelli;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.kohsuke.graphviz.Arrow;
import org.kohsuke.graphviz.Attribute;
import static org.kohsuke.graphviz.Attribute.ARROWHEAD;
import static org.kohsuke.graphviz.Attribute.ARROWTAIL;
import org.kohsuke.graphviz.Graph;
import org.kohsuke.graphviz.Node;
import org.kohsuke.graphviz.Style;
import org.kohsuke.graphviz.Edge;
import org.kohsuke.scotland.xstream.XmlFile;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.jelly.groovy.JellyBuilder;
import org.xml.sax.SAXException;
import torricelli.tasks.RemoteCloneTask;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inter-related group of {@link Repository}s.
 *
 * <p>
 * Persisted with {@link Torricelli}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Group extends AbstractModelObject {
    public transient final String name;
    public transient final File home;

    private transient final UUID uuid = UUID.randomUUID();

    /**
     * Repository list.
     */
    private transient final ConcurrentHashMap<String,Repository> repositories = new ConcurrentHashMap<String,Repository>();

    public Group(String name) throws IOException {
        this.name = name;
        this.home = new File(Torricelli.INSTANCE.home,name);
        XmlFile xml = getXmlFile();
        if(xml.exists())
            xml.unmarshal(this);
    }

    public XmlFile getXmlFile() {
        return new XmlFile(new File(home,"group.xml"));
    }

    /**
     * Gets the repository.
     */
    public Repository getRepository(String name) throws IOException {
        Repository r = repositories.get(name);
        if(r==null) {
            File repoDir = new File(home,name);
            if(!repoDir.exists())
                return null;

            r = Torricelli.NEW ? new Repository2(this,repoDir) : new Repository(this,repoDir);
            Repository prev = repositories.putIfAbsent(name, r);
            if(prev!=null)  r=prev;
        } else {
            if(!r.home.exists()) {
                // no longer a valid repository. files might have been removed on the file system
                repositories.remove(name);
                r = null;
            }
        }
        return r;
    }

    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) throws IOException {
        return getRepository(name);
    }

    /**
     * List up all the repositories in the root
     */
    public List<Repository> listRepositories() throws IOException {
        File[] repos = home.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return new File(f, ".hg").exists();
            }
        });
        if (repos == null) return Collections.emptyList();

        List<Repository> r = new ArrayList<Repository>();
        for (File repo : repos) {
            r.add(getRepository(repo.getName()));
        }

        return r;
    }

    public void save() throws IOException {
        home.mkdirs();
        getXmlFile().write(this);
    }

    /**
     * Creates a new repository.
     */
    public void doCreate(StaplerResponse rsp, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        // create a new mercurial repository
        File repoHome = new File(home,name);
        repoHome.mkdirs();
        if(!repoHome.exists()) {
            sendError("Failed to create "+repoHome);
            return;
        }

        HgInvoker hgi = new HgInvoker(repoHome,"init");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = hgi.launch(baos).join();
        if(r!=0) {
            sendError("hg init failed: "+r+"\n<PRE>"+baos+"</PRE>");
            return;
        }

        rsp.sendRedirect2(name);
    }
    
    /**
     * Clones a repository to a new one.
     */
    public void doClone(StaplerResponse rsp, @QueryParameter("src") String src, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        Repository srcRepo = getRepository(src);
        if(srcRepo==null) {
            sendError("No such repository: "+src);
            return;
        }

        // create a new mercurial repository
        HgInvoker hgi = new HgInvoker(home,"clone","--quiet",src,name);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int r = hgi.launch(baos).join();
        if(r!=0) {
            sendError("hg clone failed: "+r+"\n<PRE>"+baos+"</PRE>");
            return;
        }

        // assume the source to be the upstream
        Repository dst = getRepository(name);
        dst.setUpstream(srcRepo);
        dst.save();

        rsp.sendRedirect(name);
    }

    /**
     * Clones a remote repository to a new one.
     */
    public void doRemoteClone(StaplerResponse rsp, @QueryParameter("src") String src, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        File newHome = new File(home,name);
        newHome.mkdirs();
        Repository r = getRepository(name);
        r.startTask(new RemoteCloneTask(r,src,newHome));

        rsp.sendRedirect(name);
    }

    /**
     * Deletes a repository.
     */
    public void doDoDelete(StaplerResponse rsp, @QueryParameter("src") String src) throws IOException, InterruptedException, ServletException {
        Repository srcRepo = getRepository(src);
        if(srcRepo==null) {
            sendError("No such repository: "+src);
            return;
        }

        Delete del = new Delete();
        del.setProject(new Project());
        del.setDir(srcRepo.home);
        del.execute();

        rsp.sendRedirect(".");
    }

    /**
     * Generates the repository dependency graph.
     */
    public void doGraph(StaplerResponse rsp) throws IOException, InterruptedException {
        rsp.setContentType("image/gif");
        createRepositoryGraph().generateTo(Arrays.asList("dot","-Tgif"),rsp.getOutputStream());
    }

    public void generateClickableMap(JellyBuilder out) throws IOException, InterruptedException, SAXException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        createRepositoryGraph().generateTo(Arrays.asList("dot","-Tcmapx"),buf);
        out.raw(buf);
    }

    private Graph createRepositoryGraph() throws IOException {
        Graph graph = new Graph();
        graph.id(uuid.toString());
        //graph.attr(Attribute.RANKDIR, RankDir.LR);
        graph.attr("rankdir","LR");
        Style s = new Style();
        s.attr(Attribute.FONTNAME,"sans serif");
        s.attr(Attribute.FONTSIZE,10f);
        s.attr(Attribute.SHAPE, org.kohsuke.graphviz.Shape.NONE);
        graph.nodeWith(s);

        String pkgPng = Torricelli.INSTANCE.context.getRealPath("/img/48x48/package.png");
        Map<Repository,Node> nodes = new HashMap<Repository,Node>();

        List<Repository> repositories = listRepositories();
        for (Repository r : repositories) {
            String name = r.name;
            Node n = new Node();
            n.attr("html", "<TABLE BORDER=\"0\"><TR><TD><IMG SRC=\""+pkgPng+"\" /></TD></TR><TR><TD>"+name+"</TD></TR></TABLE>");
            n.attr(Attribute.URL, name);
            n.attr("tooltip",
                    r.getDescription()!=null ? r.getDescription() : name);
            graph.node(n);
            nodes.put(r,n);
        }

        graph.edgeWith(new Style()
                .attr(ARROWHEAD, Arrow.NONE)
                .attr(ARROWTAIL, Arrow.NORMAL));

        for (Repository r : repositories) {
            Repository up = r.getUpstream();
            if(up!=null) {
                Edge e = new Edge(nodes.get(up),nodes.get(r));
                r.getChangeDirection().decorate(e);
                graph.edge(e);
        }
        }
        return graph;
    }

    /**
     * Make sure that the name is usable as the repository name.
     */
    protected boolean checkName(String name) throws IOException, ServletException {
        Repository repo = getRepository(name);
        if(repo!=null) {
            sendError("Repository "+ name +" already exists");
            return false;
        }

        return super.checkName(name);
    }
}
