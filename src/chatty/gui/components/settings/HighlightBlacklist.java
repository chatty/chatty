
package chatty.gui.components.settings;

import static chatty.gui.components.settings.HighlightSettings.getMatchingHelp;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;

/**
 *
 * @author tduva
 */
public class HighlightBlacklist extends JDialog {

    private final ListSelector setting;

    public HighlightBlacklist(SettingsDialog d, String type, String settingName) {
        super(d);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setTitle(type+" Blacklist");
        setLayout(new GridBagLayout());

        GridBagConstraints gbc;

        gbc = d.makeGbc(0, 0, 1, 1);
        add(new JLabel("<html><body style='width:340px;padding:4px;'>"+SettingsUtil.getInfo("info-blacklist.html", null)), gbc);

        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        setting = d.addListSetting(settingName, "Blacklist", 100, 250, false, true);
        setting.setInfo(getMatchingHelp("highlightBlacklist"));
        setting.setDataFormatter(input -> input.trim());
        HighlighterTester tester = new HighlighterTester(d, true, type+":");
        tester.setEditingBlacklistItem(true);
        tester.setLinkLabelListener(d.getLinkLabelListener());
        setting.setInfoLinkLabelListener(d.getLinkLabelListener());
        setting.setEditor(tester);

        add(setting, gbc);

        JButton closeButton = new JButton(Language.getString("dialog.button.close"));
        closeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        gbc = d.makeGbc(0, 5, 2, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        add(closeButton, gbc);

        pack();
        setMinimumSize(getPreferredSize());
    }

    public void addItem(String item) {
        item = item.trim();
        List<String> values = setting.getData();
        if (!item.isEmpty() && !values.contains(item)) {
            values.add(item);
            setting.setData(values);
        }
    }

}
