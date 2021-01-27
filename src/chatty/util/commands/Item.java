
package chatty.util.commands;

import java.util.HashSet;
import java.util.List;
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
    
    /**
     * Returns all identifiers if this replacement is required.
     * 
     * @return 
     */
    public Set<String> getRequiredIdentifiers();
    
    /**
     * If isRequired is true, returns all identifiers in the specified items.
     * 
     * @param isRequired Whether to return identifiers
     * @param input The items to look for identifiers in
     * @return A Set of identifiers, or null
     */
    public static Set<String> getRequiredIdentifiers(boolean isRequired, Object... input) {
        if (isRequired) {
            return getIdentifiersWithPrefix("", input);
        }
        return null;
    }

    /**
     * Returns all prefixes with the given prefix contained in {@code input}.
     * 
     * @param prefix The prefix
     * @param input Can be one or several {@code String} or {@code Item} objects
     * @return The prefixes, may be empty, but never {@code null}
     */
    public static Set<String> getIdentifiersWithPrefix(String prefix, Object... input) {
        return getIdentifiersWithPrefix(prefix, false, input);
    }
    
    /**
     * Returns all prefixes with the given prefix contained in {@code input}.
     * 
     * @param prefix The prefix
     * @param required Whether to get required identifiers
     * @param input Can be one or several {@code String} or {@code Item} objects
     * @return The prefixes, may be empty, but never {@code null}
     */
    public static Set<String> getIdentifiersWithPrefix(String prefix, boolean required, Object... input) {
        Set<String> output = new HashSet<>();
        for (Object value : input) {
            if (value != null) {
                if (value instanceof String) {
                    if (((String) value).startsWith(prefix)) {
                        output.add((String) value);
                    }
                }
                else if (value instanceof List) {
                    for (Object o : (List) value) {
                        if (o instanceof Item) {
                            addItemIdentifiers(prefix, required, (Item) o, output);
                        }
                    }
                }
                else if (value instanceof Item) {
                    addItemIdentifiers(prefix, required, (Item) value, output);
                }
            }
        }
        return output;
    }

    public static void addItemIdentifiers(String prefix, boolean required, Item item, Set<String> result) {
        Set<String> value;
        if (required) {
            value = item.getRequiredIdentifiers();
        }
        else {
            value = item.getIdentifiersWithPrefix(prefix);
        }

        if (value != null) {
            result.addAll(value);
        }
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
