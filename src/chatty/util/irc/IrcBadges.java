
package chatty.util.irc;

import chatty.util.MiscUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Parses and stores the badge info received through IRC.
 *
 * @author tduva, Regynate
 */
public class IrcBadges {
    private static class Badge {
        public final String badge;
        public final String version;

        public Badge(String badge, String version) {
            this.badge = badge;
            this.version = version;
        }

        @Override
        public String toString() {
            return badge + '/' + version;
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
            final Badge other = (Badge) obj;
            return other.version.equals(version) && other.badge.equals(badge);
        }

        @Override
        public int hashCode() {
            return 17 * badge.hashCode() + 93 * version.hashCode();
        }
    }

    private final Badge[] badges;

    private IrcBadges(Badge[] badges) {
        this.badges = badges;
    }

    public String getVersion(String id) {
        for (Badge badge : badges) {
            if (badge.badge.equals(id)) {
                return badge.version;
            }
        }
        return null;
    }

    public boolean hasId(String id) {
        return getVersion(id) != null;
    }

    public boolean hasIdVersion(String id, String version) {
        String actualVersion = getVersion(id);
        return actualVersion != null && actualVersion.equals(version);
    }

    public String getVersion(int i) {
        return badges[i].version;
    }

    public String getId(int i) {
        return badges[i].badge;
    }

    public int size() {
        return badges.length;
    }

    public boolean isEmpty() {
        return badges.length == 0;
    }

    public void forEach(BiConsumer<String, String> consumer) {
        for (Badge badge : badges) {
            consumer.accept(badge.badge, badge.version);
        }
    }

    @Override
    public String toString() {
        return Arrays.stream(badges).map(Badge::toString).collect(Collectors.joining(","));
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
        return Arrays.deepHashCode(this.badges);
    }

    private static final IrcBadges EMPTY = new IrcBadges(new Badge[0]);

    public static IrcBadges parse(String data) {
        if (data == null || data.isEmpty()) {
            return EMPTY;
        }
        String[] badges = data.split(",");
        List<Badge> result = new ArrayList<Badge>();
        for (String badge : badges) {
            String[] split = badge.split("/", 2);
            if (split.length == 2) {
                String id = MiscUtil.intern(split[0]);
                String version = MiscUtil.intern(split[1]);
                result.add(new Badge(id, version));
            }
        }
        return new IrcBadges(result.toArray(new Badge[0]));
    }
}