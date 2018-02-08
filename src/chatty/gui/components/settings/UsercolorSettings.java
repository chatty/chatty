
package chatty.gui.components.settings;

import chatty.gui.HtmlColors;
import chatty.gui.colors.UsercolorItem;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

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
    
    private final ItemColorEditor<UsercolorItem> data;
    
    public UsercolorSettings(SettingsDialog d) {
        super(true);
        
        JPanel customPanel = addTitledPanel("Custom Usercolors", 0, true);
        JPanel mainPanel = addTitledPanel(Language.getString("settings.section.usercolorsOther"), 1);
        
        GridBagConstraints gbc;
        
        //===================
        // Custom Usercolors
        //===================
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        customPanel.add(d.addSimpleBooleanSetting("customUsercolors",
                "Enable custom usercolors", ""), gbc);

        data = new ItemColorEditor<>(d,
                (id, color) -> { return new UsercolorItem(id, color); });
        data.setRendererForColumn(0, new ItemIdRenderer());
        data.setPreferredSize(new Dimension(1,150));
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        customPanel.add(data, gbc);
        
        LinkLabel info = new LinkLabel(INFO_TEXT, d.getSettingsHelpLinkLabelListener());
        customPanel.add(info, d.makeGbc(1, 1, 1, 1));
        
        //================
        // Other Settings
        //================
        mainPanel.add(d.addSimpleBooleanSetting("colorCorrection"),
                d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.WEST));
    }
    
    public void setData(List<UsercolorItem> data) {
        this.data.setData(data);
    }
    
    public List<UsercolorItem> getData() {
        return data.getData();
    }
    
    public void setBackgroundColor(Color color) {
        data.setBackgroundColor(color);
    }
    
    public void editItem(String item) {
        data.edit(item);
    }
    
    /**
     * Renderer for the item column, which uses a {@code UsercolorItem} to
     * display the item id and displays an error if the item type is undefined.
     */
    public static class ItemIdRenderer extends JLabel implements TableCellRenderer {

        public ItemIdRenderer() {
            // So background can be seen
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (value == null) {
                setText("");
                setToolTipText("error");
                return this;
            }

            UsercolorItem item = (UsercolorItem) value;

            // Use the default table font so it's not bold
            setFont(table.getFont());

            // Set text and color based on the item type and id
            if (item.type == UsercolorItem.TYPE_UNDEFINED) {
                setText(item.id + " (error)");
                setForeground(Color.RED);
            } else {
                setText(item.id);
                setForeground(table.getForeground());
            }

            // Add a tooltip text if the id is a color
            if (item.type == UsercolorItem.TYPE_COLOR) {
                setToolTipText(HtmlColors.getColorString(item.idColor));
            } else {
                setToolTipText(null);
            }

            // Set background color based on selection status
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }

    }
    
}
