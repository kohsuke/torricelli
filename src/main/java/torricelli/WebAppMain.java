package torricelli;

import org.jvnet.localizer.LocaleProvider;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Web application entry point.
 *
 * @author Kohsuke Kawaguchi
 */
// TODO: define extensible entry point inside Stapler
public class WebAppMain implements ServletContextListener {
    private static final String APP = "app";

    /**
     * Creates the sole instance of {@link Torricelli} and register it to the {@link ServletContext}.
     */
    public void contextInitialized(ServletContextEvent event) {
        try {
            final ServletContext context = event.getServletContext();

            // use the current request to determine the language
            // TODO: reuse
            LocaleProvider.setProvider(new LocaleProvider() {
                public Locale get() {
                    Locale locale=null;
                    StaplerRequest req = Stapler.getCurrentRequest();
                    if(req!=null)
                        locale = req.getLocale();
                    if(locale==null)
                        locale = Locale.getDefault();
                    return locale;
                }
            });

            final File home = getHomeDir(event).getAbsoluteFile();
            home.mkdirs();
            System.out.println("Torricelli home directory: "+home);

            // check that home exists (as mkdirs could have failed silently), otherwise throw a meaningful error
            if (!home.exists())
                throw new Error("No such home directory: "+home);

            context.setAttribute(APP,new Torricelli(home,context));
        } catch (Error e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Torricelli",e);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize Torricelli",e);
            throw e;
        }
    }

    /**
     * Determines the home directory for Hudson.
     *
     * People makes configuration mistakes, so we are trying to be nice
     * with those by doing {@link String#trim()}.
     */
    private File getHomeDir(ServletContextEvent event) {
        // check JNDI for the home directory first
        try {
            InitialContext iniCtxt = new InitialContext();
            Context env = (Context) iniCtxt.lookup("java:comp/env");
            String value = (String) env.lookup("TORRICELLI_HOME");
            if(value!=null && value.trim().length()>0)
                return new File(value.trim());
            // look at one more place. See HUDSON-1314
            value = (String) iniCtxt.lookup("TORRICELLI_HOME");
            if(value!=null && value.trim().length()>0)
                return new File(value.trim());
        } catch (NamingException e) {
            // ignore
        }

        // finally check the system property
        String sysProp = System.getProperty("TORRICELLI_HOME");
        if(sysProp!=null)
            return new File(sysProp.trim());

        // look at the env var next
        String env = System.getenv("TORRICELLI_HOME");
        if(env!=null)
            return new File(env.trim()).getAbsoluteFile();

        // otherwise pick a place by ourselves
        return new File(new File(System.getProperty("user.home")),".torricelli");
    }

    public void contextDestroyed(ServletContextEvent event) {
        Torricelli instance = (Torricelli)event.getServletContext().getAttribute(APP);
        if(instance!=null)
            instance.cleanUp();
    }

    private static final Logger LOGGER = Logger.getLogger(WebAppMain.class.getName());
}
