package torricelli;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class NewRepository extends Repository {
    public NewRepository(File home) throws IOException {
        super(home);
    }
}
