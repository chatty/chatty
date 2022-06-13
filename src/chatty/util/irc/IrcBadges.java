
package chatty.util.irc;

import chatty.util.MiscUtil;
import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * Parses and stores the badge info received through IRC.
 * 
 * The implementation uses a String array for compact storage, which is a bit
 * convoluted, however one of these is created and stored for any user. In busy
 * channels there can be tens of thousands of users after a few hours, so small
 * memory inefficiencies can really add up.
 * 
 * @author tduva
 */
public class IrcBadges {
        
        private final String[] badges;
        
        public IrcBadges(String[] badges) {
            this.badges = badges;
        }
        
        public String get(String id) {
            for (int i = 0; i < badges.length; i += 2) {
                if (badges[i].equals(id)) {
                    return badges[i + 1];
                }
            }
            return null;
        }
        
        public boolean hasId(String id) {
            return get(id) != null;
        }
        
        public boolean hasIdVersion(String id, String version) {
            String actualVersion = get(id);
            return actualVersion != null && actualVersion.equals(version);
        }
        
        public String getVersion(int i) {
            return badges[i*2+1];
        }
        
        public String getId(int i) {
            return badges[i*2];
        }
        
        public int size() {
            return badges.length / 2;
        }
        
        public boolean isEmpty() {
            return badges.length == 0;
        }
        
        public void forEach(BiConsumer<String, String> consumer) {
            for (int i = 0; i < badges.length; i += 2) {
                consumer.accept(badges[i], badges[i+1]);
            }
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
            final IrcBadges other = (IrcBadges) obj;
            return Arrays.deepEquals(this.badges, other.badges);
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Arrays.deepHashCode(this.badges);
            return hash;
        }
        
        private static final IrcBadges EMPTY = new IrcBadges(new String[0]);
        
        public static IrcBadges parse(String data) {
            if (data == null || data.isEmpty()) {
                return EMPTY;
            }
            String[] badges = data.split(",");
            String[] result = new String[badges.length * 2];
            int counter = 0;
            for (String badge : badges) {
                String[] split = badge.split("/", 2);
                if (split.length == 2) {
                    String id = MiscUtil.intern(split[0]);
                    String version = MiscUtil.intern(split[1]);
                    result[counter++] = id;
                    result[counter++] = version;
                }
            }
            if (counter < result.length) {
                String[] newResult = new String[counter];
                System.arraycopy(result, 0, newResult, 0, newResult.length);
                return new IrcBadges(newResult);
            }
            return new IrcBadges(result);
        }
        
    }