
package chatty.util.commands;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Randomly chooses one of the given parameters as output.
 * 
 * @author tduva
 */
public class Rand implements Item {

    private final boolean isRequired;
    private final List<Item> params;
    
    public Rand(boolean isRequired, List<Item> params) {
        this.isRequired = isRequired;
        this.params = params;
    }
    
    @Override
    public String replace(Parameters parameters) {
        Item random = params.get(ThreadLocalRandom.current().nextInt(0, params.size()));
        String replaced = random.replace(parameters);
        if (!Item.checkReq(isRequired, replaced)) {
            return null;
        }
        return replaced;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, params);
    }

    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, params);
    }
    
    @Override
    public String toString() {
        return "Rand " + params;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Rand other = (Rand) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.params, other.params)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.isRequired ? 1 : 0);
        hash = 73 * hash + Objects.hashCode(this.params);
        return hash;
    }
    
}
