
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.hotkeys.Hotkey;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/**
 * 
 * @author tduva
 */
public class HotkeySettings extends SettingsPanel {
    
    private final SettingsDialog d;
    private final HotkeyEditor data;
    
    private final List<HotkeyPanel> hotkeyPanels = new ArrayList<>();
    
    public HotkeySettings(SettingsDialog d) {
        super(true);
        
        this.d = d;
        
        JPanel main = addTitledPanel("Hotkeys", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(d.addSimpleBooleanSetting("globalHotkeysEnabled",
                "Enable global hotkeys",
                "Enable or disable currently defined global hotkeys"), gbc);
        
        // Use same localized formatting for keys as in the table
        JButton addTabSwitchKeys = new JButton(String.format("<html><body>Add <kbd>%s</kbd> to <kbd>%s</kbd> for switching tabs",
                Hotkey.keyStrokeToText(KeyStroke.getKeyStroke("ctrl 1")),
                Hotkey.keyStrokeToText(KeyStroke.getKeyStroke("ctrl 9"))));
        addTabSwitchKeys.setToolTipText("Add hotkeys to directly switch to a tab index in the active tab pane");
        GuiUtil.smallButtonInsets(addTabSwitchKeys);
        gbc = d.makeGbc(1, 1, 1, 1);
        gbc.insets = new Insets(5, 5, 5, 30);
        gbc.anchor = GridBagConstraints.EAST;
        main.add(addTabSwitchKeys, gbc);
        
        data = new HotkeyEditor(d, item -> {
            if (item.type == Hotkey.Type.GLOBAL
                    && !d.getBooleanSettingValue("globalHotkeysEnabled")) {
                JOptionPane.showMessageDialog(d, "You have added a global hotkey, but global hotkeys are currently disabled (see setting at bottom).");
            }
        }, new TableEditor.TableEditorListener<Hotkey>() {
            @Override
            public void itemAdded(Hotkey item) {
                updateHotkeyPanel(null, item);
            }

            @Override
            public void itemRemoved(Hotkey item) {
                updateHotkeyPanel(item, null);
            }

            @Override
            public void itemEdited(Hotkey oldItem, Hotkey newItem) {
                updateHotkeyPanel(oldItem, newItem);
            }

            @Override
            public void allItemsChanged(List<Hotkey> newItems) {
                //
            }

            @Override
            public void itemsSet() {
                
            }

            @Override
            public void refreshData() {
                
            }
        });
        data.setPreferredSize(new Dimension(1,270));
        gbc = d.makeGbc(0, 0, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        main.add(data, gbc);
        
        addTabSwitchKeys.addActionListener(e -> {
            List<Hotkey> hotkeys = data.getData();
            for (int i = 1; i <= 9; i++) {
                Hotkey hotkey = new Hotkey("tabs.switch", KeyStroke.getKeyStroke("ctrl "+i), Hotkey.Type.APPLICATION, String.valueOf(i), 0);
                boolean actionAlreadyExists = false;
                for (Hotkey existingHotkey : hotkeys) {
                    if (existingHotkey.actionId.equals(hotkey.actionId) && existingHotkey.custom.equals(hotkey.custom)) {
                        actionAlreadyExists = true;
                    }
                }
                if (!actionAlreadyExists) {
                    data.addHotkey(hotkey);
                }
            }
            data.updateConflicts();
        });
    }
    
    public void setData(Map<String, String> actions,
                        Map<String, String> descriptions,
                        List<Hotkey> hotkeys,
                        boolean globalHotkeysAvailable) {
        data.setData(actions, descriptions, hotkeys, globalHotkeysAvailable);
        
        for (Hotkey hotkey : hotkeys) {
            updateHotkeyPanel(null, hotkey);
        }
        for (HotkeyPanel panel : hotkeyPanels) {
            panel.setActions(actions);
        }
    }
    
    public List<Hotkey> getData() {
        return data.getData();
    }
    
    public void edit(String id) {
        data.edit(id);
    }
    
    public HotkeyPanel createHotkeyPanel(String actionId, Hotkey.Type type) {
        HotkeyPanel panel = new HotkeyPanel(d, actionId, type,
                                            k -> data.getExistingHotkey(k),
                                            new HotkeyPanel.HotkeyHelperListener() {
                                        @Override
                                        public void changeHotkey(Hotkey current, Hotkey changed) {
                                            List<Hotkey> currentHotkeys = getData();
                                            Hotkey toRemove = getMatchingHotkey(currentHotkeys, current);
                                            if (toRemove != null) {
                                                currentHotkeys.remove(toRemove);
                                            }
                                            currentHotkeys.add(changed);
                                            data.setData(currentHotkeys);
                                        }

                                        @Override
                                        public void deleteHotkey(Hotkey current) {
                                            List<Hotkey> currentHotkeys = getData();
                                            Hotkey toRemove = getMatchingHotkey(currentHotkeys, current);
                                            if (toRemove != null) {
                                                currentHotkeys.remove(toRemove);
                                                data.setData(currentHotkeys);
                                            }
                                        }
                                    });
        hotkeyPanels.add(panel);
        return panel;
    }
    
    private Hotkey getMatchingHotkey(Collection<Hotkey> hotkeys, Hotkey search) {
        for (Hotkey hotkey : hotkeys) {
            if (hotkey.actionId.equals(search.actionId)
                    && hotkey.keyStroke.equals(search.keyStroke)) {
                return hotkey;
            }
        }
        return null;
    }
    
    private void updateHotkeyPanel(Hotkey current, Hotkey changed) {
        for (HotkeyPanel panel : hotkeyPanels) {
            panel.updateHotkey(current, changed);
        }
    }
    
}
