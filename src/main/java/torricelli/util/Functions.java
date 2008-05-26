package torricelli.util;

/**
 * Helper functions for the UI.
 *
 * @author Kohsuke Kawaguchi
 */
public class Functions {
    public Object ifThenElse(boolean cond, Object _then, Object _else) {
        return cond?_then:_else;
    }

    public Object defaultsTo(Object value, Object defaultValue) {
        return value!=null ? value : defaultValue;
    }
}
