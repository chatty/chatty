
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.WEST;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

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
        JPanel entries = addTitledPanel(Language.getString("settings.section.completion"), 0);
        
        entries.add(d.addSimpleBooleanSetting("completionEnabled"),
                SettingsDialog.makeGbc(0, 0, 4, 1, WEST));
        
        //--------------
        // TAB/Shift-TAB
        //--------------
        Map<String, String> tabChoices = new LinkedHashMap<>();
        tabChoices.put("names", Language.getString("settings.completion.option.names"));
        tabChoices.put("emotes", Language.getString("settings.completion.option.emotes"));
        tabChoices.put("both", Language.getString("settings.completion.option.namesEmotes"));
        tabChoices.put("both2", Language.getString("settings.completion.option.emotesNames"));
        tabChoices.put("custom", Language.getString("settings.completion.option.custom"));

        entries.add(new JLabel("TAB:"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));

        entries.add(
                d.addComboStringSetting("completionTab", 0, false, tabChoices),
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        entries.add(new JLabel("Shift-TAB:"),
                d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST));
        
        entries.add(
                d.addComboStringSetting("completionTab2", 0, false, tabChoices),
                d.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST));
        
        entries.add(new JLabel("<html><body style='width:300px;padding-bottom:3px;'>"
                + Language.getString("settings.completion.info")),
                d.makeGbc(0, 2, 4, 1));
        
        Map<String, String> emotePrefixValues = new LinkedHashMap<>();
        emotePrefixValues.put("", Language.getString("settings.completionEmotePrefix.option.none"));
        for (String item : ":,;-#~!'$§%&".split("")) {
            emotePrefixValues.put(item, item);
        }
        
        ComboStringSetting prefix = d.addComboStringSetting("completionEmotePrefix", 10, false, emotePrefixValues);
        ComboLongSetting mixed = d.addComboLongSetting("completionMixed", 0, 1, 2);
        Consumer<String> preferEmojiTest = s -> mixed.setEnabled(prefix.getSettingValue().equals(s));
        preferEmojiTest.accept(":");
        prefix.addActionListener(e -> preferEmojiTest.accept(":"));
        
        entries.add(SettingsUtil.createPanel("completionEmotePrefix",
                prefix,
                mixed),
                d.makeGbc(0, 3, 4, 1, GridBagConstraints.WEST));

        //-------------------
        // Custom Completion
        //-------------------
        CustomCompletionEntries customCompletionDialog = new CustomCompletionEntries(d);
        
        JButton editCustomCompletion = new JButton("Edit Custom Completion Items");
        editCustomCompletion.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        editCustomCompletion.addActionListener(e -> {
            customCompletionDialog.setLocationRelativeTo(d);
            customCompletionDialog.setVisible(true);
        });
        entries.add(editCustomCompletion,
                d.makeGbc(0, 6, 4, 1, GridBagConstraints.WEST));
        
        //=================
        // Localized Names
        //=================
        JPanel localized = addTitledPanel(Language.getString("settings.section.completionNames"), 2);
        
        localized.add(d.addSimpleBooleanSetting("completionPreferUsernames"),
                d.makeGbc(0, 2, 4, 1, GridBagConstraints.WEST));
        
        JCheckBox completionAllNameTypes = d.addSimpleBooleanSetting("completionAllNameTypes");
        localized.add(completionAllNameTypes,
                d.makeGbcCloser(0, 3, 4, 1, GridBagConstraints.WEST));
        
        JCheckBox completionAllNameTypesRestriction = d.addSimpleBooleanSetting("completionAllNameTypesRestriction");
        localized.add(completionAllNameTypesRestriction,
                d.makeGbcSub(0, 4, 4, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(completionAllNameTypes, completionAllNameTypesRestriction);
        
        //============
        // Appearance
        //============
        JPanel appearance = addTitledPanel(Language.getString("settings.section.completionAppearance"), 1);
        
        final JCheckBox popup = d.addSimpleBooleanSetting("completionShowPopup");
        JPanel numResults = SettingsUtil.createStandardGapPanel();
        final JTextField max = d.addSimpleLongSetting("completionMaxItemsShown", 3, true);
        numResults.add(popup);
        numResults.add(max);
        numResults.add(SettingsUtil.createLabel("searchResults"));
        appearance.add(numResults, d.makeNoGapGbc(0, 0, 2, 1, GridBagConstraints.LINE_START));
        
        JPanel popupSettings = new JPanel(new GridBagLayout());
        
        final JCheckBox auto = d.addSimpleBooleanSetting("completionAuto");
        popupSettings.add(auto,
                d.makeGbcCloser(0, 1, 1, 1, GridBagConstraints.LINE_START));
        
        final JCheckBox common = d.addSimpleBooleanSetting("completionCommonPrefix");
        popupSettings.add(common,
                d.makeGbcCloser(0, 2, 1, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(popup, max, common, auto);
        
        appearance.add(popupSettings,
                d.makeGbcSub(0, 1, 2, 1, GridBagConstraints.WEST));
        
        //-------------------
        // Sorting and Other
        //-------------------
        appearance.add(new JLabel(Language.getString("settings.completion.nameSorting")),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        
        Map<String, String> choices = new HashMap<>();
        choices.put("predictive", Language.getString("settings.completion.option.predictive"));
        choices.put("alphabetical", Language.getString("settings.completion.option.alphabetical"));
        choices.put("userlist", Language.getString("settings.completion.option.userlist"));
        
        appearance.add(
            d.addComboStringSetting("completionSorting", 4, false, choices),
            d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        
        appearance.add(new JLabel(Language.getString("settings.string.completionSearch")),
            d.makeGbc(0, 3, 1, 1));
        
        appearance.add(
            d.addComboStringSetting("completionSearch", false, new String[]{"start", "words", "anywhere"}),
            d.makeGbc(1, 3, 1, 1, GridBagConstraints.WEST));
        
        appearance.add(d.addSimpleBooleanSetting("completionSpace"),
                d.makeGbcCloser(0, 4, 4, 1, GridBagConstraints.WEST));
    }
    
    private class CustomCompletionEntries extends JDialog {

        public CustomCompletionEntries(Dialog owner) {
            super(owner);
            
            setTitle("Custom Completion Items");
            setDefaultCloseOperation(HIDE_ON_CLOSE);
            
            add(new JLabel("<html><body style='width:300px;padding:7px 7px 10px 7px;'>"
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
