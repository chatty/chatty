
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class StreamSettings extends SettingsPanel {
    
    public StreamSettings(SettingsDialog d) {
        JPanel hlPanel = addTitledPanel(Language.getString("settings.section.streamHighlights"), 0);
        
        hlPanel.add(new JLabel(SettingConstants.HTML_PREFIX+Language.getString("settings.streamHighlights.info")),
                d.makeGbc(0, 0, 2, 1));
        
        hlPanel.add(new JLabel(Language.getString("settings.streamHighlights.channel")),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        hlPanel.add(d.addSimpleStringSetting("streamHighlightChannel", 20, true),
                d.makeGbc(1, 1, 1, 1));
        
        hlPanel.add(new JLabel(Language.getString("settings.streamHighlights.command")),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        hlPanel.add(d.addSimpleStringSetting("streamHighlightCommand", 20, true),
                d.makeGbc(1, 2, 1, 1));
        
        hlPanel.add(d.addSimpleBooleanSetting("streamHighlightChannelRespond"),
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
        hlPanel.add(d.addSimpleBooleanSetting("streamHighlightMarker"),
                d.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST));
    }
    
}
