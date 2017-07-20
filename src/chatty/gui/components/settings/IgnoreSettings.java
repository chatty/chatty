
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.IgnoredMessages;
import chatty.gui.components.LinkLabel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;

/**
 *
 * @author tduva
 */
public class IgnoreSettings extends SettingsPanel {
    
    private static final String INFO_IGNORE = HighlightSettings.INFO
            +"Example: <code>chan:joshimuz re:!bet.*</code>";
    
    private final IgnoredUsers ignoredUsers;
    
    public IgnoreSettings(SettingsDialog d) {
        super(true);
        
        ignoredUsers = new IgnoredUsers(d);
        
        JPanel base = addTitledPanel("Ignore Messages", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("ignoreEnabled", "Enable Ignore",
                "If enabled, shows messages that match the highlight criteria "
                + "in another color"), gbc);
        
        //---------
        // Settings
        //---------
        
        gbc = d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 1;
        base.add(new JLabel("Ignore Hints:"),
                gbc);
        
        final JCheckBox ignoreShowNotDialog = d.addSimpleBooleanSetting(
                "ignoreShowNotDialog",
                "Only show if ignored messages dialog is not open",
                "");
        
        HashMap<Long, String> modeDef = new HashMap<>();
        modeDef.put((long)IgnoredMessages.MODE_HIDE, "Hide");
        modeDef.put((long)IgnoredMessages.MODE_COUNT, "Show count");
        modeDef.put((long)IgnoredMessages.MODE_COMPACT, "Show names");
        final ComboLongSetting mode = new ComboLongSetting(modeDef);
        mode.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (mode.getSettingValue() == IgnoredMessages.MODE_HIDE) {
                    ignoreShowNotDialog.setEnabled(false);
                } else if (mode.getSettingValue() == IgnoredMessages.MODE_COUNT) {
                    ignoreShowNotDialog.setEnabled(true);
                } else if (mode.getSettingValue() == IgnoredMessages.MODE_COMPACT) {
                    ignoreShowNotDialog.setEnabled(true);
                }
            }
        });
        d.addLongSetting("ignoreMode", mode);
        gbc = d.makeGbc(3, 1, 1, 1);
        gbc.insets = new Insets(0,12,2,5);
        gbc.anchor = GridBagConstraints.EAST;
        base.add(mode, gbc);
        
        gbc = d.makeGbc(0, 2, 4, 1, GridBagConstraints.EAST);
        gbc.insets = new Insets(0, 5, 4, 4);
        base.add(ignoreShowNotDialog, gbc);
        
        gbc = d.makeGbc(0, 1, 2, 1, GridBagConstraints.EAST);
        gbc.insets = new Insets(0,12,2,5);
        base.add(d.addSimpleBooleanSetting(
                "ignoreOwnText",
                "Check own text for ignoring",
                "If enabled, allows messages you wrote yourself to be ignored as well. Good for testing."),
                gbc);
        
        //----------
        // Main List
        //----------
        
        gbc = d.makeGbc(0, 3, 4, 1);
        gbc.insets = new Insets(5,10,5,5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        ListSelector items = d.addListSetting("ignore", 390, 160, true, true);
        items.setInfo(INFO_IGNORE);
        items.setDataFormatter(new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.trim();
            }
        });
        items.setTester(new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                HighlighterTester tester = new HighlighterTester(parent, value);
                return tester.test();
            }
        });
        base.add(items, gbc);
        
        //-------
        // Footer
        //-------
        
        JButton ignoredUsersButton = new JButton("Ignored Users");
        ignoredUsersButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        ignoredUsersButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ignoredUsers.setLocationRelativeTo(IgnoreSettings.this);
                ignoredUsers.setVisible(true);
            }
        });
        gbc = d.makeGbc(0, 4, 1, 1);
        gbc.insets = new Insets(1,10,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(ignoredUsersButton, gbc);
        
        gbc = d.makeGbc(1,4,3,1, GridBagConstraints.CENTER);
        gbc.insets = new Insets(0, 5, 5, 5);
        base.add(new LinkLabel("<html><body style=\"width:220px;\">"
                + "Click on the Help link on the bottom left for help."
                , d.getLinkLabelListener()), gbc);
    }
    
    private static class IgnoredUsers extends JDialog {

        private static final DataFormatter<String> FORMATTER = new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.replaceAll("\\s", "").toLowerCase();
            }
        };
        
        public IgnoredUsers(SettingsDialog d) {
            super(d);
            
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setTitle("Ignored Users");
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            add(new JLabel("Ignored in chat"), gbc);
            
            gbc = d.makeGbc(1, 0, 1, 1);
            add(new JLabel("Ignored for whispers"), gbc);
            
            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.5;
            gbc.weighty = 1;
            ListSelector ignoredChat = d.addListSetting("ignoredUsers", 180, 250, false, true);
            ignoredChat.setDataFormatter(FORMATTER);
            add(ignoredChat, gbc);
            
            gbc = d.makeGbc(1, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.5;
            gbc.weighty = 1;
            ListSelector ignoredWhispers = d.addListSetting("ignoredUsersWhisper", 180, 250, false, true);
            ignoredWhispers.setDataFormatter(FORMATTER);
            add(ignoredWhispers, gbc);
            
            gbc = d.makeGbc(0, 2, 2, 1);
            add(new JLabel("<html><body style='width:260px;'>These lists are "
                    + "independant from the main ignore list, so the users are "
                    + "ignored even if the Ignore system is disabled."), gbc);
            
            gbc = d.makeGbc(0, 3, 2, 1);
            add(d.addSimpleBooleanSetting("ignoredUsersHideInGUI",
                    "Hide 'Ignored in chat' users from the userlist/joins/parts",
                    "This only applies to users that are not already on the userlist when you ignore them"),
                    gbc);
            
            JButton closeButton = new JButton("Close");
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
        
    }
    
}
