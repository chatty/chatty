
package chatty.util.commands;

import chatty.User;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem.Type;
import chatty.util.irc.MsgTags;
import java.util.Objects;
import java.util.Set;

/**
 * Uses a HighlightItem to check user, localUser and message properties (if
 * available).
 * 
 * @author tduva
 */
public class Is implements Item {
    
    private final Item item;
    private final boolean isRequired;

    public Is(Item item, boolean isRequired) {
        this.item = item;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = item.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        Highlighter.HighlightItem hl = new Highlighter.HighlightItem(value);
        User user = (User)parameters.getObject("user");
        User localUser = (User)parameters.getObject("localUser");
        String msg = parameters.get("msg");
        String result = "";
        if (hl.matches(Type.ANY, msg, user, localUser, MsgTags.EMPTY)) {
            result = "true";
        }
        return Item.checkReq(isRequired, result) ? result : null;
    }

    @Override
    public String toString() {
        return "Is " + item;
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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Is other = (Is) obj;
        if (!Objects.equals(this.item, other.item)) {
            return false;
        }
        if (this.isRequired != other.isRequired) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + Objects.hashCode(this.item);
        hash = 47 * hash + (this.isRequired ? 1 : 0);
        return hash;
    }
    
}
