package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class AutoReplySettings extends SettingsPanel {

    private static final String INFO_IGNORE = HighlightSettings.INFO
            + "<li><code>resp:</code> - reply to send when a matching message is found</li>"
            + "</ul>Example: <code>user:cohhilitionbot re:\\*\\*\\* NEW GIVEAWAY OPENED: resp:!enter</code>";

    public AutoReplySettings(SettingsDialog d) {
        super(true);

        JPanel base = addTitledPanel("Auto Reply to Messages", 0, true);

        GridBagConstraints gbc;

        gbc = d.makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("autoReplyEnabled", "Enable Auto Reply",
                "If enabled, sends a reply to any message that matches."), gbc);

        gbc = d.makeGbc(0,3,3,1);
        gbc.insets = new Insets(5,10,5,5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        ListSelector items = d.addListSetting("autoReply", 390, 160, true, false);
        items.setInfo(INFO_IGNORE);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        base.add(items, gbc);
    }

}
