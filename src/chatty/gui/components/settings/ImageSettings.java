
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import static chatty.gui.components.settings.EmoteSettings.makeScaleValues;
import chatty.lang.Language;
import chatty.util.api.usericons.Usericon;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class ImageSettings extends SettingsPanel {
    
    private final UsericonEditor usericonsData;
    private final HiddenBadgesDialog hiddenBadgesDialog;
    
    public ImageSettings(SettingsDialog d) {
        super(true);
        
        GridBagConstraints gbc;
        
        JPanel usericons = addTitledPanel(Language.getString("settings.section.usericons"), 0, false);
        JPanel custom = addTitledPanel(Language.getString("settings.section.customUsericons"), 1, true);

        JCheckBox usericonsEnabled = d.addSimpleBooleanSetting("usericonsEnabled");
        JCheckBox botBadgeEnabled = d.addSimpleBooleanSetting("botBadgeEnabled");
        JCheckBox customUsericonsEnabled = d.addSimpleBooleanSetting("customUsericonsEnabled");
        
        //==================
        // General Settings
        //==================
        gbc = d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST);
        usericons.add(usericonsEnabled, gbc);

        gbc = d.makeGbc(2, 0, 1, 1, GridBagConstraints.WEST);
        usericons.add(botBadgeEnabled, gbc);
        
        gbc = d.makeGbc(0, 1, 4, 1, GridBagConstraints.CENTER);
        usericons.add(new JLabel(Language.getString("settings.ffzBadgesInfo")), gbc);
        
        ComboLongSetting usericonScale = new ComboLongSetting(makeScaleValues());
        d.addLongSetting("usericonScale", usericonScale);
        SettingsUtil.addLabeledComponent(usericons, "usericonScale", 0, 2, 2, GridBagConstraints.WEST, usericonScale);
        
        ComboLongSetting customUsericonScaleMode = d.addComboLongSetting("customUsericonScaleMode", 0, 1, 2);
        d.addLongSetting("customUsericonScaleMode", customUsericonScaleMode);
        SettingsUtil.addLabeledComponent(usericons, "customUsericonScaleMode", 0, 3, 2, GridBagConstraints.WEST, customUsericonScaleMode);
        
        hiddenBadgesDialog = new HiddenBadgesDialog(d);
        JButton hiddenBadgesButton = new JButton("View Hidden Badges");
        GuiUtil.smallButtonInsets(hiddenBadgesButton);
        hiddenBadgesButton.addActionListener(e -> {
            hiddenBadgesDialog.show(d);
        });
        usericons.add(hiddenBadgesButton,
                d.makeGbc(3, 0, 1, 1, GridBagConstraints.WEST));
        
        //==================
        // Custom Usericons
        //==================
        gbc = d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST);
        custom.add(customUsericonsEnabled, gbc);
        
        usericonsData = new UsericonEditor(d, d.getLinkLabelListener());
        usericonsData.setPreferredSize(new Dimension(150, 250));
        gbc = d.makeGbc(0, 2, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        custom.add(usericonsData, gbc);
        
        gbc = d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        custom.add(new JLabel(Language.getString("settings.customUsericons.info")), gbc);
        
        SettingsUtil.addSubsettings(usericonsEnabled, customUsericonsEnabled, botBadgeEnabled);
        SettingsUtil.addSubsettings(customUsericonsEnabled, usericonsData);
    }
    
    public void setData(List<Usericon> data) {
        usericonsData.setData(data);
    }
    
    public void setTwitchBadgeTypes(Set<String> data) {
        usericonsData.setTwitchBadgeTypes(data);
    }
    
    public List<Usericon> getData() {
        return usericonsData.getData();
    }
    
    public void setHiddenBadgesData(Collection<Usericon> data) {
        hiddenBadgesDialog.setData(data);
    }
    
    public List<Usericon> getHiddenBadgesData() {
        return hiddenBadgesDialog.getData();
    }
    
    public void addUsericonOfBadgeType(Usericon.Type type, String idVersion) {
        usericonsData.addUsericonOfBadgeType(type, idVersion);
    }
    
    private static class HiddenBadgesDialog extends LazyDialog {
        
        private final SettingsDialog d;
        private final TableEditor<Usericon> editor;
        
        private HiddenBadgesDialog(SettingsDialog d) {
            this.d = d;
            editor = new TableEditor<>(TableEditor.SORTING_MODE_SORTED, false);
            editor.setItemEditor(() -> new TableEditor.ItemEditor<Usericon>() {
                
                @Override
                public Usericon showEditor(Usericon preset, Component c, boolean edit, int column) {
                    JOptionPane.showMessageDialog(c, "Badge types can be added through the Badge Context Menu (right-click on a Badge in chat).");
                    return null;
                }
            });
            editor.setModel(new ListTableModel<Usericon>(new String[]{"Badge Type"}) {

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    return get(rowIndex).readableLenientType();
                }
            });
        }
        
        @Override
        public JDialog createDialog() {
            return new Dialog();
        }
        
        private class Dialog extends JDialog {

            private Dialog() {
                super(d);
                setTitle("Hidden Badges");
                setModal(true);
                setLayout(new GridBagLayout());

                GridBagConstraints gbc;
                gbc = SettingsDialog.makeGbc(0, 1, 1, 1);
                add(new JLabel(SettingConstants.HTML_PREFIX + "All types of badges on this list won't show up in chat. Custom Badges always take precedence though, so if e.g. you have configured a custom Partner badge, then it may show up even if it is hidden here.<br /><br />"
                        + "It is also possible to add a Custom Badge with no image to hide a badge, which allows for more control over which badge should be hidden under what conditions."), gbc);

                gbc = SettingsDialog.makeGbc(0, 2, 1, 1);
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.fill = GridBagConstraints.BOTH;
                add(editor, gbc);

                pack();
                GuiUtil.installEscapeCloseOperation(this);
            }
        }
        
        public void setData(Collection<Usericon> data) {
            editor.setData(new ArrayList<>(data));
        }
        
        public List<Usericon> getData() {
            return editor.getData();
        }
        
    }

}
