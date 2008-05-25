package torricelli;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;

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

    public Process launch() throws IOException {
        pb.redirectErrorStream(true);
        return pb.start();
    }
}
