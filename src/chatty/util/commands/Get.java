
package chatty.util.commands;

import chatty.util.settings.Settings;
import java.util.Objects;
import java.util.Set;

/**
 * Get setting values.
 * 
 * @author tduva
 */
class Get implements Item {
    
    private final Item item;
    private final Item sub;
    private final boolean isRequired;

    public Get(Item item, Item sub, boolean isRequired) {
        this.item = item;
        this.sub = sub;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = item.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        Settings settings = (Settings)parameters.getObject("settings");
        if (settings == null) {
            return isRequired ? null : "";
        }
        String result = null;
        if (settings.isStringSetting(value)) {
            result = settings.getString(value);
        }
        if (settings.isBooleanSetting(value)) {
            result = settings.getBoolean(value) ? "true" : "";
        }
        if (settings.isLongSetting(value)) {
            result = String.valueOf(settings.getLong(value));
        }
        if (settings.isMapSetting(value)) {
            if (sub != null) {
                String value2 = sub.replace(parameters);
                if (!Item.checkReq(isRequired, value2)) {
                    return null;
                }
                Object o = settings.mapGet(value, value2);
                result = o != null ? String.valueOf(o) : "";
            }
            else {
                result = String.valueOf(settings.getMap(value));
            }
        }
        if (settings.isListSetting(value)) {
            result = String.valueOf(settings.getList(value));
        }
        return Item.checkReq(isRequired, result) ? result : null;
    }

    @Override
    public String toString() {
        return "Get " + item + "["+ sub+"]";
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, item, sub);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, item, sub);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Get other = (Get) obj;
        if (!Objects.equals(this.item, other.item)) {
            return false;
        }
        if (!Objects.equals(this.sub, other.sub)) {
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
        hash = 59 * hash + Objects.hashCode(this.item);
        hash = 59 * hash + Objects.hashCode(this.sub);
        hash = 59 * hash + (this.isRequired ? 1 : 0);
        return hash;
    }
    
}
