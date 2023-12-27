
package chatty.gui.components.settings;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
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


        JPanel externalHistory = addTitledPanel("History service", 2);


        externalHistory.add(new JLabel("<html><body width=300>"
                                    + "Chatty uses recent-messages API from robotty go get messages while the user was offline.<br><br>"
                                    + "For more information please refer to https://recent-messages.robotty.de"),
                            SettingsDialog.makeGbc(0, 0, 2, 1, GridBagConstraints.NORTH));

        externalHistory.add(d.addSimpleBooleanSetting("historyEnableService",
                                                      "Enable history Service",
                                                      "Use an external service to get the channel history"),
                            SettingsDialog.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        externalHistory.add(d.addSimpleBooleanSetting("historyEnableRowLimit",
                                                      "Limit messages",
                                                      "Limit the history message"),
                            SettingsDialog.makeGbc(0, 2,1,1, GridBagConstraints.WEST));
        externalHistory.add(d.addSimpleLongSetting("historyCountMessages",10, true),
                            SettingsDialog.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        externalHistory.add(new JLabel("Excluded Channels:"),
                            SettingsDialog.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));

        ListSelector exclusionchannels = d.addListSetting("externalHistoryExclusion",
                                                        "Channel to be excluded from history",
                                                        250, 200,
                                                        false, true);
        final ChannelFormatter formatter = new ChannelFormatter();
        exclusionchannels.setDataFormatter(formatter);
        externalHistory.add(exclusionchannels, SettingsDialog.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST));
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
