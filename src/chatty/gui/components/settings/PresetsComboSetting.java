
package chatty.gui.components.settings;

import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.awt.BorderLayout;
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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * Combo setting where the preset values are read from the settings.
 * 
 * @author tduva
 */
public class PresetsComboSetting<E> extends JPanel {

    private final GenericComboSetting<E> combo = new GenericComboSetting<>();
    private final Settings settings;

    private final Editor settingEditor;
    private final JTextField customValueInput = new JTextField();

    private final GridBagConstraints customInputGbc;

    private final String settingName;

    private final Function<String, E> stringToValue;
    private final Function<E, String> valueToString;

    private final String defaultLabel;
    private final String customValueLabel;
    private final boolean customInputEnabled;
    
    private final boolean shortcuts;
    
    private final int[] codes = new int[]{KeyEvent.VK_1,
        KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
        KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
    
    private final Set<ActionListener> listeners = new HashSet<>();
    private boolean isUpdating;

    /**
     * 
     * 
     * @param parent
     * @param settings
     * @param settingName The setting the presets are saved in
     * @param stringToValue Parse a value from a string (for loading presets)
     * @param valueToString Turn a value into a display string
     * @param defaultLabel Add a default entry with value null
     * @param customValueLabel Add an entry that shows an inputbox when selected
     * that allows entering a custom value
     * @param shortcuts Whether to show shortcuts for the entries
     */
    public PresetsComboSetting(final Window parent, Settings settings, String settingName,
                               Function<String, E> stringToValue,
                               Function<E, String> valueToString,
                               String defaultLabel,
                               String customValueLabel,
                               boolean shortcuts) {
        this.settings = settings;
        this.settingName = settingName;
        this.stringToValue = stringToValue;
        this.valueToString = valueToString;
        this.defaultLabel = defaultLabel;
        this.customValueLabel = customValueLabel;
        this.shortcuts = shortcuts;
        this.customInputEnabled = customValueLabel != null;

        setLayout(new GridBagLayout());

        JButton editButton = new JButton();
        editButton.setIcon(new ImageIcon(SettingsDialog.class.getResource("edit.png")));
        editButton.setMargin(new Insets(0, 2, 0, 2));
        editButton.addActionListener((ActionEvent e) -> {
            editPresets();
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
        add(customValueInput, customInputGbc);

        /**
         * Custom renderer to display the value of the items for the selected
         * item, instead of the label (hide the shortcut).
         */
        combo.setRenderer(new BasicComboBoxRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list,
                                                          Object value, int index, boolean isSelected,
                                                          boolean hasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);

                if (index == -1 && value != null) {
                    String text = valueToString.apply(((GenericComboSetting.Entry<E>) value).value);
                    if (text != null && !text.isEmpty()) {
                        setText(text);
                    }
                }
                return this;
            }

        });

        /**
         * Add/remove custom value input box if last item is selected.
         */
        combo.addActionListener((ActionEvent e) -> {
            if (customInputEnabled && combo.getItemCount() > 1 && combo.getSelectedIndex() == combo.getItemCount() - 1) {
                addCustomInput();
            }
            else {
                removeCustomInput();
            }
            if (!isUpdating) {
                listeners.forEach(l -> l.actionPerformed(e));
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
                for (int i = 0; i < codes.length; i++) {
                    if (codes[i] == e.getKeyCode()) {
                        int indexToSelect = i;
                        if (defaultLabel != null) {
                            indexToSelect++;
                        }
                        int indexToCheck = indexToSelect;
                        if (customInputEnabled) {
                            indexToCheck++;
                        }
                        if (combo.getItemCount() > indexToCheck) {
                            combo.setPopupVisible(false);
                            combo.setSelectedIndex(indexToSelect);
                        }
                        e.consume();
                    }
                }
                if (e.getKeyCode() == KeyEvent.VK_C && customInputEnabled) {
                    combo.setSelectedIndex(combo.getItemCount() - 1);
                    e.consume();
                }
            }
        });
        
        customValueInput.addActionListener((ActionEvent e) -> {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
        });
        customValueInput.addKeyListener(new KeyAdapter() {

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

    public void init() {
        updatePresetsFromSettings();
        reset();
    }

    private void editPresets() {
        String currentPresets = settings.getString(settingName);
        String editedPresets = settingEditor.showDialog(
                Language.getString("settings.editor." + settingName),
                currentPresets,
                null);
        if (editedPresets != null) {
            settings.setString(settingName, editedPresets);
            updatePresetsFromSettings();
        }
    }

    public void updatePresetsFromSettings() {
        isUpdating = true;
        
        E selectedBefore = getSelectedValue();
        
        String values = settings.getString(settingName);
        String[] split = values.split("\n");
        combo.removeAllItems();
        if (defaultLabel != null) {
            combo.add(stringToValue.apply(null), defaultLabel);
        }
        for (int i = 0; i < split.length; i++) {
            if (!split[i].trim().isEmpty()) {
                String shortcut = "-";
                if (codes.length > i) {
                    shortcut = KeyEvent.getKeyText(codes[i]);
                }
                E value = stringToValue.apply(split[i]);
                String shortcutLabel = "";
                if (shortcuts) {
                    shortcutLabel = "[" + shortcut + "] ";
                }
                combo.add(value, shortcutLabel + valueToString.apply(value));
            }
        }
        if (customInputEnabled) {
            combo.add(stringToValue.apply(""), "[C] " + customValueLabel);
        }
        
        if (selectedBefore != null) {
            setSelectedValue(selectedBefore);
        }
        
        isUpdating = false;
    }

    public E getSelectedValue() {
        int index = combo.getSelectedIndex();
        if ((index == 0 && customInputEnabled) || index == -1) {
            return null;
        }
        if (customInputEnabled && index == combo.getItemCount() - 1) {
            return stringToValue.apply(customValueInput.getText());
        }
        return combo.getSettingValue();
    }
    
    public void setSelectedValue(E value) {
        isUpdating = true;
        if (!combo.containsValue(value)) {
            combo.add(value, valueToString.apply(value));
        }
        combo.setSettingValue(value);
        isUpdating = false;
    }

    private void reset() {
        isUpdating = true;
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
        isUpdating = false;
    }

    private void addCustomInput() {
        add(customValueInput, customInputGbc);
        revalidate();
        customValueInput.requestFocusInWindow();
        customValueInput.setSelectionStart(0);
        customValueInput.setSelectionEnd(customValueInput.getText().length());
    }

    private void removeCustomInput() {
        remove(customValueInput);
        revalidate();
    }
    
    public void addChangeListener(ActionListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * For testing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Settings settings = new Settings("", null);
            settings.addString("testSetting", "1\n2\n3");
            
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            PresetsComboSetting<Long> settingPanel = new PresetsComboSetting<>(frame, settings, "testSetting",
                                                                               s -> {
                                                                                   if (s == null) {
                                                                                       return -2L;
                                                                                   }
                                                                                   if (s.isEmpty()) {
                                                                                       return -1L;
                                                                                   }
                                                                                   return Long.valueOf(s);
                                                                               },
                    v -> String.valueOf(v),
                "Relatively wide default label", "Custom value", false);
            settingPanel.init();
            
            frame.add(settingPanel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

}
