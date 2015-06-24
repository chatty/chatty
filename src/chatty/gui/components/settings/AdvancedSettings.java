
package chatty.gui.components.settings;

import chatty.WhisperConnection;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Settings that should only be changed if you know what you're doing, includes
 * a warning about that.
 * 
 * @author tduva
 */
public class AdvancedSettings extends SettingsPanel {
    
    public AdvancedSettings(SettingsDialog d) {

        JPanel warning = new JPanel();
        
        warning.add(new JLabel("<html><body style='width:300px'>"
                + "These settings can break Chatty if you change them, "
                + "so you should only change these settings if you "
                + "know what you are doing."));
        
        addPanel(warning, getGbc(0));
        
        JPanel connection = addTitledPanel("Connection", 1);

        connection.add(new JLabel("Server:"), d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("serverDefault", 20, true), d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("Port:"), d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("portDefault", 10, true), d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("(These might be overridden by commandline parameters.)"), d.makeGbc(0, 2, 2, 1));
        
        JPanel other = addTitledPanel("Other", 2);

        other.add(d.addSimpleBooleanSetting("membershipEnabled",
                "Correct Userlist (receives joins/parts, userlist)",
                "Enables the membership capability while connecting, which allows receiving of joins/parts/userlist"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.NORTHWEST));
        
        JPanel whisper = addTitledPanel("Whisper (experimental, read help!)", 3);
        
        whisper.add(
                d.addSimpleBooleanSetting("whisperEnabled", "Whisper Enabled",
                        "Connects to group chat to allow for whispering"),
                d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST)
        );
        
        whisper.add(
                d.addSimpleBooleanSetting("whisperWhitelist", "Whitelist",
                        "Only users in the Addressbook category 'whisper' may send messages to you."),
                d.makeGbc(4, 1, 1, 1, GridBagConstraints.EAST)
        );
        
//        whisper.add(
//                d.addSimpleBooleanSetting("whisperSeparate", "Separate",
//                        "Add whispers to seperate TAB instead of active channel."),
//                
//        );
        
        whisper.add(new JLabel("Display:"),
                d.makeGbc(3, 0, 1, 1));
        
        Map<Long, String> displayMode = new LinkedHashMap<>();
        displayMode.put(Long.valueOf(WhisperConnection.DISPLAY_IN_CHAT), "Active Chat");
        displayMode.put(Long.valueOf(WhisperConnection.DISPLAY_ONE_WINDOW), "One Window");
        displayMode.put(Long.valueOf(WhisperConnection.DISPLAY_PER_USER), "Per User");
        ComboLongSetting displayModeSetting = new ComboLongSetting(displayMode);
        d.addLongSetting("whisperDisplayMode", displayModeSetting);
        whisper.add(displayModeSetting,
                d.makeGbc(4, 0, 1, 1));
        
        whisper.add(new JLabel("Server:"), d.makeGbc(0, 1, 1, 1));
        whisper.add(d.addSimpleStringSetting("groupChatServer", 10, true),
                d.makeGbc(1, 1, 1, 1));
        whisper.add(new JLabel("Port:"), d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST));
        whisper.add(d.addSimpleStringSetting("groupChatPort", 4, true),
                d.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST));
        
        
    }
    
}
