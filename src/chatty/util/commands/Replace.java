
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Replace stuff in text literal and via regex.
 * 
 * @author tduva
 */
public class Replace implements Item {

    private final boolean isRequired;
    private final Item item;
    private final Item search;
    private final Item replace;
    private final Item type;
    
    public Replace(Item item, Item search, Item replace, boolean isRequired,
            Item type) {
        this.item = item;
        this.search = search;
        this.replace = replace;
        this.isRequired = isRequired;
        this.type = type;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String itemValue = item.replace(parameters);
        String searchValue = search.replace(parameters);
        String replaceValue = replace.replace(parameters);
        if (!Item.checkReq(false, itemValue, searchValue, replaceValue)) {
            return null;
        }
        String typeValue = "";
        if (type != null) {
            typeValue = type.replace(parameters);
            if (typeValue == null) {
                return null;
            }
        }
        String result;
        try {
            switch (typeValue) {
                case "reg":
                    result = itemValue.replaceAll(searchValue, Matcher.quoteReplacement(replaceValue));
                    break;
                case "regFirst":
                    result = itemValue.replaceFirst(searchValue, Matcher.quoteReplacement(replaceValue));
                    break;
                case "regRef":
                    result = itemValue.replaceAll(searchValue, replaceValue);
                    break;
                case "regFirstRef":
                    result = itemValue.replaceFirst(searchValue, replaceValue);
                    break;
                case "regFunc":
                case "regCustom":
                    String typeValue2 = typeValue;
                    result = StringUtil.replaceFunc(itemValue, searchValue, m -> {
                        Parameters replaceParameters = parameters.copy();
                        replaceParameters.putArgs(m.group());
                        for (int i=1;i<=m.groupCount();i++) {
                            replaceParameters.put("g"+i, m.group(i));
                        }
                        if (typeValue2.equals("regFunc")) {
                            // Retrieve custom replacement to run
                            CustomCommand c = (CustomCommand) parameters.getObject(replaceValue);
                            if (c != null) {
                                return c.replace(replaceParameters);
                            }
                            return "";
                        }
                        // Use replace Item directly, but with additional params
                        return replace.replace(replaceParameters);
                    });
                    break;
                case "cs":
                    result = itemValue.replace(searchValue, replaceValue);
                    break;
                default:
                    result = itemValue.replaceAll("(?iu)" + Pattern.quote(searchValue), Matcher.quoteReplacement(replaceValue));
            }
        } catch (Exception ex) {
            return "Error: " + ex.getLocalizedMessage();
        }
        if (!Item.checkReq(isRequired, result)) {
            return null;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("Replace %s with %s in %s via %s",
                search, replace, item, type == null ? "default" : type);
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, item, search, replace);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, item, search, replace);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Replace other = (Replace) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.item, other.item)) {
            return false;
        }
        if (!Objects.equals(this.search, other.search)) {
            return false;
        }
        if (!Objects.equals(this.replace, other.replace)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 11 * hash + (this.isRequired ? 1 : 0);
        hash = 11 * hash + Objects.hashCode(this.item);
        hash = 11 * hash + Objects.hashCode(this.search);
        hash = 11 * hash + Objects.hashCode(this.replace);
        hash = 11 * hash + Objects.hashCode(this.type);
        return hash;
    }

}
