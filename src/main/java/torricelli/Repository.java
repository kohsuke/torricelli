package torricelli;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import groovy.util.XmlParser;
import groovy.util.Node;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Mercurial repository.
 *
 * @author Kohsuke Kawaguchi
 */
public class Repository {
    public final String name;

    /**
     * Root directory of the repository.
     */
    public final File home;


    public Repository(File home) throws IOException {
        this.name = home.getName();
        this.home = home;
    }

    /**
     * Delegate the processing to "hg serv".
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException {
        getRunner().proxy(req, rsp);
    }

    /**
     * Sends the request to the backend "hg serve" and parses the result into
     * the format suitable for processing by Groovy.
     */
    public Node parse(String relative) throws SAXException, ParserConfigurationException, IOException {
        return getRunner().parse('/'+name+relative);
    }

    private HgServeRunner getRunner() {
        return Torricelli.INSTANCE.getRunner();
    }
}
