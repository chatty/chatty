
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class CommandSettings extends SettingsPanel {
    
    private static final String INFO_HEADER = "<html>"
            + "<style type='text/css'>code { border: 1px solid #000; } p { margin: 2px; }</style>"
            + "<body style='width:300px;font-weight:normal;'>";
    
    private static final String INFO = INFO_HEADER
            + "<p>Add commands as <code>/Command</code> or <code>Command</code> (no parameters), "
            + "seperated by space or comma, <code>//Command</code> to put into submenu, <code>|</code> "
            + "(vertical bar) to add seperator.</p>";
    
        private static final String INFO_TIMEOUT = INFO_HEADER
                + "<p>Add commands as <code>/Command</code> (no parameters), "
                + "seperated by space or comma, <code>//Command</code> to put into second row of buttons.</p>"
                + "<p>Timeout buttons: <code>30</code> interpreted as seconds, <code>30s/m/h/d</code> "
                + "interpreted as seconds/minutes/hours/days respectively.</p>"
                + "<p>Add shortcuts in brackets: <code>/Ban[B]</code>.</p>";
        
        private static final String INFO_COMMANDS = INFO_HEADER
                + "<p>Each entry is one custom command: <code>/commandName Text to send to chat or regular command</code></p>"
                + "<p>Parameters (replaced when executing the command): "
                + "<code>$$1</code> required parameter, <code>$1</code> optional "
                + "parameter, <code>$2-</code> second parameter to end</p>"
                + "<p>Example: <code>/hello /me welcomes $$1 to chat</code></p>";
    
    public CommandSettings(SettingsDialog d) {
        super(true);
        
        JPanel base = addTitledPanel("Custom Commands", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 0, 1, 1);
        ListSelector items = d.addListSetting("commands", 400, 150, true);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        items.setInfo(INFO_COMMANDS);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
        JPanel menus = addTitledPanel("Menu/Button Commands", 1);
        
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("User Context Menu:"), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        menus.add(d.addEditorStringSetting("userContextMenu", 20, true, "Edit User Context Menu:", true, INFO), gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("Channel Context Menu:"), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1);
        menus.add(d.addEditorStringSetting("channelContextMenu", 20, true, "Edit Channel Context Menu:", true, INFO), gbc);
        
        gbc = d.makeGbc(0, 2, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("User Dialog Buttons:"), gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1);
        menus.add(d.addEditorStringSetting("timeoutButtons", 20, true, "Edit User Dialog Buttons:", true, INFO_TIMEOUT), gbc);
        
    }
    
}
