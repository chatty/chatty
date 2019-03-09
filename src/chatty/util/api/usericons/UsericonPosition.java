
package chatty.util.api.usericons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 *
 * @author tduva
 */
public class UsericonPosition {

    private static final Ref[] modStuff = new Ref[]{
        Ref.create("moderator"),
        Ref.create("staff"),
        Ref.create("admin"),
        Ref.create("global_mod"),
        Ref.create("broadcaster"),
        Ref.create("vip")
    };
    
    private static final Ref[] subscriber = new Ref[]{
        Ref.create("subscriber")
    };
    
    private static final Ref[] other = new Ref[]{
        Ref.create(Usericon.Type.OTHER, null)
    };
    
    private static final Ref[] bot = new Ref[]{
        Ref.create(Usericon.Type.BOT, null)
    };
    
    /**
     * Create a new UserPosition object.
     * 
     * @param input
     * @param first
     * @return 
     */
    public static UsericonPosition parse(String input, boolean first) {
        if (first) {
            return new UsericonPosition(new ArrayList<>());
        }
        if (input == null || input.isEmpty()) {
            return null;
        }
        Collection<Ref> result = new HashSet<>();
        String[] split = input.split(",");
        for (String part : split) {
            part = part.trim();
            if (part.equals("$start")) {
                // Add nothing
            } else if (part.equals("$mod")) {
                result.addAll(Arrays.asList(modStuff));
            } else if (part.equals("$sub")) {
                result.addAll(Arrays.asList(modStuff));
                result.addAll(Arrays.asList(subscriber));
            } else if (part.equals("$end")) {
                // Ignore everything else, icons should go to the end
                return null;
            } else if (part.equals("$bot")) {
                result.addAll(Arrays.asList(modStuff));
                result.addAll(Arrays.asList(subscriber));
                result.addAll(Arrays.asList(bot));
            } else if (part.startsWith("$other:")) {
                String[] split2 = part.split(":");
                if (split2.length == 2) {
                    result.add(Ref.create(Usericon.Type.OTHER, split2[1]));
                }
            } else if (part.equals("$other")) {
                result.addAll(Arrays.asList(other));
            } else {
                result.add(Ref.create(part));
            }
        }
        return new UsericonPosition(result);
    }
    
    
    private final Collection<Ref> data;
    
    private UsericonPosition(Collection<Ref> data) {
        this.data = data;
    }
    
    /**
     * Returns true if a Usericon is encountered that doesn't match anything
     * in this object's data.
     * 
     * @param icon
     * @return 
     */
    public boolean insertHere(Usericon icon) {
        for (Ref ref : data) {
            if (ref.continueSearching(icon)) {
                return false;
            }
        }
        return true;
    }
    
    
    /**
     * Combines an Usericon type and badge type for comparing it to icons to be
     * inserted.
     */
    private static class Ref {
        
        private final Usericon.Type reqType;
        private final BadgeType reqBadgeType;
        
        public static Ref create(String badgeType) {
            return new Ref(Usericon.Type.TWITCH, BadgeType.parse(badgeType));
        }
        
        public static Ref create(Usericon.Type type, String badgeType) {
            return new Ref(type, BadgeType.parse(badgeType));
        }
        
        private Ref(Usericon.Type type, BadgeType badgeType) {
            this.reqType = type;
            this.reqBadgeType = badgeType;
        }
        
        public boolean continueSearching(Usericon icon) {
            // Addon icons should always be ignored (inserted afterwards)
            if (icon.type == Usericon.Type.ADDON) {
                return true;
            }
            // No required badge type defined, so just check Usericon type
            if (this.reqBadgeType.isEmpty()) {
                return icon.type == this.reqType;
            }
            // Icon doesn't have a badge type, so might be e.g. a fallback icon
            // that's not of type TWITCH, so check type according to id
            if (icon.badgeType.isEmpty()) {
                return icon.type == Usericon.typeFromBadgeId(reqBadgeType.id);
            }
            if (icon.type == Usericon.Type.TWITCH || icon.type == Usericon.Type.OTHER) {
                return reqType == icon.type && reqBadgeType.matchesLenient(icon.badgeType);
            }
            return false;
        }
        
        @Override
        public String toString() {
            return reqType+" "+reqBadgeType;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Ref other = (Ref) obj;
            if (this.reqType != other.reqType) {
                return false;
            }
            if (!Objects.equals(this.reqBadgeType, other.reqBadgeType)) {
                return false;
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.reqType);
            hash = 97 * hash + Objects.hashCode(this.reqBadgeType);
            return hash;
        }
        
    }
    
}
