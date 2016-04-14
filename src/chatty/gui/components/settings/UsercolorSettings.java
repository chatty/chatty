
package chatty.gui.components.settings;

import chatty.UsercolorItem;
import chatty.gui.components.LinkLabel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.List;
import javax.swing.JPanel;

/**
 * Provides a list to add/remove/sort usercolor items.
 * 
 * @author tduva
 */
public class UsercolorSettings extends SettingsPanel {
    
    private static final String INFO_TEXT = "<html><body style='width:105px'>"
            + "Add items to the list to assign them colors. The order matters, items "
            + "on the top are matched first.<br /><br />"
            + "Special Items:<br />"
            + "$mod - Moderators<br />"
            + "$sub - Subscribers<br />"
            + "$turbo - Turbo Users<br />"
            + "$admin - Admin<br />"
            + "$staff - Staff<br />"
            + "$all - All Users<br />"
            + "<br />"
            + "[help:Usercolors And more..]";
    
    private final UsercolorEditor data;
    
    public UsercolorSettings(SettingsDialog d) {
        super(true);
        
        JPanel main = addTitledPanel("Usercolors", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(d.addSimpleBooleanSetting("customUsercolors", "Enable custom usercolors", "Changes colors and stuff.."), gbc);
        
        data = new UsercolorEditor(d);
        data.setPreferredSize(new Dimension(1,150));
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        main.add(data, gbc);
        
        LinkLabel info = new LinkLabel(INFO_TEXT, d.getSettingsHelpLinkLabelListener());
        main.add(info, d.makeGbc(1, 1, 1, 1));
        
    }
    
    public void setData(List<UsercolorItem> data) {
        this.data.setData(data);
    }
    
    public List<UsercolorItem> getData() {
        return data.getData();
    }
    
    public void editItem(String item) {
        data.edit(item);
    }
    
}
