
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.Usericon;
import chatty.Usericon.Type;
import chatty.gui.GuiUtil;
import chatty.util.MiscUtil;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Table to add/remove/edit usericons (badges).
 * 
 * @author tduva
 */
class UsericonEditor extends TableEditor<Usericon> {
    
    private static final Map<Usericon.Type, String> typeNames;
    
    public UsericonEditor(JDialog owner) {
        super(SORTING_MODE_MANUAL, false);
        
        setModel(new MyTableModel());
        setItemEditor(new MyItemEditor(owner));
        setRendererForColumn(1, new IdRenderer(getForeground()));
        setRendererForColumn(2, new ImageRenderer());
        setRendererForColumn(3, new ChannelRenderer(getForeground()));
    }
    
    /**
     * Names for the usericon types.
     */
    static {
        typeNames = new LinkedHashMap<>();
        typeNames.put(Usericon.Type.ADDON, "Addon");
        typeNames.put(Usericon.Type.MOD, "Moderator");
        typeNames.put(Usericon.Type.SUB, "Subscriber");
        typeNames.put(Usericon.Type.TURBO, "Turbo");
        typeNames.put(Usericon.Type.ADMIN, "Admin");
        typeNames.put(Usericon.Type.STAFF, "Staff");
        typeNames.put(Usericon.Type.BROADCASTER, "Broadcaster");
        typeNames.put(Usericon.Type.GLOBAL_MOD, "Global Moderator");
        typeNames.put(Usericon.Type.BOT, "Bot");
    }
    
    private static String getTypeName(Type type) {
        return type.label;
    }
    
    /**
     * The table model defining the columns and what data is returned on which
     * column.
     */
    private static class MyTableModel extends ListTableModel<Usericon> {

        public MyTableModel() {
            super(new String[]{"Type","Restriction","Image","Channel"});
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Usericon icon = get(rowIndex);
            if (columnIndex == 0) {
                return getTypeName(icon.type);
            } else if (columnIndex == 1) {
                return icon;
            } else if (columnIndex == 2) {
                return icon;
            } else if (columnIndex == 3) {
                return icon;
            }
            return null;
        }
        
        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex >= 1) {
                return Usericon.class;
            } else if (columnIndex == 2) {
                return ImageIcon.class;
            } else {
                return String.class;
            }
        }
        
    }
    
    /**
     * The renderer for the restriction column, which shows whether the value
     * is valid.
     */
    private static class IdRenderer extends DefaultTableCellRenderer {
        
        private final Color defaultColor;
        
        IdRenderer(Color defaultColor) {
            this.defaultColor = defaultColor;
        }
        
        @Override
        public void setValue(Object value) {
            Usericon icon = (Usericon) value;
            
            if (icon.matchType == Usericon.MatchType.UNDEFINED) {
                setText(icon.restriction+" (error)");
                setForeground(Color.red);
            } else {
                setText(icon.restrictionValue
                        +(icon.first ? " (first)" : "")
                        +(icon.stop ? " (stop)" : ""));
                setForeground(defaultColor);
            }
        }
        
    }
    
    /**
     * The renderer for the image column, displaying the image or the reference
     * to another icon if the filename starts with "$".
     */
    private static class ImageRenderer extends DefaultTableCellRenderer {

        ImageRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }
        
        @Override
        public void setValue(Object value) {
            Usericon icon = (Usericon) value;
            
            if (icon.fileName != null && icon.fileName.startsWith("$")) {
                setIcon(null);
                setText(icon.fileName.substring(1));
            } else {
                setIcon(icon.image);
                setText(null);
            }
        }
    }
    
    /**
     * The renderer for the channel restriction column, showing if the channel
     * value is valid and displaying the "NOT" if used.
     */
    private static class ChannelRenderer extends DefaultTableCellRenderer {
        
        private final Color defaultColor;
        
        ChannelRenderer(Color defaultColor) {
            this.defaultColor = defaultColor;
        }
        
        @Override
        public void setValue(Object value) {
            Usericon icon = (Usericon) value;
            
            if (!icon.channelRestriction.isEmpty() && icon.channel.isEmpty()) {
                setText(icon.channelRestriction+" (error)");
                setForeground(Color.red);
            } else {
                setText((icon.channelInverse ? "NOT " : "") + icon.channel);
                setForeground(defaultColor);
            }
        }
        
    }
    
    /**
     * The editor for a single usericon, which does the most work here, having
     * to load a list of icons for use, creating the icon when selected,
     * updating the preview and so on.
     */
    private static class MyItemEditor implements ItemEditor<Usericon> {

        private static final String ERROR_LOADING_IMAGE = "Error loading image.";
        
        private final JDialog dialog;
        
        private final JComboBox<String> fileName;
        private final GenericComboSetting<Type> type;
        private final JTextField id = new JTextField();
        private final JTextField stream = new JTextField();
        
        private final JButton okButton = new JButton("Done");
        private final JButton cancelButton = new JButton("Cancel");
        private final JButton openDir = new JButton("Open dir");
        private final JButton scanDir = new JButton("Rescan");
        
        private final JPanel folderPanel;
        
        private final JLabel scanResult = new JLabel(ERROR_LOADING_IMAGE);
        
        private final JLabel preview = new JLabel();
        private boolean save;
        private Usericon currentIcon;
        
        public MyItemEditor(Window owner) {
            dialog = new JDialog(owner);
            dialog.setLayout(new GridBagLayout());
            dialog.setResizable(false);
            dialog.setModal(true);
            
            type = new GenericComboSetting<>(typeNames);
            type.setToolTipText("Choosing a type other than Addon replaces the corresponding default icon.");
            
            fileName = new JComboBox<>();
            fileName.setEditable(true);
            fileName.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    //System.out.println(e);
                    if (e.getActionCommand().equals("comboBoxChanged")) {
                        createIcon(true);
                        updatePreview();
                        updateOkButton();
                    }
                }
            });
            
//            ActionListener changeListener = new ActionListener() {
//
//                @Override
//                public void actionPerformed(ActionEvent e) {
//                    System.out.println("changed");
//                    updateOkButton();
//                }
//            };
//            type.addActionListener(changeListener);
//            fileName.addActionListener(changeListener);

            GridBagConstraints gbc;
            
            folderPanel = createFolderPanel();
            folderPanel.setVisible(false);
            gbc = GuiUtil.makeGbc(0, 1, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            dialog.add(folderPanel, gbc);

            gbc = GuiUtil.makeGbc(0, 0, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            dialog.add(createMainPanel(), gbc);
            
            gbc = GuiUtil.makeGbc(0, 6, 1, 1);
            dialog.add(new JLabel(), gbc);
            
            gbc = GuiUtil.makeGbc(1, 6, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.7;
            dialog.add(okButton, gbc);

            gbc = GuiUtil.makeGbc(2, 6, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.3;
            dialog.add(cancelButton, gbc);
            
            ActionListener buttonAction = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (e.getSource() == okButton) {
                        save = true;
                    }
                    if (e.getSource() == okButton || e.getSource() == cancelButton) {
                        dialog.setVisible(false);
                    }
                    else if (e.getSource() == openDir) {
                        MiscUtil.openFolder(new File(Chatty.getImageDirectory()), dialog);
                    }
                    else if (e.getSource() == scanDir) {
                        scanFiles();
                    }
                }
            };
            okButton.addActionListener(buttonAction);
            cancelButton.addActionListener(buttonAction);
            openDir.addActionListener(buttonAction);
            scanDir.addActionListener(buttonAction);
            
            dialog.pack();
        }
        
        private JPanel createFolderPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder("Image Source"));
            GridBagConstraints gbc;
            
            gbc = GuiUtil.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST);
            panel.add(new JLabel("Chatty looks for image files (.png) in this "
                    + "folder:  "), gbc);
        
            gbc = GuiUtil.makeGbc(0, 1, 3, 1);
            JTextField path = new JTextField(Chatty.getImageDirectory());
            path.setEditable(false);
            path.setPreferredSize(new Dimension(0, path.getPreferredSize().height));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            panel.add(path, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            panel.add(scanResult, gbc);
            
            scanDir.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            gbc = GuiUtil.makeGbc(1, 2, 1, 1);
            panel.add(scanDir, gbc);
            
            openDir.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            gbc = GuiUtil.makeGbc(2, 2, 1, 1);
            panel.add(openDir, gbc);

            return panel;
        }
        
        private JPanel createMainPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            panel.add(new JLabel("Type:"), GuiUtil.makeGbc(0, 1, 1, 1));
            
            
            gbc = GuiUtil.makeGbc(1, 1, 2, 1, GridBagConstraints.WEST);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(type, gbc);

            panel.add(new JLabel("Restriction:"), GuiUtil.makeGbc(0, 3, 1, 1));
            
            gbc = GuiUtil.makeGbc(1, 3, 2, 1, GridBagConstraints.WEST);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            //id.setColumns(14);
            panel.add(id, gbc);
            
            panel.add(new JLabel("Channel:"), GuiUtil.makeGbc(0, 4, 1, 1));
            
            gbc = GuiUtil.makeGbc(1, 4, 2, 1, GridBagConstraints.WEST);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            //stream.setColumns(14);
            panel.add(stream, gbc);
            
            panel.add(new JLabel("Image File:"), GuiUtil.makeGbc(0, 5, 1, 1));
            
            
            gbc = GuiUtil.makeGbc(1, 5, 2, 1, GridBagConstraints.WEST);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(fileName, gbc);

            panel.add(new JLabel("Preview:"), GuiUtil.makeGbc(0, 6, 1, 1));
            
            gbc = GuiUtil.makeGbc(1, 6, 1, 1, GridBagConstraints.WEST);
            panel.add(preview, gbc);
            
            final JToggleButton sourceInfoButton = new JToggleButton("Image Folder");
            sourceInfoButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    folderPanel.setVisible(sourceInfoButton.isSelected());
                    updateSize();
                }
            });
            sourceInfoButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            gbc = GuiUtil.makeGbc(2, 6, 1, 1);
            panel.add(sourceInfoButton, gbc);

            return panel;
        }
        
        private void createIcon(boolean preview) {
            String file = (String)fileName.getSelectedItem();
            if (preview) {
                currentIcon = Usericon.createCustomIcon(Type.UNDEFINED, null, file, null);
            } else if (type.getSettingValue() != null) {
                currentIcon = Usericon.createCustomIcon(type.getSettingValue(), id.getText(), file, stream.getText());
            } else {
                currentIcon = null;
            }
        }
        
        private void updateOkButton() {
            okButton.setEnabled(currentIcon != null && type.getSettingValue() != null);
        }
        
        /**
         * Sets the icon preview and the text depending on the state of the
         * current icon.
         */
        private void updatePreview() {
            preview.setText(null);
            preview.setIcon(null);
            if (currentIcon == null) {
                preview.setText("No image.");
            } else if (currentIcon.fileName.startsWith("$")) {
                preview.setText("Ref image.");
            } else if (currentIcon.image == null) {
                preview.setText(ERROR_LOADING_IMAGE);
            } else {
                ImageIcon image = currentIcon.image;
                preview.setIcon(image);
                preview.setText(image.getIconWidth()+"x"+image.getIconHeight());
            }
            updateSize();
        }
        
        private void updateSize() {
            dialog.pack();
        }
        
        @Override
        public Usericon showEditor(Usericon preset, Component c, boolean edit) {
            scanFiles();
            if (edit) {
                dialog.setTitle("Edit item");
            } else {
                dialog.setTitle("Add item");
            }
            if (preset != null) {
                id.setText(preset.restriction);
                type.setSettingValue(preset.type);
                fileName.setSelectedItem(preset.fileName);
                stream.setText(preset.channelRestriction);
                currentIcon = preset;
            } else {
                id.setText(null);
                type.setSelectedIndex(0);
                fileName.setSelectedItem(null);
                stream.setText(null);
                currentIcon = null;
            }
            updatePreview();
            
            save = false;
            
            
            dialog.setLocationRelativeTo(c);
            dialog.setVisible(true);
            // Modal dialog, so blocks here and stuff can be changed via the GUI
            // until the dialog is closed
            
            if (save) {
                createIcon(false);
                return currentIcon;
            }
            return null;
        }
        
        private void scanFiles() {
            File file = new File(Chatty.getImageDirectory());
            File[] files = file.listFiles(new ImageFilenameFilter());
            String resultText = "";
            if (files == null) {
                resultText = "Error scanning folder.";
            }
            else {
                if (files.length == 0) {
                    resultText = "No files found.";
                } else {
                    resultText = files.length + " files found.";
                }
                String[] fileNames = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    fileNames[i] = files[i].getName();
                }
                Arrays.sort(fileNames);
                
                String selected = (String) fileName.getSelectedItem();
                fileName.removeAllItems();
                for (String item : fileNames) {
                    fileName.addItem(item);
                }
                fileName.setSelectedItem(selected);
            }
            scanResult.setText(resultText);
        }
    }

    private static class ImageFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".png")) {
                return true;
            }
            return false;
        }
    }
    
}
