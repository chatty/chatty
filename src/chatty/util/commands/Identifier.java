
package chatty.util.commands;

import java.util.Set;

/**
 * Item using a named identifier for replacement.
 * 
 * @author tduva
 */
class Identifier implements Item {

    private final String name;

    public Identifier(String name) {
        this.name = name.toLowerCase();
    }

    @Override
    public String replace(Parameters parameters) {
        return parameters.get(name);
    }

    @Override
    public String toString() {
        return "$" + name;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, name);
    }

}
