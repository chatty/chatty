
package chatty.gui.components.settings;

import chatty.gui.Channels;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 *
 * @author tduva
 */
public class TabSettings extends SettingsPanel {
    
    public TabSettings(final SettingsDialog d) {
        
        JPanel other = addTitledPanel(Language.getString("settings.section.tabs"), 0);
        JPanel infoPanel = addTitledPanel("Tab Info", 1);
        JPanel popout = addTitledPanel("Popout", 2);
        
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
        
        //--------------------------
        // Open Tabs
        //--------------------------
        other.add(SettingsUtil.createLabel("settings.string.tabsOpen"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));

        other.add(d.addComboStringSetting("tabsOpen", false, "main", "active", "active2"),
                d.makeGbc(1, 1, 3, 1, GridBagConstraints.WEST));
        
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
        other.add(new JLabel(Language.getString("settings.tabs.placement")),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        other.add(tabPlacementSetting,
                d.makeGbc(1, 2, 3, 1, GridBagConstraints.WEST)
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
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        other.add(tabLayoutSetting,
                d.makeGbc(1, 3, 3, 1, GridBagConstraints.WEST));
        
        //----------------
        // Tabs Scrolling
        //----------------
        JCheckBox scroll = d.addSimpleBooleanSetting("tabsMwheelScrolling");
        JCheckBox scroll2 = d.addSimpleBooleanSetting("tabsMwheelScrollingAnywhere");
        other.add(scroll,
                d.makeGbc(0, 5, 4, 1, GridBagConstraints.WEST));
        other.add(scroll2,
                d.makeGbcSub(0, 6, 4, 1, GridBagConstraints.WEST));

        SettingsUtil.addSubsettings(scroll, scroll2);

        //--------------------------
        // Tab Info
        //--------------------------
        JTabbedPane infoPanelTabs = new JTabbedPane();
        infoPanelTabs.addTab("Live Stream", new TabInfoOptions("tabsLive", d));
        infoPanelTabs.addTab("New Stream Status", new TabInfoOptions("tabsStatus", d));
        infoPanelTabs.addTab("New Message", new TabInfoOptions("tabsMessage", d));
        infoPanelTabs.addTab("New Highlight", new TabInfoOptions("tabsHighlight", d));
        infoPanelTabs.addTab("Active Tab", new TabInfoOptions("tabsActive", d));
        
        GridBagConstraints gbc = SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        infoPanel.add(infoPanelTabs, gbc);
        
        //--------
        // Popout
        //--------
        popout.add(d.addSimpleBooleanSetting("popoutSaveAttributes", "Restore location/size",
                "Save and restore the location and size of popout dialogs during the same session"),
                d.makeGbc(0,0,1,1));
        popout.add(d.addSimpleBooleanSetting("popoutCloseLastChannel", "Close popout when only channel",
                "Automatically close a popout if the last channel in the main window is closed"),
                d.makeGbc(1, 0, 1, 1));
        
        popout.add(SettingsUtil.createLabel("tabsPopoutDrag"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        popout.add(d.addComboLongSetting("tabsPopoutDrag", new int[]{0,1,2}),
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        popout.add(SettingsUtil.createLabel("popoutClose"),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        
        popout.add(d.addComboStringSetting("popoutClose", false, "ask", "close", "move"),
                d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        
        popout.add(new JLabel(Language.getString("popoutClose.keyTip")),
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
    }
    
    private static class TabInfoOptions extends JPanel implements LongSetting {
        
        private final Map<Integer, JCheckBox> options = new HashMap<>();
        
        TabInfoOptions(String settingName, SettingsDialog settings) {
            settings.addLongSetting(settingName, this);
            setLayout(new GridBagLayout());
            add(makeOption(Channels.DockChannelContainer.BOLD, "bold"),
                    SettingsDialog.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.ITALIC, "italic"),
                    SettingsDialog.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.COLOR1, "color1"),
                    SettingsDialog.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.COLOR2, "color2"),
                    SettingsDialog.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.ASTERISK, "asterisk"),
                    SettingsDialog.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.DOT1, "dot1"),
                    SettingsDialog.makeGbc(2, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.DOT2, "dot2"),
                    SettingsDialog.makeGbc(2, 2, 1, 1, GridBagConstraints.WEST));
            add(makeOption(Channels.DockChannelContainer.LINE, "line"),
                    SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        }
        
        private JCheckBox makeOption(int option, String labelKey) {
            String text = Language.getString("settings.tabs."+labelKey);
            String tip = Language.getString("settings.tabs."+labelKey + ".tip", false);
            JCheckBox check = new JCheckBox(text);
            check.setToolTipText(SettingsUtil.addTooltipLinebreaks(tip));
            options.put(option, check);
            return check;
        }

        @Override
        public Long getSettingValue() {
            long result = 0;
            for (Map.Entry<Integer, JCheckBox> entry : options.entrySet()) {
                if (entry.getValue().isSelected()) {
                    result = result | entry.getKey();
                }
            }
            return result;
        }

        @Override
        public Long getSettingValue(Long def) {
            return getSettingValue();
        }

        @Override
        public void setSettingValue(Long setting) {
            for (Map.Entry<Integer, JCheckBox> entry : options.entrySet()) {
                entry.getValue().setSelected((setting & entry.getKey()) != 0);
            }
        }
        
    }
    
}
