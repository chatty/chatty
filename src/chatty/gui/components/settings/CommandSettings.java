
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.TestContextMenu;
import chatty.util.commands.CustomCommand;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Window;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class CommandSettings extends SettingsPanel {
    
    private static String getInfo(String type) {
        String info = INFO_HEADER
            + "<ul>"
            + "<li>"
            + "Add Custom Commands as <code>/Command</code> (no parameters), "
            + "separated by spaces (several per line), <code>//Command</code> "
            + "to put into submenu, <code>|</code> (vertical bar) to add "
            + "separator (or <code>-</code> on it's own line)."
            + "</li>"
            + "<li>"
            + "Add timeouts by specifying a number (<code>30</code> interpreted "
            + "as 30 seconds, <code>30s/m/h/d</code> as seconds/minutes/hours "
            + "days respectively)."
            + "</li>"
            + "<li>"
            + "Or add a command directly (without it having to be added as a "
            + "Custom Command), one per line:<br />"
            + "<code>Slap=/me slaps $$1 around a bit with a large trout</code>"
            + "</li>";
        
        if (type.equals("userDialog")) {
            info += "<li>"
                    + "Put buttons in different rows, e.g. <code>@a1</code> on "
                    + "a line, subsequent lines to put into that row starting "
                    + "with a <code>.</code> (point). <code>@a1</code> is the "
                    + "default row, <code>@b1</code> the default bottom row "
                    + "(otherwise <code>//Command</code>), <code>@a2</code> to "
                    + "put into second top row and so on:<br />"
                    + "<code>@a2<br />./No_Spam /No_Spoilers<br />.Spoiler=/timeout $$1 600 no spoilers</code>";
        } else {
            info += "<li>"
                    + "Add custom submenus, <code>@Name of menu</code> on a line, "
                    + "subsequent lines to put in that menu starting with a "
                    + "<code>.</code> (point):<br />"
                    + "<code>@Rules<br />./No_Spam /No_Spoilers<br />.Spoiler=/timeout $$1 600 no spoilers</code>"
                    + "</li>";
        }
        switch(type) {
            case "userDialog":
                info += "<li>"
                        + "User Dialog specific parameters: "
                        + "<code>$1</code> Name of user, "
                        + "<code>$2-</code> Ban reason (if selected)"
                        + "</li>";
                break;
            case "streamsMenu":
                info += "<li>"
                        + "Streams Context Menu specific parameters: "
                        + "<code>$1-</code> Names of selected streams"
                        + "</li>";
                break;
            case "channelMenu":
                info += "<li>"
                        + "Channel Context Menu specific parameters: "
                        + "<code>$1</code> Name of currently active channel "
                        + "(without leading #)"
                        + "</li>";
                break;
            case "userMenu":
                info += "<li>"
                        + "User Context Menu specific parameters: "
                        + "<code>$1</code> Name of the user"
                        + "</li>";
                break;
        }
        info += "</ul>";
        
        if (type.equals("userDialog")) {
            info += "<p><em>Note:</em> You can also add [help-commands:shortcuts shortcuts] in brackets, "
                    + "which can be triggered when the User Dialog has focus "
                    + "(<code>/Ban[B]</code> or <code>Slap[B]=/me ..</code>).";
        }
        
        info += INFO_MORE;
        return info;
    }
    
    private static final String INFO_HEADER = "<html>"
            + "<style type='text/css'>"
            + "code { background: white; color: black; }"
            + "p { margin: 2px; }"
            + "ul { margin-left: 10px; }"
            + "li { margin-top: 2px; }"
            + "</style>"
            + "<body style='width:300px;font-weight:normal;'>";
    
    private static final String INFO_MORE =
            "<p>See the help for more information on "
            + "[help-commands: Custom Commands] and adding them to "
            + "[help-commands:menus Menus/User Dialog Buttons].</p>";

    private static final String INFO_COMMANDS = INFO_HEADER
            + "<p>Each entry is one custom command:<br /><code>/commandName Text to send to chat or regular command</code></p>"
            + "<p>Parameters (replaced when executing the command): "
            + "<code>$1</code> first parameter (optional), <code>$$1</code> first parameter (required) "
            + ", <code>$2-</code> second parameter to end.</p>"
            + "<p>Example: <code>/hello /me welcomes $$1 to chat</code></p>"
            + "<p>Backslash (<code>\\</code>) is used as an "
            + "escape character, which means the subsequent character is "
            + "taken literal (instead of a special meaning). Example: "
            + "<code>\\$1</code> would output <code>$1</code> instead of "
            + "being replaced with the first parameter. To have the "
            + "<code>\\</code> itself show up you have to escape it as well "
            + "(<code>\\\\</code> shows up as <code>\\</code>).</p>"
            + "<p>You can restrict commands to a channel by adding it "
            + "to the command name: <code>/hello#joshimuz /me welcomes ..</code></p>"
            + INFO_MORE;
    
    public CommandSettings(SettingsDialog d) {
        super(true);
        
        JPanel base = addTitledPanel("Custom Commands", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 0, 1, 1);
        ListSelector items = d.addListSetting("commands", "Custom Command", 400, 150, true, true);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        items.setTester(new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                CustomCommand command = null;
                String[] split = value.split(" ", 2);
                if (split.length == 2) {
                    command = CustomCommand.parse(split[1].trim());
                }
                showCommandInfoPopup(component, command);
                return null;
            }
        });
        items.setInfo(INFO_COMMANDS);
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
        JPanel menus = addTitledPanel("Menu/Button Commands", 1);
        
        Editor.Tester menuTester = new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                ContextMenu m = new TestContextMenu(value);
                m.show(component, x, y);
                return null;
            }
        };
        
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("User Context Menu:"), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        EditorStringSetting userContextMenu = d.addEditorStringSetting("userContextMenu", 20, true, "Edit User Context Menu:", true, getInfo("userMenu"), menuTester);
        userContextMenu.setLinkLabelListener(d.getLinkLabelListener());
        menus.add(userContextMenu, gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("Channel Context Menu:"), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1);
        EditorStringSetting channelContextMenu = d.addEditorStringSetting("channelContextMenu", 20, true, "Edit Channel Context Menu:", true, getInfo("channelMenu"), menuTester);
        channelContextMenu.setLinkLabelListener(d.getLinkLabelListener());
        menus.add(channelContextMenu, gbc);
        
        gbc = d.makeGbc(0, 2, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("Streams Context Menu:"), gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1);
        EditorStringSetting streamsContextMenu = d.addEditorStringSetting("streamsContextMenu", 20, true, "Edit Streams Context Menu:", true, getInfo("streamsMenu"), menuTester);
        streamsContextMenu.setLinkLabelListener(d.getLinkLabelListener());
        menus.add(streamsContextMenu, gbc);
        
        gbc = d.makeGbc(0, 3, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        menus.add(new JLabel("User Dialog Buttons:"), gbc);
        
        gbc = d.makeGbc(1, 3, 1, 1);
        EditorStringSetting userDialogButtons = d.addEditorStringSetting("timeoutButtons", 20, true, "Edit User Dialog Buttons:", true, getInfo("userDialog"));
        userDialogButtons.setLinkLabelListener(d.getLinkLabelListener());
        menus.add(userDialogButtons, gbc);
        
    }
    
    public static void showCommandInfoPopup(Component parent, CustomCommand command) {
        String message = "<p style='font-family:sans-serif;'>This shows how the "
                + "parser understands the part to be executed. It may not be "
                + "very obvious what it means, but it can be helpful for "
                + "debugging.</p><br />";
        if (command == null) {
            message += "No command.";
        } else if (command.hasError()) {
            message += "<p style='font-family:monospaced;'>"
                    + "Error: "+formatCommandInfo(command.getError())+"</p>";
        } else {
            message += formatCommandInfo(command.toString());
        }
        GuiUtil.showNonModalMessage(parent, "Custom Command", message,
                JOptionPane.INFORMATION_MESSAGE, true);
    }
    
    public static String formatCommandInfo(String input) {
        return Helper.htmlspecialchars_encode(input)
                .replace("\n", "<br />").replace(" ", "&nbsp;");
    }
    
}
