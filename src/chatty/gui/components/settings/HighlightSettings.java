
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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;

/**
 *
 * @author tduva
 */
public class HighlightSettings extends SettingsPanel {
    
    public static final String INFO = "<html><body style='width:300px;font-weight:normal;'>"
            + "Quick reference (see [help-settings:Highlight_Matching help] for more):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>Bets open</code> - 'Bets open' anywhere in the message"
            + "<li><code>cs:Bets open</code> - Same, but case-sensitive</li>"
            + "<li><code>w:Clip</code> - 'Clip' as a word, so e.g. not 'Clipped'</li>"
            + "<li><code>wcs:Clip</code> - Same, but case-sensitive</li>"
            + "<li><code>start:!slot</code> - Message beginning with '!slot'</li>"
            + "<li><code>reg:(?i)^!\\w+$</code> - Regular expression, anywhere</li>"
            + "</ul>"
            + "Meta prefixes (in front of text matching):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>chan:joshimuz</code> - Restrict to channel 'joshimuz'</li>"
            + "<li><code>user:Elorie</code> - Restrict to user 'Elorie'</li>"
            + "<li><code>cat:vip</code> - Restrict to users in Addressbook category 'vip'</li>"
            + "<li><code>config:info</code> - Match info messages</li>"
            + "</ul>";
    
    private static final String INFO_HIGHLIGHTS = INFO+"Example: <code>user:botimuz cs:Bets open</code>";
    
    private final NoHighlightUsers noHighlightUsers;
    private final HighlightBlacklist highlightBlacklist;
    
    public HighlightSettings(SettingsDialog d) {
        super(true);
        
        noHighlightUsers = new NoHighlightUsers(d);
        highlightBlacklist = new HighlightBlacklist(d);
        
        JPanel base = addTitledPanel("Highlight Messages", 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.insets.bottom -= 3;
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightEnabled", "Enable Highlight",
                "If enabled, shows messages that match the highlight criteria "
                + "in another color"), gbc);
        
        Insets settingInsets = new Insets(1,14,1,4);
        
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = settingInsets;
        base.add(d.addSimpleBooleanSetting("highlightUsername", "Highlight "
                + "own name",
                "If enabled, highlights messages containing your current "
                + "username, even if you didn't add it to the list."), gbc);

        gbc = d.makeGbc(1,1,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightNextMessages", "Highlight follow-up",
                "If enabled, highlights messages from the same user that are written"
                        + "shortly after the last highlighted one."), gbc);
        
        gbc = d.makeGbc(0,2,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightOwnText", "Check own text for highlights",
                "If enabled, allows own messages to be highlighted, otherwise "
                + "your own messages are NEVER highlighted. Good for testing."),
                gbc);
        
        gbc = d.makeGbc(1,2,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("highlightIgnored", "Check ignored messages",
                "If enabled, checks ignored messages as well, otherwise they are"
                        + " just ignored for highlighting."), gbc);
        
        gbc = d.makeGbc(0,5,2,1);
        gbc.insets = new Insets(5,10,5,5);
        ListSelector items = d.addListSetting("highlight", "Highlight", 220, 250, true, true);
        items.setInfo(INFO_HIGHLIGHTS);
        HighlighterTester tester = new HighlighterTester(d, true);
        tester.setAddToBlacklistListener(e -> {
            highlightBlacklist.addItem(e.getActionCommand());
        });
        tester.setLinkLabelListener(d.getLinkLabelListener());
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
    
    private static class HighlightBlacklist extends JDialog {
        
        private final ListSelector setting;
        
        public HighlightBlacklist(SettingsDialog d) {
            super (d);
            
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setTitle("Highlight Blacklist");
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            add(new JLabel("<html><body style='width:260px;padding:4px;'>"
                    + "Any text regions matched by items in this list will "
                    + "never trigger a Highlight.<br /><br />"
                    + "For example if the Highlight list contains "
                    + "<code>kerbo</code>, it would highlight the message "
                    + "\"<code>Welcome :) kerboHowdy</code>\", however if "
                    + "<code>kerboHowdy</code> was blacklisted it would "
                    + "prevent that Highlight."), gbc);
            
            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            setting = d.addListSetting("highlightBlacklist", "Blacklist", 100, 250, false, true);
            setting.setInfo(INFO);
            setting.setDataFormatter(input -> input.trim());
            HighlighterTester tester = new HighlighterTester(d, true);
            tester.setEditingBlacklistItem(true);
            tester.setLinkLabelListener(d.getLinkLabelListener());
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
}
