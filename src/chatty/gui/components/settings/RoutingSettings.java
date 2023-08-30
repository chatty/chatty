
package chatty.gui.components.settings;

import chatty.gui.components.routing.RoutingEntry;
import static chatty.gui.components.settings.HighlightSettings.getMatchingHelp;
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

    private final RoutingSettingsTable<RoutingEntry> table;
    private final ListSelector items;
    
    public RoutingSettings(SettingsDialog d) {
        
        table = new RoutingSettingsTable<>(d, new JLabel(""));
        
        JPanel main = addTitledPanel("Custom Tabs Routing", 0);
        JPanel settings = addTitledPanel("Custom Tabs Settings", 1);
        
        items = d.addListSetting("routing", "Custom Tab Routing", 220, 250, true, true);
        items.setInfo(getMatchingHelp("routing"));
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        items.setEditor(() -> {
            HighlighterTester tester = new HighlighterTester(d, false, "routing");
            tester.setLinkLabelListener(d.getLinkLabelListener());
            return tester;
        });
        items.setDataFormatter(input -> input.trim());
        
        main.add(d.addSimpleBooleanSetting("routingMulti"),
                SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        main.add(items,
                SettingsDialog.makeGbcStretchHorizontal(0, 1, 1, 1));
        settings.add(table,
                SettingsDialog.makeGbcStretchHorizontal(0, 0, 1, 1));
    }

    public void setData(List<RoutingEntry> data) {
        table.setData(data);
    }

    public List<RoutingEntry> getData() {
        return table.getData();
    }
    
    public void selectItem(String item) {
        items.setSelected(item);
    }
    
    public void selectItems(Collection<String> selectItems) {
        items.setSelected(selectItems);
    }
    
}
