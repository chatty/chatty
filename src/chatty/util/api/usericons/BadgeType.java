
package chatty.util.api.usericons;

import java.util.Objects;

/**
 *
 * @author tduva
 */
public class BadgeType {
    
    public final String id;
    public final String version;
    
    public static final BadgeType EMPTY = new BadgeType(null, null);
    
    public BadgeType(String id, String version) {
        this.id = id;
        this.version = version;
    }
    
    public static BadgeType parse(String idVersion) {
        if (idVersion == null) {
            return EMPTY;
        }
        String[] split = idVersion.split("/", 2);
        String id = null;
        String version = null;
        if (!split[0].trim().isEmpty()) {
            id = split[0].trim();
        }
        if (split.length == 2 && !split[1].trim().isEmpty()) {
            version = split[1].trim();
        }
        return new BadgeType(id, version);
    }
    
    public boolean isEmpty() {
        return id == null && version == null;
    }
    
    public int compareTo(BadgeType other) {
        if (this.equals(other)) {
            return 0;
        }
        if (other == null) {
            return -1;
        }
        if (!Objects.equals(id, other.id)) {
            return compareString(id, other.id);
        }
        if (!Objects.equals(version, other.version)) {
            return compareString(version, other.version);
        }
        return 0;
    }
    
    private static int compareString(String a, String b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BadgeType other = (BadgeType) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.version, other.version)) {
            return false;
        }
        return true;
    }
    
    public boolean equals(String id, String version) {
        return Objects.equals(this.id, id) && Objects.equals(this.version, version);
    }
    
    /**
     * Returns {@code true} if both the id and version in the arguments and of
     * this object are equal respectively, or if the id is equal but the version
     * of this object is {@code null}.
     * 
     * @param id
     * @param version
     * @return 
     */
    public boolean matchesLenient(String id, String version) {
        if (Objects.equals(this.id, id)) {
            if (this.version == null) {
                return true;
            }
            if (Objects.equals(this.version, version)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean matchesLenient(BadgeType badgeType) {
        return matchesLenient(badgeType.id, badgeType.version);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + Objects.hashCode(this.id);
        hash = 83 * hash + Objects.hashCode(this.version);
        return hash;
    }
    
    @Override
    public String toString() {
        if (id != null && version != null) {
            return id+"/"+version;
        }
        if (id != null) {
            return id;
        }
        return null;
    }
}
