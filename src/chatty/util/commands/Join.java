
package chatty.util.commands;

import java.util.Set;

/**
 * Replaces all spaces in the contained Item with the separator defined in the
 * Items object.
 * 
 * In case of Custom Command parameters this essentially joins the parameters
 * together with the given separator.
 * 
 * @author tduva
 */
class Join implements Item {

    private final boolean isRequired;
    private final Item identifier;
    private final Items separator;

    public Join(Item identifier, Items separator, boolean isRequired) {
        this.identifier = identifier;
        this.separator = separator;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = identifier.replace(parameters);
        if (value != null && !value.isEmpty()) {
            String sep = separator.replace(parameters);
            if (sep == null) {
                return null;
            }
            return value.replaceAll(" ", sep);
        }
        if (isRequired) {
            return null;
        }
        return "";
    }

    @Override
    public String toString() {
        return "JOIN:" + identifier + "/" + separator;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier, separator);
    }

}
