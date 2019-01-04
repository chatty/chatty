
package chatty.util.commands;

import java.util.Objects;
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
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        String sep = separator.replace(parameters);
        if (sep == null) {
            return null;
        }
        return value.replaceAll(" ", sep);
    }

    @Override
    public String toString() {
        return "Join " + identifier + "/" + separator;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier, separator);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, identifier, separator);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Join other = (Join) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.identifier, other.identifier)) {
            return false;
        }
        if (!Objects.equals(this.separator, other.separator)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + (this.isRequired ? 1 : 0);
        hash = 29 * hash + Objects.hashCode(this.identifier);
        hash = 29 * hash + Objects.hashCode(this.separator);
        return hash;
    }
    
}
