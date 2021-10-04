
package chatty.gui.components.settings;

import chatty.gui.Channels;
import chatty.gui.GuiUtil;
import chatty.gui.RegexDocumentFilter;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.userinfo.PastMessages;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.EAST;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

/**
 *
 * @author tduva
 */
public class ModerationSettings extends SettingsPanel {
    
    public ModerationSettings(final SettingsDialog d) {
        
        //==========================
        // Mod Infos
        //==========================
        JPanel modInfoPanel = addTitledPanel(Language.getString("settings.section.modInfos"), 0);
        
        JCheckBox showModActions = d.addSimpleBooleanSetting("showModActions");
        JCheckBox showModActionsRestrict = d.addSimpleBooleanSetting("showModActionsRestrict");
        
        SettingsUtil.addSubsettings(showModActions, showModActionsRestrict);
        
        modInfoPanel.add(showModActions,
                d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST));
        
        modInfoPanel.add(showModActionsRestrict,
                d.makeGbcSub(0, 1, 3, 1, GridBagConstraints.WEST));
        
        modInfoPanel.add(d.addSimpleBooleanSetting("showActionBy"),
                d.makeGbc(0, 4, 3, 1, GridBagConstraints.WEST));
        
        modInfoPanel.add(d.addSimpleBooleanSetting("showAutoMod", "Show messages rejected by AutoMod", ""),
                d.makeGbc(0, 5, 3, 1, GridBagConstraints.WEST));
        
        modInfoPanel.add(new JLabel(SettingConstants.HTML_PREFIX
                + "Approve/deny AutoMod messages in chat through their context menu (right-click) or the User Dialog (left-click) or <code>Extra - AutoMod</code>."),
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.CENTER));
        
        //==========================
        // User Dialog
        //==========================
        JPanel userInfo = addTitledPanel(Language.getString("settings.section.userDialog"), 1);
        
        JTabbedPane userInfoTabs = new JTabbedPane();
        JPanel userInfoGeneral = new JPanel(new GridBagLayout());
        JPanel userInfoMsg = new JPanel(new GridBagLayout());
        
        userInfoGeneral.add(d.addSimpleBooleanSetting(
                "closeUserDialogOnAction"),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        userInfoGeneral.add(d.addSimpleBooleanSetting(
                "openUserDialogByMouse"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        userInfoGeneral.add(d.addSimpleBooleanSetting(
                "reuseUserDialog"),
                d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));
        
        HotkeyTextField banReasonsHotkey = new HotkeyTextField(12, null);
        d.addStringSetting("banReasonsHotkey", banReasonsHotkey);
        SettingsUtil.addLabeledComponent(userInfoGeneral, "banReasonsHotkey", 0, 6, 1, EAST, banReasonsHotkey);
        
        userInfoGeneral.add(SettingsUtil.createLabel("banReasonsInfo", true),
                d.makeGbc(0, 7, 2, 1));
        
        SettingsUtil.addLabeledComponent(userInfoMsg, "settings.otherMessageSettings.timestamp", 0, 3, 1, EAST, MessageSettings.createTimestampPanel(d, "userDialogTimestamp"));

        SettingsUtil.addLabeledComponent(userInfoMsg, "settings.long.clearUserMessages.label", 0, 4, 1, EAST,
                d.addComboLongSetting("clearUserMessages", new int[]{-1, 3, 6, 12, 24}));
        
        SettingsUtil.addLabeledComponent(userInfoMsg, "userDialogMessageLimit", 0, 5, 1, EAST,
                d.addSimpleLongSetting("userDialogMessageLimit", 3, true));
        
        SettingsUtil.addLabeledComponent(userInfoMsg, "userMessagesHighlight",
                0, 6, 2, GridBagConstraints.EAST,
                new HighlightOptions("userMessagesHighlight", d));
        
        userInfoTabs.addTab(Language.getString("settings.userInfo.tab.general"), userInfoGeneral);
        userInfoTabs.addTab(Language.getString("settings.userInfo.tab.userMessages"), userInfoMsg);
        
        userInfo.add(userInfoTabs, SettingsDialog.makeGbcStretchHorizontal(0, 0, 1, 1));
        
        //==========================
        // Repeated Messages
        //==========================
        JPanel repeatMsgPanel = addTitledPanel(Language.getString("settings.section.repeatMsg"), 2);
        
        JCheckBox repeatMsg = d.addSimpleBooleanSetting("repeatMsg");
        repeatMsgPanel.add(repeatMsg,
                SettingsDialog.makeGbc(0, 0, 4, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgMethod", 0, 1, 1, GridBagConstraints.EAST,
                d.addComboLongSetting("repeatMsgMethod", 1, 2));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgSim", 2, 2, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgSim", 4, true));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgRep", 2, 3, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgRep", 4, true));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgTime", 0, 2, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgTime", 4, true));
        
        JTextField ignoredChars = d.addSimpleStringSetting("repeatMsgIgnored", 4, true);
        ((AbstractDocument) ignoredChars.getDocument()).setDocumentFilter(TestSimilarity.IGNORED_CHARS_FILTER);
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgIgnored", 0, 3, 1, GridBagConstraints.EAST,
                ignoredChars);
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgLen", 0, 4, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgLen", 4, true));
        
        EditorStringSetting editor = d.addEditorStringSetting("repeatMsgMatch", 10, true, "Restrict detection", false, SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-restriction-repeat.html", null));
        editor.setShowInfoByDefault(true);
        editor.setLinkLabelListener(d.getLinkLabelListener());
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgMatch", 0, 5, 3, GridBagConstraints.EAST,
                editor, true);
        
        // All components added before this will be sub settings
        SettingsUtil.addSubsettings(repeatMsg, repeatMsgPanel.getComponents());
        
        JButton testSim = new JButton("Test Similarity");
        testSim.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        testSim.addActionListener(e -> {
            new TestSimilarity(d, ignoredChars.getText()).setVisible(true);
        });
        repeatMsgPanel.add(testSim,
                SettingsDialog.makeGbc(2, 1, 2, 1, GridBagConstraints.EAST));
        
        repeatMsgPanel.add(new LinkLabel("Tip: Add <code>config:repeatedmsg</code> to e.g. [help:Highlight Highlight] list to match on detected repetition.",
                d.getSettingsHelpLinkLabelListener()),
                SettingsDialog.makeGbc(0, 6, 4, 1));
    }
    
    /**
     * Dialog to test similarity settings.
     */
    private static class TestSimilarity extends JDialog {
        
        /**
         * Remove all characters that are not in the BMP (except surrogates, so
         * do remove those).
         */
        public static DocumentFilter IGNORED_CHARS_FILTER = new RegexDocumentFilter("[^\u0000-\uD7FF\uE000-\uFFFF]");
        
        private static final String DEFAULT_EXAMPLE_A = "Have you already checked out Chatty's YouTube channel? Might have some useful video guides.";
        private static final String DEFAULT_EXAMPLE_B = "Chatty's YouTube channel might have some useful video guides. Have you checked it out yet?";
        
        private static final Map<String, String> EXAMPLES = new LinkedHashMap<>();
        
        static {
            EXAMPLES.put(DEFAULT_EXAMPLE_A+"|"+DEFAULT_EXAMPLE_B, "Default");
            EXAMPLES.put("How is it going?|How is it going??????????", "Questionmarks");
            EXAMPLES.put("Kappa Kappa Kappa Kappa FrankerZ Kappa Kappa Kappa Kappa|Kappa Kappa Kappa FrankerZ", "Emote Repetition");
            EXAMPLES.put("aa|aaaaaaaa", "aaaaaaa");
            EXAMPLES.put("How much wood could a woodchuck chuck if a woodchuck could chuck wood?|How much wood indeed?", "Wood?");
        }
        
        private final JTextArea m1;
        private final JTextArea m2;
        private final JLabel label;
        private final JTextField ignoredChars;
        
        public TestSimilarity(Window parent, String ignoredCharsValue) {
            super(parent);
            setTitle("Test Similarity");
            
            setLayout(new GridBagLayout());
            
            m1 = new JTextArea(4, 30);
            m2 = new JTextArea(4, 30);
            GuiUtil.resetFocusTraversalKeys(m1);
            GuiUtil.resetFocusTraversalKeys(m2);
            m1.setLineWrap(true);
            m1.setWrapStyleWord(true);
            m2.setLineWrap(true);
            m2.setWrapStyleWord(true);
            m1.setText(DEFAULT_EXAMPLE_A);
            m2.setText(DEFAULT_EXAMPLE_B);
            ignoredChars = new JTextField(10);
            ignoredChars.setText(ignoredCharsValue);
            ((AbstractDocument)ignoredChars.getDocument()).setDocumentFilter(IGNORED_CHARS_FILTER);
            
            DocumentListener listener = new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    update();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    update();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    update();
                }
            };
            m1.getDocument().addDocumentListener(listener);
            m2.getDocument().addDocumentListener(listener);
            ignoredChars.getDocument().addDocumentListener(listener);
            
            GridBagConstraints gbc = SettingsDialog.makeGbc(0, 1, 2, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(new JScrollPane(m1), gbc);
            gbc = SettingsDialog.makeGbc(0, 2, 2, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(new JScrollPane(m2), gbc);
            
            JPanel ignorePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            
            ignorePanel.add(new JLabel("Ignored characters:"));
            ignorePanel.add(ignoredChars);
            
            JButton ignoredCharsLoad = new JButton("Reset");
            ignoredCharsLoad.addActionListener(e -> ignoredChars.setText(ignoredCharsValue));
            JButton ignoredCharsClear = new JButton("Clear");
            ignoredCharsClear.addActionListener(e -> ignoredChars.setText(null));
            
            ignoredCharsLoad.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            ignoredCharsClear.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            
            ignorePanel.add(ignoredCharsLoad);
            ignorePanel.add(ignoredCharsClear);
            
            gbc = SettingsDialog.makeGbc(0, 0, 2, 1);
            add(ignorePanel, gbc);
            
            add(new JLabel(SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-similarity.html", null)),
                    SettingsDialog.makeGbc(0, 5, 2, 1));
            
            label = new JLabel("Result");
            add(label, SettingsDialog.makeGbc(0, 3, 2, 1));
            
            gbc = SettingsDialog.makeGbc(0, 4, 1, 1, GridBagConstraints.EAST);
            gbc.weightx = 1;
            add(new JLabel("Examples:"), gbc);
            ComboStringSetting selectExample = new ComboStringSetting(EXAMPLES);
            gbc = SettingsDialog.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST);
            gbc.weightx = 1;
            add(selectExample, gbc);
            selectExample.addActionListener(e -> {
                String example = selectExample.getSettingValue();
                String[] split = example.split("\\|");
                m1.setText(split[0]);
                m2.setText(split[1]);
            });
            
            JButton closeButton = new JButton(Language.getString("dialog.button.close"));
            closeButton.addActionListener(e -> dispose());
            gbc = SettingsDialog.makeGbc(0, 6, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(closeButton, gbc);
            
            pack();
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(parent);
            update();
        }
        
        private void update() {
            char[] ignoredCharacters = StringUtil.getCharsFromString(ignoredChars.getText());
            String a = StringUtil.prepareForSimilarityComparison(m1.getText(), ignoredCharacters);
            String b = StringUtil.prepareForSimilarityComparison(m2.getText(), ignoredCharacters);
            int similarity = Math.round(StringUtil.getSimilarity(a, b) * 100);
            int similarity2 = Math.round(StringUtil.getSimilarity2(a, b) * 100);
            int lengthSimilarity = Math.round(StringUtil.getLengthSimilarity(a, b) * 100);
            if (lengthSimilarity < similarity) {
                label.setText(String.format("Strict: %s%%* - Lenient: %s%%",
                        similarity, similarity2));
            }
            else {
                label.setText(String.format("Strict: %s%% - Lenient: %s%%",
                        similarity, similarity2));
            }
        }
        
    }
    
    private static class HighlightOptions extends JPanel implements LongSetting {
        
        private final Map<Integer, JCheckBox> options = new HashMap<>();
        
        HighlightOptions(String settingName, SettingsDialog settings) {
            settings.addLongSetting(settingName, this);
            setLayout(new GridBagLayout());
            add(makeOption(PastMessages.CURRENT_MSG, "currentMsg"),
                    SettingsDialog.makeNoGapGbc(0, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(PastMessages.REPEATED_MSG, "repeatedMsg"),
                    SettingsDialog.makeNoGapGbc(1, 1, 1, 1, GridBagConstraints.WEST));
            add(makeOption(PastMessages.MOD_ACTION, "modAction"),
                    SettingsDialog.makeNoGapGbc(0, 2, 1, 1, GridBagConstraints.WEST));
            add(makeOption(PastMessages.AUTO_MOD, "autoMod"),
                    SettingsDialog.makeNoGapGbc(1, 2, 1, 1, GridBagConstraints.WEST));
        }
        
        private JCheckBox makeOption(int option, String labelKey) {
            String text = Language.getString("settings.userMessagesHighlight."+labelKey);
            String tip = Language.getString("settings.userMessagesHighlight."+labelKey + ".tip", false);
            JCheckBox check = new JCheckBox(text);
            check.setToolTipText(SettingsUtil.addTooltipLinebreaks(tip));
            options.put(option, check);
            return check;
        }

        @Override
        public Long getSettingValue() {
            long result = 0;
            for (Map.Entry<Integer, JCheckBox> entry : options.entrySet()) {
                if (entry.getValue().isSelected()) {
                    result = result | entry.getKey();
                }
            }
            return result;
        }

        @Override
        public Long getSettingValue(Long def) {
            return getSettingValue();
        }

        @Override
        public void setSettingValue(Long setting) {
            for (Map.Entry<Integer, JCheckBox> entry : options.entrySet()) {
                entry.getValue().setSelected((setting & entry.getKey()) != 0);
            }
        }
        
    }
    
    public static void main(String[] args) {
        new TestSimilarity(null, "?").setVisible(true);
    }
    
}
