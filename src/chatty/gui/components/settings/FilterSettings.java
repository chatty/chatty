
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class FilterSettings extends SettingsPanel {
    
    public FilterSettings(SettingsDialog d) {
        super(true);
        
        JPanel base = addTitledPanel(Language.getString("settings.section.filterMessages"), 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.insets.bottom -= 3;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox filterEnabled = d.addSimpleBooleanSetting("filterEnabled");
        base.add(filterEnabled, gbc);

        gbc = d.makeGbc(0,1,1,1);
        gbc.insets = new Insets(5,10,5,5);
        ListSelector items = d.addListSetting("filter", "Filter", 220, 250, true, true);
        items.setInfo(HighlightSettings.getMatchingHelp("filter"));
        HighlighterTester tester = new HighlighterTester(d, false, "filter");
        tester.setLinkLabelListener(d.getLinkLabelListener());
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        items.setEditor(tester);
        items.setDataFormatter(input -> input.trim());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
        SettingsUtil.addSubsettings(filterEnabled, items);
    }
    
}
