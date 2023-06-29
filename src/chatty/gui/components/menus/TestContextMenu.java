
package chatty.gui.components.menus;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.settings.CommandSettings;
import chatty.util.StringUtil;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

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
        addItem("", "d) Restrictions/dynamic labels won't work properly in this context", readme);
        
        int errorCount = 0;
        Map<CustomCommand, Integer> restrictions = new HashMap<>();
        
        List<CommandMenuItem> items = CommandMenuItems.parse(value);
        for (CommandMenuItem item : items) {
            // Error of the regular command
            if (item.getCommand() != null && item.getCommand().getError() != null) {
                String errorDescription = String.format(
                        "<p style='font-family:monospaced;'>%s=Error%s: %s</p>",
                        item.getLabel(),
                        makeLineNumber(item.getLineNumber()),
                        CommandSettings.formatCommandInfo(item.getCommand().getError(), true));
                errorsInfo = StringUtil.append(errorsInfo, "<br />", errorDescription);
                errorCount++;
            }
            // Error of label command
            if (item.getLabelCommand() != null && item.getLabelCommand().getError() != null) {
                String errorDescription = String.format(
                        "<p style='font-family:monospaced;'>Error in label%s: %s</p>",
                        makeLineNumber(item.getLineNumber()),
                        CommandSettings.formatCommandInfo(item.getLabelCommand().getError(), true));
                errorsInfo = StringUtil.append(errorsInfo, "<br />", errorDescription);
                errorCount++;
            }
            // Collect restrictions (the same may be in several menu entries)
            if (item.hasRestrictionCommands()) {
                for (CustomCommand r : item.getRestrictionCommands()) {
                    // Only add the first one, so line number is kind of correct
                    if (!restrictions.containsKey(r)) {
                        // Kind of guess line number
                        restrictions.put(r, item.getLineNumber() - 1);
                    }
                }
            }
        }
        
        // Errors of collected restriction commands
        for (Map.Entry<CustomCommand, Integer> restriction : restrictions.entrySet()) {
            if (restriction.getKey().getError() != null) {
                String errorDescription = String.format(
                        "<p style='font-family:monospaced;'>Error in restriction%s: %s</p>",
                        makeLineNumber(restriction.getValue()),
                        CommandSettings.formatCommandInfo(restriction.getKey().getError(), true));
                errorsInfo = StringUtil.append(errorsInfo, "<br />", errorDescription);
                errorCount++;
            }
        }
        
        // Add menu entry if there are any errors
        if (errorCount > 0) {
            int size = getFontMetrics(getFont()).getHeight();
            Icon icon = GuiUtil.getFallbackIcon(UIManager.getIcon("OptionPane.warningIcon"), MainGui.class, "warning.png");
            addItem("errors", errorCount+" Errors", -1, null, GuiUtil.getScaledIcon(icon, size, size));
        }
        
        addSeparator();
        
        // Add the actual menu entries defined by the setting value
        for (CommandMenuItem item : items) {
            Parameters parameters = Parameters.create("");
            // Disable restrictions
            parameters.put("menu-test", "true");
            JMenuItem mItem = addCommandItem(item, CommandMenuItems.getMenuParameters(item, parameters));
            if (mItem != null && mItem.getText().isEmpty()) {
                // Add original label if it presumably got removed by using a Custom Command label
                mItem.setToolTipText(StringUtil.append(
                        mItem.getToolTipText(),
                        " ",
                        String.format("[Original label: %s]", item.getLabel())));
            }
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
    
    private static String makeLineNumber(int lineNumber) {
        return lineNumber > 0 ? " (on line "+lineNumber+")" : "";
    }
    
}
