
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class TabSettings extends SettingsPanel {
    
    public TabSettings(final SettingsDialog d) {
        
        JPanel other = addTitledPanel(Language.getString("settings.section.tabs"), 0);
        
        //------------
        // Tabs Order
        //------------
        other.add(new JLabel(Language.getString("settings.tabs.order")), d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        Map<String, String> options = new HashMap<>();
        options.put("normal", Language.getString("settings.tabs.option.normal"));
        options.put("alphabetical", Language.getString("settings.tabs.option.alphabetical"));
        other.add(
                d.addComboStringSetting("tabOrder", 1, false, options),
                d.makeGbc(1, 0, 3, 1, GridBagConstraints.WEST)
        );
        
        //---------------
        // Tabs Location
        //---------------
        Map<String, String> tabPlacementOptions = new HashMap<>();
        tabPlacementOptions.put("top", Language.getString("settings.tabs.option.top"));
        tabPlacementOptions.put("left", Language.getString("settings.tabs.option.left"));
        tabPlacementOptions.put("bottom", Language.getString("settings.tabs.option.bottom"));
        tabPlacementOptions.put("right", Language.getString("settings.tabs.option.right"));
        ComboStringSetting tabPlacementSetting = new ComboStringSetting(tabPlacementOptions);
        d.addStringSetting("tabsPlacement", tabPlacementSetting);
        other.add(new JLabel( Language.getString("settings.tabs.placement")), d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        other.add(tabPlacementSetting,
            d.makeGbc(1, 1, 3, 1, GridBagConstraints.WEST)
        );
        
        //-------------
        // Tabs Layout
        //-------------
        Map<String, String> tabLayoutOptions = new HashMap<>();
        tabLayoutOptions.put("wrap", Language.getString("settings.tabs.option.wrap"));
        tabLayoutOptions.put("scroll",  Language.getString("settings.tabs.option.scroll"));
        ComboStringSetting tabLayoutSetting = new ComboStringSetting(tabLayoutOptions);
        d.addStringSetting("tabsLayout", tabLayoutSetting);
        other.add(new JLabel(Language.getString("settings.tabs.layout")),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        other.add(tabLayoutSetting,
                d.makeGbc(1, 2, 3, 1, GridBagConstraints.WEST));
        
        //----------------
        // Tabs Scrolling
        //----------------
        JCheckBox scroll = d.addSimpleBooleanSetting("tabsMwheelScrolling");
        JCheckBox scroll2 = d.addSimpleBooleanSetting("tabsMwheelScrollingAnywhere");
        other.add(scroll,
                d.makeGbc(0, 5, 4, 1, GridBagConstraints.WEST));
        other.add(scroll2,
                d.makeGbcSub(0, 6, 4, 1, GridBagConstraints.WEST));

        // Tab scrolling checkbox status
        scroll2.setEnabled(false);
        scroll.addItemListener(e -> {
            scroll2.setEnabled(scroll.isSelected());
        });
    }
    
}
