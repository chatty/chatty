
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
     * which usually means that the entire process should be aborted. If a
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
    
    public Set<String> getRequiredIdentifiers();
    
    public static Set<String> getRequiredIdentifiers(boolean isRequired, Object... input) {
        if (isRequired) {
            return getIdentifiersWithPrefix("", input);
        }
        return null;
    }

    public static Set<String> getIdentifiersWithPrefix(String prefix, Object... input) {
        return getIdentifiersWithPrefix(prefix, false, input);
    }
    
    public static Set<String> getIdentifiersWithPrefix(String prefix, boolean required, Object... input) {
        Set<String> output = new HashSet<>();
        for (Object value : input) {
            if (value != null) {
                if (value instanceof String) {
                    if (((String) value).startsWith(prefix)) {
                        output.add((String) value);
                    }
                } else if (value instanceof Item) {
                    Set<String> value2;
                    if (required) {
                        value2 = ((Item) value).getRequiredIdentifiers();
                    } else {
                        value2 = ((Item) value).getIdentifiersWithPrefix(prefix);
                    }
                    
                    if (value2 != null) {
                        output.addAll(value2);
                    }
                }
            }
        }
        return output;
    }
    
    /**
     * Returns {@code false} if:
     * 
     * <ul>
     * <li>Any of the {@code values} is {@code null}</li>
     * <li>Any of the {@code values} is empty and {@code isRequired} is {@code true}.</li>
     * </ul>
     * 
     * So if this returns {@code true} then none of the {@code values} can be
     * {@code null}, but they may be empty if not required.
     * 
     * @param isRequired
     * @param values
     * @return {@code true} if replacing can continue, {@code false} if it
     * should be stopped due to not available required values
     */
    public static boolean checkReq(boolean isRequired, String... values) {
        for (String value : values) {
            if (value == null) {
                return false;
            }
            if (isRequired && value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

}
