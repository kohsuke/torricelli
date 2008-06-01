package torricelli;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Inter-related group of {@link Repository}s.
 *
 * <p>
 * Persisted with {@link Torricelli}.
 *
 * @author Kohsuke Kawaguchi
 */
public class Group {
    private final List<String> members = new ArrayList<String>();

    public final String name;

    public Group(String name) {
        this.name = name;
    }

    public List<Repository> getRepositories() throws IOException {
        List<Repository> repos = new ArrayList<Repository>();
        for (String name : members) {
            Repository r = Torricelli.INSTANCE.getRepository(name);
            if(r!=null) repos.add(r);
        }
        return repos;
    }
}
