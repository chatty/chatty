
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
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
public class HighlightSettings extends SettingsPanel {
    
    public static final String INFO_HEADER = "<html><body style='width:350px;font-weight:normal;'>";
    
    public static String getMatchingHelp(String type) {
        return INFO_HEADER+SettingsUtil.getInfo("info-matching.html", type);
    }
    
    private final NoHighlightUsers noHighlightUsers;
    private final HighlightBlacklist highlightBlacklist;
    
    public HighlightSettings(SettingsDialog d) {
        super(true);
        
        noHighlightUsers = new NoHighlightUsers(d);
        highlightBlacklist = new HighlightBlacklist(d, "Highlight", "highlightBlacklist");
        
        JPanel base = addTitledPanel(Language.getString("settings.section.highlightMessages"), 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.insets.bottom -= 3;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightEnabled = d.addSimpleBooleanSetting("highlightEnabled");
        base.add(highlightEnabled, gbc);
        
        Insets settingInsets = new Insets(1,14,1,4);
        
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = settingInsets;
        JCheckBox highlightUsername = d.addSimpleBooleanSetting("highlightUsername");
        base.add(highlightUsername, gbc);

        gbc = d.makeGbc(1,1,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightNextMessages = d.addSimpleBooleanSetting("highlightNextMessages");
        base.add(highlightNextMessages, gbc);
        
        gbc = d.makeGbc(0,2,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightOwnText = d.addSimpleBooleanSetting("highlightOwnText");
        base.add(highlightOwnText, gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightIgnored = d.addSimpleBooleanSetting("highlightIgnored");
        base.add(highlightIgnored, gbc);
        
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST);
        gbc.insets = settingInsets;
        JCheckBox highlightMatches = d.addSimpleBooleanSetting("highlightMatches");
        base.add(highlightMatches, gbc);
        
        gbc = d.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST);
        gbc.insets = settingInsets;
        JCheckBox highlightMatchesAll = d.addSimpleBooleanSetting("highlightMatchesAll");
        base.add(highlightMatchesAll, gbc);
        
        gbc = d.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST);
        JCheckBox highlightByPoints = d.addSimpleBooleanSetting("highlightByPoints");
        base.add(highlightByPoints, gbc);
        
        gbc = d.makeGbc(0,5,2,1);
        gbc.insets = new Insets(5,10,5,5);
        ListSelector items = d.addListSetting("highlight", "Highlight", 220, 250, true, true);
        items.setInfo(getMatchingHelp("highlight"));
        HighlighterTester tester = new HighlighterTester(d, true, "Highlight:");
        tester.setAddToBlacklistListener(e -> {
            highlightBlacklist.addItem(e.getActionCommand());
        });
        tester.setLinkLabelListener(d.getLinkLabelListener());
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        items.setEditor(tester);
        items.setDataFormatter(input -> input.trim());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
        JButton noHighlightUsersButton = new JButton("Users to never highlight");
        noHighlightUsersButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        noHighlightUsersButton.addActionListener(e -> {
            noHighlightUsers.setLocationRelativeTo(HighlightSettings.this);
            noHighlightUsers.setVisible(true);
        });
        gbc = d.makeGbc(0, 6, 1, 1);
        gbc.insets = new Insets(1,10,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        base.add(noHighlightUsersButton, gbc);
        
        JButton highlightBlacklistButton = new JButton("Highlight Blacklist");
        highlightBlacklistButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        highlightBlacklistButton.addActionListener(e -> {
            highlightBlacklist.setLocationRelativeTo(HighlightSettings.this);
            highlightBlacklist.setVisible(true);
        });
        gbc = d.makeGbc(1, 6, 1, 1);
        gbc.insets = new Insets(1,5,5,30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        base.add(highlightBlacklistButton, gbc);
        
        SettingsUtil.addSubsettings(highlightEnabled, highlightUsername,
                highlightNextMessages, highlightOwnText, highlightIgnored,
                highlightMatches, items, noHighlightUsersButton,
                highlightBlacklistButton);
        
        SettingsUtil.addSubsettings(highlightMatches, highlightMatchesAll);
    }
    
    private static class NoHighlightUsers extends JDialog {

        private static final DataFormatter<String> FORMATTER = new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return StringUtil.toLowerCase(input.replaceAll("\\s", ""));
            }
        };
        
        public NoHighlightUsers(SettingsDialog d) {
            super(d);
            
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setTitle("Users to never highlight");
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;

            gbc = d.makeGbc(0, 0, 1, 1);
            add(new JLabel("<html><body style='width:260px;padding:4px;'>Users on this list "
                    + "will never trigger a Highlight. This can be useful e.g. "
                    + "for bots in your channel that repeatedly post messages "
                    + "containing your name."), gbc);
            
            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.5;
            gbc.weighty = 1;
            ListSelector noHighlightUsers = d.addListSetting("noHighlightUsers", "No Highlight User", 180, 250, false, true);
            noHighlightUsers.setDataFormatter(FORMATTER);
            add(noHighlightUsers, gbc);

            JButton closeButton = new JButton(Language.getString("dialog.button.close"));
            closeButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                }
            });
            gbc = d.makeGbc(0, 2, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(closeButton, gbc);
            
            pack();
            setMinimumSize(getPreferredSize());
        }
        
    }
    
}
