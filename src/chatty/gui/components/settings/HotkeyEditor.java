
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.gui.GuiUtil;
import static chatty.gui.components.settings.CommandSettings.showCommandInfoPopup;
import chatty.lang.Language;
import chatty.util.commands.CommandSyntaxHighlighter;
import chatty.util.commands.CustomCommand;
import chatty.util.hotkeys.Hotkey;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author tduva
 */
public class HotkeyEditor extends TableEditor<Hotkey> {
    
    private static final Set<String> RECOMMENDED_APPLICATION_ACTIONS =
            new HashSet<>(Arrays.asList(new String[]{"about"}));
    
    private static final Set<String> RECOMMENDED_GLOBAL_ACTIONS =
            new HashSet<>(Arrays.asList(new String[]{"dialog.toggleTransparency"}));

    private MyItemEditor itemEditor;
    private final MyTableModel data = new MyTableModel();
    private final Set<KeyStroke> conflictWarning = new HashSet<>();
    
    private Map<String, String> actions;
    private Map<String, String> descriptions;
    private boolean globalHotkeysAvailable;
    
    public HotkeyEditor(JDialog owner, Consumer<Hotkey> globalHotkeyCheck) {
        super(SORTING_MODE_SORTED, false);
        setModel(data);
        setItemEditor(() -> {
            if (itemEditor == null) {
                itemEditor = new MyItemEditor(owner, data);
                itemEditor.setActions(actions);
                itemEditor.setDescriptions(descriptions);
                itemEditor.setGlobalHotkeysAvailable(globalHotkeysAvailable);
            }
            return itemEditor;
        });
        
        setFixedColumnWidth(2, 60);
        
        setTableEditorListener(new TableEditorListener<Hotkey>() {
            @Override
            public void itemAdded(Hotkey item) {
                updateConflicts();
                globalHotkeyCheck.accept(item);
            }

            @Override
            public void itemRemoved(Hotkey item) {
                updateConflicts();
            }

            @Override
            public void itemEdited(Hotkey oldItem, Hotkey newItem) {
                updateConflicts();
                globalHotkeyCheck.accept(newItem);
            }

            @Override
            public void allItemsChanged(List<Hotkey> newItems) {
                updateConflicts();
            }

            @Override
            public void itemsSet() {
                updateConflicts();
            }

            @Override
            public void refreshData() {
                
            }
        });
    }
    
    /**
     * Update the set of keys that appear more than once.
     */
    public void updateConflicts() {
        conflictWarning.clear();
        Set<KeyStroke> usedKeys = new HashSet<>();
        for (Hotkey hotkey : data.getData()) {
            if (usedKeys.contains(hotkey.keyStroke)) {
                conflictWarning.add(hotkey.keyStroke);
            }
            usedKeys.add(hotkey.keyStroke);
        }
        repaint();
    }
    
    /**
     * Updates with the current data.
     * 
     * @param actions The map (id->label) of current actions
     * @param descriptions
     * @param hotkeys The list of current hotkeys
     * @param globalHotkeysAvailable Whether global hotkeys are available
     */
    public void setData(Map<String, String> actions,
                        Map<String, String> descriptions,
                        List<Hotkey> hotkeys,
                        boolean globalHotkeysAvailable) {
        // Set actions first, so it's correctly sorted in the table
        data.setActions(actions);
        setData(hotkeys);
        if (itemEditor != null) {
            itemEditor.setActions(actions);
            itemEditor.setDescriptions(descriptions);
            itemEditor.setGlobalHotkeysAvailable(globalHotkeysAvailable);
        }
        else {
            this.actions = actions;
            this.descriptions = descriptions;
            this.globalHotkeysAvailable = globalHotkeysAvailable;
        }
    }
    
    public void addHotkey(Hotkey hotkey) {
        data.add(hotkey);
    }
    
    public void edit(String id) {
        for (int i = 0; i < data.getRowCount(); i++) {
            Hotkey hotkey = data.get(i);
            if (hotkey.actionId.equals(id)) {
                editItem(i);
                return;
            }
        }
        addItem(new Hotkey(id, null, Hotkey.Type.GLOBAL, null, 0));
    }

    /**
     * Data storage for the table.
     */
    private class MyTableModel extends ListTableModel<Hotkey> {
        
        private Map<String, String> actions = new HashMap<>();
        
        public MyTableModel() {
            super(new String[]{"Action", "Hotkey", "Type"});
        }
        
        public void setActions(Map<String, String> actions) {
            this.actions = new HashMap<>(actions);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Hotkey hotkey = get(rowIndex);
            if (columnIndex == 0) {
                String action = actions.get(hotkey.actionId);
                if (hotkey.custom != null && !hotkey.custom.isEmpty()) {
                    action += " ("+hotkey.custom+")";
                }
                return action;
            } else if (columnIndex == 1) {
                if (conflictWarning.contains(hotkey.keyStroke)) {
                    return get(rowIndex).getHotkeyText()+" (duplicate)";
                }
                else {
                    return get(rowIndex).getHotkeyText();
                }
            } else {
                return get(rowIndex).type.name;
            }
        }
        
        @Override
        public Class getColumnClass(int c) {
            return String.class;
        }
        
    }
    
    /**
     * The dialog to add/edit hotkeys. Lets you select an action and configure
     * a key combination.
     */
    public static class MyItemEditor implements TableEditor.ItemEditor<Hotkey> {
        
        private static final String GLOBAL_HOTKEY_WARNING_GENERAL = "<br />"
                                        + "<br />"
                                        + "This message will not be shown again "
                                        + "this session.";
        
        private static final String GLOBAL_HOTKEY_WARNING_VERSION = "<html><body style='width:300px;'>Global Hotkey "
                                        + "feature is not available in this "
                                        + "version, so global hotkeys will not "
                                        + "work."+GLOBAL_HOTKEY_WARNING_GENERAL;
        
        private static final String GLOBAL_HOTKEY_WARNING_ERROR = "<html><body style='width:300px;'>Global Hotkey "
                                        + "feature was not initialized "
                                        + "properly, so global hotkeys will not "
                                        + "work."+GLOBAL_HOTKEY_WARNING_GENERAL;
        
        private final JDialog dialog;
        private final ComboStringSetting actionId = new ComboStringSetting(new String[0]);
        private final JTextField custom = new JTextField();
        private final EditorStringSetting anonymousCustomCommand;
        private final JLabel description = new JLabel();
        private final HotkeyTextField hotkeyChooser;
        //private final JCheckBox global = new JCheckBox("Global");
        private final JLabel hotkeyInfo = new JLabel();
        private final LongTextField delay;
        private KeyStroke currentHotkey;
        private boolean globalHotkeysAvailable;
        private boolean globalHotkeysAvailableWarningShown;
        private final MyTableModel data;
        private Map<String, String> actionNames;
        private Map<String, String> descriptions;
        private Hotkey preset;
        
        private final JRadioButton regular = new JRadioButton("Regular");
        private final JRadioButton applicationWide = new JRadioButton("Application");
        private final JRadioButton global = new JRadioButton("Global");
        private final JLabel scopeTip = new JLabel();
        
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        public MyItemEditor(JDialog owner, MyTableModel data) {
            this.data = data;
            delay = new LongTextField(4, true);
            
            hotkeyChooser = new HotkeyTextField(12, new HotkeyTextField.HotkeyEditListener() {

                @Override
                public void hotkeyChanged(KeyStroke newHotkey) {
                    currentHotkey = newHotkey;
                    updateButtons();
                }

                @Override
                public void hotkeyEntered(KeyStroke newHotkey) {
                    Hotkey hotkey = getHotkeyForKeyStroke(newHotkey);
                    if (hotkey != null && hotkey != preset) {
                        String action = actionNames.get(hotkey.actionId);
                        String message = "Used already: "+action;
                        hotkeyInfo.setText(message);
                        dialog.pack();
                        JOptionPane.showMessageDialog(dialog, "Hotkey already used for action: "+action);
                    } else {
                        hotkeyInfo.setText(null);
                        dialog.pack();
                    }
                }
            });
            
            custom.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateButtons();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateButtons();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateButtons();
                }
            });
            
            anonymousCustomCommand = new EditorStringSetting(owner, "Anonymous Custom Command", 10, false, false, "", new Editor.Tester() {

                @Override
                public String test(Window parent, Component component, int x, int y, String value) {
                    CustomCommand command = CustomCommand.parse(value);
                    showCommandInfoPopup(component, command);
                    return null;
                }
            });
            anonymousCustomCommand.setInfo(SettingConstants.HTML_PREFIX
                    +"Enter the command to execute, for example <code>/echo Currently open channels are: $(chans)</code>.");
            anonymousCustomCommand.setShowInfoByDefault(true);
            anonymousCustomCommand.setSettingValue("");
            anonymousCustomCommand.setChangeListener(e -> {
                updateButtons();
            });
            anonymousCustomCommand.setSyntaxHighlighter(new CommandSyntaxHighlighter());
            
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            //hotkeyChooser.setEditable(false);
            dialog.setModal(true);
            
            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == ok) {
                        dialog.setVisible(false);
                    } else if (e.getSource() == cancel) {
                        currentHotkey = null;
                        dialog.setVisible(false);
                    } else if (e.getSource() == actionId) {
                        updateButtons();
                        updateDescription();
                    } else if (e.getSource() == global) {
                        checkForGlobalHotkeyWarning();
                    }
                }
            };
            ok.addActionListener(listener);
            cancel.addActionListener(listener);
            actionId.addActionListener(listener);
            global.addActionListener(listener);
            
            regular.setToolTipText("Hotkey that can only be triggered if the focus is on the Chatty main window");
            applicationWide.setToolTipText("Hotkey that can be triggered anywhere in Chatty");
            global.setToolTipText("Hotkey that can be triggered globally on your computer");

            // Hotkey scope selection radio buttons
            final ButtonGroup scopeSelection = new ButtonGroup();
            scopeSelection.add(regular);
            scopeSelection.add(applicationWide);
            scopeSelection.add(global);
            
            JPanel scopeSelectionPanel = new JPanel();
            ((FlowLayout)scopeSelectionPanel.getLayout()).setVgap(0);
            scopeSelectionPanel.add(regular);
            scopeSelectionPanel.add(applicationWide);
            scopeSelectionPanel.add(global);

            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            gbc = GuiUtil.makeGbc(0, 0, 1, 1);
            JLabel actionLabel = new JLabel(Language.getString("settings.hotkeys.action"));
            actionLabel.setLabelFor(actionId);
            dialog.add(actionLabel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 0, 4, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(actionId, gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 4, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(custom, gbc);
            
            gbc = GuiUtil.makeGbc(1, 1, 4, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(anonymousCustomCommand, gbc);
            
            gbc = GuiUtil.makeGbc(1, 2, 4, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(description, gbc);
            
            gbc = GuiUtil.makeGbc(0, 3, 1, 1);
            dialog.add(new JLabel("Hotkey:"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 3, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(hotkeyChooser, gbc);

            gbc = GuiUtil.makeGbc(1, 4, 4, 1);
            gbc.anchor = GridBagConstraints.WEST;
            hotkeyInfo.setForeground(Color.red);
            gbc.insets = new Insets(-1, 5, 3, 5);
            dialog.add(hotkeyInfo, gbc);
            
            gbc = GuiUtil.makeGbc(1, 5, 3, 1);
            gbc.anchor = GridBagConstraints.WEST;
            dialog.add(scopeSelectionPanel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 6, 3, 1);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(-1, 5, 7, 5);
            dialog.add(scopeTip, gbc);
            
            JLabel delayLabel = new JLabel(Language.getString("settings.hotkeys.delay"));
            delayLabel.setToolTipText(Language.getString("settings.hotkeys.delay.tip"));
            delayLabel.setLabelFor(delay);
            gbc = GuiUtil.makeGbc(0, 7, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            dialog.add(delayLabel, gbc);
            
            gbc = GuiUtil.makeGbc(1, 7, 1, 1);
            gbc.anchor = GridBagConstraints.WEST;
            dialog.add(delay, gbc);
            
            gbc = GuiUtil.makeGbc(2, 7, 2, 1);
            gbc.anchor = GridBagConstraints.WEST;
            dialog.add(new JLabel("(1/10th seconds)"), gbc);
            
            gbc = GuiUtil.makeGbc(1, 8, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(ok, gbc);
            
            gbc = GuiUtil.makeGbc(3, 8, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.1;
            dialog.add(cancel, gbc);
            
            dialog.pack();
            dialog.setResizable(false);
        }
        
        @Override
        public Hotkey showEditor(Hotkey preset, Component c, boolean edit, int column) {
            this.preset = preset;
            
            // Set title
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            dialog.setLocationRelativeTo(c);
            
            // Initialize fields with preset values
            if (preset != null) {
                actionId.setSettingValue(preset.actionId);
                hotkeyChooser.setHotkey(preset.keyStroke);
                setCustom(preset.custom);
                delay.setText(""+preset.delay);
                setHotkeyType(preset.type);
            } else {
                actionId.setSettingValue(null);
                hotkeyChooser.setHotkey(null);
                setCustom("");
                delay.setText("0");
                setHotkeyType(null);
            }
            
            // Update other stuff
            hotkeyInfo.setText(null);
            dialog.pack();
            updateButtons();
            
            // Wait for dialog to close
            dialog.setVisible(true);
            
            // Create and return new Hotkey, or null if canceled
            if (actionId.getSettingValue() != null && currentHotkey != null) {
                String action = actionId.getSettingValue();
                return new Hotkey(action, currentHotkey, getHotkeyType(),
                        getCustom(), getDelay());
            }
            return null;
        }
        
        private boolean isAnonCommand() {
            return "custom.anonCommand".equals(actionId.getSettingValue());
        }
        
        private void setCustom(String text) {
            if (isAnonCommand()) {
                anonymousCustomCommand.setSettingValue(text);
                custom.setText("");
            }
            else {
                anonymousCustomCommand.setSettingValue("");
                custom.setText(text);
            }
        }
        
        private String getCustom() {
            if (isAnonCommand()) {
                return anonymousCustomCommand.getSettingValue();
            }
            return custom.getText();
        }
        
        private int getDelay() {
            try {
                return Integer.parseInt(delay.getText());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        
        private void setHotkeyType(Hotkey.Type type) {
            if (type == Hotkey.Type.APPLICATION) {
                applicationWide.setSelected(true);
            } else if (type == Hotkey.Type.GLOBAL) {
                global.setSelected(true);
            } else {
                // This by default if no proper value is set
                regular.setSelected(true);
            }
        }
        
        public Hotkey.Type getHotkeyType() {
            if (applicationWide.isSelected()) {
                return Hotkey.Type.APPLICATION;
            } else if (global.isSelected()) {
                return Hotkey.Type.GLOBAL;
            }
            return Hotkey.Type.REGULAR;
        }
        
        private void updateButtons() {
            String action = actionId.getSettingValue();
            
            // Custom input field
            boolean customEnabled = action != null && (action.startsWith("custom.") || action.equals("tabs.switch"));
            boolean anonCustom = isAnonCommand();
            custom.setEnabled(customEnabled);
            custom.setEditable(customEnabled);
            custom.setVisible(!anonCustom);
            anonymousCustomCommand.setVisible(anonCustom);
            
            // Scope tip
            if (action != null
                    && RECOMMENDED_GLOBAL_ACTIONS.contains(action)) {
                scopeTip.setVisible(true);
                scopeTip.setText("Global scope recommended for this action");
            }
            else if (action != null
                    && (action.startsWith("dialog.") || RECOMMENDED_APPLICATION_ACTIONS.contains(action))) {
                scopeTip.setVisible(true);
                scopeTip.setText("Application scope recommended for this action");
            }
            else {
                scopeTip.setVisible(false);
            }
            dialog.pack();
            
            // Button state
            boolean enabled = actionId.getSettingValue() != null && currentHotkey != null
                    && (!customEnabled || !getCustom().isEmpty());
            ok.setEnabled(enabled);
        }
        
        private void updateDescription() {
            if (descriptions == null) {
                return;
            }
            String text = descriptions.get(actionId.getSettingValue());
            description.setVisible(text != null);
            if (text != null) {
                description.setText(SettingConstants.HTML_PREFIX+text);
            }
            GuiUtil.packKeepCenter(dialog);
        }
        
        private void checkForGlobalHotkeyWarning() {
            if (global.isSelected() && !globalHotkeysAvailable
                    && !globalHotkeysAvailableWarningShown) {
                JOptionPane.showMessageDialog(dialog,
                        Chatty.HOTKEY ? GLOBAL_HOTKEY_WARNING_ERROR
                        : GLOBAL_HOTKEY_WARNING_VERSION,
                        "Global Hotkeys not available",
                        JOptionPane.WARNING_MESSAGE);
                globalHotkeysAvailableWarningShown = true;
            }
        }
        
        public void setActions(Map<String, String> actions) {
            actionId.clear();
            actionId.add((String)null, "--Select Action--");
            actionId.addData(actions);
            this.actionNames = actions;
            dialog.pack();
        }
        
        public void setDescriptions(Map<String, String> descriptions) {
            this.descriptions = descriptions;
            updateDescription();
        }
        
        public void setGlobalHotkeysAvailable(boolean available) {
            this.globalHotkeysAvailable = available;
        }
        
        private Hotkey getHotkeyForKeyStroke(KeyStroke keyStroke) {
            for (int i=0;i<data.getRowCount();i++) {
                Hotkey hotkey = data.get(i);
                if (hotkey.keyStroke.equals(keyStroke)) {
                    return hotkey;
                }
            }
            return null;
        }
        
    }
    
}