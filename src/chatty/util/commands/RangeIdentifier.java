
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Collection;
import java.util.Set;

/**
 * Item using a range of Custom Command parameters for replacement.
 * 
 * @author tduva
 */
class RangeIdentifier implements Item {

    private final int index;
    private final boolean toEnd;

    public RangeIdentifier(int index, boolean toEnd) {
        this.index = index;
        this.toEnd = toEnd;
    }

    @Override
    public String replace(Parameters parameters) {
        Collection<String> range = parameters.getRange(index - 1, toEnd);
        if (range == null) {
            return null;
        }
        return StringUtil.join(range, " ");
    }

    @Override
    public String toString() {
        return "$" + index + (toEnd ? "-" : "");
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return null;
    }

}
