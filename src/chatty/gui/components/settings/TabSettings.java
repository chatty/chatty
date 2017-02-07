
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author tduva
 */
public class TabSettings extends SettingsPanel {
    
    public TabSettings(final SettingsDialog d) {
        
        JPanel other = addTitledPanel("Tab Settings", 0);
        other.add(new JLabel("Tab Order:"), d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        other.add(
                d.addComboStringSetting("tabOrder", 1, false, new String[]{"normal", "alphabetical"}),
                d.makeGbc(1, 0, 3, 1, GridBagConstraints.WEST)
        );
        
        Map<String, String> tabPlacementOptions = new HashMap<>();
        tabPlacementOptions.put("top", "Top");
        tabPlacementOptions.put("left", "Left");
        tabPlacementOptions.put("bottom", "Bottom");
        tabPlacementOptions.put("right", "Right");
        ComboStringSetting tabPlacementSetting = new ComboStringSetting(tabPlacementOptions);
        d.addStringSetting("tabsPlacement", tabPlacementSetting);
        other.add(new JLabel("Tab Placement:"), d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        other.add(tabPlacementSetting,
            d.makeGbc(1, 1, 3, 1, GridBagConstraints.WEST)
        );
        
        Map<String, String> tabLayoutOptions = new HashMap<>();
        tabLayoutOptions.put("wrap", "Wrap (Multiple Rows)");
        tabLayoutOptions.put("scroll", "Scroll (Single Row)");
        ComboStringSetting tabLayoutSetting = new ComboStringSetting(tabLayoutOptions);
        d.addStringSetting("tabsLayout", tabLayoutSetting);
        other.add(new JLabel("Tab Layout:"), d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        other.add(tabLayoutSetting,
                d.makeGbc(1, 2, 3, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("tabsMwheelScrolling",
                "Scroll through tabs with mousewheel",
                "Scrolling over the tabs changes between them"),
                d.makeGbc(0, 5, 4, 1, GridBagConstraints.WEST));
    }
    
}
