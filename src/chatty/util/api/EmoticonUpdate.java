
package chatty.util.api;

import chatty.util.TwitchEmotesApi.EmotesetInfo;
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
        USER_EMOTES, OTHER, HELIX_CHANNEL, HELIX_SETS
    }
    
    public final Source source;
    public final Set<Emoticon> emotesToAdd;
    public final Type typeToRemove;
    public final SubType subTypeToRemove;
    public final String roomToRemove;
    public final Set<String> setsToRemove;
    public final Set<String> setsAdded;
    public final Set<EmotesetInfo> setInfos;
    
    public static class Builder {
        
        private final Set<Emoticon> emotesToAdd;
        private Source source = Source.OTHER;
        private Type typeToRemove;
        private SubType subTypeToRemove;
        private Set<String> setsToRemove;
        private Set<String> setsAdded;
        private String roomToRemove;
        private Set<EmotesetInfo> setInfos;
        
        public Builder(Set<Emoticon> emotesToAdd) {
            if (emotesToAdd == null) {
                emotesToAdd = new HashSet<>();
            }
            this.emotesToAdd = emotesToAdd;
        }
        
        public Builder setSource(Source source) {
            this.source = source;
            return this;
        }
        
        public Builder setTypeToRemove(Type typeToRemove) {
            this.typeToRemove = typeToRemove;
            return this;
        }
        
        public Builder setSubTypeToRemove(SubType subTypeToRemove) {
            this.subTypeToRemove = subTypeToRemove;
            return this;
        }
        
        public Builder setSetsToRemove(Set<String> setsToRemove) {
            this.setsToRemove = setsToRemove;
            return this;
        }
        
        /**
         * Which emotesets the added emotes are part of.
         * 
         * @param setsAdded
         * @return 
         */
        public Builder setSetsAdded(Set<String> setsAdded) {
            this.setsAdded = setsAdded;
            return this;
        }
        
        /**
         * Which emotesets the added emotes are part of. Previous emotes in
         * these emotesets will also be removed prior to adding the new ones.
         * 
         * @param sets
         * @return 
         */
        public Builder setsSetsAddedToRemove(Set<String> sets) {
            this.setsAdded = sets;
            this.setsToRemove = sets;
            return this;
        }
        
        public Builder setRoomToRemove(String roomToRemove) {
            this.roomToRemove = roomToRemove;
            return this;
        }
        
        public Builder setSetInfos(Set<EmotesetInfo> setInfos) {
            this.setInfos = setInfos;
            return this;
        }
        
        public EmoticonUpdate build() {
            return new EmoticonUpdate(this);
        }
        
    }
    
    private EmoticonUpdate(Builder builder) {
        this.emotesToAdd = builder.emotesToAdd;
        this.typeToRemove = builder.typeToRemove;
        this.subTypeToRemove = builder.subTypeToRemove;
        this.roomToRemove = builder.roomToRemove;
        this.setsToRemove = builder.setsToRemove;
        this.setsAdded = builder.setsAdded;
        this.source = builder.source;
        this.setInfos = builder.setInfos;
    }
    
    @Override
    public String toString() {
        return String.format("+%d|%s/%s/-%s|%s|%s|%s",
                             emotesToAdd.size(),
                             setsAdded,
                             source,
                             typeToRemove,
                             subTypeToRemove,
                             roomToRemove,
                             setsToRemove);
    }
    
}
