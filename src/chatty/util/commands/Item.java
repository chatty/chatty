
package chatty.util.commands;

import java.util.HashSet;
import java.util.Set;

/**
 * An Item is an element in a Custom Command which will return a String when
 * given a set of Parameters.
 * 
 * @author tduva
 */
interface Item {

    /**
     * Return the text with any special tokens replaced with the given
     * parameters, depending on the individual implementation of the Item. For
     * example a Replacement would simply look up the value of the parameter and
     * return that, a Literal would just return it's text unchanged.
     *
     * A null return value indicates that a required parameter was not found,
     * which mostly means that the entire process should be aborted. If a
     * non-required parameter was not found, then an empty String may be
     * returned.
     *
     * @param parameters
     * @return
     */
    public String replace(Parameters parameters);

    /**
     * Returns all identifiers that start with the given prefix (can be empty to
     * return all).
     *
     * @param prefix
     * @return A set of identifiers, empty if none are found
     */
    public Set<String> getIdentifiersWithPrefix(String prefix);

    public static Set<String> getIdentifiersWithPrefix(String prefix, Object... input) {
        Set<String> output = new HashSet<>();
        for (Object value : input) {
            if (value != null) {
                if (value instanceof String) {
                    if (((String) value).startsWith(prefix)) {
                        output.add((String) value);
                    }
                } else if (value instanceof Item) {
                    Set<String> value2 = ((Item) value).getIdentifiersWithPrefix(prefix);
                    if (value2 != null) {
                        output.addAll(value2);
                    }
                }
            }
        }
        return output;
    }

}
