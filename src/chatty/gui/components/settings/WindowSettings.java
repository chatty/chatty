
package chatty.gui.components.settings;

import chatty.gui.WindowStateManager;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class WindowSettings extends SettingsPanel {
    
    public WindowSettings(final SettingsDialog d) {
        
        JPanel dialogs = addTitledPanel(Language.getString("settings.section.dialogs"), 0);
        
        dialogs.add(new JLabel(Language.getString("settings.dialogs.restore")), d.makeGbc(0, 0, 1, 1));
        
        Map<Long, String> restoreModeOptions = new LinkedHashMap<>();
        restoreModeOptions.put((long)WindowStateManager.RESTORE_MAIN, Language.getString("settings.dialogs.option.openDefault"));
        restoreModeOptions.put((long)WindowStateManager.RESTORE_ALL, Language.getString("settings.dialogs.option.keepSession"));
        restoreModeOptions.put((long)WindowStateManager.RESTORE_ON_START, Language.getString("settings.dialogs.option.restore"));
        restoreModeOptions.put((long)WindowStateManager.REOPEN_ON_START, Language.getString("settings.dialogs.option.reopen"));
        ComboLongSetting restoreMode = new ComboLongSetting(restoreModeOptions);
        d.addLongSetting("restoreMode", restoreMode);
        dialogs.add(restoreMode, d.makeGbc(1, 0, 1, 1));
        
        dialogs.add(d.addSimpleBooleanSetting("restoreOnlyIfOnScreen"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        dialogs.add(d.addSimpleBooleanSetting("attachedWindows"),
                d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));

        
        JPanel other = addTitledPanel(Language.getString("settings.section.otherWindow"), 1);
        
        other.add(d.addSimpleBooleanSetting("urlPrompt"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("chatScrollbarAlways"),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("minimizeToTray"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("closeToTray"),
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));

        other.add(d.addSimpleBooleanSetting("userlistEnabled"),
                d.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST));
        
        JPanel userlistWidthPanel = new JPanel();
        userlistWidthPanel.add(new JLabel("Default Userlist Width:"));
        userlistWidthPanel.add(d.addSimpleLongSetting("userlistWidth", 3, true));
        userlistWidthPanel.add(new JLabel("Min. Width:"));
        userlistWidthPanel.add(d.addSimpleLongSetting("userlistMinWidth", 3, true));
        other.add(userlistWidthPanel,
                d.makeGbc(0, 5, 2, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("inputEnabled"),
                d.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));   
        
        JPanel popout = addTitledPanel("Popout", 2);
        
        popout.add(d.addSimpleBooleanSetting("popoutSaveAttributes", "Restore location/size",
                "Save and restore the location and size of popout dialogs during the same session"),
                d.makeGbc(0,0,1,1));
        popout.add(d.addSimpleBooleanSetting("popoutCloseLastChannel", "Close popout when only channel",
                "Automatically close a popout if the last channel in the main window is closed"),
                d.makeGbc(1, 0, 1, 1));
        
    }
    
}
