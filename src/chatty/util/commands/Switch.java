
package chatty.util.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Smiliar to a switch statement: $switch(value,case1,result1,case2,result2,default)
 * 
 * @author tduva
 */
public class Switch implements Item {

    private final Item param;
    private final Map<Item, Item> cases;
    private final Item def;
    private final boolean isRequired;
    
    /**
     * 
     * @param param Must be non-null
     * @param cases Must contain non-null key/value pairs
     * @param def Must be non-null
     * @param isRequired 
     */
    public Switch(Item param, Map<Item, Item> cases, Item def, boolean isRequired) {
        this.param = param;
        this.cases = cases;
        this.def = def;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String ref = param.replace(parameters);
        if (!Item.checkReq(isRequired, ref)) {
            return null;
        }
        
        Item resultItem = null;
        
        Iterator<Map.Entry<Item, Item>> it = cases.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Item, Item> entry = it.next();
            String caseString = entry.getKey().replace(parameters);
            if (Objects.equals(caseString, ref)) {
                resultItem = entry.getValue();
                break;
            }
        }
        if (resultItem == null) {
            resultItem = def;
        }
        
        String result = resultItem.replace(parameters);
        return Item.checkReq(isRequired, result) ? result : null;
    }
    
    @Override
    public String toString() {
        return "Switch "+param+"/"+cases+"/"+def;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, getAllParams());
    }

    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, getAllParams());
    }
    
    private Object[] getAllParams() {
        List<Item> result = new ArrayList<>();
        result.addAll(cases.keySet());
        result.addAll(cases.values());
        result.add(param);
        result.add(def);
        return result.toArray();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Switch other = (Switch) obj;
        if (!Objects.equals(this.param, other.param)) {
            return false;
        }
        if (!Objects.equals(this.cases, other.cases)) {
            return false;
        }
        if (!Objects.equals(this.def, other.def)) {
            return false;
        }
        if (this.isRequired != other.isRequired) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 71 * hash + Objects.hashCode(this.param);
        hash = 71 * hash + Objects.hashCode(this.cases);
        hash = 71 * hash + Objects.hashCode(this.def);
        hash = 71 * hash + (this.isRequired ? 1 : 0);
        return hash;
    }
    
}
