
package chatty.util.commands;

import chatty.Helper;
import chatty.util.Debugging;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps a top-level item to escape some stuff if necessary.
 * 
 * @author tduva
 */
public class SpecialEscape implements Item {

    private final Item item;
    
    SpecialEscape(Item item) {
        this.item = item;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String result = item.replace(parameters);
        if (parameters.get(Helper.ESCAPE_FOR_CHAIN_COMMAND) != null && result != null) {
            result = Helper.escapeForChainCommand(result);
        }
        if (parameters.get(Helper.ESCAPE_FOR_FOREACH_COMMAND) != null && result != null) {
            result = Helper.escapeForForeachCommand(result);
        }
        return result;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return item.getIdentifiersWithPrefix(prefix);
    }

    @Override
    public Set<String> getRequiredIdentifiers() {
        return item.getRequiredIdentifiers();
    }
    
    @Override
    public String toString() {
        if (Debugging.isEnabled("cc-tli")) {
            return "{"+item.toString()+"}";
        }
        return item.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SpecialEscape other = (SpecialEscape) obj;
        if (!Objects.equals(this.item, other.item)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.item);
        return hash;
    }

}
