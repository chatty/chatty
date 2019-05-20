
package chatty.util.commands;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.Set;

/**
 * Applies URLEncoder.encode() to the input. This encodes into the format
 * "application/x-www-form-urlencoded", so "URLEncoder" may be a confusing name,
 * but it should work well enough for what it should be used.
 * 
 * @author tduva
 */
class UrlEncode implements Item {

    private final Item item;
    private final boolean isRequired;
    
    public UrlEncode(Item item, boolean isRequired) {
        this.item = item;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String value = item.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Shouldn't happen
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "UrlEncode "+item;
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
        final UrlEncode other = (UrlEncode) obj;
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
        int hash = 3;
        hash = 67 * hash + Objects.hashCode(this.item);
        hash = 67 * hash + (this.isRequired ? 1 : 0);
        return hash;
    }
    
}