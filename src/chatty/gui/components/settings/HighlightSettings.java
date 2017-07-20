
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.Highlighter;
import chatty.gui.components.LinkLabel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
            + "Quick Reference (see regular help for more):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>cs:</code> - match case sensitive</li>"
            + "<li><code>w:/wcs:</code> - match as whole word / case-sensitive</li>"
            + "<li><code>re:</code> - use regular expression</li>"
            + "<li><code>chan:chan1,chan2/!chan:</code> - restrict to channel(s) / inverted</li>"
            + "<li><code>user:name</code> - restrict to user with that name</li>"
            + "<li><code>cat:category</code> - restrict to users in that category</li>"
            + "</ul>";
    
    private static final String INFO_HIGHLIGHTS = INFO+"Example: <code>user:botimuz cs:Bets open</code>";
    
    private final NoHighlightUsers noHighlightUsers;
    
    public HighlightSettings(SettingsDialog d) {
        super(true);
        
        noHighlightUsers = new NoHighlightUsers(d);
        
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
        ListSelector items = d.addListSetting("highlight", 220, 250, true, true);
        items.setInfo(INFO_HIGHLIGHTS);
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
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
        JButton noHighlightUsersButton = new JButton("Users to never higlight");
        noHighlightUsersButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        noHighlightUsersButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                noHighlightUsers.setLocationRelativeTo(HighlightSettings.this);
                noHighlightUsers.setVisible(true);
            }
        });
        gbc = d.makeGbc(0, 6, 2, 1);
        gbc.insets = new Insets(1,10,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        base.add(noHighlightUsersButton, gbc);
    }
    
    private static class NoHighlightUsers extends JDialog {

        private static final DataFormatter<String> FORMATTER = new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return input.replaceAll("\\s", "").toLowerCase();
            }
        };
        
        public NoHighlightUsers(SettingsDialog d) {
            super(d);
            
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            setTitle("Users to never highlight");
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;

            gbc = d.makeGbc(0, 1, 2, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 0.5;
            gbc.weighty = 1;
            ListSelector noHighlightUsers = d.addListSetting("noHighlightUsers", 180, 250, false, true);
            noHighlightUsers.setDataFormatter(FORMATTER);
            add(noHighlightUsers, gbc);
            
            gbc = d.makeGbc(0, 2, 2, 1);
            add(new JLabel("<html><body style='width:260px;'>Users on this list "
                    + "will never trigger a highlight. This can be useful e.g. "
                    + "for bots in your channel that repeatedly post messages "
                    + "containing your name."), gbc);
            
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
