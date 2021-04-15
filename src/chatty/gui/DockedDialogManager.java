
package chatty.gui;

import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockManager;
import chatty.util.settings.Settings;
import java.util.HashMap;
import java.util.Map;

/**
 * Info Panels that can be docked are registered in this, so settings can be
 * loaded and panels can be opened/closed by their id.
 * 
 * Each Info Panel contains a helper that manages all the docking related stuff.
 * 
 * @author tduva
 */
public class DockedDialogManager {

    private final Map<String, DockedDialogHelper> dialogs = new HashMap<>();
    private final Channels channels;
    private final MainGui gui;
    private final Settings settings;
    
    public DockedDialogManager(MainGui gui, Channels channels, Settings settings) {
        this.gui = gui;
        this.channels = channels;
        this.settings = settings;
    }
    
    public DockManager getDockManager() {
        return channels.getDock();
    }
    
    public DockedDialogHelper createHelper(DockedDialogHelper.DockedDialog dialog) {
        DockedDialogHelper helper = new DockedDialogHelper(dialog, gui, channels, settings);
        register(helper);
        return helper;
    }
    
    public void register(DockedDialogHelper helper) {
        dialogs.put(helper.getContent().getId(), helper);
    }
    
    public void loadSettings() {
        for (DockedDialogHelper helper : dialogs.values()) {
            helper.loadSettings();
        }
    }
    
    public void openInDock(String id) {
        if (dialogs.containsKey(id)) {
            DockedDialogHelper helper = dialogs.get(id);
            helper.setDocked(true);
            helper.setVisible(true, false);
        }
    }
    
    public void closeFromDock(String id) {
        if (dialogs.containsKey(id)) {
            DockedDialogHelper helper = dialogs.get(id);
            if (helper.isDocked()) {
                helper.setVisible(false, false);
            }
        }
    }
    
}
