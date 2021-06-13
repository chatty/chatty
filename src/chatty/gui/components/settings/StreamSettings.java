
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
        
        EditorStringSetting responseMsg = d.addEditorStringSetting("streamHighlightResponseMsg", 30, true,
                Language.getString("settings.streamHighlights.responseMsg"),
                false,
                SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-streamhighlightmsg.html", null),
                new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                CustomCommand command = CustomCommand.parse(value);
                if (command.hasError()) {
                    CommandSettings.showCommandInfoPopup(component, command);
                }
                else {
                    Parameters params = Parameters.create("");
                    params.put("added", "highlight" + (d.getBooleanSetting("streamHighlightMarker") ? "/marker" : ""));
                    params.put("chan", "#channel");
                    params.put("uptime", "1h 57m 30s");
                    params.put("comment", "(Funny jump in th..)");
                    params.put("fullcomment", "(Funny jump in the first level)");
                    String localCommand = command.replace(params);
                    params.put("chatuser", "Username");
                    params.put("comment", "([Username] Funny jum..)");
                    params.put("fullcomment", "([Username] Funny jump in the first level)");
                    String chatCommand = command.replace(params);
                    
                    GuiUtil.showNonModalMessage(parent, "Example",
                            String.format("Chat command:<br />%s<br /><br />Local command:<br />%s",
                                    Helper.htmlspecialchars_encode(chatCommand),
                                    Helper.htmlspecialchars_encode(localCommand)),
                            JOptionPane.INFORMATION_MESSAGE, true);
                }
                return null;
            }
        });
        commandPanel.add(responseMsg,
                d.makeGbc(0, 7, 2, 1, GridBagConstraints.WEST));
    }
    
}
