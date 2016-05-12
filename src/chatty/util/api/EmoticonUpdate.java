
package chatty.util.api;

import chatty.util.api.Emoticon.SubType;
import chatty.util.api.Emoticon.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Container to remove and add emoticons.
 * 
 * @author tduva
 */
public class EmoticonUpdate {
    
    public final Set<Emoticon> emotesToAdd;
    public final Type typeToRemove;
    public final SubType subTypeToRemove;
    public final String roomToRemove;
    
    public EmoticonUpdate(Set<Emoticon> emotes) {
        this(emotes, null, null, null);
    }
    
    public EmoticonUpdate(Set<Emoticon> emotes, Type typeToRemove,
            SubType subTypeToRemove, String roomToRemove) {
        if (emotes == null) {
            emotes = new HashSet<>();
        }
        this.emotesToAdd = emotes;
        this.typeToRemove = typeToRemove;
        this.subTypeToRemove = subTypeToRemove;
        this.roomToRemove = roomToRemove;
    }
    
}
