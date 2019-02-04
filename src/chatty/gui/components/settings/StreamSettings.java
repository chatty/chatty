
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class StreamSettings extends SettingsPanel {
    
    public StreamSettings(SettingsDialog d) {
        JPanel hlPanel = addTitledPanel(Language.getString("settings.section.streamHighlights"), 0);
        
        hlPanel.add(new JLabel(SettingConstants.HTML_PREFIX
                +Language.getString("settings.streamHighlights.info")),
                d.makeGbc(0, 0, 2, 1));
        
        hlPanel.add(d.addSimpleBooleanSetting("streamHighlightMarker"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        JPanel commandPanel = addTitledPanel(Language.getString("settings.section.streamHighlightsCommand"), 1);
        
        commandPanel.add(new JLabel(SettingConstants.HTML_PREFIX
                +Language.getString("settings.streamHighlights.matchInfo")),
                d.makeGbc(0, 2, 2, 1));
        
        commandPanel.add(new JLabel(Language.getString("settings.streamHighlights.channel")),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        commandPanel.add(d.addSimpleStringSetting("streamHighlightChannel", 20, true, DataFormatter.TRIM),
                d.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST));
        
        commandPanel.add(new JLabel(Language.getString("settings.streamHighlights.command")),
                d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST));
        commandPanel.add(d.addSimpleStringSetting("streamHighlightCommand", 20, true, DataFormatter.TRIM),
                d.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST));
        
        Map<String, String> presets = new LinkedHashMap<>();
        presets.put("", "Nobody");
        presets.put("status:bm", "Moderators");
        presets.put("status:bmv", "Moderators/VIPs");
        presets.put("status:bmvs", "Moderators/VIPs/Subscribers");
        commandPanel.add(new JLabel(Language.getString("settings.streamHighlights.match")),
                d.makeGbc(0, 5, 1, 1, GridBagConstraints.WEST));
        commandPanel.add(d.addComboStringSetting("streamHighlightMatch", 20, false, presets),
                d.makeGbc(1, 5, 1, 1, GridBagConstraints.WEST));
        
        commandPanel.add(d.addSimpleBooleanSetting("streamHighlightChannelRespond"),
                d.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        
    }
    
}
