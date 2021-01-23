
package chatty.util.commands;

import java.util.Objects;
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
    private final Item args;

    public Replacement(Item name, Item args, boolean isRequired) {
        this.identifier = name;
        this.isRequired = isRequired;
        this.args = args;
    }

    @Override
    public String replace(Parameters parameters) {
        if (args != null) {
            String argsValue = args.replace(parameters);
            if (argsValue == null) {
                return null;
            }
            parameters = parameters.copy();
            parameters.putArgs(argsValue);
        }
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
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, identifier);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Replacement other = (Replacement)obj;
        if (!Objects.equals(identifier, other.identifier)) {
            return false;
        }
        if (isRequired != other.isRequired) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.isRequired ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.identifier);
        return hash;
    }

}
