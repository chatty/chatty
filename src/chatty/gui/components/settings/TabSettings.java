
package chatty.gui.components.settings;

import chatty.gui.Channels;
import chatty.gui.GuiUtil;
import chatty.lang.Language;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 *
 * @author tduva
 */
public class TabSettings extends SettingsPanel {
    
    public TabSettings(final SettingsDialog d) {
        
        JPanel main = addTitledPanel(Language.getString("settings.section.tabs"), 0);
        JPanel infoPanel = addTitledPanel("Tab Info", 1);
        JPanel popout = addTitledPanel("Popout", 2);
        
        //==========================
        // Main Settings
        //==========================
        JTabbedPane mainTabs = new JTabbedPane();
        
        GridBagConstraints gbc;
        
        gbc = SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        main.add(mainTabs, gbc);
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        JPanel orderPanel = new JPanel(new GridBagLayout());
        
        mainTabs.addTab("Main", mainPanel);
        mainTabs.addTab("Tab Order", orderPanel);
        
        //--------------------------
        // Tabs Order
        //--------------------------
        orderPanel.add(new JLabel(Language.getString("settings.tabs.order")), d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        Map<String, String> options = new HashMap<>();
        options.put("normal", Language.getString("settings.tabs.option.normal"));
        options.put("alphabetical", Language.getString("settings.tabs.option.alphabetical"));
        orderPanel.add(
                d.addComboStringSetting("tabOrder", 1, false, options),
                d.makeGbc(1, 0, 3, 1, GridBagConstraints.WEST)
        );
        
        TabsPos tabsPos = new TabsPos(d);
        
        TabsPrefixPos channelPrefix = new TabsPrefixPos(d, "#", tabsPos.getSetting());
        TabsPrefixPos whisperPrefix = new TabsPrefixPos(d, "$", tabsPos.getSetting());
        TabsPrefixPos dialogsPrefix = new TabsPrefixPos(d, "-", tabsPos.getSetting());
        
        SettingsUtil.addLabeledComponent(orderPanel, "settings.tabsOrder.channel", 0, 1, 3, GridBagConstraints.WEST, channelPrefix);
        SettingsUtil.addLabeledComponent(orderPanel, "settings.tabsOrder.whisper", 0, 2, 3, GridBagConstraints.WEST, whisperPrefix);
        SettingsUtil.addLabeledComponent(orderPanel, "settings.tabsOrder.dialogs", 0, 3, 3, GridBagConstraints.WEST, dialogsPrefix);
        
        JButton tabsPosButton = new JButton("Advanced Tabs Order");
        tabsPosButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        tabsPosButton.addActionListener(e -> {
            tabsPos.setLocationRelativeTo(TabSettings.this);
            tabsPos.setVisible(true);
        });
        
        tabsPos.setChangeListener(data -> {
            channelPrefix.update(data);
            whisperPrefix.update(data);
            dialogsPrefix.update(data);
        });
        orderPanel.add(tabsPosButton,
                SettingsDialog.makeGbc(0, 5, 1, 1, GridBagConstraints.WEST));
        
        orderPanel.add(d.addSimpleBooleanSetting("tabsAutoSort"),
                SettingsDialog.makeGbc(0, 6, 2, 1, GridBagConstraints.WEST));
        
        orderPanel.add(new JLabel(SettingConstants.HTML_PREFIX+"Tab order applies when opening new tabs and when tabs are resorted. Tabs can always be moved manually via drag&amp;drop, however they may not keep their position when reopened or resorted."),
                SettingsDialog.makeGbc(0, 10, 2, 1, GridBagConstraints.CENTER));
        
        //--------------------------
        // Open Tabs
        //--------------------------
        mainPanel.add(SettingsUtil.createLabel("settings.string.tabsOpen"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));

        mainPanel.add(d.addComboStringSetting("tabsOpen", false, "main", "active", "active2", "activeChan"),
                d.makeGbc(1, 1, 3, 1, GridBagConstraints.WEST));
        
        //--------------------------
        // Tabs Location
        //--------------------------
        Map<String, String> tabPlacementOptions = new HashMap<>();
        tabPlacementOptions.put("top", Language.getString("settings.tabs.option.top"));
        tabPlacementOptions.put("left", Language.getString("settings.tabs.option.left"));
        tabPlacementOptions.put("bottom", Language.getString("settings.tabs.option.bottom"));
        tabPlacementOptions.put("right", Language.getString("settings.tabs.option.right"));
        ComboStringSetting tabPlacementSetting = new ComboStringSetting(tabPlacementOptions);
        d.addStringSetting("tabsPlacement", tabPlacementSetting);
        mainPanel.add(new JLabel(Language.getString("settings.tabs.placement")),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        mainPanel.add(tabPlacementSetting,
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
        mainPanel.add(new JLabel(Language.getString("settings.tabs.layout")),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        mainPanel.add(tabLayoutSetting,
                d.makeGbc(1, 3, 3, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addLabeledComponent(mainPanel, "tabsMaxWidth", 0, 4, 3, GridBagConstraints.WEST,
                d.addSimpleLongSetting("tabsMaxWidth", 3, true));
        
        mainPanel.add(d.addSimpleBooleanSetting("tabsCloseEmpty"),
                SettingsDialog.makeGbc(0, 5, 4, 1, GridBagConstraints.WEST));
        
        //----------------
        // Tabs Scrolling
        //----------------
        JCheckBox scroll = d.addSimpleBooleanSetting("tabsMwheelScrolling");
        JCheckBox scroll2 = d.addSimpleBooleanSetting("tabsMwheelScrollingAnywhere");
        mainPanel.add(scroll,
                d.makeGbc(0, 6, 4, 1, GridBagConstraints.WEST));
        mainPanel.add(scroll2,
                d.makeGbcSub(0, 7, 4, 1, GridBagConstraints.WEST));

        SettingsUtil.addSubsettings(scroll, scroll2);

        //==========================
        // Tab Info
        //==========================
        JTabbedPane infoPanelTabs = new JTabbedPane();
        infoPanelTabs.addTab("Live Stream", new TabInfoOptions("tabsLive", d));
        infoPanelTabs.addTab("New Stream Status", new TabInfoOptions("tabsStatus", d));
        infoPanelTabs.addTab("New Message", new TabInfoOptions("tabsMessage", d));
        infoPanelTabs.addTab("New Highlight", new TabInfoOptions("tabsHighlight", d));
        infoPanelTabs.addTab("Active Channel", new TabInfoOptions("tabsActive", d));
        
        gbc = SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        infoPanel.add(infoPanelTabs, gbc);
        
        infoPanel.add(d.addSimpleBooleanSetting("tabsChanTitles"), SettingsDialog.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        //==========================
        // Popout
        //==========================
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
    
    private static class TabsPrefixPos extends JPanel {
        
        private final SettingsDialog d;
        private final ComboLongSetting setting;
        private final String prefix;
        
        private TabsPrefixPos(SettingsDialog d, String prefix, MapSetting<String, Long> mapSetting) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            this.d = d;
            this.prefix = prefix;
            Map<Long, String> items = new HashMap<>();
            items.put((long)-1, "Before Default Location (-1)");
            items.put((long)0, "Default Location (0)");
            items.put((long)1, "After Default Location (1)");
            setting = new ComboLongSetting(items);
            setting.addItemListener(e -> {
                Map<String, Long> data = mapSetting.getSettingValue();
                Long pos = setting.getSettingValue((long)0);
                if (!pos.equals(data.get(prefix))
                        && (pos != 0 || data.containsKey(prefix))) {
                    // Only trigger update if necessary
                    data.put(prefix, pos);
                    mapSetting.setSettingValue(data);
                }
            });
            
            add(setting);
        }
        
        public void update(Map<String, Long> items) {
            setting.setSettingValue(items.getOrDefault(prefix, (long)0));
        }
        
    }
    
    private static class TabsPos extends JDialog {
        
        private static final String INFO = SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-tabs_order.html", null);
        
        private final SimpleTableEditor<Long> editor;
        private Consumer<Map<String, Long>> listener;
        
        private TabsPos(SettingsDialog d) {
            super(d);
            setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            setLayout(new GridBagLayout());
            setTitle("Advanced Tabs Order");
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            add(new JLabel(INFO), gbc);
            
            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            editor = d.addLongMapSetting("tabsPos", 300, 200);
            editor.setRendererForColumn(0, new TabsPosRenderer());
            editor.setRendererForColumn(1, new TabsPosRenderer());
            editor.getSorter().setSortKeys(Arrays.asList(new SortKey[]{
                new SortKey(1, SortOrder.ASCENDING),
                new SortKey(0, SortOrder.ASCENDING)}));
            editor.setValueFilter("[^0-9-]");
            editor.setTableEditorListener(new TableEditor.TableEditorListener<SimpleTableEditor.MapItem<Long>>() {
                @Override
                public void itemAdded(SimpleTableEditor.MapItem<Long> item) {
                    informListener(editor.getSettingValue());
                }

                @Override
                public void itemRemoved(SimpleTableEditor.MapItem<Long> item) {
                    informListener(editor.getSettingValue());
                }

                @Override
                public void itemEdited(SimpleTableEditor.MapItem<Long> oldItem, SimpleTableEditor.MapItem<Long> newItem) {
                    informListener(editor.getSettingValue());
                }

                @Override
                public void allItemsChanged(List<SimpleTableEditor.MapItem<Long>> newItems) {
                    informListener(editor.getSettingValue());
                }

                @Override
                public void refreshData() {
                }

                @Override
                public void itemsSet() {
                    informListener(editor.getSettingValue());
                }
            });
            add(editor, gbc);
            
            gbc = d.makeGbc(0, 2, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            JButton closeButton = new JButton("Close");
            add(closeButton, gbc);
            closeButton.addActionListener(e -> {
                setVisible(false);
            });
            
            pack();
            setMinimumSize(getPreferredSize());
        }
        
        private void informListener(Map<String, Long> items) {
            if (listener != null) {
                listener.accept(items);
            }
        }
        
        public void setChangeListener(Consumer<Map<String, Long>> listener) {
            this.listener = listener;
        }

        private MapSetting<String, Long> getSetting() {
            return editor;
        }
        
    }
    
    private static class TabsPosRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                setText(null);
                return;
            }
            if (value instanceof String) {
                setText(tabPosLabel((String) value));
            }
            else if (Objects.equals(value, (long)0)) {
                setText("0 (Default)");
            }
            else {
                setText(String.valueOf(value));
            }
        }
        
    }
    
    public static String tabPosLabel(String value) {
        if (value.equals("-")) {
            return "Info Tabs (-)";
        }
        else if (value.equals("#")) {
            return "Channel Tabs (#)";
        }
        else if (value.equals("$")) {
            return "Whisper Tabs ($)";
        }
        else if (value.equals(Channels.DEFAULT_CHANNEL_ID)) {
            return String.format("'%s' tab", Language.getString("tabs.noChannel"));
        }
        return value;
    }
    
}
