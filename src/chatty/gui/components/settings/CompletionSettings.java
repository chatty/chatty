
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author tduva
 */
public class CompletionSettings extends SettingsPanel {
    
    private final SettingsDialog d;
    
    public CompletionSettings(SettingsDialog d) {
        super(false);
        
        this.d = d;

        //========
        // Entries
        //========
        JPanel entries = addTitledPanel("TAB Completion (Names, Emotes, Commands)", 0);
        
        //--------------
        // TAB/Shift-TAB
        //--------------
        Map<String, String> tabChoices = new LinkedHashMap<>();
        tabChoices.put("names", "Names");
        tabChoices.put("emotes", "Emotes");
        tabChoices.put("both", "Names, then Emotes");
        tabChoices.put("both2", "Emotes, then Names");
        tabChoices.put("custom", "Custom Completion");

        entries.add(new JLabel("TAB:"),
                d.makeGbc(0, 0, 1, 1));

        entries.add(
                d.addComboStringSetting("completionTab", 0, false, tabChoices),
                d.makeGbc(1, 0, 1, 1));
        
        entries.add(new JLabel("Shift-TAB:"),
                d.makeGbc(2, 0, 1, 1));
        
        entries.add(
                d.addComboStringSetting("completionTab2", 0, false, tabChoices),
                d.makeGbc(3, 0, 1, 1));
        
        entries.add(new JLabel("<html><body style='width:300px;padding-bottom:5px;'>"
                + "<em>Tip:</em> Independant of these settings, you can prefix "
                + "with <code>@</code> to always TAB-complete to Names, with "
                + "<code>.</code> (dot) to use Custom Completion and with "
                + "<code>:</code> to complete Emoji."),
                d.makeGbc(0, 1, 4, 1));

        //================
        // Localized Names
        //================
        JPanel localized = addTitledPanel("Localized Names", 1);
        
        localized.add(d.addSimpleBooleanSetting(
                "completionPreferUsernames",
                "Prefer Regular name for username-based commands",
                "Prefer Regular name for commands like /ban even when entering a Localized or Custom name"),
                d.makeGbc(0, 2, 4, 1, GridBagConstraints.WEST));
        
        localized.add(d.addSimpleBooleanSetting(
                "completionAllNameTypes",
                "Include all name types in result (Regular/Localized/Custom)",
                "For example entering the Localized name will also put the Regular name in the results"),
                d.makeGbcCloser(0, 3, 4, 1, GridBagConstraints.WEST));
        
        localized.add(d.addSimpleBooleanSetting("completionAllNameTypesRestriction",
                "Only when no more than two matches",
                ""),
                d.makeGbcSub(0, 4, 4, 1, GridBagConstraints.WEST));
        
        //===========
        // Appearance
        //===========
        JPanel appearance = addTitledPanel("Appearance / Behaviour", 2);

        final JCheckBox popup = d.addSimpleBooleanSetting("completionShowPopup", "Show popup",
                "Shows the info popup (also requirement for \"Complete to common prefix\")");
        appearance.add(popup,
            d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox common = d.addSimpleBooleanSetting("completionCommonPrefix", "Complete to common prefix",
                "If more than one match, complete to common prefix (\"Show popup\" required as well)");
        appearance.add(common,
                d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST));
        
        
        
        appearance.add(new JLabel("Max Items Shown:"),
                d.makeGbcSub(0, 1, 1, 1, GridBagConstraints.WEST));
        final JTextField max = d.addSimpleLongSetting("completionMaxItemsShown", 3, true);
        appearance.add(max,
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        popup.addChangeListener(e -> {
                common.setEnabled(popup.isSelected());
                max.setEnabled(popup.isSelected());
            }
        );
        
        //-----------------
        // Username Sorting
        //-----------------
        appearance.add(new JLabel("Name Sorting:"),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        String[] choices = new String[]{"predictive", "alphabetical", "userlist"};
        
        appearance.add(
            d.addComboStringSetting("completionSorting", 4, false, choices),
            d.makeGbc(1, 2, 2, 1));

        //==================
        // Custom Completion
        //==================
        CustomCompletionEntries customCompletionDialog = new CustomCompletionEntries(d);
        
        JButton editCustomCompletion = new JButton("Edit Custom Completion Items");
        editCustomCompletion.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        editCustomCompletion.addActionListener(e -> {
            customCompletionDialog.setLocationRelativeTo(d);
            customCompletionDialog.setVisible(true);
        });
        entries.add(editCustomCompletion,
                d.makeGbc(0, 5, 4, 1, GridBagConstraints.WEST));
    }
    
    private class CustomCompletionEntries extends JDialog {

        public CustomCompletionEntries(Dialog owner) {
            super(owner);
            
            setTitle("Custom Completion Items");
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            
            add(new JLabel("<html><body style='width:300px;padding:7 7 10 7;'>"
                    + "Use <kbd>TAB</kbd> to complete '.Key' (prefixed "
                    + "with a dot) to 'Value'.<br />"
                    + "<br />"
                    + "Example: Add <code>chatty</code> as Key and <code>http://chatty.github.io</code>"
                    + " as Value, then <code>.chatty</code> completes to <code>http://chatty.github.io</code>. "
                    + "<br /><br />You have to enter the key exactly, so for example just <code>chat</code> won't find <code>chatty</code>.<br />"
                    + "<br />"
                    + "If you have selected 'Custom Completion' for the "
                    + "<kbd>TAB</kbd> or <kbd>Shift-Tab</kbd> setting then you "
                    + "can also use that to perform the completion without the "
                    + "dot in front."
            ), BorderLayout.NORTH);
            
            SimpleTableEditor editor = d.addStringMapSetting("customCompletion", 270, 180);
            editor.setKeyFilter("[^\\w]");
            add(editor, BorderLayout.CENTER);
            
            JButton close = new JButton("Close");
            close.addActionListener(e -> setVisible(false));
            add(close, BorderLayout.SOUTH);
            
            pack();
        }
        
    }
    
}
