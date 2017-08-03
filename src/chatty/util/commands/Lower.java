
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
class Lower implements Item {

    private final boolean isRequired;
    private final Item identifier;
    
    public Lower(Item identifier, boolean isRequired) {
        this.identifier = identifier;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String value = identifier.replace(parameters);
        if (value != null) {
            return StringUtil.toLowerCase(value);
        }
        return isRequired ? null : "";
    }
    
    @Override
    public String toString() {
        return "Lower "+identifier;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Lower other = (Lower) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.identifier, other.identifier)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + (this.isRequired ? 1 : 0);
        hash = 79 * hash + Objects.hashCode(this.identifier);
        return hash;
    }

}
