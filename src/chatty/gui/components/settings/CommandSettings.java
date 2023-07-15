
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.Room;
import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.CommandMenuItem;
import chatty.gui.components.menus.CommandMenuItems;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.TestContextMenu;
import chatty.gui.components.userinfo.UserInfoDialog;
import chatty.gui.components.userinfo.UserInfoListener;
import chatty.util.StringUtil;
import chatty.util.commands.CommandSyntaxHighlighter;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.CustomCommands;
import chatty.util.commands.Parameters;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class CommandSettings extends SettingsPanel {
    
    private static String getInfo(String type) {
        return INFO_HEADER+SettingsUtil.getInfo("info-menu.html", type);
    }
    
    private static final String INFO_HEADER = "<html>"
            + "<style type='text/css'>"
            + "p { margin: 2px; }"
            + "dt { margin-top: 8px; }"
            + "dd { margin-left: 10px; margin-top: 4px; }"
            + "li { margin-top: 4px; }"
            + "</style>"
            + "<body style='width:300px;font-weight:normal;'>";
    
    private static final String INFO_COMMANDS = INFO_HEADER
            +SettingsUtil.getInfo("info-commands.html", null);
    
    private final SettingsDialog d;
    
    public CommandSettings(SettingsDialog d) {
        super(true);
        this.d = d;
        
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
        items.setTester(createCommandTester());
        items.setInfo(INFO_COMMANDS);
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        items.setSyntaxHighlighter(new CommandSyntaxHighlighter());
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
        
        Editor.Tester userDialogTester = new Editor.Tester() {

            private final User user = new User("testUser", Room.createRegular("#testchannel"));
            
            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                updateErrors(value);
                UserInfoDialog dialog = new UserInfoDialog(parent, new UserInfoListener() {

                    @Override
                    public void anonCustomCommand(Room room, CustomCommand command, Parameters parameters) {
                        CustomCommands.addChans(room, parameters);
                        String result = String.format("<html><body><p style='font-family:monospaced;'>%s</p>",
                                formatCommandInfo(command.replace(parameters)));
                        JOptionPane.showMessageDialog(parent, result, "Command result", JOptionPane.INFORMATION_MESSAGE);
                    }
                }, null, d.settings, null);
                dialog.setUserDefinedButtonsDef(value, true);
                GuiUtil.setLocationRelativeTo(dialog, parent);
                dialog.show(component, user, "s0m3-msg-1d", null, null);
                return null;
            }
            
            /**
             * Add command error messages to User messages.
             * 
             * @param value
             * @return 
             */
            private void updateErrors(String value) {
                user.clearLines();
                List<CommandMenuItem> items = CommandMenuItems.parse(value);
                for (CommandMenuItem item : items) {
                    if (item.getCommand() != null && item.getCommand().hasError()) {
                        user.addMessage(String.format("Error in command '%s': %s",
                                item.getLabel(), item.getCommand().getSingleLineError()
                        ), false, null);
                    }
                }
                user.addMessage("Note that some replacements may not work in this test dialog.", false, "s0m3-msg-1d");
            }
            
        };
        
        addSetting("channelContextMenu", "channelMenu", 0, menus, menuTester);
        addSetting("streamsContextMenu", "streamsMenu", 1, menus, menuTester);
        addSetting("userContextMenu", "userMenu", 2, menus, menuTester);
        addSetting("timeoutButtons", "userDialog", 3, menus, userDialogTester);
        addSetting("textContextMenu", "textMenu", 4, menus, menuTester);
        addSetting("adminContextMenu", "adminMenu", 5, menus, menuTester);
        menus.add(d.addSimpleBooleanSetting("menuCommandLabels"), SettingsDialog.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        menus.add(d.addSimpleBooleanSetting("menuRestrictions"), SettingsDialog.makeGbc(0, 7, 2, 1, GridBagConstraints.WEST));
    }
    
    private void addSetting(String settingName, String infoName, int y, JPanel panel, Editor.Tester tester) {
        GridBagConstraints gbc;
        JLabel label = SettingsUtil.createLabel(settingName);
        gbc = d.makeGbc(0, y, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(label, gbc);
        
        gbc = d.makeGbc(1, y, 1, 1);
        EditorStringSetting setting = d.addEditorStringSetting(
                settingName, 20, true, label.getText(), true,
                getInfo(infoName), tester);
        label.setLabelFor(setting);
        setting.setLinkLabelListener(d.getLinkLabelListener());
        panel.add(setting, gbc);
    }
    
    public static Editor.Tester createCommandTester() {
        return new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                CustomCommand command = CustomCommands.parseCommandWithName(value);
                showCommandInfoPopup(component, command);
                return null;
            }
        };
    }
    
    public static void showCommandInfoPopup(Component parent, CustomCommand command) {
        String message = "<p style='font-family:sans-serif;'>This shows how the "
                + "parser understands the part to be executed. It may not be "
                + "very obvious what it means, but it can be helpful for "
                + "debugging. If no error is shown here, it's at least formally "
                + "correct.</p><br />";
        if (command == null) {
            message += "No command.";
        } else if (command.hasError()) {
            message += "<p style='font-family:monospaced;'>"
                    + "Error: "+formatCommandInfo(command.getError(), true)+"</p>";
        } else {
            message += formatCommandInfo(command.toString(), false);
        }
        String name = "";
        String chan = "";
        if (command != null) {
            name = command.hasName() ? " ("+command.getName()+")" : "";
            chan = command.hasChan() ? " [#"+command.getChan()+"]" : "";
        }
        GuiUtil.showNonModalMessage(parent, "Custom Command"+name+chan, message,
                JOptionPane.INFORMATION_MESSAGE, true);
    }
    
    public static String formatCommandInfo(String input) {
        return formatCommandInfo(input, false);
    }
    
    public static String formatCommandInfo(String input, boolean singleLine) {
        if (input == null) {
            return "<em>Empty</em>";
        }
        String result = Helper.htmlspecialchars_encode(input)
                .replace("\n", "<br>");
        if (singleLine) {
            result = result.replace(" ", "&nbsp;");
        }
        else {
            // Preserve display of several spaces in a row
            result = result.replace("  ", "&nbsp;&nbsp;");
        }
        return result;
    }
    
}
