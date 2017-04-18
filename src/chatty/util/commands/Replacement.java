
package chatty.util.commands;

import java.util.Set;

/**
 * A Replacement contains a single Item (usually some kind of identifier), which
 * it uses for replacement.
 * 
 * @author tduva
 */
class Replacement implements Item {

    private final boolean isRequired;
    private final Item identifier;

    public Replacement(Item name, boolean isRequired) {
        this.identifier = name;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = identifier.replace(parameters);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return isRequired ? null : "";
    }

    @Override
    public String toString() {
        return (isRequired ? "$" : "") + identifier.toString();
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return identifier.getIdentifiersWithPrefix(prefix);
    }

}
