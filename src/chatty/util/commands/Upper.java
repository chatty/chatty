
package chatty.util.commands;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Makes the text from the given Item uppercase.
 * 
 * @author tduva
 */
class Upper implements Item {

    private final boolean isRequired;
    private final Item identifier;
    
    public Upper(Item identifier, boolean isRequired) {
        this.identifier = identifier;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String value = identifier.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        return value.toUpperCase(Locale.ENGLISH);
    }
    
    @Override
    public String toString() {
        return "Upper "+identifier;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier);
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
        final Upper other = (Upper) obj;
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
        hash = 31 * hash + (this.isRequired ? 1 : 0);
        hash = 31 * hash + Objects.hashCode(this.identifier);
        return hash;
    }

}
