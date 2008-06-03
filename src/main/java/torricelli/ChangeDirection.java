package torricelli;

import org.kohsuke.graphviz.Edge;
import org.kohsuke.graphviz.Arrow;
import static org.kohsuke.graphviz.Attribute.ARROWHEAD;
import static org.kohsuke.graphviz.Attribute.ARROWTAIL;

/**
 * How does the changes flow to upstream?
 *
 * @author Kohsuke Kawaguchi
 */
public enum ChangeDirection {
    UP {
        public Edge decorate(Edge e) {
            return e.attr(ARROWHEAD, Arrow.NONE)
                    .attr(ARROWTAIL, Arrow.NORMAL);
        }
    },
    DOWN {
        public Edge decorate(Edge e) {
            return e.attr(ARROWHEAD, Arrow.NONE)
                    .attr(ARROWTAIL, Arrow.NORMAL);
        }
    },
    BOTH {
        public Edge decorate(Edge e) {
            return e.attr(ARROWHEAD, Arrow.NORMAL)
                    .attr(ARROWTAIL, Arrow.NORMAL);
        }
    };

    public abstract Edge decorate(Edge e);
}
