package torricelli;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import torricelli.util.StreamCopyThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.Map.Entry;

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
        hgServe = new HgInvoker(root.home,"serve",
                "-a","127.0.0.1",
                "-p",port,
                "--webdir-conf","hgweb.conf",
                "--config","web.push_ssl=false",
                "--config","web.allow_push=*"
                ).launch();
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

    private static final Logger LOGGER = Logger.getLogger(HgServeRunner.class.getName());
}
