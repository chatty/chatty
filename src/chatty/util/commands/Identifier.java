
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Objects;
import java.util.Set;

/**
 * Item using a named identifier for replacement.
 * 
 * @author tduva
 */
class Identifier implements Item {

    private final String name;

    public Identifier(String name) {
        this.name = StringUtil.toLowerCase(name);
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Identifier other = (Identifier) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.name);
        return hash;
    }

}
