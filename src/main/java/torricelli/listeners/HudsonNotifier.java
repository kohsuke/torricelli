package torricelli.listeners;

import org.kohsuke.scotland.extensibility.Descriptor;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.Stapler;
import torricelli.Repository;
import torricelli.Torricelli;
import torricelli.TorricelliDescriptor;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Notifies Hudson that there was a change in the repository.
 *
 * TODO: move to a plugin
 *
 * @author Kohsuke Kawaguchi
 */
public class HudsonNotifier extends CommitListener {

    private final URL hudson;

    @DataBoundConstructor
    public HudsonNotifier(URL hudson) {
        this.hudson = hudson;
    }

    public void onChanged(Repository r) {
        try {
            Ancestor a = Stapler.getCurrentRequest().findAncestor(r);
            URL url = new URL(hudson, "plugin/mercurial/change?serverId=" +
                    Torricelli.INSTANCE.serverIdString+"&url="+a.getUrl()+'/');
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.connect();
            con.getOutputStream().close();
            if(con.getResponseCode()!=200)
            throw new IOException("Notification to "+hudson+" failed with "+con.getResponseCode()+" "+con.getResponseMessage());
        } catch (IOException e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
    }

    public Descriptor getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends TorricelliDescriptor<HudsonNotifier> {
        private DescriptorImpl() {
            super(HudsonNotifier.class);
        }

        public String getDisplayName() {
            return "Notify Hudson";
        }
    }

    private static final Logger LOGGER = Logger.getLogger(HudsonNotifier.class.getName());
}
