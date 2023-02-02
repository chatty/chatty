
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class Quote implements Item {
    
    private final boolean isRequired;
    private final Item input;
    private final Item quote;
    
    public Quote(Item input, Item quote, boolean isRequired) {
        this.input = input;
        this.quote = quote;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        String value = input.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        String quoteValue = "\"";
        if (quote != null) {
            quoteValue = quote.replace(parameters);
            if (!Item.checkReq(isRequired, quoteValue)) {
                return null;
            }
        }
        return StringUtil.quote(value, quoteValue);
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, input, quote);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, input, quote);
    }

    @Override
    public String toString() {
        if (quote != null) {
            return "Quote("+quote+") "+input;
        }
        return "Quote "+input;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Quote other = (Quote) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.input, other.input)) {
            return false;
        }
        return Objects.equals(this.quote, other.quote);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.isRequired ? 1 : 0);
        hash = 97 * hash + Objects.hashCode(this.input);
        hash = 97 * hash + Objects.hashCode(this.quote);
        return hash;
    }

}
