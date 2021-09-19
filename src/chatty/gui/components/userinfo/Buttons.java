
package chatty.gui.components.userinfo;

import chatty.gui.GuiUtil;
import chatty.gui.components.menus.CommandMenuItem;
import chatty.gui.components.menus.CommandMenuItems;
import chatty.gui.components.settings.CommandSettings;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 *
 * @author tduva
 */
public class Buttons {
    
    private final UserInfoDialog owner;
    private final ActionListener listener;
    
    private final JPanel primary = new JPanel();
    private final JPanel secondary = new JPanel();
    
    private final Map<String, JPanel> rows = new HashMap<>();
    private final Map<JButton, CustomCommand> commands = new HashMap<>();
    
    private final CustomCommand modCommand = CustomCommand.parse("/mod $$1");
    private final CustomCommand unmodCommand = CustomCommand.parse("/unmod $$1");
    private final CustomCommand approveCommand = CustomCommand.parse("/automod_approve $(automod-msg-id)");
    private final CustomCommand denyCommand = CustomCommand.parse("/automod_deny $(automod-msg-id)");
    private JButton modUnmodButton;
    private JButton approveButton;
    private JButton denyButton;

    public Buttons(UserInfoDialog owner, ActionListener listener) {
        this.owner = owner;
        this.listener = listener;
        
        primary.setLayout(new BoxLayout(primary, BoxLayout.Y_AXIS));
        secondary.setLayout(new BoxLayout(secondary, BoxLayout.Y_AXIS));
    }
    
    public void set(String setting) {
        remove();
        add(setting);
    }
    
    protected void updateModButtons(boolean localIsStreamer, boolean userIsMod) {
        if (modUnmodButton == null) {
            return;
        }
        // Check that local user is the streamer here
        if (localIsStreamer) {
            // Need to exchange the text like this because of the way the
            // shortcut label is hacked in
            String text = modUnmodButton.getText();
            if (userIsMod) {
                text = text.replace("Mod", "Unmod");
                commands.put(modUnmodButton, unmodCommand);
                modUnmodButton.setToolTipText("Unmod user");
            } else {
                text = text.replace("Unmod", "Mod");
                commands.put(modUnmodButton, modCommand);
                modUnmodButton.setToolTipText("Mod user");
            }
            modUnmodButton.setText(text);
        }
        modUnmodButton.setVisible(localIsStreamer);
    }
    
    protected void updateAutoModButtons(String autoModMsgId) {
        boolean show = autoModMsgId != null;
        if (approveButton != null) {
            approveButton.setVisible(show);
        }
        if (denyButton != null) {
            denyButton.setVisible(show);
        }
    }
    
    private void remove() {
        for (JButton button : commands.keySet()) {
            clearShortcut(button);
        }
        primary.removeAll();
        secondary.removeAll();
        commands.clear();
        rows.clear();
        modUnmodButton = null;
    }
    
    private void add(String setting) {
        boolean noKeyLabels = false;
        if (setting.contains("nokeylabels")) {
            setting = setting.replaceAll("nokeylabels", "");
            noKeyLabels = true;
        }
        List<CommandMenuItem> items = CommandMenuItems.parse(setting);
        for (CommandMenuItem item : items) {
            if (item.getCommand() == null) {
                continue;
            }
            JButton button = new JButton(item.getLabel());
            button.addActionListener(listener);
            button.setToolTipText("<html><body><p style='font-family:monospaced;'>"
                    +CommandSettings.formatCommandInfo(item.getCommand().toString()));
            commands.put(button, item.getCommand());
            
            boolean secondaryButton = false;
            if (item.getParent() != null && !item.getParent().startsWith("a")) {
                secondaryButton = true;
                button.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            }
            
            getRow(item.getParent()).add(button);

            if (item.getCommand() != null && item.getCommand().getCommandName() != null) {
                String commandName = item.getCommand().getCommandName();
                if (commandName.equalsIgnoreCase("modunmod")) {
                    button.setVisible(false);
                    modUnmodButton = button;
                    // Needs to be set to one of the two valid states so it can
                    // be replaced properly
                    button.setText("Mod");
                }
                else if (commandName.equalsIgnoreCase("automod_approve")) {
                    button.setVisible(false);
                    approveButton = button;
                    commands.put(button, approveCommand);
                }
                else if (commandName.equalsIgnoreCase("automod_deny")) {
                    button.setVisible(false);
                    denyButton = button;
                    commands.put(button, denyCommand);
                }
            }
            addShortcut(item.getKey(), button, secondaryButton, noKeyLabels);
        }
    }
    
    public JPanel getRow(String row) {
        if (row == null) {
            row = "a1";
        }
        if (row.equals(CommandMenuItems.CUSTOM_COMMANDS_SUBMENU)) {
            row = "b1";
        }
        if (!rows.containsKey(row)) {
            JPanel newRow = new JPanel();
            ((FlowLayout)(newRow.getLayout())).setVgap(4);
            // This should be default, but just to be clear
            ((FlowLayout)(newRow.getLayout())).setHgap(5);
            if (row.startsWith("a")) {
                primary.add(newRow);
            } else {
                secondary.add(newRow);
            }
            rows.put(row, newRow);
        }
        return rows.get(row);
    }
    
    public CustomCommand getCommand(JButton button) {
        return commands.get(button);
    }
    
    public JPanel getPrimary() {
        return primary;
    }
    
    public JPanel getSecondary() {
        return secondary;
    }
    
    private void addShortcut(String key, final JButton button, boolean smallButton,
            boolean noKeyLabel) {
        String label = null;
        if (key != null) {
            int index = key.indexOf("|");
            if (index > 0) {
                label = key.substring(index+1);
                key = key.substring(0, index);
            }
        }
        KeyStroke keyStroke = getKeyStroke(key);
        if (keyStroke == null) {
            return;
        }
        
        if (!noKeyLabel && label == null) {
            label = key;
        }
        if (label != null && !label.isEmpty()) {
            button.setText("<html>" + button.getText() + 
                "<span style='font-size:0.85em;font-weight:normal;color:gray;'>" + " ["+label+"]");
            if (smallButton) {
                button.setMargin(GuiUtil.SPECIAL_SMALL_BUTTON_INSETS);
            } else {
                button.setMargin(GuiUtil.SPECIAL_BUTTON_INSETS);
            }
        }
        button.setToolTipText(button.getToolTipText()+" [Shortcut: "+key+"]");
        
        
        owner.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, button);
        owner.getRootPane().getActionMap().put(button, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (owner.getFocusOwner().getClass() == JTextField.class) {
                    return;
                }
                button.doClick();
            }
        });
    }
    
    private KeyStroke getKeyStroke(String key) {
        return KeyStroke.getKeyStroke(key != null ? key.replace("+", " ") : key);
    }
    
    /**
     * Removes the action for this button. Since a new button will be created
     * everytime they are updated, the action will not be readded for this
     * button, so the shortcut will have no action to perform. It's easier to
     * remove from the action map based on the button than from the input map.
     * 
     * @param button 
     */
    private void clearShortcut(JButton button) {
        owner.getRootPane().getActionMap().remove(button);
    }
    
    /**
     * Update button enable status based on whether all named (not numbered)
     * parameters are available for the associated command.
     * 
     * @param parameters 
     */
    protected void updateButtonForParameters(Parameters parameters) {
        for (Map.Entry<JButton, CustomCommand> entry : commands.entrySet()) {
            JButton button = entry.getKey();
            CustomCommand command = entry.getValue();
            boolean allParams = !command.hasError() &&
                    parameters.notEmpty(command.getRequiredIdentifiers());
            button.setEnabled(allParams);
        }
    }
    
    /**
     * Hide rows that don't have any visible elements.
     */
    protected void updateButtonRows() {
        for (JPanel row : rows.values()) {
            boolean hasVisibleElements = false;
            synchronized(row.getTreeLock()) {
                for (int i=0;i<row.getComponentCount();i++) {
                    if (row.getComponent(i).isVisible()) {
                        hasVisibleElements = true;
                        break;
                    }
                }
            }
            row.setVisible(hasVisibleElements);
        }
    }
    
}
