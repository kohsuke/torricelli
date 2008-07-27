package torricelli;

import org.kohsuke.scotland.xstream.XmlFile;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.adjunct.AdjunctManager;
import org.kohsuke.stapler.framework.io.LargeText;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import torricelli.listeners.HudsonNotifier;

/**
 * The root object of the web application.
 *
 * @author Kohsuke Kawaguchi
 */
public class Torricelli extends AbstractModelObject implements StaplerProxy {
    public transient final File home;
    public transient final ServletContext context;
    public transient final AdjunctManager adjuncts;

    public static Torricelli INSTANCE;

    /**
     * Group list.
     */
    private transient final ConcurrentHashMap<String,Group> groups = new ConcurrentHashMap<String,Group>();

    private transient volatile HgServeRunner runner;

    /**
     * Unique identifier of this server.
     *
     * <p>
     * This allows external systems to identify this server.
     *
     * @see #serverIdString
     */
    public final UUID serverId = UUID.randomUUID();

    /**
     * The cached value of {@code serverId.toString()}
     */
    public final String serverIdString;

    public Torricelli(File home, ServletContext context) throws IOException {
        INSTANCE = this;

        this.home = home;
        this.context = context;
        this.runner = new HgServeRunner(this);
        this.adjuncts = new AdjunctManager(context,getClass().getClassLoader(),"/_");

        XmlFile xml = getXmlFile();
        if(xml.exists())
            xml.unmarshal(this);

        this.serverIdString = serverId.toString();

        // load notifiers
        HudsonNotifier.DESCRIPTOR.getDisplayName();
    }

    private XmlFile getXmlFile() {
        return new XmlFile(new File(home,"torricelli.xml"));
    }

    /**
     * Serves adjuncts from "/_/..." URL.
     */
    public AdjunctManager get_() {
        return adjuncts;
    }

    public void cleanUp() {
        // no-op
    }

    public Object getDynamic(String name, StaplerRequest req, StaplerResponse rsp) throws IOException {
        return getGroup(name);
    }

    public List<Group> listGroups() throws IOException {
        File[] groups = home.listFiles(new FileFilter() {
            public boolean accept(File f) {
                return new File(f, "group.xml").exists();
            }
        });
        if (groups == null) return Collections.emptyList();

        List<Group> r = new ArrayList<Group>();
        for (File group : groups)
            r.add(getGroup(group.getName()));

        return r;
    }

    /**
     * Gets the group.
     */
    public Group getGroup(String name) throws IOException {
        Group g = groups.get(name);
        if(g==null) {
            File groupXml = new File(home,name+"/group.xml");
            if(!groupXml.exists())
                return null;

            g = new Group(name);
            Group prev = groups.putIfAbsent(name, g);
            if(prev!=null)  g=prev;
        } else {
            if(!g.home.exists()) {
                // no longer a valid repository. files might have been removed on the file system
                groups.remove(name);
                g = null;
            }
        }
        return g;
    }

    public LargeText getLogFile() {
        return new LargeText(new File("log"),false);
    }

    /**
     * Creates a new group.
     */
    public void doCreate(StaplerResponse rsp, @QueryParameter("name") String name) throws IOException, InterruptedException, ServletException {
        if (!checkName(name)) return;

        if(getGroup(name)!=null) {
            sendError("Group {0} exists",name);
            return;
        }

        new Group(name).save();
        save();
        rsp.sendRedirect(name);
    }

    /**
     * Make sure that the name is usable as the repository name.
     */
    protected boolean checkName(String name) throws IOException, ServletException {
        Group g = getGroup(name);
        if(g!=null) {
            sendError("Group "+ name +" already exists");
            return false;
        }

        return super.checkName(name);
    }

    public HgServeRunner getRunner() {
        // TODO: dispose every 100 requests or so
        return runner;
    }

    public void save() throws IOException {
        getXmlFile().write(this);
    }

    /**
     * To isolate the front-ending stuff.
     */
    public static boolean NEW = Boolean.getBoolean("new");

    /**
     * Date format. Notice that {@link SimpleDateFormat} is thread unsafe.
     */
    private static final ThreadLocal<SimpleDateFormat> HTTP_DATE_FORMAT =
        new ThreadLocal<SimpleDateFormat>() {
            protected SimpleDateFormat initialValue() {
                // RFC1945 section 3.3 Date/Time Formats states that timezones must be in GMT
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                return format;
            }
        };

    /**
     * Parses the RFC 822 date format used by Mercurial.
     */
    public static Date parseDate(String s) throws ParseException {
        return HTTP_DATE_FORMAT.get().parse(s);
    }

    /**
     * This hook is to attach server ID to all the HTTP responses.
     */
    public Object getTarget() {
        Stapler.getCurrentResponse().addHeader("X-Torricelli-Id",serverIdString);
        return this;
    }
}
