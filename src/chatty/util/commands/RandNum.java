
package chatty.util.commands;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chooses a random number as output.
 * 
 * @author tduva
 */
public class RandNum implements Item {

    private final boolean isRequired;
    private final Item a;
    private final Item b;
    
    public RandNum(boolean isRequired, Item a, Item b) {
        this.isRequired = isRequired;
        this.a = a;
        this.b = b;
    }
    
    @Override
    public String replace(Parameters parameters) {
        int origin = 0;
        int bound;
        if (b == null) {
            // Only bound
            String valueA = a.replace(parameters);
            if (!Item.checkReq(isRequired, valueA)) {
                return null;
            }
            bound = parseInt(valueA, Integer.MAX_VALUE - 1);
        } else {
            // Origin and bound
            String valueA = a.replace(parameters);
            String valueB = b.replace(parameters);
            if (!Item.checkReq(isRequired, valueA, valueB)) {
                return null;
            }
            origin = parseInt(valueA, 0);
            bound = parseInt(valueB, Integer.MAX_VALUE - 1);
        }
        try {
            return String.valueOf(ThreadLocalRandom.current().nextInt(origin, bound+1));
        } catch (Exception ex) {
            return "0";
        }
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, a, b);
    }

    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, a, b);
    }
    
    @Override
    public String toString() {
        if (b == null) {
            return "RandNum 0 to "+a;
        }
        return "RandNum " +a+" to "+b;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RandNum other = (RandNum) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.a, other.a)) {
            return false;
        }
        if (!Objects.equals(this.b, other.b)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 11 * hash + (this.isRequired ? 1 : 0);
        hash = 11 * hash + Objects.hashCode(this.a);
        hash = 11 * hash + Objects.hashCode(this.b);
        return hash;
    }
    
    private static int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
    
}
