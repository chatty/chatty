
package chatty.gui.components.settings;

import chatty.gui.components.routing.RoutingTargetSettings;
import static chatty.gui.components.settings.HighlightSettings.getMatchingHelp;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.Collection;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class RoutingSettings extends SettingsPanel {

    private final RoutingSettingsTable<RoutingTargetSettings> table;
    private final ListSelector items;
    
    public RoutingSettings(SettingsDialog d) {
        super(true);
        
        table = new RoutingSettingsTable<>(d, new JLabel(""));
        table.setPreferredSize(new Dimension(10,150));
        
        JPanel main = addTitledPanel("Custom Tabs Routing", 0, true);
        JPanel settings = addTitledPanel("Custom Tabs Settings", 1, true);
        
        items = d.addListSetting("routing", "Custom Tab Routing", 220, 190, true, true);
        items.setInfo(getMatchingHelp("routing"));
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        items.setEditor(() -> {
            HighlighterTester tester = new HighlighterTester(d, false, "routing");
            tester.setLinkLabelListener(d.getLinkLabelListener());
            return tester;
        });
        items.setDataFormatter(input -> input.trim());
        
        GridBagConstraints gbc;
        
        main.add(d.addSimpleBooleanSetting("routingMulti"),
                SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        gbc = SettingsDialog.makeGbcStretchHorizontal(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        main.add(items, gbc);
        
        gbc = SettingsDialog.makeGbcStretchHorizontal(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        settings.add(table, gbc);
    }

    public void setData(List<RoutingTargetSettings> data) {
        table.setData(data);
    }

    public List<RoutingTargetSettings> getData() {
        return table.getData();
    }
    
    public void selectItem(String item) {
        items.setSelected(item);
    }
    
    public void selectItems(Collection<String> selectItems) {
        items.setSelected(selectItems);
    }
    
}
