
package chatty.util.settings;

/**
 * Defines a single Setting that has a subtype, which defines which
 * kind of values are saved in the datastructure, although it is not really enforced,
 * except when loading from Json.
 * 
 * @author tduva
 */
public class SubtypeSetting extends Setting {
    
    private final int subType;
    
    public SubtypeSetting(Object value, int type, int subtype, boolean save,
            String file) {
        super(value, type, save, file);
        subType = subtype;
    }
    
    public boolean isOfSubType(int type) {
        return subType == type;
    }
    
    public int getSubType() {
        return subType;
    }
    
}
