
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
        if (Objects.equals(value, compareTo)) {
            return output1.replace(parameters, isRequired);
        }
        if (output2 != null) {
            return output2.replace(parameters, isRequired);
        }
        return isRequired ? null : "";
    }

    @Override
    public String toString() {
        return "IfEq " + identifier + " == " + compare + " ? " + output1 + " : " + output2;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier, compare, output1, output2);
    }

}
