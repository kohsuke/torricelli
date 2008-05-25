/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package torricelli;

import org.apache.commons.io.IOUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  CGI-invoking servlet for web applications, used to execute scripts which
 *  comply to the Common Gateway Interface (CGI) specification and are named
 *  in the path-info used to invoke this servlet.
 *
 * <p>
 * <i>Note: This code compiles and even works for simple CGI cases.
 *          Exhaustive testing has not been done.  Please consider it beta
 *          quality.  Feedback is appreciated to the author (see below).</i>
 * </p>
 * <p>
 *
 * <b>Example</b>:<br>
 * If an instance of this servlet was mapped (using
 *       <code>&lt;web-app&gt;/WEB-INF/web.xml</code>) to:
 * </p>
 * <p>
 * <code>
 * &lt;web-app&gt;/cgi-bin/*
 * </code>
 * </p>
 * <p>
 * then the following request:
 * </p>
 * <p>
 * <code>
 * http://localhost:8080/&lt;web-app&gt;/cgi-bin/dir1/script/pathinfo1
 * </code>
 * </p>
 * <p>
 * would result in the execution of the script
 * </p>
 * <p>
 * <code>
 * &lt;web-app-root&gt;/WEB-INF/cgi/dir1/script
 * </code>
 * </p>
 * <p>
 * with the script's <code>PATH_INFO</code> set to <code>/pathinfo1</code>.
 * </p>
 * <p>
 * Recommendation:  House all your CGI scripts under
 * <code>&lt;webapp&gt;/WEB-INF/cgi</code>.  This will ensure that you do not
 * accidentally expose your cgi scripts' code to the outside world and that
 * your cgis will be cleanly ensconced underneath the WEB-INF (i.e.,
 * non-content) area.
 * </p>
 * <p>
 * The default CGI location is mentioned above.  You have the flexibility to
 * put CGIs wherever you want, however:
 * </p>
 * <p>
 *   The CGI search path will start at
 *   webAppRootDir + File.separator + cgiPathPrefix
 *   (or webAppRootDir alone if cgiPathPrefix is
 *   null).
 * </p>
 * <p>
 *   cgiPathPrefix is defined by setting
 *   this servlet's cgiPathPrefix init parameter
 * </p>
 *
 * <p>
 *
 * <B>CGI Specification</B>:<br> derived from
 * <a href="http://cgi-spec.golux.com">http://cgi-spec.golux.com</a>.
 * A work-in-progress & expired Internet Draft.  Note no actual RFC describing
 * the CGI specification exists.  Where the behavior of this servlet differs
 * from the specification cited above, it is either documented here, a bug,
 * or an instance where the specification cited differs from Best
 * Community Practice (BCP).
 * Such instances should be well-documented here.  Please email the
 * <a href="mailto:tomcat-dev@jakarta.apache.org">Jakarta Tomcat group [tomcat-dev@jakarta.apache.org]</a>
 * with amendments.
 *
 * </p>
 * <p>
 *
 * <b>Canonical metavariables</b>:<br>
 * The CGI specification defines the following canonical metavariables:
 * <br>
 * [excerpt from CGI specification]
 * <PRE>
 *  AUTH_TYPE
 *  CONTENT_LENGTH
 *  CONTENT_TYPE
 *  GATEWAY_INTERFACE
 *  PATH_INFO
 *  PATH_TRANSLATED
 *  QUERY_STRING
 *  REMOTE_ADDR
 *  REMOTE_HOST
 *  REMOTE_IDENT
 *  REMOTE_USER
 *  REQUEST_METHOD
 *  SCRIPT_NAME
 *  SERVER_NAME
 *  SERVER_PORT
 *  SERVER_PROTOCOL
 *  SERVER_SOFTWARE
 * </PRE>
 * <p>
 * Metavariables with names beginning with the protocol name (<EM>e.g.</EM>,
 * "HTTP_ACCEPT") are also canonical in their description of request header
 * fields.  The number and meaning of these fields may change independently
 * of this specification.  (See also section 6.1.5 [of the CGI specification].)
 * </p>
 * [end excerpt]
 *
 * </p>
 * <h2> Implementation notes</h2>
 * <p>
 *
 * <b>standard input handling</b>: If your script accepts standard input,
 * then the client must start sending input within a certain timeout period,
 * otherwise the servlet will assume no input is coming and carry on running
 * the script.  The script's the standard input will be closed and handling of
 * any further input from the client is undefined.  Most likely it will be
 * ignored.  If this behavior becomes undesirable, then this servlet needs
 * to be enhanced to handle threading of the spawned process' stdin, stdout,
 * and stderr (which should not be too hard).
 * <br>
 * If you find your cgi scripts are timing out receiving input, you can set
 * the init parameter <code></code> of your webapps' cgi-handling servlet
 * to be
 * </p>
 * <p>
 *
 * <b>Metavariable Values</b>: According to the CGI specificion,
 * implementations may choose to represent both null or missing values in an
 * implementation-specific manner, but must define that manner.  This
 * implementation chooses to always define all required metavariables, but
 * set the value to "" for all metavariables whose value is either null or
 * undefined.  PATH_TRANSLATED is the sole exception to this rule, as per the
 * CGI Specification.
 *
 * </p>
 * <p>
 *
 * <b>NPH --  Non-parsed-header implementation</b>:  This implementation does
 * not support the CGI NPH concept, whereby server ensures that the data
 * supplied to the script are preceisely as supplied by the client and
 * unaltered by the server.
 * </p>
 * <p>
 * The function of a servlet container (including Tomcat) is specifically
 * designed to parse and possible alter CGI-specific variables, and as
 * such makes NPH functionality difficult to support.
 * </p>
 * <p>
 * The CGI specification states that compliant servers MAY support NPH output.
 * It does not state servers MUST support NPH output to be unconditionally
 * compliant.  Thus, this implementation maintains unconditional compliance
 * with the specification though NPH support is not present.
 * </p>
 * <p>
 *
 * The CGI specification is located at
 * <a href="http://cgi-spec.golux.com">http://cgi-spec.golux.com</a>.
 *
 * </p>
 * <p>
 * <h3>TODO:</h3>
 * <ul>
 * <li> Support for setting headers (for example, Location headers don't work)
 * <li> Support for collapsing multiple header lines (per RFC 2616)
 * <li> Ensure handling of POST method does not interfere with 2.3 Filters
 * <li> Refactor some debug code out of core
 * <li> Ensure header handling preserves encoding
 * <li> Possibly rewrite CGIRunner.run()?
 * <li> Possibly refactor CGIRunner and CGIEnvironment as non-inner classes?
 * <li> Document handling of cgi stdin when there is no stdin
 * <li> Revisit IOException handling in CGIRunner.run()
 * <li> Better documentation
 * <li> Confirm use of ServletInputStream.available() in CGIRunner.run() is
 *      not needed
 * <li> Make checking for "." and ".." in servlet & cgi PATH_INFO less
 *      draconian
 * <li> [add more to this TODO list]
 * </ul>
 * </p>
 *
 * @author Martin T Dengler [root@martindengler.com]
 * @author Amy Roh
 * @version $Revision: 543681 $, $Date: 2007-06-01 17:42:59 -0700 (Fri, 01 Jun 2007) $
 * @since Tomcat 4.0
 *
 */
public class CGI {
    /* some vars below copied from Craig R. McClanahan's InvokerServlet */

    /**
     *  The CGI search path will start at
     *    webAppRootDir + File.separator + cgiPathPrefix
     *    (or webAppRootDir alone if cgiPathPrefix is
     *    null)
     */
    private String cgiPathPrefix = null;

    /** the executable to use with the script */
    private String cgiExecutable = "perl";

    /** the encoding to use for parameters */
    private String parameterEncoding = System.getProperty("file.encoding",
                                                          "UTF-8");

    /** the shell environment variables to be passed to the CGI script */
    static Map<String,String> shellEnv = System.getenv();


    /**
     * Encapsulates the CGI environment and rules to derive
     * that environment from the servlet container and request information.
     *
     * <p>
     * </p>
     *
     * @version  $Revision: 543681 $, $Date: 2007-06-01 17:42:59 -0700 (Fri, 01 Jun 2007) $
     * @since    Tomcat 4.0
     *
     */
    protected class CGIEnvironment {


        /** context of the enclosing servlet */
        private ServletContext context = null;

        /** context path of enclosing servlet */
        private String contextPath = null;

        /** servlet URI of the enclosing servlet */
        private String servletPath = null;

        /** pathInfo for the current request */
        private String pathInfo = null;

        /** real file system directory of the enclosing servlet's web app */
        private String webAppRootDir = null;

        /** derived cgi environment */
        private Hashtable env = null;

        /** cgi command to be invoked */
        private String command = null;

        /** cgi command's desired working directory */
        private File workingDirectory = null;

        /** cgi command's command line parameters */
        private ArrayList<String> cmdLineParameters = new ArrayList<String>();

        /** whether or not this object is valid or not */
        private boolean valid = false;


        /**
         * Creates a CGIEnvironment and derives the necessary environment,
         * query parameters, working directory, cgi command, etc.
         *
         * @param  req       HttpServletRequest for information provided by
         *                   the Servlet API
         * @param  context   ServletContext for information provided by the
         *                   Servlet API
         *
         */
        public CGIEnvironment(HttpServletRequest req, ServletContext context) throws IOException {
            setupFromContext(context);
            setupFromRequest(req);

            this.valid = setCGIEnvironment(req);

            if (this.valid) {
                workingDirectory = new File(command.substring(0,
                      command.lastIndexOf(File.separator)));
            }

        }



        /**
         * Uses the ServletContext to set some CGI variables
         *
         * @param  context   ServletContext for information provided by the
         *                   Servlet API
         */
        protected void setupFromContext(ServletContext context) {
            this.context = context;
            this.webAppRootDir = context.getRealPath("/");
        }



        /**
         * Uses the HttpServletRequest to set most CGI variables
         *
         * @param  req   HttpServletRequest for information provided by
         *               the Servlet API
         * @throws UnsupportedEncodingException
         */
        protected void setupFromRequest(HttpServletRequest req)
                throws UnsupportedEncodingException {

            this.contextPath = req.getContextPath();
            this.servletPath = req.getServletPath();
            this.pathInfo = req.getPathInfo();
            // If getPathInfo() returns null, must be using extension mapping
            // In this case, pathInfo should be same as servletPath
            if (this.pathInfo == null) {
                this.pathInfo = this.servletPath;
            }

            // If the request method is GET, POST or HEAD and the query string
            // does not contain an unencoded "=" this is an indexed query.
            // The parsed query string becomes the command line parameters
            // for the cgi command.
            if (req.getMethod().equals("GET")
                || req.getMethod().equals("POST")
                || req.getMethod().equals("HEAD")) {
                String qs = req.getQueryString();
                if (qs != null && qs.indexOf("=") == -1) {
                    StringTokenizer qsTokens = new StringTokenizer(qs, "+");
                    while ( qsTokens.hasMoreTokens() ) {
                        cmdLineParameters.add(URLDecoder.decode(qsTokens.nextToken(),
                                              parameterEncoding));
                    }
                }
            }
        }


        /**
         * Resolves core information about the cgi script.
         *
         * <p>
         * Example URI:
         * <PRE> /servlet/cgigateway/dir1/realCGIscript/pathinfo1 </PRE>
         * <ul>
         * <LI><b>path</b> = $CATALINA_HOME/mywebapp/dir1/realCGIscript
         * <LI><b>scriptName</b> = /servlet/cgigateway/dir1/realCGIscript
         * <LI><b>cgiName</b> = /dir1/realCGIscript
         * <LI><b>name</b> = realCGIscript
         * </ul>
         * </p>
         * <p>
         * CGI search algorithm: search the real path below
         *    &lt;my-webapp-root&gt; and find the first non-directory in
         *    the getPathTranslated("/"), reading/searching from left-to-right.
         *</p>
         *<p>
         *   The CGI search path will start at
         *   webAppRootDir + File.separator + cgiPathPrefix
         *   (or webAppRootDir alone if cgiPathPrefix is
         *   null).
         *</p>
         *<p>
         *   cgiPathPrefix is defined by setting
         *   this servlet's cgiPathPrefix init parameter
         *
         *</p>
         *
         * @param pathInfo       String from HttpServletRequest.getPathInfo()
         * @param webAppRootDir  String from context.getRealPath("/")
         * @param contextPath    String as from
         *                       HttpServletRequest.getContextPath()
         * @param servletPath    String as from
         *                       HttpServletRequest.getServletPath()
         * @param cgiPathPrefix  subdirectory of webAppRootDir below which
         *                       the web app's CGIs may be stored; can be null.
         *                       The CGI search path will start at
         *                       webAppRootDir + File.separator + cgiPathPrefix
         *                       (or webAppRootDir alone if cgiPathPrefix is
         *                       null).  cgiPathPrefix is defined by setting
         *                       the servlet's cgiPathPrefix init parameter.
         *
         *
         * @return
         *      null if script was not found.
         *
         * @since Tomcat 4.0
         */
        protected CGIScript findCGI(String pathInfo, String webAppRootDir,
                                   String contextPath, String servletPath,
                                   String cgiPathPrefix) {
            String scriptname;
            String cginame = "";

            if ((webAppRootDir != null)
                && (webAppRootDir.lastIndexOf(File.separator) ==
                    (webAppRootDir.length() - 1))) {
                    //strip the trailing "/" from the webAppRootDir
                    webAppRootDir =
                    webAppRootDir.substring(0, (webAppRootDir.length() - 1));
            }

            if (cgiPathPrefix != null) {
                webAppRootDir = webAppRootDir + File.separator
                    + cgiPathPrefix;
            }

            LOGGER.fine("findCGI: path=" + pathInfo + ", " + webAppRootDir);

            File currentLocation = new File(webAppRootDir);
            StringTokenizer dirWalker =
            new StringTokenizer(pathInfo, "/");
            LOGGER.finer("findCGI: currentLoc=" + currentLocation);
            while (!currentLocation.isFile() && dirWalker.hasMoreElements()) {
                LOGGER.finer("findCGI: currentLoc=" + currentLocation);
                String nextElement = (String) dirWalker.nextElement();
                currentLocation = new File(currentLocation, nextElement);
                cginame = cginame + "/" + nextElement;
            }
            if (!currentLocation.isFile())
                return null;

            LOGGER.fine("findCGI: FOUND cgi at " + currentLocation);

            if (".".equals(contextPath)) {
                scriptname = servletPath;
            } else {
                scriptname = contextPath + servletPath;
            }
            if (!servletPath.equals(cginame)) {
                scriptname = scriptname + cginame;
            }

            LOGGER.config("findCGI calc: path=" + currentLocation
                    + ", scriptname=" + scriptname + ", cginame=" + cginame);
            return new CGIScript(currentLocation,scriptname,cginame);
        }

        /**
         * Constructs the CGI environment to be supplied to the invoked CGI
         * script; relies heavliy on Servlet API methods and findCGI
         *
         * @param    req request associated with the CGI
         *           invokation
         *
         * @return   true if environment was set OK, false if there
         *           was a problem and no environment was set
         */
        protected boolean setCGIEnvironment(HttpServletRequest req) throws IOException {

            /*
             * This method is slightly ugly; c'est la vie.
             * "You cannot stop [ugliness], you can only hope to contain [it]"
             * (apologies to Marv Albert regarding MJ)
             */

            Hashtable<String,String> envp = new Hashtable<String,String>();

            // Add the shell environment variables (if any)
            envp.putAll(shellEnv);

            // Add the CGI environment variables
            String sPathInfoOrig;
            String sPathInfoCGI;
            String sPathTranslatedCGI;


            sPathInfoOrig = this.pathInfo;
            sPathInfoOrig = sPathInfoOrig == null ? "" : sPathInfoOrig;

            CGIScript script = findCGI(sPathInfoOrig,
                                webAppRootDir,
                                contextPath,
                                servletPath,
                                cgiPathPrefix);
            if(script==null)    return false;


            envp.put("SERVER_SOFTWARE", "TOMCAT");

            envp.put("SERVER_NAME", nullsToBlanks(req.getServerName()));

            envp.put("GATEWAY_INTERFACE", "CGI/1.1");

            envp.put("SERVER_PROTOCOL", nullsToBlanks(req.getProtocol()));

            int port = req.getServerPort();
            Integer iPort = (port != 0 ? port : -1);
            envp.put("SERVER_PORT", iPort.toString());

            envp.put("REQUEST_METHOD", nullsToBlanks(req.getMethod()));

            envp.put("REQUEST_URI", nullsToBlanks(req.getRequestURI()));


            /*-
             * PATH_INFO should be determined by using sCGIFullName:
             * 1) Let sCGIFullName not end in a "/" (see method findCGI)
             * 2) Let sCGIFullName equal the pathInfo fragment which
             *    corresponds to the actual cgi script.
             * 3) Thus, PATH_INFO = request.getPathInfo().substring(
             *                      sCGIFullName.length())
             *
             * (see method findCGI, where the real work is done)
             *
             */
            if (pathInfo == null
                || (pathInfo.substring(script.cgiName.length()).length() <= 0)) {
                sPathInfoCGI = "";
            } else {
                sPathInfoCGI = pathInfo.substring(script.cgiName.length());
            }
            envp.put("PATH_INFO", sPathInfoCGI);


            /*-
             * PATH_TRANSLATED must be determined after PATH_INFO (and the
             * implied real cgi-script) has been taken into account.
             *
             * The following example demonstrates:
             *
             * servlet info   = /servlet/cgigw/dir1/dir2/cgi1/trans1/trans2
             * cgifullpath    = /servlet/cgigw/dir1/dir2/cgi1
             * path_info      = /trans1/trans2
             * webAppRootDir  = servletContext.getRealPath("/")
             *
             * path_translated = servletContext.getRealPath("/trans1/trans2")
             *
             * That is, PATH_TRANSLATED = webAppRootDir + sPathInfoCGI
             * (unless sPathInfoCGI is null or blank, then the CGI
             * specification dictates that the PATH_TRANSLATED metavariable
             * SHOULD NOT be defined.
             *
             */
            if (sPathInfoCGI != null && !("".equals(sPathInfoCGI))) {
                sPathTranslatedCGI = context.getRealPath(sPathInfoCGI);
            } else {
                sPathTranslatedCGI = null;
            }
            if (sPathTranslatedCGI == null || "".equals(sPathTranslatedCGI)) {
                //NOOP
            } else {
                envp.put("PATH_TRANSLATED", nullsToBlanks(sPathTranslatedCGI));
            }


            envp.put("SCRIPT_NAME", nullsToBlanks(script.scriptName));

            envp.put("QUERY_STRING", nullsToBlanks(req.getQueryString()));

            envp.put("REMOTE_HOST", nullsToBlanks(req.getRemoteHost()));

            envp.put("REMOTE_ADDR", nullsToBlanks(req.getRemoteAddr()));

            envp.put("AUTH_TYPE", nullsToBlanks(req.getAuthType()));

            envp.put("REMOTE_USER", nullsToBlanks(req.getRemoteUser()));

            envp.put("REMOTE_IDENT", ""); //not necessary for full compliance

            envp.put("CONTENT_TYPE", nullsToBlanks(req.getContentType()));


            /* Note CGI spec says CONTENT_LENGTH must be NULL ("") or undefined
             * if there is no content, so we cannot put 0 or -1 in as per the
             * Servlet API spec.
             */
            int contentLength = req.getContentLength();
            String sContentLength = (contentLength <= 0 ? "" :
                                     (new Integer(contentLength)).toString());
            envp.put("CONTENT_LENGTH", sContentLength);


            Enumeration headers = req.getHeaderNames();
            String header;
            while (headers.hasMoreElements()) {
                header = ((String) headers.nextElement()).toUpperCase();
                //REMIND: rewrite multiple headers as if received as single
                //REMIND: change character set
                //REMIND: I forgot what the previous REMIND means
                if ("AUTHORIZATION".equalsIgnoreCase(header) ||
                    "PROXY_AUTHORIZATION".equalsIgnoreCase(header)) {
                    //NOOP per CGI specification section 11.2
                } else {
                    envp.put("HTTP_" + header.replace('-', '_'),
                             req.getHeader(header));
                }
            }

            command = script.script.getCanonicalPath();

            envp.put("X_TOMCAT_SCRIPT_PATH", command);  //for kicks

            envp.put("SCRIPT_FILENAME", command);  //for PHP

            this.env = envp;

            return true;

        }

        /**
         * Print important CGI environment information in a easy-to-read HTML
         * table
         *
         * @return  HTML string containing CGI environment info
         *
         */
        public String toString() {

            StringBuffer sb = new StringBuffer();

            sb.append("<TABLE border=2>");

            sb.append("<tr><th colspan=2 bgcolor=grey>");
            sb.append("CGIEnvironment Info</th></tr>");

            sb.append("<tr><td>Validity:</td><td>");
            sb.append(isValid());
            sb.append("</td></tr>");

            if (isValid()) {
                Enumeration envk = env.keys();
                while (envk.hasMoreElements()) {
                    String s = (String) envk.nextElement();
                    sb.append("<tr><td>");
                    sb.append(s);
                    sb.append("</td><td>");
                    sb.append(blanksToString((String) env.get(s),
                                             "[will be set to blank]"));
                    sb.append("</td></tr>");
                }
            }

            sb.append("<tr><td colspan=2><HR></td></tr>");

            sb.append("<tr><td>Derived Command</td><td>");
            sb.append(nullsToBlanks(command));
            sb.append("</td></tr>");

            sb.append("<tr><td>Working Directory</td><td>");
            if (workingDirectory != null) {
                sb.append(workingDirectory.toString());
            }
            sb.append("</td></tr>");

            sb.append("<tr><td>Command Line Params</td><td>");
            for (String param : cmdLineParameters) {
                sb.append("<p>");
                sb.append(param);
                sb.append("</p>");
            }
            sb.append("</td></tr>");

            sb.append("</TABLE><p>end.");

            return sb.toString();
        }



        /**
         * Gets derived command string
         *
         * @return  command string
         *
         */
        protected String getCommand() {
            return command;
        }



        /**
         * Gets derived CGI working directory
         *
         * @return  working directory
         *
         */
        protected File getWorkingDirectory() {
            return workingDirectory;
        }



        /**
         * Gets derived CGI environment
         *
         * @return   CGI environment
         *
         */
        protected Hashtable getEnvironment() {
            return env;
        }



        /**
         * Gets derived CGI query parameters
         *
         * @return   CGI query parameters
         *
         */
        protected ArrayList<String> getParameters() {
            return cmdLineParameters;
        }



        /**
         * Gets validity status
         *
         * @return   true if this environment is valid, false
         *           otherwise
         *
         */
        protected boolean isValid() {
            return valid;
        }



        /**
         * Converts null strings to blank strings ("")
         *
         * @param    s string to be converted if necessary
         * @return   a non-null string, either the original or the empty string
         *           ("") if the original was <code>null</code>
         */
        protected String nullsToBlanks(String s) {
            return nullsToString(s, "");
        }



        /**
         * Converts null strings to another string
         *
         * @param    couldBeNull string to be converted if necessary
         * @param    subForNulls string to return instead of a null string
         * @return   a non-null string, either the original or the substitute
         *           string if the original was <code>null</code>
         */
        protected String nullsToString(String couldBeNull,
                                       String subForNulls) {
            return (couldBeNull == null ? subForNulls : couldBeNull);
        }



        /**
         * Converts blank strings to another string
         *
         * @param    couldBeBlank string to be converted if necessary
         * @param    subForBlanks string to return instead of a blank string
         * @return   a non-null string, either the original or the substitute
         *           string if the original was <code>null</code> or empty ("")
         */
        protected String blanksToString(String couldBeBlank,
                                      String subForBlanks) {
            return (("".equals(couldBeBlank) || couldBeBlank == null)
                    ? subForBlanks
                    : couldBeBlank);
        }

        public CGIRunner createRunner() {
            return new CGIRunner(getCommand(), getEnvironment(), getWorkingDirectory(), getParameters());
        }
    } //class CGIEnvironment


    /**
     * Encapsulates the knowledge of how to run a CGI script, given the
     * script's desired environment and (optionally) input/output streams
     *
     * <p>
     *
     * Exposes a <code>run</code> method used to actually invoke the
     * CGI.
     *
     * </p>
     * <p>
     *
     * The CGI environment and settings are derived from the information
     * passed to the constuctor.
     *
     * </p>
     * <p>
     *
     * The input and output streams can be set by the <code>setInput</code>
     * and <code>setResponse</code> methods, respectively.
     * </p>
     *
     * @version   $Revision: 543681 $, $Date: 2007-06-01 17:42:59 -0700 (Fri, 01 Jun 2007) $
     */

    protected class CGIRunner {

        /** script/command to be executed */
        private String command = null;

        /** environment used when invoking the cgi script */
        private Map<String,String> env = null;

        /** working directory used when invoking the cgi script */
        private File wd = null;

        /** command line parameters to be passed to the invoked script */
        private ArrayList<String> params = null;

        /** stdin to be passed to cgi script */
        private InputStream stdin = null;

        /** response object used to set headers & get output stream */
        private HttpServletResponse response = null;




        /**
         *  Creates a CGIRunner and initializes its environment, working
         *  directory, and query parameters.
         *  <BR>
         *  Input/output streams (optional) are set using the
         *  <code>setInput</code> and <code>setResponse</code> methods,
         *  respectively.
         *
         * @param  command  string full path to command to be executed
         * @param  env      Hashtable with the desired script environment
         * @param  wd       File with the script's desired working directory
         * @param  params   ArrayList with the script's query command line
         *                  paramters as strings
         */
        protected CGIRunner(String command, Map<String,String> env, File wd,
                            ArrayList<String> params) {
            this.command = command;
            this.env = env;
            this.wd = wd;
            this.params = params;
        }


        /**
         * Gets ready status
         *
         * @return   false if not ready (<code>run</code> will throw
         *           an exception), true if ready
         */
        protected boolean isReady() {
            return command != null
                    && env != null
                    && wd != null
                    && params != null
                    && response != null;
        }



        /**
         * Sets HttpServletResponse object used to set headers and send
         * output to
         *
         * @param  response   HttpServletResponse to be used
         *
         */
        protected void setResponse(HttpServletResponse response) {
            this.response = response;
        }



        /**
         * Sets standard input to be passed on to the invoked cgi script
         *
         * @param  stdin   InputStream to be used
         *
         */
        protected void setInput(InputStream stdin) {
            this.stdin = stdin;
        }



        /**
         * Converts a Hashtable to a String array by converting each
         * key/value pair in the Hashtable to a String in the form
         * "key=value" (hashkey + "=" + hash.get(hashkey).toString())
         *
         * @param  h   Hashtable to convert
         *
         * @return     converted string array
         *
         * @exception  NullPointerException   if a hash key has a null value
         *
         */
        protected String[] hashToStringArray(Map<String,String> h) {
            ArrayList<String> v = new ArrayList<String>();
            for (Entry<String, String> e : h.entrySet())
                v.add(e.getKey()+'='+e.getValue());
            return v.toArray(new String[v.size()]);
        }



        /**
         * Executes a CGI script with the desired environment, current working
         * directory, and input/output streams
         *
         * <p>
         * This implements the following CGI specification recommedations:
         * <UL>
         * <LI> Servers SHOULD provide the "<code>query</code>" component of
         *      the script-URI as command-line arguments to scripts if it
         *      does not contain any unencoded "=" characters and the
         *      command-line arguments can be generated in an unambiguous
         *      manner.
         * <LI> Servers SHOULD set the AUTH_TYPE metavariable to the value
         *      of the "<code>auth-scheme</code>" token of the
         *      "<code>Authorization</code>" if it was supplied as part of the
         *      request header.  See <code>getCGIEnvironment</code> method.
         * <LI> Where applicable, servers SHOULD set the current working
         *      directory to the directory in which the script is located
         *      before invoking it.
         * <LI> Server implementations SHOULD define their behavior for the
         *      following cases:
         *     <ul>
         *     <LI> <u>Allowed characters in pathInfo</u>:  This implementation
         *             does not allow ASCII NUL nor any character which cannot
         *             be URL-encoded according to internet standards;
         *     <LI> <u>Allowed characters in path segments</u>: This
         *             implementation does not allow non-terminal NULL
         *             segments in the the path -- IOExceptions may be thrown;
         *     <LI> <u>"<code>.</code>" and "<code>..</code>" path
         *             segments</u>:
         *             This implementation does not allow "<code>.</code>" and
         *             "<code>..</code>" in the the path, and such characters
         *             will result in an IOException being thrown;
         *     <LI> <u>Implementation limitations</u>: This implementation
         *             does not impose any limitations except as documented
         *             above.  This implementation may be limited by the
         *             servlet container used to house this implementation.
         *             In particular, all the primary CGI variable values
         *             are derived either directly or indirectly from the
         *             container's implementation of the Servlet API methods.
         *     </ul>
         * </UL>
         * </p>
         *
         * @exception IOException if problems during reading/writing occur
         *
         * @see    java.lang.Runtime#exec(String command, String[] envp,
         *                                File dir)
         */
        protected void run() throws IOException {

            /*
             * REMIND:  this method feels too big; should it be re-written?
             */

            if (!isReady()) {
                throw new IllegalStateException(this.getClass().getName()
                                      + ": not ready to run.");
            }

            LOGGER.config("runCGI(envp=[" + env + "], command=" + command + ")");

            if ((command.indexOf(File.separator + "." + File.separator) >= 0)
                || (command.indexOf(File.separator + "..") >= 0)
                || (command.indexOf(".." + File.separator) >= 0)) {
                throw new IOException(this.getClass().getName()
                                      + "Illegal Character in CGI command "
                                      + "path ('.' or '..') detected.  Not "
                                      + "running CGI [" + command + "].");
            }

            /* original content/structure of this section taken from
             * http://developer.java.sun.com/developer/
             *                               bugParade/bugs/4216884.html
             * with major modifications by Martin Dengler
             */
            Runtime rt;
            InputStream cgiOutput = null;
            BufferedReader commandsStdErr;
            BufferedOutputStream commandsStdIn;
            Process proc = null;
            int bufRead = -1;

            //create query arguments
            StringBuffer cmdAndArgs = new StringBuffer();
            if (command.indexOf(" ") < 0) {
                cmdAndArgs.append(command);
            } else {
                // Spaces used as delimiter, so need to use quotes
                cmdAndArgs.append("\"");
                cmdAndArgs.append(command);
                cmdAndArgs.append("\"");
            }

            for (String param : params) {
                cmdAndArgs.append(" ");
                if (param.indexOf(" ") < 0) {
                    cmdAndArgs.append(param);
                } else {
                    // Spaces used as delimiter, so need to use quotes
                    cmdAndArgs.append("\"");
                    cmdAndArgs.append(param);
                    cmdAndArgs.append("\"");
                }
            }

            StringBuffer command = new StringBuffer(cgiExecutable);
            command.append(" ");
            command.append(cmdAndArgs.toString());
            cmdAndArgs = command;

            try {
                rt = Runtime.getRuntime();
                proc = rt.exec(cmdAndArgs.toString(), hashToStringArray(env), wd);

                String sContentLength = env.get("CONTENT_LENGTH");

                if(!"".equals(sContentLength)) {
                    commandsStdIn = new BufferedOutputStream(proc.getOutputStream());
                    IOUtils.copy(stdin, commandsStdIn);
                    commandsStdIn.flush();
                    commandsStdIn.close();
                }

                /* we want to wait for the process to exit,  Process.waitFor()
                 * is useless in our situation; see
                 * http://developer.java.sun.com/developer/
                 *                               bugParade/bugs/4223650.html
                 */

                boolean isRunning = true;
                commandsStdErr = new BufferedReader
                    (new InputStreamReader(proc.getErrorStream()));
                final BufferedReader stdErrRdr = commandsStdErr ;

                new Thread() {
                    public void run () {
                        sendToLog(stdErrRdr) ;
                    }
                }.start() ;

                InputStream cgiHeaderStream =
                    new HTTPHeaderInputStream(proc.getInputStream());
                BufferedReader cgiHeaderReader =
                    new BufferedReader(new InputStreamReader(cgiHeaderStream));

                while (isRunning) {
                    try {
                        //set headers
                        String line;
                        while (((line = cgiHeaderReader.readLine()) != null)
                               && !("".equals(line))) {
                            LOGGER.fine("runCGI: addHeader(\"" + line + "\")");
                            if (line.startsWith("HTTP")) {
                                response.setStatus(getSCFromHttpStatusLine(line));
                            } else if (line.indexOf(":") >= 0) {
                                String header =
                                    line.substring(0, line.indexOf(":")).trim();
                                String value =
                                    line.substring(line.indexOf(":") + 1).trim();
                                if (header.equalsIgnoreCase("status")) {
                                    response.setStatus(getSCFromCGIStatusHeader(value));
                                } else {
                                    response.addHeader(header , value);
                                }
                            } else {
                                LOGGER.warning("runCGI: bad header line \"" + line + "\"");
                            }
                        }

                        //write output
                        byte[] bBuf = new byte[2048];

                        OutputStream out = response.getOutputStream();
                        cgiOutput = proc.getInputStream();

                        try {
                            while ((bufRead = cgiOutput.read(bBuf)) != -1) {
                                LOGGER.finest("runCGI: output " + bufRead +
                                    " bytes of data");
                                out.write(bBuf, 0, bufRead);
                            }
                        } finally {
                            // Attempt to consume any leftover byte if something bad happens,
                            // such as a socket disconnect on the servlet side; otherwise, the
                            // external process could hang
                            if (bufRead != -1) {
                                while ((bufRead = cgiOutput.read(bBuf)) != -1) {}
                            }
                        }

                        proc.exitValue(); // Throws exception if alive

                        isRunning = false;

                    } catch (IllegalThreadStateException e) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {
                        }
                    }
                } //replacement for Process.waitFor()

                // Close the output stream used
                cgiOutput.close();
            } catch (IOException e){
                LOGGER.log(Level.WARNING,"Caught exception", e);
                throw e;
            } finally {
                if (proc != null){
                    proc.destroy();
                }
            }
        }

        /**
         * Parses the Status-Line and extracts the status code.
         *
         * @param line The HTTP Status-Line (RFC2616, section 6.1)
         * @return The extracted status code or the code representing an
         * internal error if a valid status code cannot be extracted.
         */
        private int getSCFromHttpStatusLine(String line) {
            int statusStart = line.indexOf(' ') + 1;

            if (statusStart < 1 || line.length() < statusStart + 3) {
                // Not a valid HTTP Status-Line
                LOGGER.warning("runCGI: invalid HTTP Status-Line:" + line);
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            String status = line.substring(statusStart, statusStart + 3);

            int statusCode;
            try {
                statusCode = Integer.parseInt(status);
            } catch (NumberFormatException nfe) {
                // Not a valid status code
                LOGGER.warning("runCGI: invalid status code:" + status);
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            return statusCode;
        }

        /**
         * Parses the CGI Status Header value and extracts the status code.
         *
         * @param value The CGI Status value of the form <code>
         *             digit digit digit SP reason-phrase</code>
         * @return The extracted status code or the code representing an
         * internal error if a valid status code cannot be extracted.
         */
        private int getSCFromCGIStatusHeader(String value) {
            if (value.length() < 3) {
                // Not a valid status value
                LOGGER.warning("runCGI: invalid status value:" + value);
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            String status = value.substring(0, 3);

            int statusCode;
            try {
                statusCode = Integer.parseInt(status);
            } catch (NumberFormatException nfe) {
                // Not a valid status code
                LOGGER.warning("runCGI: invalid status code:" + status);
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }

            return statusCode;
        }

        private void sendToLog(BufferedReader rdr) {
            String line;
            int lineCount = 0 ;
            try {
                while ((line = rdr.readLine()) != null) {
                    LOGGER.warning("runCGI (stderr):" +  line);
                    lineCount++ ;
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "sendToLog error", e);
            } finally {
                try {
                    rdr.close() ;
                } catch (IOException ce) {
                    LOGGER.log(Level.WARNING, "sendToLog error", ce) ;
                }
            }
            if ( lineCount > 0) {
                LOGGER.fine("runCGI: " + lineCount + " lines received on stderr");
            }
        }
    } //class CGIRunner

    /**
     * This is an input stream specifically for reading HTTP headers. It reads
     * upto and including the two blank lines terminating the headers. It
     * allows the content to be read using bytes or characters as appropriate.
     */
    private final class HTTPHeaderInputStream extends InputStream {
        private static final int STATE_CHARACTER = 0;
        private static final int STATE_FIRST_CR = 1;
        private static final int STATE_FIRST_LF = 2;
        private static final int STATE_SECOND_CR = 3;
        private static final int STATE_HEADER_END = 4;

        private InputStream input;
        private int state;

        HTTPHeaderInputStream(InputStream theInput) {
            input = theInput;
            state = STATE_CHARACTER;
        }

        /**
         * @see java.io.InputStream#read()
         */
        public int read() throws IOException {
            if (state == STATE_HEADER_END) {
                return -1;
            }

            int i = input.read();

            // Update the state
            // State machine looks like this
            //
            //    -------->--------
            //   |      (CR)       |
            //   |                 |
            //  CR1--->---         |
            //   |        |        |
            //   ^(CR)    |(LF)    |
            //   |        |        |
            // CHAR--->--LF1--->--EOH
            //      (LF)  |  (LF)  |
            //            |(CR)    ^(LF)
            //            |        |
            //          (CR2)-->---

            if (i == 10) {
                // LF
                switch(state) {
                    case STATE_CHARACTER:
                        state = STATE_FIRST_LF;
                        break;
                    case STATE_FIRST_CR:
                        state = STATE_FIRST_LF;
                        break;
                    case STATE_FIRST_LF:
                    case STATE_SECOND_CR:
                        state = STATE_HEADER_END;
                        break;
                }

            } else if (i == 13) {
                // CR
                switch(state) {
                    case STATE_CHARACTER:
                        state = STATE_FIRST_CR;
                        break;
                    case STATE_FIRST_CR:
                        state = STATE_HEADER_END;
                        break;
                    case STATE_FIRST_LF:
                        state = STATE_SECOND_CR;
                        break;
                }

            } else {
                state = STATE_CHARACTER;
            }

            return i;
        }
    }  // class HTTPHeaderInputStream

    public static final class CGIScript {
        /**
         * Location of the CGI script.
         */
        public final File script;
        /**
         * CGI variable SCRIPT_NAME. This is the full URL path
         * to the CGI script.
         */
        public final String scriptName;
        /**
         * Servlet pathInfo fragment corresponding to the CGI script.
         */
        public final String cgiName;

        public CGIScript(File script, String scriptName, String cgiName) {
            this.script = script;
            this.scriptName = scriptName;
            this.cgiName = cgiName;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CGI.class.getName());
}
