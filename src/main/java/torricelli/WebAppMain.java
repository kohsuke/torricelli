package torricelli;

import org.kohsuke.stapler.framework.AbstractWebAppMain;

/**
 * Web application entry point.
 *
 * @author Kohsuke Kawaguchi
 */
public class WebAppMain extends AbstractWebAppMain {
    public WebAppMain() {
        super(Torricelli.class);
    }

    protected String getApplicationName() {
        return "torricelli";
    }

    protected Object createApplication() throws Exception {
        return new Torricelli(home,context);
    }
}
