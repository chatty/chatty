
package chatty.gui.components.routing;

import chatty.util.MiscUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 *
 * Custom Tab:
 * openOnMessage
 * openOnStart
 * channelLogo
 * exclusive
 * log
 * clear message on close
 * id (automatically title, unless chosen otherwise)
 * title? (saved upper/lowercase for reopening later, otherwise derive from id)
 * strip out custom colors/highlight?
 * 
 * When using "to:" in other place, it will automatically create an entry with
 * default settings if not yet exists.
 * 
 * Routing:
 * matcher
 * target dropdown
 * move/copy
 * 
 * 
 * bans/timeouts in custom tab?
 * 
 * 
 * @author tduva
 */
public class RoutingEntry {
    
    private static Logger LOGGER = Logger.getLogger(RoutingEntry.class.getName());
    
    private final String targetName;
    public final int openOnMessage;
    public final boolean exclusive;
    private final String id;
    
    public RoutingEntry(String targetName, int openOnMessage, boolean exlusive) {
        this.targetName = targetName;
        this.openOnMessage = openOnMessage;
        this.exclusive = exlusive;
        this.id = RoutingManager.toId(targetName);
    }
    
    public String getName() {
        return targetName;
    }
    
    public String getId() {
        return id;
    }
    
    public List toList() {
        List<Object> result = new ArrayList<>();
        result.add(targetName);
        result.add(openOnMessage);
        result.add(exclusive ? 1 : 0);
        return result;
    }
    
    public static RoutingEntry fromList(List list) {
        try {
            String name = (String) list.get(0);
            int openOnMessage = ((Number) list.get(1)).intValue();
            boolean exclusive = MiscUtil.isNumTrue(list.get(2));
            return new RoutingEntry(name, openOnMessage, exclusive);
        }
        catch (Exception ex) {
            LOGGER.warning("Error parsing routing entry: "+ex);
        }
        return null;
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
        final RoutingEntry other = (RoutingEntry) obj;
        return Objects.equals(this.id, other.id);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
}
