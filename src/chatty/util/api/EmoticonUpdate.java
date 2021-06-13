
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
    
    public enum Source {
        USER_EMOTES, OTHER
    }
    
    public final Source source;
    public final Set<Emoticon> emotesToAdd;
    public final Type typeToRemove;
    public final SubType subTypeToRemove;
    public final String roomToRemove;
    public final Set<String> setsToRemove;
    
    public EmoticonUpdate(Set<Emoticon> emotes) {
        this(emotes, null, null, null, null, Source.OTHER);
    }
    
    public EmoticonUpdate(Set<Emoticon> emotes, Type typeToRemove,
            SubType subTypeToRemove, String roomToRemove,
            Set<String> setsToRemove) {
        this(emotes, typeToRemove, subTypeToRemove, roomToRemove, setsToRemove, Source.OTHER);
    }
    
    public EmoticonUpdate(Set<Emoticon> emotes, Type typeToRemove,
            SubType subTypeToRemove, String roomToRemove,
            Set<String> setsToRemove, Source source) {
        if (emotes == null) {
            emotes = new HashSet<>();
        }
        this.emotesToAdd = emotes;
        this.typeToRemove = typeToRemove;
        this.subTypeToRemove = subTypeToRemove;
        this.roomToRemove = roomToRemove;
        this.setsToRemove = setsToRemove;
        this.source = source;
    }
    
    @Override
    public String toString() {
        return String.format("+%d/%s/-%s|%s|%s|%s",
                             emotesToAdd.size(),
                             source,
                             typeToRemove,
                             subTypeToRemove,
                             roomToRemove,
                             setsToRemove);
    }
    
}
