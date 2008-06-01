package torricelli;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Kohsuke Kawaguchi
 */
public class AbstractModelObject {
    protected boolean checkName(String name) throws IOException, ServletException {
        if(name==null || name.length()==0) {
            sendError("No name given");
            return false;
        }

        for( int i=0; i<name.length(); i++ ) {
            char ch = name.charAt(i);
            if(Character.isISOControl(ch)) {
                sendError("Control character is not allowed");
                return false;
            }
            if("?*/\\%!@#$^&|<>[]:;".indexOf(ch)!=-1) {
                sendError("Unsafe character '"+ch+"' is not allowed");
                return false;
            }
        }

        // looks good
        return true;
    }

    protected void sendError(String format, Object... args) throws IOException, ServletException {
        sendError(MessageFormat.format(format,args));
    }

    protected void sendError(String msg) throws IOException, ServletException {
        StaplerRequest req = Stapler.getCurrentRequest();
        StaplerResponse rsp = Stapler.getCurrentResponse();
        req.setAttribute("text",msg);
        rsp.forward(this,"error",req);
    }

}
