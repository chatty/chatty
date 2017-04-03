
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JCheckBox;
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
    
    public CompletionSettings(SettingsDialog d) {
        super(true);
        
        JPanel main = addTitledPanel("TAB Completion", 0);
        final JCheckBox popup = d.addSimpleBooleanSetting("completionShowPopup", "Show popup",
                "Shows the info popup (also requirement for \"Complete to common prefix\")");
        main.add(popup,
            d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox common = d.addSimpleBooleanSetting("completionCommonPrefix", "Complete to common prefix",
                "If more than one match, complete to common prefix (\"Show popup\" required as well)");
        main.add(common,
                d.makeGbc(2, 1, 2, 1, GridBagConstraints.WEST));
        
        main.add(new JLabel("Sorting:"),
                d.makeGbc(2, 0, 1, 1));
        String[] choices = new String[]{"predictive", "alphabetical", "userlist"};
        
        main.add(
            d.addComboStringSetting("completionSorting", 4, false, choices),
            d.makeGbc(3, 0, 1, 1));
        
        main.add(new JLabel("Max Items Shown:"),
                d.makeGbc(0, 1, 1, 1));
        final JTextField max = d.addSimpleLongSetting("completionMaxItemsShown", 3, true);
        main.add(max,
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        popup.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                common.setEnabled(popup.isSelected());
                max.setEnabled(popup.isSelected());
            }
        });
        
        main.add(d.addSimpleBooleanSetting(
                "completionPreferUsernames",
                "Prefer Regular name for username-based commands",
                "Prefer Regular name for commands like /ban even when entering a Localized or Custom name"),
                d.makeGbc(0, 2, 4, 1, GridBagConstraints.WEST));
        
        main.add(d.addSimpleBooleanSetting(
                "completionAllNameTypes",
                "Include all name types in result (Regular/Localized/Custom)",
                "For example entering the Localized name will also put the Regular name in the results"),
                d.makeGbcCloser(0, 3, 4, 1, GridBagConstraints.WEST));
        
        main.add(d.addSimpleBooleanSetting("completionAllNameTypesRestriction",
                "Only when no more than two matches",
                ""),
                d.makeGbcSub(0, 4, 4, 1, GridBagConstraints.WEST));
        
        
        /**
         * Custom Completion
         */
        GridBagConstraints gbc;
        gbc = d.makeGbc(0, 0, 1, 1);
        JPanel custom = addTitledPanel("Custom Completion Items", 1, true);
        SimpleTableEditor editor = d.addStringMapSetting("customCompletion", 270, 200);
        editor.setKeyFilter("[^\\w]");
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        custom.add(editor, gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.anchor = GridBagConstraints.NORTH;
        custom.add(new JLabel("<html><body style='width:100px'>Use <kbd>Shift-TAB</kbd> "
                + "to complete '.Key' (prefixed with a dot) to 'Value'.<br />"
                + "<br />"
                + "Example: Add <code>dsh</code> as Key and <code>DatSheffy</code>"
                + " as Value, then <code>.dsh</code> completes to <code>DatSheffy</code>."), gbc);
        
        
    }
    
}
