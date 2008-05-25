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
        hgServe = new HgInvoker(root.home,"serve","-a","127.0.0.1","-p",port,"--webdir-conf","hgweb.conf").launch();
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

        HttpURLConnection con = (HttpURLConnection) new URL(buf.toString()).openConnection();
        con.setDoOutput(true);

        // copy the request body
        con.setRequestMethod(req.getMethod());
        // TODO: how to set request headers?
        copyAndClose(req.getInputStream(), con.getOutputStream());

        // copy the response
        rsp.setStatus(con.getResponseCode(),con.getResponseMessage());
        Map<String,List<String>> rspHeaders = con.getHeaderFields();
        for (Entry<String, List<String>> header : rspHeaders.entrySet()) {
            if(header.getKey()==null)   continue;   // response line
            for (String value : header.getValue()) {
                rsp.addHeader(header.getKey(),value);
            }
        }

        copyAndClose(con.getInputStream(), rsp.getOutputStream());
    }

    private void copyAndClose(InputStream in, OutputStream out) throws IOException {
        IOUtils.copy(in, out);
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
    }

    private static final Logger LOGGER = Logger.getLogger(HgServeRunner.class.getName());
}
