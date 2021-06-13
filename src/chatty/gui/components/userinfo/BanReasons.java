
package chatty.gui.components.userinfo;

import chatty.gui.components.settings.Editor;
import chatty.gui.components.settings.GenericComboSetting;
import chatty.gui.components.settings.SettingsDialog;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *
 * @author tduva
 */
public class BanReasons extends JPanel {
        
        private final GenericComboSetting<String> combo = new GenericComboSetting<>();
        private final Settings settings;
        
        private final Editor settingEditor;
        private final JTextField customReasonInput = new JTextField();
        
        private final GridBagConstraints customInputGbc;
        
        private String currentReasons;

        private final int[] codes = new int[]{KeyEvent.VK_1,
            KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
            KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
        
        public BanReasons(final Window parent, Settings settings) {
            this.settings = settings;
            
            setLayout(new GridBagLayout());

            JButton editButton = new JButton();
            editButton.setIcon(new ImageIcon(SettingsDialog.class.getResource("edit.png")));
            editButton.setMargin(new Insets(0,2,0,2));
            editButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    editReasons();
                }
            });
            
            settingEditor = new Editor(parent);
            settingEditor.setAllowEmpty(true);
            settingEditor.setAllowLinebreaks(true);
            
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(combo, gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.VERTICAL;
            add(editButton, gbc);
            customInputGbc = new GridBagConstraints();
            customInputGbc.gridx = 0;
            customInputGbc.gridy = 1;
            customInputGbc.gridwidth = 2;
            customInputGbc.fill = GridBagConstraints.HORIZONTAL;
            add(customReasonInput, customInputGbc);
            
            /**
             * Custom renderer to display the value of the items for the
             * selected item, instead of the label (hide the shortcut).
             */
            combo.setRenderer(new BasicComboBoxRenderer() {
            
                @Override
                public Component getListCellRendererComponent(JList list,
                        Object value, int index, boolean isSelected,
                        boolean hasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
                    
                    if (index == -1 && value != null) {
                        String text = ((GenericComboSetting.Entry<String>)value).value;
                        if (text != null && !text.isEmpty()) {
                            setText(text);
                        }
                    }
                    return this;
                }
            
            });
            
            /**
             * Add/remove custom reason input box if last item is selected.
             */
            combo.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (combo.getItemCount() > 1 && combo.getSelectedIndex() == combo.getItemCount() - 1) {
                        addCustomInput();
                    } else {
                        removeCustomInput();
                    }
                    
                }
            });
            
            /**
             * When the popup is open, allow shortcuts to select items.
             */
            combo.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    if (!combo.isPopupVisible()) {
                        return;
                    }
                    for (int i=0;i<codes.length;i++) {
                        if (codes[i] == e.getKeyCode()) {
                            int indexToSelect = i+1;
                            if (combo.getItemCount() > indexToSelect+1) {
                                combo.setPopupVisible(false);
                                combo.setSelectedIndex(indexToSelect);
                            }
                            e.consume();
                        }
                    }
                    if (e.getKeyCode() == KeyEvent.VK_C) {
                        combo.setSelectedIndex(combo.getItemCount() - 1);
                        e.consume();
                    }
                }
            });
            
            updateHotkey();
            combo.getActionMap().put(combo, new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (parent.getFocusOwner().getClass() == JTextField.class) {
                        return;
                    }
                    if (!combo.isPopupVisible() && combo.getSelectedIndex() != 0) {
                        combo.setSelectedIndex(0);
                    }
                    combo.requestFocusInWindow();
                    combo.setPopupVisible(!combo.isPopupVisible());
                }
            });
            customReasonInput.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
                }
            });
            customReasonInput.addKeyListener(new KeyAdapter() {
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        combo.requestFocusInWindow();
                        combo.setPopupVisible(true);
                        combo.setSelectedIndex(combo.getItemCount() - 2);
                    }
                }
            });
        }
        
        private void editReasons() {
            String currentReasons = settings.getString("banReasons");
            String editedReasons = settingEditor.showDialog(
                    Language.getString("userDialog.editBanReasons"),
                    currentReasons,
                    null);
            if (editedReasons != null) {
                settings.setString("banReasons", editedReasons);
                updateReasonsFromSettings();
            }
        }
        
        public void updateReasonsFromSettings() {
            String reasons = settings.getString("banReasons");
            if (reasons.equals(currentReasons)) {
                return;
            }
            String[] split = reasons.split("\n");
            combo.removeAllItems();
//            combo.add("", "Select a Ban Reason (optional) [R]");
            combo.add("", Language.getString("userDialog.selectBanReason"));
            for (int i=0;i<split.length;i++) {
                if (!split[i].trim().isEmpty()) {
                    String shortcut = "-";
                    if (codes.length > i) {
                        shortcut = KeyEvent.getKeyText(codes[i]);
                    }
                    combo.add(split[i], "["+shortcut+"] "+split[i]);
                }
            }
            combo.add("[C] "+Language.getString("userDialog.customReason"));
            currentReasons = reasons;
        }
        
        public String getSelectedReason() {
            int index = combo.getSelectedIndex();
            if (index == 0 || index == -1) {
                return "";
            }
            if (index == combo.getItemCount() - 1) {
                return customReasonInput.getText();
            }
            return combo.getSettingValue();
        }
        
        public void reset() {
            if (combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            }
        }
        
        public void addCustomInput() {
            add(customReasonInput, customInputGbc);
            revalidate();
            customReasonInput.requestFocusInWindow();
            customReasonInput.setSelectionStart(0);
            customReasonInput.setSelectionEnd(customReasonInput.getText().length());
        }
        
        public void removeCustomInput() {
            remove(customReasonInput);
            revalidate();
        }
        
        public void updateHotkey() {
            combo.getInputMap(WHEN_IN_FOCUSED_WINDOW).clear();
            KeyStroke openReasonsHotkey = KeyStroke.getKeyStroke(settings.getString("banReasonsHotkey"));
            if (openReasonsHotkey != null) {
                combo.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(openReasonsHotkey, combo);
            }
        }
        
    }