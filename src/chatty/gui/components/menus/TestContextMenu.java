
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.components.settings.CommandSettings;
import chatty.util.StringUtil;
import chatty.util.commands.CustomCommand;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * Context Menu for the "Test" button in the settings.
 * 
 * @author tduva
 */
public class TestContextMenu extends ContextMenu {
    
    private String errorsInfo = "";
    
    public TestContextMenu(String value) {
        final String readme = "Readme";
        addItem("", "Test Context Menu", readme);
        addItem("", "a) Only shows custom menu items (not the pre-defined ones)", readme);
        addItem("", "b) Clicking on an item shows info about the associated command", readme);
        addItem("", "c) The associated command doesn't get executed", readme);
        addItem("", "(I hope you enjoy my abusing of a popup menu as an info list.)", readme);
        int errorCount = 0;
        List<CommandMenuItem> items = CommandMenuItems.parse(value);
        for (CommandMenuItem item : items) {
            if (item.getCommand() != null && item.getCommand().getError() != null) {
                String errorDescription = String.format(
                        "<p style='font-family:monospaced;'>%s=Error: %s</p>",
                        item.getLabel(),
                        CommandSettings.formatCommandInfo(item.getCommand().getError()));
                errorsInfo = StringUtil.append(errorsInfo, "<br />", errorDescription);
                errorCount++;
            }
        }
        if (errorCount > 0) {
            addItem("errors", errorCount+" Errors");
        }
        addSeparator();
        for (CommandMenuItem item : items) {
            JMenuItem mItem = addCommandItem(item, CommandMenuItems.getMenuParameters(item));
            // Still requires menus to have it as well, otherwise it doesn't make much sense
//            if (item.hasKey()) {
//                mItem.setMnemonic(KeyEvent.getExtendedKeyCodeForChar(item.getKey().toLowerCase().charAt(0)));
//            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("errors")) {
            GuiUtil.showNonModalMessage(getInvoker(), "Errors", errorsInfo, JOptionPane.WARNING_MESSAGE, true);
        }
        if (e instanceof CommandActionEvent) {
            CustomCommand command = ((CommandActionEvent)e).getCommand();
            CommandSettings.showCommandInfoPopup(getInvoker(), command);
        }
    }
    
}
