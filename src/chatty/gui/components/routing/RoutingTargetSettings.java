
package chatty.gui.components.routing;

import chatty.util.MiscUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class RoutingTargetSettings {
    
    private static Logger LOGGER = Logger.getLogger(RoutingTargetSettings.class.getName());
    
    private static final Map<Long, String> openOnMessageValues = new HashMap<>();
    
    static {
        openOnMessageValues.put(0L, "Don't open on message");
        openOnMessageValues.put(1L, "Open on any message");
        openOnMessageValues.put(2L, "Open on regular chat message");
        openOnMessageValues.put(3L, "Open on info message");
    }
    
    private final String targetName;
    public final int openOnMessage;
    public final boolean exclusive;
    private final String id;
    public final boolean logEnabled;
    public final String logFile;
    public final int multiChannel;
    public final boolean channelFixed;
    public final boolean showAll;
    
    public RoutingTargetSettings(String targetName, int openOnMessage,
            boolean exlusive, boolean logEnabled, String logFile,
            int multiChannel, boolean channelFixed, boolean showAll) {
        this.targetName = targetName;
        this.openOnMessage = openOnMessage;
        this.exclusive = exlusive;
        this.id = RoutingManager.toId(targetName);
        this.logEnabled = logEnabled;
        this.logFile = logFile;
        this.multiChannel = multiChannel;
        this.channelFixed = channelFixed;
        this.showAll = showAll;
    }
    
    public String getName() {
        return targetName;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean shouldLog() {
        return logEnabled && logFile != null && !logFile.isEmpty();
    }
    
    public String makeInfo() {
        String info = openOnMessageValues.get((long) openOnMessage);
        if (multiChannel == 1) {
            info += ", by channel";
        }
        else if (multiChannel == 2) {
            info += ", by channel/all";
        }
        if (shouldLog()) {
            info += ", write to "+logFile+".log";
        }
        return info;
    }
    
    /**
     * Creates a new RoutingTargetSettings object with the changed value.
     * 
     * @param newValue
     * @return 
     */
    public RoutingTargetSettings setChannelFixed(boolean newValue) {
        if (channelFixed == newValue) {
            return this;
        }
        return new RoutingTargetSettings(targetName, openOnMessage, exclusive, logEnabled, logFile, multiChannel, newValue, showAll);
    }
    
    public RoutingTargetSettings setShowAll(boolean newValue) {
        if (showAll == newValue) {
            return this;
        }
        return new RoutingTargetSettings(targetName, openOnMessage, exclusive, logEnabled, logFile, multiChannel, channelFixed, newValue);
    }
    
    public List toList() {
        List<Object> result = new ArrayList<>();
        result.add(targetName);
        result.add(openOnMessage);
        result.add(exclusive ? 1 : 0);
        result.add(logEnabled ? 1 : 0);
        result.add(logFile);
        result.add(multiChannel);
        result.add(channelFixed ? 1 : 0);
        result.add(showAll ? 1 : 0);
        return result;
    }
    
    public static RoutingTargetSettings fromList(List list) {
        try {
            String name = (String) list.get(0);
            int openOnMessage = ((Number) list.get(1)).intValue();
            boolean exclusive = MiscUtil.isNumTrue(list.get(2));
            boolean logEnabled = false;
            String logFile = "";
            int multiChannel = 0;
            boolean channelFixed = false;
            boolean showAll = false;
            if (list.size() > 3) {
                logEnabled = MiscUtil.isNumTrue(list.get(3));
                logFile = (String) list.get(4);
                multiChannel = ((Number) list.get(5)).intValue();
                channelFixed = MiscUtil.isNumTrue(list.get(6));
                showAll = MiscUtil.isNumTrue(list.get(7));
            }
            return new RoutingTargetSettings(name, openOnMessage, exclusive, logEnabled, logFile, multiChannel, channelFixed, showAll);
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
        final RoutingTargetSettings other = (RoutingTargetSettings) obj;
        return Objects.equals(this.id, other.id);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }
    
    public static Map<Long, String> getOpenOnMessageValues() {
        return new HashMap<>(openOnMessageValues);
    }
    
}
