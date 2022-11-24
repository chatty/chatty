
package chatty.gui.transparency;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.admin.AdminDialog;
import chatty.gui.components.settings.SettingsUtil;
import chatty.util.dnd.DockContent;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JToggleButton;

/**
 *
 * @author tduva
 */
public class TransparencyDialog extends JDialog {
    
    private static TransparencyDialog instance;
    
    public static TransparencyDialog instance(MainGui main) {
        if (instance == null) {
            instance = new TransparencyDialog(main);
        }
        return instance;
    }
    
    public static DockContent selectedContent() {
        if (instance != null) {
            return (DockContent) instance.selection.getSelectedItem();
        }
        return null;
    }

    private final JComboBox<DockContent> selection = new JComboBox<>();
    private final JButton toggleButton = new JButton("Toggle Transparency");
    private final JCheckBox clickThroughNative = new JCheckBox("Click-through (Windows OS only)");
    private final JComboBox<Integer> colorTransparency = new JComboBox<>();
    private final JButton refreshButton = new JButton(new ImageIcon(AdminDialog.class.getResource("view-refresh.png")));
    private final JToggleButton toggleHelpButton = new JToggleButton("Help");
    private final JLabel help = new JLabel("<html><body style='width:320px;'>"+SettingsUtil.getInfo("info-transparency.html", null));
    private final JButton setHotkeyButton = new JButton("Set hotkey");
    
    private TransparencyDialog(MainGui main) {
        super(main);
        setTitle("Transparency");
        
        setLayout(new GridBagLayout());
        
        for (int i=10;i<=100;i+=10) {
            colorTransparency.addItem(i);
        }
        
        clickThroughNative.setToolTipText("The transparent window ignores any mouse clicks.");
        
        GridBagConstraints gbc;
        
        gbc = GuiUtil.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(selection, gbc);
        add(refreshButton, GuiUtil.makeGbc(1, 0, 1, 1));
        add(toggleButton, GuiUtil.makeGbc(2, 0, 1, 1));
        add(clickThroughNative, GuiUtil.makeGbc(0, 1, 2, 1, GridBagConstraints.EAST));
        add(setHotkeyButton, GuiUtil.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST));
        add(new JLabel("Window Background Transparency (%):"), GuiUtil.makeGbc(0, 2, 2, 1, GridBagConstraints.EAST));
        add(colorTransparency, GuiUtil.makeGbc(2, 2, 1, 1, GridBagConstraints.WEST));
        add(toggleHelpButton, GuiUtil.makeGbc(0, 10, 1, 1, GridBagConstraints.WEST));
        add(help, GuiUtil.makeGbc(0, 11, 3, 1, GridBagConstraints.WEST));
        help.setVisible(false);
        
        toggleButton.addActionListener(e -> {
            DockContent selectedContent = (DockContent) selection.getSelectedItem();
            if (selectedContent != null) {
                if (TransparencyManager.getCurrent() != selectedContent) {
                    TransparencyManager.setTransparent(selectedContent);
                }
                else {
                    TransparencyManager.toggleTransparent();
                }
            }
        });
        
        clickThroughNative.addItemListener(e -> {
            TransparencyManager.setClickThrough(clickThroughNative.isSelected());
        });
        
        colorTransparency.addItemListener(e -> {
            if (colorTransparency.getSelectedItem() != null) {
                TransparencyManager.setColorTransparency((Integer) colorTransparency.getSelectedItem());
            }
        });
        
        refreshButton.addActionListener(e -> {
            refresh();
        });
        
        toggleHelpButton.addActionListener(e -> {
            help.setVisible(toggleHelpButton.isSelected());
            pack();
        });
        
        setHotkeyButton.addActionListener(e -> {
            main.getSettingsDialog(s -> s.showSettings("editHotkey", "dialog.toggleTransparency"));
        });
        
        clickThroughNative.setSelected(TransparencyManager.getClickThrough());
        colorTransparency.setSelectedItem(TransparencyManager.getColorTransparency());
    }
    
    public void refresh() {
        selection.removeAllItems();
        selection.setRenderer(new DefaultListCellRenderer() {
            
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText(((DockContent)value).getTitle());
                }
                return this;
            }
            
        });
        for (DockContent content : TransparencyManager.getEligible()) {
            selection.addItem(content);
        }
        if (TransparencyManager.getCurrent() == null) {
            selection.setSelectedItem(TransparencyManager.getCurrentById());
        }
        else {
            selection.setSelectedItem(TransparencyManager.getCurrent());
        }
        pack();
    }
    
}
