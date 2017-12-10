
package chatty.gui.components.settings;

import chatty.gui.colors.UsercolorItem;
import chatty.gui.HtmlColors;
import chatty.gui.colors.ColorItem;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.swing.JButton;
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
 */
public class ItemColorEditor<T extends ColorItem> extends TableEditor<T> {

    private final MyTableModel data = new MyTableModel();
    private final BiFunction<String, Color, T> createItem;
    private final ColorRenderer colorRenderer = new ColorRenderer();
    
    public ItemColorEditor(JDialog owner,
            BiFunction<String, Color, T> createItem) {
        super(SORTING_MODE_MANUAL, false);
        this.createItem = createItem;
        setModel(data);
        setItemEditor(new MyItemEditor<T>(owner, createItem));
        setDefaultRenderer(Color.class, colorRenderer);
    }
    
    public void edit(String item) {
        T preset = createItem.apply(item, Color.BLACK);
        int index = data.indexOf(preset);
        if (index == -1) {
            addItem(preset);
        } else {
            editItem(index);
        }
    }
   
    public void setBackgroundColor(Color color) {
        colorRenderer.setBackgroundColor(color);
    }
    
    private static class MyTableModel<T extends ColorItem> extends ListTableModel<T> {
        
        public MyTableModel() {
            super(new String[]{"Item", "Color"});
        }

//        public int indexOfId(String id) {
//            return indexOf(new UsercolorItem(id, Color.BLACK));
//        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return get(rowIndex);
            } else {
                return get(rowIndex).getColor();
            }
        }
        
        @Override
        public String getSearchValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return get(rowIndex).getId();
            } else {
                return HtmlColors.getNamedColorString(get(rowIndex).getColor());
            }
        }
        
        @Override
        public Class getColumnClass(int c) {
            if (c == 0) {
                return ColorItem.class;
            } else {
                return Color.class;
            }
        }
        
    }
    
    /**
     * Renderer for cells containing a {@code Color}. Used for the color column.
     */
    public static class ColorRenderer extends JLabel implements TableCellRenderer {

        private Color backgroundColor;
        
        public ColorRenderer() {
            setOpaque(true);
        }
        
        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // Just return if null
            if (value == null) {
                return this;
            }
            
            // Set text color
            Color color = (Color) value;
            setForeground(color);
            
            // Set text and tooltip text
            String namedColorString = HtmlColors.getNamedColorString(color);
            String colorString = HtmlColors.getColorString(color);
            setText(namedColorString);
            if (namedColorString.equals(colorString)) {
                setToolTipText(colorString);
            } else {
                setToolTipText(namedColorString+" ("+colorString+")");
            }
            
            // Set background color based on selection status
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(backgroundColor);
            }
            return this;
        }

    }
    
    public static class MyItemEditor<T extends ColorItem> implements ItemEditor<T> {
        
        private final BiFunction<String, Color, T> createItem;
        
        private final ColorChooser colorChooser;
        private final JDialog dialog;
        private final JTextField id = new JTextField(10);
        private final JTextField color = new JTextField(10);
        private final JButton changeColor = new JButton("Select Color");
        private Color currentColor;
        
        private final JButton ok = new JButton("Done");
        private final JButton cancel = new JButton("Cancel");
        
        public MyItemEditor(JDialog owner, BiFunction<String, Color, T> createItem) {
            colorChooser = new ColorChooser(owner);
            dialog = new JDialog(owner);
            dialog.setTitle("Edit Item");
            color.setEditable(false);
            dialog.setModal(true);
            this.createItem = createItem;
            
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
                    } else if (e.getSource() == cancel) {
                        currentColor = null;
                        dialog.setVisible(false);
                    } else if (e.getSource() == changeColor) {
                        Color newColor = colorChooser.chooseColor(ColorChooser.FOREGROUND, currentColor, Color.WHITE, "Test", "Test");
                        updateColor(newColor);
                    }
                }
            };
            changeColor.addActionListener(listener);
            ok.addActionListener(listener);
            cancel.addActionListener(listener);
            
            dialog.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            gbc.insets = new Insets(3, 3, 3, 3);
            dialog.add(id, gbc);
            gbc.gridx = 1;
            dialog.add(color, gbc);
            gbc.gridx = 2;
            changeColor.setMargin(new Insets(0,8,0,8));
            dialog.add(changeColor, gbc);
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(7, 2, 4, 2);
            dialog.add(ok, gbc);
            gbc.gridx = 1;
            dialog.add(cancel, gbc);
            dialog.pack();
            dialog.setResizable(false);
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
                updateColor(preset.getColor());
            } else {
                id.setText(null);
                color.setText(null);
            }
            updateButtons();
            id.requestFocusInWindow();
            dialog.setVisible(true);
            if (!id.getText().isEmpty() && currentColor != null) {
                return createItem.apply(id.getText(), currentColor);
            }
            return null;
        }
        
        private void updateColor(Color newColor) {
            color.setForeground(newColor);
            currentColor = newColor;
            color.setText(HtmlColors.getNamedColorString(newColor));
            updateButtons();
        }
        
        private void updateButtons() {
            boolean enabled = !id.getText().isEmpty() && currentColor != null;
            ok.setEnabled(enabled);
        }
        
    }
    
}
