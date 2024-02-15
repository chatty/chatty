
package chatty.gui.components.settings;

import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;


/**
 *
 * @author tduva
 */
public class HistorySettings extends SettingsPanel implements ActionListener {

    private final JButton clearHistory = new JButton("Clear history");
    private final SettingsDialog d;
    //private final JButton removeOld = new JButton("Remove old entries");

    public HistorySettings(SettingsDialog d) {
        this.d = d;

        // History group
        JPanel main = addTitledPanel("Channel History (shown in the Favorites/History Dialog)", 0);

        GridBagConstraints gbc;

        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(d.addSimpleBooleanSetting("saveChannelHistory", "Enable History", "If enabled, automatically add joined channels to the history"), gbc);

        JPanel days = new JPanel();
        ((FlowLayout)days.getLayout()).setVgap(0);
        days.add(d.addSimpleBooleanSetting("historyClear", "Only keep channels joined in the last ", ""));
        days.add(d.addSimpleLongSetting("channelHistoryKeepDays", 3, true));
        days.add(new JLabel("days"));

        gbc = d.makeGbc(0, 2, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0,20,5,5);
        main.add(days, gbc);

        gbc = d.makeGbc(0, 3, 1, 1);
        gbc.insets = new Insets(5, 20, 10, 5);
        main.add(new JLabel("<html><body style='width: 280px'>"
                + "Expired entries (defined as per the setting above) "
                + "are automatically deleted from the history "
                + "when you start Chatty."), gbc);

        gbc = d.makeGbc(0, 4, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        main.add(clearHistory, gbc);

        clearHistory.addActionListener(this);

        JPanel presets = addTitledPanel("Status Presets (shown in the Presets in the Admin Dialog)", 1);

        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        presets.add(d.addSimpleBooleanSetting("saveStatusHistory", "Enable History",
                "If enabled, automatically add used status (title/game) to the history"), gbc);

        JPanel daysPresets = new JPanel();
        ((FlowLayout)daysPresets.getLayout()).setVgap(0);
        daysPresets.add(d.addSimpleBooleanSetting("statusHistoryClear",
                "Only keep entries used in the last ",
                "Whether to remove old status history entries."));
        daysPresets.add(d.addSimpleLongSetting("statusHistoryKeepDays", 3, true));
        daysPresets.add(new JLabel("days"));

        gbc = d.makeGbc(0, 2, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0,20,5,5);
        presets.add(daysPresets, gbc);
        
        
        JPanel externalHistory = addTitledPanel("History Service", 2);

        externalHistory.add(new LinkLabel(SettingConstants.HTML_PREFIX
                + "Chatty uses the [url:https://recent-messages.robotty.de recent-messages.robotty.de] API "
                + "to get messages of the last 24 hours when joining a channel.",
                d.getLinkLabelListener()),
                            SettingsDialog.makeGbc(0, 0, 2, 1, GridBagConstraints.NORTH));

        JCheckBox historyServiceEnabled = d.addSimpleBooleanSetting("historyServiceEnabled",
                                                      "Enable History Service",
                                                      "Use an external service to get the channel history");
        externalHistory.add(historyServiceEnabled,
                            SettingsDialog.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        ComboLongSetting historyServiceLimit = d.addComboLongSetting("historyServiceLimit",
                                                               5, 10, 15, 20,
                                                               30, 40, 50, 60,
                                                               70, 80, 90, 100);
        SettingsUtil.addStandardSetting(externalHistory, "historyServiceLimit", 2, historyServiceLimit, true);

        String matchOptionsTip = "Using the matching prefix \"config:historic2\" will allow "
                + "the match even for features that are disabled here, while \"config:historic\" does the same but will restrict the match to history messages only.";
        JLabel matchOptionsLabel = new JLabel("Allow for history messages:");
        matchOptionsLabel.setToolTipText(SettingsUtil.addTooltipLinebreaks(matchOptionsTip));
        externalHistory.add(matchOptionsLabel,
                            SettingsDialog.makeGbcSub2(0, 3, 2, 1, GridBagConstraints.WEST));
        
        JPanel matchOptions = new JPanel();
        matchOptions.add(d.addSimpleBooleanSetting("historyMessageHighlight", Language.getString("settings.page.highlight"), matchOptionsTip));
        matchOptions.add(d.addSimpleBooleanSetting("historyMessageIgnore", Language.getString("settings.page.ignore"), matchOptionsTip));
        matchOptions.add(d.addSimpleBooleanSetting("historyMessageMsgColors", Language.getString("settings.page.msgColors"), matchOptionsTip));
        matchOptions.add(d.addSimpleBooleanSetting("historyMessageRouting", "Routing", matchOptionsTip));
        matchOptions.add(d.addSimpleBooleanSetting("historyMessageNotifications", Language.getString("settings.page.notifications"), matchOptionsTip));
        
        externalHistory.add(matchOptions,
                            SettingsDialog.makeGbcSub2(0, 4, 2, 1, GridBagConstraints.WEST));
        
        externalHistory.add(new JLabel("Excluded channels:"),
                            SettingsDialog.makeGbcSub2(0, 5, 2, 1, GridBagConstraints.WEST));
        
        ListSelector excludedChannels = d.addListSetting("historyServiceExcluded",
                                                        "Channel to be excluded from history",
                                                        250, 200,
                                                        false, true);
        final ChannelFormatter formatter = new ChannelFormatter();
        excludedChannels.setDataFormatter(formatter);
        externalHistory.add(excludedChannels, SettingsDialog.makeGbcSub2(0, 6, 2, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(historyServiceEnabled, historyServiceLimit, excludedChannels);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == clearHistory) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Do you want to delete all history entries?",
                    "Clear history",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            
            if (result == JOptionPane.YES_OPTION) {
                d.clearHistory();
            }
        }
    }
}
