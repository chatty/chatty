
package chatty.util.commands;

import chatty.User;
import chatty.util.StringUtil;
import chatty.util.api.usericons.Usericon;
import java.util.Objects;
import java.util.Set;

/**
 * Item using a named identifier for replacement.
 * 
 * @author tduva
 */
class Identifier implements Item {

    private final String name;

    public Identifier(String name) {
        this.name = StringUtil.toLowerCase(name);
    }
    
    private static String getUserParameter(String name, User user) {
        if (user == null) {
            return null;
        }
        switch (name) {
            case "nick": return user.getRegularDisplayNick();
            case "user-id": return user.getId();
            case "display-nick": return user.getDisplayNick();
            case "custom-nick": return user.getCustomNick();
            case "full-nick": return user.getFullNick();
            case "special-nick": return !user.hasRegularDisplayNick() ? "true" : null;
        }
        
        if (user.hasRegularDisplayNick()) {
            switch (name) {
                case "display-nick2": return user.getDisplayNick();
                case "full-nick2": return user.getFullNick();
            }
        }
        else {
            // Special nick (with spaces or localized)
            switch (name) {
                case "display-nick2": return user.getDisplayNick()+" ("+user.getRegularDisplayNick()+")";
                case "full-nick2": return user.getFullNick()+" ("+user.getRegularDisplayNick()+")";
            }
        }
        
        if (user.getTwitchBadges() != null) {
            switch (name) {
                case "twitch-badge-info": return user.getTwitchBadges().toString();
                case "twitch-badges": return Usericon.makeBadgeInfo(user.getTwitchBadges());
            }
        }
        return null;
    }

    /**
     * Return the parameter or an empty value if the parameter doesn't exist
     * (returning null would indicate a required parameter, which can't be
     * determined here).
     * 
     * @param parameters
     * @return 
     */
    @Override
    public String replace(Parameters parameters) {
        String value = parameters.get(name);
        if (value == null) {
            value = getUserParameter(name, (User)parameters.getObject("user"));
        }
        if (value == null && name.startsWith("my-")) {
            value = getUserParameter(name.substring("my-".length()),
                    (User)parameters.getObject("localUser"));
        }
        if (value == null && name.startsWith("_")) {
            CustomCommand command = (CustomCommand)parameters.getObject(name);
            if (command != null) {
                value = command.replace(parameters);
            }
        }
        return value != null ? value : "";
    }

    @Override
    public String toString() {
        return "$" + name;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, name);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getIdentifiersWithPrefix("", name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Identifier other = (Identifier) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.name);
        return hash;
    }

}
