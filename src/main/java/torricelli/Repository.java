package torricelli;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.apache.commons.io.IOUtils;
import torricelli.util.StreamCopyThread;

import javax.servlet.ServletInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    /**
     * "hg serve" process, or null if none.
     */
    private Process hgServe;
    /**
     * TCP/IP port where "hg serve" is listening.
     */
    private int port;
    /**
     * Consumes the output stream from "hg serve".
     */
    private StreamCopyThread hgServeDrainer;

    public Repository(File home) throws IOException {
        this.name = home.getName();
        this.home = home;

        start();
    }

    /**
     * Starts "hg serve" process.
     */
    public synchronized void start() throws IOException {
        port = allocatePort();
        hgServe = new HgInvoker(home,"serve","-a","127.0.0.1","-p",port).launch();
        hgServeDrainer = new StreamCopyThread("drainer for "+name,hgServe.getInputStream(),System.out);
        hgServeDrainer.start();
    }

    /**
     * Assigns a TCP/IP port.
     *
     * This is not truly reliable, as someone could snatch
     * the port number after we close it.
     */
    private int allocatePort() throws IOException {
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();
        return port;
    }

    /**
     * Delegate the processing to "hg serv".
     */
    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException {

        StringBuilder buf = new StringBuilder("http://localhost:");
        buf.append(port);
        buf.append(req.getRestOfPath());

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
}
