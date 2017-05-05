package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class AutoReplySettings extends SettingsPanel {

    private static final String INFO_AUTOREPLY = "<html><body style='width:300px;font-weight:normal;'>"
            + "Quick Reference (see regular help for more):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>cs:</code> - match case sensitive</li>"
            + "<li><code>w:/wcs:</code> - match as whole word / case-sensitive</li>"
            + "<li><code>re:</code> - use regular expression</li>"
            + "<li><code>chan:chan1,chan2/!chan:</code> - restrict to channel(s) / inverted</li>"
            + "<li><code>user:name</code> - restrict to user with that name</li>"
            + "<li><code>cat:category</code> - restrict to users in that category</li>"
            + "<li><code>reply:</code> - reply to send when a matching message is found</li>"
            + "</ul>Example: <code>\"user:cohhilitionbot\" \"reply:!enter\" \"re:\\*\\*\\* NEW GIVEAWAY OPENED\"</code>";

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
        items.setInfo(INFO_AUTOREPLY);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        base.add(items, gbc);
    }

}
