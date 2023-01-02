
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class Trim implements Item {

    private final boolean isRequired;
    private final Item input;
    
    public Trim(Item identifier, boolean isRequired) {
        this.input = identifier;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String value = input.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        return StringUtil.trim(value);
    }
    
    @Override
    public String toString() {
        return "Trim "+input;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, input);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, input);
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
        final Trim other = (Trim) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        return Objects.equals(this.input, other.input);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 19 * hash + (this.isRequired ? 1 : 0);
        hash = 19 * hash + Objects.hashCode(this.input);
        return hash;
    }
    
}
