
package chatty.util.commands;

import java.util.Set;

/**
 * Item that checks if the given Item (identifier) exists, and choose one of the
 * two outputs depending on the outcome.
 * 
 * @author tduva
 */
class If implements Item {

    private final boolean isRequired;
    private final Item identifier;
    private final Items output1;
    // May be null
    private final Items output2;

    public If(Item name, boolean isRequired, Items output1, Items output2) {
        this.isRequired = isRequired;
        this.identifier = name;
        this.output1 = output1;
        this.output2 = output2;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = identifier.replace(parameters);
        if (value != null && !value.isEmpty()) {
            return output1.replace(parameters, isRequired);
        }
        if (output2 != null) {
            return output2.replace(parameters, isRequired);
        }
        return isRequired ? null : "";
    }

    @Override
    public String toString() {
        return "If " + identifier + " ? " + output1 + " : " + output2;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, identifier, output1, output2);
    }

}
