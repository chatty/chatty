
package chatty.gui.components.settings;

import chatty.gui.IgnoredMessages;
import chatty.gui.components.LinkLabel;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class IgnoreSettings extends SettingsPanel {
    
    private static final String INFO_IGNORE = HighlightSettings.INFO
            +"Example: <code>chan:joshimuz re:!bet.*</code>";
    
    public IgnoreSettings(SettingsDialog d) {
        super(true);
        
        JPanel base = addTitledPanel("Ignore Messages", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("ignoreEnabled", "Enable Ignore",
                "If enabled, shows messages that match the highlight criteria "
                + "in another color"), gbc);
        
        final JLabel modeInfo = new JLabel("Shows the number of ignored messages regulary");
        final JCheckBox ignoreShowNotDialog = d.addSimpleBooleanSetting("ignoreShowNotDialog", "Only show if ignored messages dialog is not open", "");
        
        HashMap<Long, String> modeDef = new HashMap<>();
        modeDef.put((long)IgnoredMessages.MODE_HIDE, "Hide");
        modeDef.put((long)IgnoredMessages.MODE_COUNT, "Show count");
        modeDef.put((long)IgnoredMessages.MODE_COMPACT, "Show names");
        final ComboLongSetting mode = new ComboLongSetting(modeDef);
        mode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String info = "";
                if (mode.getSettingValue() == IgnoredMessages.MODE_HIDE) {
                    info = "Doesn't show ignored messages in the chat at all";
                    ignoreShowNotDialog.setEnabled(false);
                } else if (mode.getSettingValue() == IgnoredMessages.MODE_COUNT) {
                    info = "Regulary shows the number of ignored messages in chat";
                    ignoreShowNotDialog.setEnabled(true);
                } else if (mode.getSettingValue() == IgnoredMessages.MODE_COMPACT) {
                    info = "Lists the name of the sender of ignored messages in chat";
                    ignoreShowNotDialog.setEnabled(true);
                }
                modeInfo.setText(info);
            }
        });
        d.addLongSetting("ignoreMode", mode);
        gbc = d.makeGbc(2,0,1,1);
        gbc.insets = new Insets(0,12,2,5);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(mode, gbc);
        
        gbc = d.makeGbc(0, 1, 3, 1, GridBagConstraints.EAST);
        gbc.insets = new Insets(2, 5, 3, 5);
        base.add(modeInfo, gbc);
        
        gbc = d.makeGbc(0, 2, 3, 1, GridBagConstraints.EAST);
        gbc.insets = new Insets(0, 5, 4, 4);
        base.add(ignoreShowNotDialog, gbc);
        
        gbc = d.makeGbc(1,0,1,1);
        gbc.insets = new Insets(0,12,2,5);
        gbc.anchor = GridBagConstraints.EAST;
        base.add(d.addSimpleBooleanSetting("ignoreOwnText", "Ignore own messages",
                "If enabled, ignores messages your wrote yourself. Good "
                        + "for testing."), gbc);
        
        gbc = d.makeGbc(0,3,3,1);
        gbc.insets = new Insets(5,10,5,5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        ListSelector items = d.addListSetting("ignore", 390, 160, true);
        items.setInfo(INFO_IGNORE);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        base.add(items, gbc);
        
        gbc = d.makeGbc(0,4,3,1);
        gbc.insets = new Insets(0, 5, 5, 5);
        base.add(new LinkLabel("<html><body style=\"width:300px;\">"
                + "Matching messages works the same as the Highlights system. "
                + "Click on the Help link on the bottom left for help."
                , d.getLinkLabelListener()), gbc);
    }
    
}
