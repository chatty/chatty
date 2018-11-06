
package chatty.util.commands;

import java.util.Objects;
import java.util.Set;

/**
 * Item comparing an Item (identifier) to an Items object (compare) and choosing
 * one of the two outputs depending on equality.
 * 
 * @author tduva
 */
class IfEq implements Item {

    private final boolean isRequired;
    private final Item identifier;
    private final Items compare;
    private final Items output1;
    // May be null
    private final Items output2;

    public IfEq(Item identifier, boolean isRequired, Items compare,
            Items output1, Items output2) {
        this.identifier = identifier;
        this.isRequired = isRequired;
        this.compare = compare;
        this.output1 = output1;
        this.output2 = output2;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = identifier.replace(parameters);
        String compareTo = compare.replace(parameters);
        if (value == null || compareTo == null) {
            return null;
        }
        String output = "";
        if (Objects.equals(value, compareTo)) {
            output = output1.replace(parameters);
        }
        else if (output2 != null) {
            output = output2.replace(parameters);
        }
        if (!Item.checkReq(isRequired, output)) {
            return null;
        }
        return output;
    }

    @Override
    public String toString() {
        return "IfEq " + identifier + " == " + compare + " ? " + output1 + " : " + output2;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier, compare, output1, output2);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, identifier, compare, output1, output2);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IfEq other = (IfEq)obj;
        if (isRequired != other.isRequired) {
            return false;
        }
        if (Objects.equals(identifier, other.identifier)) {
            return false;
        }
        if (Objects.equals(compare, other.compare)) {
            return false;
        }
        if (Objects.equals(output1, other.output1)) {
            return false;
        }
        if (Objects.equals(output2, other.output2)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + (this.isRequired ? 1 : 0);
        hash = 59 * hash + Objects.hashCode(this.identifier);
        hash = 59 * hash + Objects.hashCode(this.compare);
        hash = 59 * hash + Objects.hashCode(this.output1);
        hash = 59 * hash + Objects.hashCode(this.output2);
        return hash;
    }

}
