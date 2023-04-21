
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.StreamCategory;
import chatty.util.api.StreamInfo;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.time.Instant;
import static java.time.temporal.ChronoUnit.HOURS;
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
        Editor.Tester tester = new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                CustomCommand command = CustomCommand.parse(value);
                if (command.hasError()) {
                    CommandSettings.showCommandInfoPopup(component, command);
                }
                else {
                    StreamInfo streamInfo = new StreamInfo("channel", null);
                    streamInfo.set("Interesting Stream Title!",
                            new StreamCategory("id", "Game"),
                            123,
                            Instant.now().minus(2, HOURS).toEpochMilli(),
                            StreamInfo.StreamType.LIVE);

                    Parameters params = Parameters.create("");
                    params.putObject("streamInfo", streamInfo);
                    params.put("added", "highlight" + (d.getBooleanSetting("streamHighlightMarker") ? "/marker" : ""));
                    params.put("addedmarker", "true");
                    params.put("chan", "#channel");
                    params.put("uptime", "1h 57m 30s");
                    params.put("timestamp", DateTime.fullDateTime());
                    params.put("comment", "(Funny jump in th..)");
                    params.put("rawcomment", "Funny jump in the first level");
                    params.put("fullcomment", "(Funny jump in the first level)");
                    String localCommand = command.replace(params);
                    params.put("chatuser", "Username");
                    params.put("comment", "([Username] Funny jum..)");
                    params.put("fullcomment", "([Username] Funny jump in the first level)");
                    String chatCommand = command.replace(params);
                    params.put("added", "highlight");
                    params.remove("addedmarker");
                    params.remove("comment");
                    params.remove("fullcomment");
                    params.remove("rawcomment");
                    String withoutComment = command.replace(params);
                    params.put("uptime", "Stream Time N/A");
                    streamInfo.setOffline();
                    streamInfo.setOffline();
                    String offlineStream = command.replace(params);
                    GuiUtil.showNonModalMessage(parent, "Example",
                            String.format("Triggered by chat command:<br />%s<br /><br />"
                                    + "Triggered by local command:<br />%s<br /><br />"
                                    + "Without comment, no marker:<br />%s<br /><br />"
                                    + "Offline stream, no marker:<br />%s",
                                    Helper.htmlspecialchars_encode(chatCommand),
                                    Helper.htmlspecialchars_encode(localCommand),
                                    Helper.htmlspecialchars_encode(withoutComment),
                                    Helper.htmlspecialchars_encode(offlineStream)),
                            JOptionPane.INFORMATION_MESSAGE, true);
                }
                return null;
            }
        };
        
        //==========================
        // General
        //==========================
        JPanel hlPanel = addTitledPanel(Language.getString("settings.section.streamHighlights"), 0);
        
        hlPanel.add(new JLabel(SettingConstants.HTML_PREFIX
                +Language.getString("settings.streamHighlights.info")),
                d.makeGbc(0, 0, 2, 1));
        
        hlPanel.add(d.addSimpleBooleanSetting("streamHighlightMarker"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        hlPanel.add(d.addSimpleBooleanSetting("streamHighlightExtra"),
                d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));
        
        JCheckBox customEnabled = d.addSimpleBooleanSetting("streamHighlightCustomEnabled");
        hlPanel.add(customEnabled,
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
        
        EditorStringSetting customOutput = d.addEditorStringSetting("streamHighlightCustom", 30, true,
                Language.getString("settings.streamHighlights.customOutput"),
                false,
                SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-streamhighlightmsg.html", null),
                tester);
        customOutput.setLinkLabelListener(d.getLinkLabelListener());
        hlPanel.add(customOutput,
                d.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(customEnabled, customOutput);
        
        SettingsUtil.addLabeledComponent(hlPanel, "streamHighlightCooldown", 
                0, 5, 2, GridBagConstraints.WEST,
                d.addComboLongSetting("streamHighlightCooldown", true, 0, 5, 10, 15, 20, 25, 30, 60, 120));
        
        //==========================
        // Chat Command
        //==========================
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
        
        JCheckBox respondWithMessage = d.addSimpleBooleanSetting("streamHighlightChannelRespond");
        commandPanel.add(respondWithMessage,
                d.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        
        EditorStringSetting responseMsg = d.addEditorStringSetting("streamHighlightResponseMsg", 30, true,
                Language.getString("settings.streamHighlights.responseMsg"),
                false,
                SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-streamhighlightmsg.html", null),
                tester);
        responseMsg.setLinkLabelListener(d.getLinkLabelListener());
        commandPanel.add(responseMsg,
                d.makeGbc(0, 7, 2, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(respondWithMessage, responseMsg);
    }
    
}
