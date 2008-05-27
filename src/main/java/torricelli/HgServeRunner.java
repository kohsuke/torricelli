package torricelli;

import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.xml.sax.SAXException;
import torricelli.util.StreamCopyThread;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

import groovy.util.Node;
import groovy.util.XmlParser;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Baby-sits "hg serve" process.
 *
 * @author Kohsuke Kawaguchi
 */
public class HgServeRunner {
    /**
     * "hg serve" process, or null if none.
     */
    private final Process hgServe;
    /**
     * TCP/IP port where "hg serve" is listening.
     */
    private final int port;
    /**
     * Consumes the output stream from "hg serve".
     */
    private final StreamCopyThread hgServeDrainer;

    /**
     * Starts "hg serve" process.
     */
    public HgServeRunner(Torricelli root) throws IOException {
        FileUtils.writeLines(new File(root.home,"hgweb.conf"), Arrays.asList(
            "[collections]",
            "/=."
        ));

        port = allocatePort();
        // we'll handle authentication ourselves, so have "hg serve" accept
        // any push
        HgInvoker inv = new HgInvoker(root.home, "serve",
                "-a", "127.0.0.1",
                "-p", port,
                "--webdir-conf", "hgweb.conf",
                "--config", "web.push_ssl=false",
                "--config", "web.allow_push=*",
                "--config", "extensions.hgext.hgserveExt="+root.context.getRealPath("/WEB-INF/hgserveExt.py")
        );
        if(Torricelli.NEW)
            inv.arg("--templates", root.context.getRealPath("/WEB-INF/templates"));
        hgServe = inv.launch();
        LOGGER.info("Started 'hg serve' on port "+port);
        hgServeDrainer = new StreamCopyThread("drainer for hg serve",hgServe.getInputStream(),System.out);
        hgServeDrainer.start();
    }

    /**
     * Assigns a TCP/IP port.
     *
     * This is not truly reliable, as someone could snatch
     * the port number after we close it.
     */
    private static int allocatePort() throws IOException {
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();
        return port;
    }

    public void proxy(StaplerRequest req, StaplerResponse rsp) throws IOException {
        StringBuilder buf = new StringBuilder("http://localhost:");
        buf.append(port);
        buf.append(req.getServletPath());

        String qs = req.getQueryString();
        if(qs!=null) buf.append('?').append(qs);

        URL url = new URL(buf.toString());
        rsp.reverseProxyTo(url,req);
    }

    /**
     * Sends the request to the backend "hg serve" and parses the result into
     * the format suitable for processing by Groovy.
     */
    public Node parse(String relative) throws SAXException, ParserConfigurationException, IOException {
        return new XmlParser().parse("http://localhost:"+port+'/'+relative);
    }

    private static final Logger LOGGER = Logger.getLogger(HgServeRunner.class.getName());
}
