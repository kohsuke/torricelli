package torricelli;

import torricelli.util.Proc;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class HgInvoker {
    private final List<String> commands = new ArrayList<String>();
    private final ProcessBuilder pb = new ProcessBuilder(commands);

    public HgInvoker(File workDir, Object... cmds) {
        commands.add("hg");
        arg(cmds);
        pb.directory(workDir);
    }

    public HgInvoker arg(Object a) {
        commands.add(a.toString());
        return this;
    }

    public HgInvoker arg(Object... args) {
        for (Object arg : args)
            arg(arg);
        return this;
    }

    public Proc launch(OutputStream out) throws IOException {
        pb.redirectErrorStream(true);
        // print out the command that it's running
        out.write('$');
        for (String command : commands) {
            out.write(' ');
            out.write(command.getBytes());
        }
        out.write(System.getProperty("line.separator").getBytes());

        return new Proc(pb.start(),out);
    }
}
