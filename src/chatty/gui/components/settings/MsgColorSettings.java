
package chatty.gui.components.settings;

import chatty.gui.colors.MsgColorItem;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class MsgColorSettings extends SettingsPanel {
    
    private static final String INFO_TEXT = "<html><body>"
            + "Customize message colors based on Highlighting rules. "
            + "[help:Message_Colors More information..]";
    
    private final ItemColorEditor<MsgColorItem> data;
    
    public MsgColorSettings(SettingsDialog d) {
        super(true);
        
        JPanel main = addTitledPanel(Language.getString("settings.section.msgColors"), 0, true);
        JPanel other = addTitledPanel(Language.getString("settings.section.msgColorsOther"), 1);
        
        GridBagConstraints gbc;
        
        JCheckBox msgColorsEnabled = d.addSimpleBooleanSetting("msgColorsEnabled");
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(msgColorsEnabled, gbc);
        
        data = new ItemColorEditor<>(d,
                (id,
                        foreground, foregroundEnabled,
                        background, backgroundEnabled) -> {
                    return new MsgColorItem(id,
                            foreground, foregroundEnabled,
                            background, backgroundEnabled);
                }, true, new LinkLabel(HighlightSettings.getMatchingHelp("msgColors"), d.getLinkLabelListener()));
        data.setPreferredSize(new Dimension(1,150));
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        main.add(data, gbc);
        
        SettingsUtil.addSubsettings(msgColorsEnabled, data);
        
        LinkLabel info = new LinkLabel(INFO_TEXT, d.getSettingsHelpLinkLabelListener());
        main.add(info, d.makeGbc(0, 2, 1, 1));
        
        other.add(d.addSimpleBooleanSetting("msgColorsPrefer"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("msgColorsLinks"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("actionColored"),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        
    }
    
    public void setData(List<MsgColorItem> data) {
        this.data.setData(data);
    }
    
    public List<MsgColorItem> getData() {
        return data.getData();
    }
    
    public void setDefaultForeground(Color color) {
        data.setDefaultForeground(color);
    }
    
    public void setDefaultBackground(Color color) {
        data.setDefaultBackground(color);
    }
    
    public void editItem(String item) {
        data.edit(item);
    }
    
    public void selectItem(String item) {
        data.setSelected(item);
    }
    
}
