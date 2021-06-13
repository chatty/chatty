
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.gui.colors.ColorItem;
import chatty.lang.Language;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author tduva
 * @param <T>
 */
public class ItemColorEditor<T extends ColorItem> extends TableEditor<T> {

    private final MyTableModel data;
    private final ItemCreator<T> itemCreator;
    private final ColorRenderer colorRenderer = new ColorRenderer();
    private final MyItemEditor<T> editor;
    
    public ItemColorEditor(JDialog owner,
            ItemCreator<T> itemCreator, boolean editBackground, Component info) {
        super(SORTING_MODE_MANUAL, false);
        this.itemCreator = itemCreator;
        this.data = new MyTableModel(editBackground);
        this.editor = new MyItemEditor<>(owner, itemCreator, editBackground, info);
        
        setModel(data);
        setItemEditor(editor);
        setRendererForColumn(1, colorRenderer);
        if (editBackground) {
            setRendererForColumn(2, colorRenderer);
        }
    }
    
    public void edit(String item) {
        // Create dummy object to find index of item to edit
        T preset = itemCreator.createItem(item, Color.BLACK, true, Color.WHITE, true);
        int index = data.indexOf(preset);
        if (index == -1) {
            addItem(preset);
        } else {
            editItem(index);
        }
    }
    
    public void setSelected(String item) {
        // Create dummy object to find index of item to select
        T preset = itemCreator.createItem(item, Color.BLACK, true, Color.WHITE, true);
        int index = data.indexOf(preset);
        if (index != -1) {
            super.selectItem(index);
        }
    }
    
    public void setDefaultForeground(Color color) {
        colorRenderer.setDefaultForeground(color);
        editor.setDefaultForeground(color);
    }
   
    public void setDefaultBackground(Color color) {
        colorRenderer.setBackgroundColor(color);
        editor.setDefaultBackground(color);
    }
    
    private static class MyTableModel<T extends ColorItem> extends ListTableModel<T> {
        
        public MyTableModel(boolean editBackground) {
            super(new String[]{"Item", "Color"});
            if (editBackground) {
                setColumnNames(new String[]{Language.getString("settings.general.item"),
                                            Language.getString("settings.general.foreground"),
                                            Language.getString("settings.general.background")});
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return get(rowIndex);
        }
        
        @Override
        public String getSearchValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return get(rowIndex).getId();
            } else if (columnIndex == 1) {
                return HtmlColors.getNamedColorString(get(rowIndex).getForeground());
            } else {
                return HtmlColors.getNamedColorString(get(rowIndex).getBackground());
            }
        }
        
        @Override
        public Class getColumnClass(int c) {
            return ColorItem.class;
        }
        
    }
    
    /**
     * Renderer for the foreground and background columns.
     */
    public static class ColorRenderer extends JLabel implements TableCellRenderer {

        private Color defaultForeground;
        private Color defaultBackground;
        
        public ColorRenderer() {
            setOpaque(true);
        }
        
        public void setDefaultForeground(Color color) {
            this.defaultForeground = color;
        }
        
        public void setBackgroundColor(Color color) {
            this.defaultBackground = color;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // Just return if null
            if (value == null) {
                return this;
            }
            
            // Set colors based on what is currently set for this item
            ColorItem item = (ColorItem) value;
            Color foreground = item.getForegroundIfEnabled();
            if (foreground == null) {
                foreground = defaultForeground;
            }
            Color background = item.getBackgroundIfEnabled();
            if (background == null) {
                background = defaultBackground;
            }
            setForeground(foreground);
            setBackground(background);
            
            // Set text and tooltip text
            Color settingColor = null;
            if (column == 1) {
                settingColor = item.getForegroundIfEnabled();
            } else if (column == 2) {
                settingColor = item.getBackgroundIfEnabled();
            }
            String output;
            String tooltip;
            if (settingColor == null) {
                output = "(default)";
                tooltip = "Using default color";
            } else {
                output = HtmlColors.getNamedColorString(settingColor);
                tooltip = HtmlColors.getColorString(settingColor);
            }
            setText(output);
            setToolTipText(tooltip);
            return this;
        }

    }
    
    public static class MyItemEditor<T extends ColorItem> implements ItemEditor<T> {
        
        private final ItemCreator<T> itemCreator;
        private final boolean editBackground;
        
        private final ColorChooser colorChooser;
        private final JDialog dialog;
        private final JTextField id = new JTextField(10);
        private final JButton changeColor = new JButton("Select Color");
        private final ColorSetting foreground;
        private final ColorSetting background;
        private final JCheckBox foregroundEnabled;
        private final JCheckBox backgroundEnabled;
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        private Color defaultForeground;
        private Color defaultBackground;
        boolean save;
        
        public MyItemEditor(JDialog owner, ItemCreator itemCreator,
                boolean editBackground, Component info) {
            this.itemCreator = itemCreator;
            this.editBackground = editBackground;
            this.colorChooser = new ColorChooser(owner);
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            dialog.setModal(true);
            
            id.getDocument().addDocumentListener(new DocumentListener() {

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
            
            ActionListener listener = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == ok) {
                        dialog.setVisible(false);
                        save = true;
                    } else if (e.getSource() == cancel) {
                        dialog.setVisible(false);
                    }
                }
            };
            changeColor.addActionListener(listener);
            ok.addActionListener(listener);
            cancel.addActionListener(listener);
            
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            dialog.add(new JLabel("Item:"),
                    GuiUtil.makeGbc(0, 0, 1, 1));
            gbc = GuiUtil.makeGbc(1, 0, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            dialog.add(id, gbc);
            
            foreground = new ColorSetting(ColorSetting.FOREGROUND, null, "Foreground", "Foreground Color", colorChooser);
            background = new ColorSetting(ColorSetting.BACKGROUND, null, "Background", "Background Color", colorChooser);
            foregroundEnabled = new JCheckBox("Enabled");
            backgroundEnabled = new JCheckBox("Enabled");
            
            foreground.addListener(() -> updateColors());
            background.addListener(() -> updateColors());
            foregroundEnabled.addItemListener(e -> updateColors());
            backgroundEnabled.addItemListener(e -> updateColors());
            
            gbc = GuiUtil.makeGbc(1, 1, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            dialog.add(foreground,
                    gbc);
            if (editBackground) {
                gbc = GuiUtil.makeGbc(1, 2, 2, 1);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                dialog.add(background,
                        gbc);
                dialog.add(foregroundEnabled,
                        GuiUtil.makeGbc(0, 1, 1, 1));
                dialog.add(backgroundEnabled,
                        GuiUtil.makeGbc(0, 2, 1, 1));
            } else {
                backgroundEnabled.setSelected(false);
            }
            
            if (info != null) {
                gbc = GuiUtil.makeGbc(0, 3, 3, 1, GridBagConstraints.CENTER);
                gbc.weightx = 1;
                dialog.add(info, gbc);
            }
            
            gbc = GuiUtil.makeGbc(1, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            dialog.add(ok, gbc);
            gbc = GuiUtil.makeGbc(2, 4, 1, 1);
            dialog.add(cancel, gbc);
            
            dialog.pack();
            dialog.setResizable(false);
        }
        
        public void setDefaultForeground(Color color) {
            this.defaultForeground = color;
            updateColors();
        }
        
        public void setDefaultBackground(Color color) {
            this.defaultBackground = color;
            updateColors();
        }
        
        @Override
        public T showEditor(T preset, Component c, boolean edit, int column) {
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            dialog.setLocationRelativeTo(c);
            if (preset != null) {
                id.setText(preset.getId());
                setColors(preset.getForeground(), preset.getBackground());
                foregroundEnabled.setSelected(preset.getForegroundEnabled());
                backgroundEnabled.setSelected(preset.getBackgroundEnabled());
            } else {
                id.setText(null);
                setColors(defaultForeground, defaultBackground);
                foregroundEnabled.setSelected(true);
                backgroundEnabled.setSelected(true);
            }
            if (!editBackground) {
                background.setSettingValue(null);
                backgroundEnabled.setSelected(false);
            }
            updateColors();
            id.requestFocusInWindow();
            
            // Save will be set to true when pressing the "OK" button
            save = false;
            dialog.setVisible(true);
            if (!id.getText().isEmpty() && save) {
                return itemCreator.createItem(id.getText(),
                        foreground.getSettingValueAsColor(),
                        foregroundEnabled.isSelected(),
                        background.getSettingValueAsColor(),
                        backgroundEnabled.isSelected());
            }
            return null;
        }
        
        private void setColors(Color foregroundColor, Color backgroundColor) {
            foreground.setSettingValue(HtmlColors.getNamedColorString(foregroundColor));
            background.setSettingValue(HtmlColors.getNamedColorString(backgroundColor));
        }
        
        private void updateColors() {
            if (foregroundEnabled.isSelected()) {
                background.setBaseColor(foreground.getSettingValue());
            } else {
                background.setBaseColor(defaultForeground);
            }
            if (backgroundEnabled.isSelected()) {
                foreground.setBaseColor(background.getSettingValue());
            } else {
                foreground.setBaseColor(defaultBackground);
            }
            background.setEnabled(backgroundEnabled.isSelected());
            foreground.setEnabled(foregroundEnabled.isSelected());
            updateButtons();
        }
        
        private void updateButtons() {
            boolean enabled = !id.getText().isEmpty()
                    && (!foreground.getSettingValue().isEmpty() || !foregroundEnabled.isSelected())
                    && (!background.getSettingValue().isEmpty() || !backgroundEnabled.isSelected());
            ok.setEnabled(enabled);
        }
        
    }
    
    public interface ItemCreator<T> {
        public T createItem(String item,
                Color foreground, boolean foregroundEnabled,
                Color background, boolean backgroundEnabled);
    }
    
}
