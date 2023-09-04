
package chatty.gui.components.routing;

import chatty.User;
import chatty.gui.Channels;
import chatty.gui.DockStyledTabContainer;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.gui.MainGui;
import chatty.gui.StyleManager;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.textpane.InfoMessage;
import chatty.gui.components.textpane.UserMessage;
import chatty.util.Pair;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class RoutingManager {
    
    private static final Logger LOGGER = Logger.getLogger(RoutingManager.class.getName());

    private final Map<String, RoutingEntry> entries = new HashMap<>();
    private final Map<String, RoutingTarget> targets = new HashMap<>();
    private final List<HighlightItem> routing = new ArrayList<>();
    private final MainGui main;
    private final StyleManager styles;
    private final Channels channels;
    private final ContextMenuListener contextMenuListener;
    
    public RoutingManager(MainGui main, Channels channels, StyleManager styles,
            ContextMenuListener contextMenuListener) {
        this.main = main;
        this.styles = styles;
        this.channels = channels;
        this.contextMenuListener = contextMenuListener;
        main.getSettings().addSettingChangeListener((setting, type, value) -> {
            if (setting.equals("tabsMessage")) {
                SwingUtilities.invokeLater(() -> loadTabSettings());
            }
        });
        loadSettings();
    }
    
    public List<RoutingTargetInfo> getInfo() {
        Map<String, RoutingTargetInfo> byId = new HashMap<>();
        for (Map.Entry<String, RoutingTarget> entry : targets.entrySet()) {
            byId.put(entry.getKey(), new RoutingTargetInfo(
                    entry.getValue().getContent().getTitle(),
                    entry.getValue().getNumMessages()));
            System.out.println(entry.getValue().getNumMessages()+" "+entry.getKey());
        }
        for (Map.Entry<String, RoutingEntry> entry : entries.entrySet()) {
            if (!byId.containsKey(entry.getKey())) {
                byId.put(entry.getKey(), new RoutingTargetInfo(entry.getValue().getName(), -1));
            }
        }
        List<RoutingTargetInfo> result = new ArrayList<>(byId.values());
        Collections.sort(result);
        return result;
    }
    
    public List<RoutingEntry> getData() {
        return new ArrayList<>(entries.values());
    }
    
    public void setData(List<RoutingEntry> data) {
        entries.clear();
        for (RoutingEntry entry : data) {
            entries.put(entry.getId(), entry);
            RoutingTarget target = targets.get(entry.getId());
            if (target != null) {
                target.setName(entry.getName());
            }
        }
        saveSettings();
    }
    
    public void updateRouting(List<String> items) {
        routing.clear();
        for (String input : items) {
            HighlightItem item = new Highlighter.HighlightItem(input, "routing");
            if (item.hasError()) {
                LOGGER.warning("Error parsing routing item: "+item.getError());
            }
            else if (item.getRoutingTargets() == null) {
                LOGGER.warning("Error parsing routing item (no targets): "+item);
            }
            else {
                routing.add(item);
            }
        }
    }
    
    public void addUserMessage(RoutingTargets targets, UserMessage message, User localUser) {
        if (!filterTargets(targets)) {
            addRoutingTargets(targets, message, localUser);
        }
        filterTargets(targets);
        
        if (!targets.hasTargets()) {
            return;
        }
        
        for (Map.Entry<String, Pair<String, HighlightItem>> t : targets.getResultTargets().entrySet()) {
            String name = t.getValue().key;
            HighlightItem hlItem = t.getValue().value;
            RoutingTarget target = getTarget(name);
            UserMessage thisMessage = message.copy();
            thisMessage.routingSource = hlItem;
            target.addMessage(thisMessage);
            
            switch (getEntry(name).openOnMessage) {
                case 1: // Any message
                case 2: // Regular chat message
                    channels.addContent(target.getContent());
            }
        }
    }
    
    public void addInfoMessage(RoutingTargets targets, InfoMessage message, User user, User localUser) {
        if (!filterTargets(targets)) {
            addRoutingTargets(targets, message, user, localUser);
        }
        filterTargets(targets);
        
        if (!targets.hasTargets()) {
            return;
        }
        
        for (Map.Entry<String, Pair<String, HighlightItem>> t : targets.getResultTargets().entrySet()) {
            String name = t.getValue().key;
            HighlightItem hlItem = t.getValue().value;
            RoutingTarget target = getTarget(name);
            InfoMessage thisMessage = message.copy();
            thisMessage.routingSource = hlItem;
            target.addInfoMessage(thisMessage);
            
            switch (getEntry(name).openOnMessage) {
                case 1: // Any message
                case 3: // Info message
                    channels.addContent(target.getContent());
            }
        }
    }
    
    private void addRoutingTargets(RoutingTargets targets, UserMessage message, User localUser) {
        for (HighlightItem item : routing) {
            if (item.matches(HighlightItem.Type.REGULAR, message.text, message.user, localUser, message.tags)) {
                targets.add(item);
                if (!isRoutingMulti()) {
                    return;
                }
            }
        }
    }
    
    private void addRoutingTargets(RoutingTargets targets, InfoMessage message, User user, User localUser) {
        for (HighlightItem item : routing) {
            if (item.matches(HighlightItem.Type.INFO, message.text, user, localUser, message.tags)) {
                targets.add(item);
                if (!isRoutingMulti()) {
                    return;
                }
            }
        }
    }
    
    private boolean filterTargets(RoutingTargets targets) {
        if (targets.hasTargets() && !main.getSettings().getBoolean("routingMulti")) {
            targets.removeAllExceptFirst();
            return true;
        }
        return false;
    }
    
    public void addBan(User user, long duration, String reason, String targetMsgId) {
        for (RoutingTarget target : targets.values()) {
            target.addBan(user, duration, reason, targetMsgId);
        }
    }
    
    private boolean isRoutingMulti() {
        return main.getSettings().getBoolean("routingMulti");
    }
    
    public void addTarget(String id) {
        channels.addContent(getTarget(id).getContent());
    }
    
    public void selectTarget(String name) {
        RoutingTarget target = getTarget(name);
        channels.addContent(target.getContent());
        channels.getDock().setActiveContent(target.getContent());
    }
    
    private RoutingTarget getTarget(String targetName) {
        String targetId = toId(targetName);
        RoutingEntry entry = getEntry(targetName);
        RoutingTarget target = targets.get(targetId);
        if (target == null) {
            target = new RoutingTarget("'"+targetId+"'", entry.getName(), main, styles, channels.getDock(), contextMenuListener);
            targets.put(targetId, target);
            loadTabSettings(target.getContent());
        }
        return target;
    }
    
    public static String toId(String name) {
        return StringUtil.toLowerCase(name);
    }
    
    private RoutingEntry getEntry(String targetName) {
        String targetId = toId(targetName);
        RoutingEntry entry = entries.get(targetId);
        if (entry == null) {
            entry = new RoutingEntry(targetName, 1, true);
            entries.put(targetId, entry);
        }
        return entry;
    }
    
    private void loadSettings() {
        Collection<Object> settingsList = main.getSettings().getList("routingTargets");
        for (Object item : settingsList) {
            RoutingEntry entry = RoutingEntry.fromList((List) item);
            entries.put(entry.getId(), entry);
        }
    }
    
    private void saveSettings() {
        List<Object> settingsData = new ArrayList<>();
        for (RoutingEntry entry : entries.values()) {
            settingsData.add(entry.toList());
        }
        main.getSettings().putList("routingTargets", settingsData);
    }
    
    private void loadTabSettings() {
        for (RoutingTarget t : targets.values()) {
            loadTabSettings(t.getContent());
        }
    }
    
    private void loadTabSettings(DockStyledTabContainer content) {
        if (content instanceof DockStyledTabContainer) {
            content.setSettings(0, main.getSettings().getLong("tabsMessage"), 0, 0, 0, -1);
        }
    }

    public void refreshStyles() {
        for (RoutingTarget target : targets.values()) {
            target.refreshStyles();
        }
    }
    
}
