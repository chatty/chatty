
package chatty.gui.components.settings;

import chatty.gui.Highlighter;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.SyntaxHighlighter;
import chatty.util.commands.CommandSyntaxHighlighter;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;

/**
 * Presets for Highlighting prefixes and stuff.
 * 
 * @author tduva
 */
public class MatchingPresets extends LazyDialog {
    
    private final SettingsDialog d;
    private final ListSelector setting;
    
    public MatchingPresets(SettingsDialog d) {
        this.d = d;
        this.setting = d.addListSetting("matchingPresets", "Presets", 100, 250, false, true);
        setting.setChangeListener(value -> {
            HighlighterTester.testPresets = Highlighter.HighlightItem.makePresets(value);
        });
    }
    
    @Override
    public JDialog createDialog() {
        return new Dialog();
    }
    
    private class Dialog extends JDialog {

        private Dialog() {
            super(d);
            setTitle("Presets");
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            setLayout(new GridBagLayout());
            GridBagConstraints gbc;

            gbc = d.makeGbc(0, 0, 1, 1);
            add(new LinkLabel("<html><body style='width:340px;padding:4px;'>" + SettingsUtil.getInfo("info-matching_presets.html", null),
                    d.getLinkLabelListener()), gbc);

            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            setting.setDataFormatter(input -> input.trim());
            setting.setInfoLinkLabelListener(d.getLinkLabelListener());
            setting.setTester(CommandSettings.createCommandTester());
            setting.setSyntaxHighlighter(new CommandSyntaxHighlighter() {
                
                @Override
                public void update(String input) {
                    if (input.startsWith("_")) {
                        super.update(input);
                    }
                    else {
                        super.update("");
                    }
                }
                
            });
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
    }
    
}
