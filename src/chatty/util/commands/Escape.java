
package chatty.util.commands;

import chatty.Helper;
import java.util.Objects;
import java.util.Set;

/**
 * Functions to escape for specific things.
 * 
 * @author tduva
 */
public class Escape implements Item {

    public enum Type {
        CHAIN, FOREACH
    }
    
    private final Item item;
    private final boolean isRequired;
    private final Type type;
    
    public Escape(Item item, Type type, boolean isRequired) {
        this.item = item;
        this.type = type;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String value = item.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        // Don't escape if SpecialEscape already will anyway
        switch (type) {
            case CHAIN:
                if (parameters.get("escape-pipe") == null) {
                    return Helper.escapeForChainCommand(value);
                }
            case FOREACH:
                if (parameters.get("escape-greater") == null) {
                    return Helper.escapeForForeachCommand(value);
                }
        }
        return value;
    }
    
    @Override
    public String toString() {
        return String.format("Escape(%s) %s",
                type, item);
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, item);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, item);
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
        final Escape other = (Escape) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.item, other.item)) {
            return false;
        }
        if (this.type != other.type) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Objects.hashCode(this.item);
        hash = 29 * hash + (this.isRequired ? 1 : 0);
        hash = 29 * hash + Objects.hashCode(this.type);
        return hash;
    }
    
}