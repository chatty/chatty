
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.WEST;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author tduva
 */
public class ModerationSettings extends SettingsPanel {
    
    public ModerationSettings(final SettingsDialog d) {
        
        JPanel blah = addTitledPanel(Language.getString("settings.section.modInfos"), 0);
        
        JCheckBox showModActions = d.addSimpleBooleanSetting("showModActions");
        JCheckBox showModActionsRestrict = d.addSimpleBooleanSetting("showModActionsRestrict");
        
        SettingsUtil.addSubsettings(showModActions, showModActionsRestrict);
        
        blah.add(showModActions,
                d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST));
        
        blah.add(showModActionsRestrict,
                d.makeGbcSub(0, 1, 3, 1, GridBagConstraints.WEST));
        
        blah.add(d.addSimpleBooleanSetting("showActionBy"),
                d.makeGbc(0, 4, 3, 1, GridBagConstraints.WEST));
        
        blah.add(d.addSimpleBooleanSetting("showAutoMod", "Show messages rejected by AutoMod", ""),
                d.makeGbc(0, 5, 3, 1, GridBagConstraints.WEST));
        
        blah.add(new JLabel("<html><body style='width:300px;'>"
                + "To approve messages open <code>Extra - AutoMod</code>. "
                + "You can also set a custom hotkey to open dialogs (go "
                + "to <code>Hotkeys</code> settings, add a new item and select "
                + "<code>Dialog: AutoMod Dialog</code> as action)."),
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.CENTER));
        
        
        JPanel userInfo = addTitledPanel(Language.getString("settings.section.userDialog"), 1);
        
        userInfo.add(d.addSimpleBooleanSetting(
                "closeUserDialogOnAction"),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        userInfo.add(d.addSimpleBooleanSetting(
                "openUserDialogByMouse"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        userInfo.add(d.addSimpleBooleanSetting(
                "reuseUserDialog"),
                d.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));
        
        userInfo.add(MessageSettings.createTimestampPanel(d, "userDialogTimestamp"),
                d.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));

        SettingsUtil.addLabeledComponent(userInfo, "settings.long.clearUserMessages.label", 0, 4, 1, EAST,
                d.addComboLongSetting("clearUserMessages", new int[]{-1, 3, 6, 12, 24}));
        
        SettingsUtil.addLabeledComponent(userInfo, "userDialogMessageLimit", 0, 5, 1, EAST,
                d.addSimpleLongSetting("userDialogMessageLimit", 3, true));
        
        HotkeyTextField banReasonsHotkey = new HotkeyTextField(12, null);
        d.addStringSetting("banReasonsHotkey", banReasonsHotkey);
        SettingsUtil.addLabeledComponent(userInfo, "banReasonsHotkey", 0, 6, 1, EAST, banReasonsHotkey);
        
        userInfo.add(SettingsUtil.createLabel("banReasonsInfo", true),
                d.makeGbc(0, 7, 2, 1));
        
        
        JPanel repeatMsgPanel = addTitledPanel(Language.getString("settings.section.repeatMsg"), 2);
        
        JCheckBox repeatMsg = d.addSimpleBooleanSetting("repeatMsg");
        repeatMsgPanel.add(repeatMsg,
                SettingsDialog.makeGbc(0, 0, 4, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgRep", 0, 1, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgRep", 4, true));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgSim", 2, 1, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgSim", 4, true));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgTime", 0, 3, 1, GridBagConstraints.EAST,
                d.addSimpleLongSetting("repeatMsgTime", 4, true));
        
        SettingsUtil.addLabeledComponent(repeatMsgPanel, "repeatMsgMatch", 0, 4, 3, GridBagConstraints.EAST,
                d.addSimpleStringSetting("repeatMsgMatch", 20, true), true);
        
        SettingsUtil.addSubsettings(repeatMsg, repeatMsgPanel.getComponents());
        
        JButton testSim = new JButton("Test Similarity");
        testSim.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        testSim.addActionListener(e -> {
            new TestSimilarity(d).setVisible(true);
        });
        repeatMsgPanel.add(testSim,
                SettingsDialog.makeGbc(2, 3, 2, 1, GridBagConstraints.EAST));
        
        repeatMsgPanel.add(new LinkLabel("Tip: Add <code>config:repeatedmsg</code> to e.g. [help:Highlight Highlight] list to match on detected repetition.",
                d.getSettingsHelpLinkLabelListener()),
                SettingsDialog.makeGbc(0, 5, 4, 1));
    }
    
    private static class TestSimilarity extends JDialog {
        
        private final JTextArea m1;
        private final JTextArea m2;
        private final JLabel label;
        
        public TestSimilarity(Window parent) {
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
            m1.setText("Have you already checked out Chatty's YouTube channel? Might have some useful video guides.");
            m2.setText("Chatty's YouTube channel might have some useful video guides. Have you checked it out yet?");
            
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
            
            GridBagConstraints gbc = SettingsDialog.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(new JScrollPane(m1), gbc);
            gbc = SettingsDialog.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(new JScrollPane(m2), gbc);
            
            add(new JLabel(SettingConstants.HTML_PREFIX+SettingsUtil.getInfo("info-similarity.html", null)), SettingsDialog.makeGbc(0, 3, 1, 1));
            
            label = new JLabel("Result");
            add(label, SettingsDialog.makeGbc(0, 2, 1, 1));
            
            JButton closeButton = new JButton(Language.getString("dialog.button.close"));
            closeButton.addActionListener(e -> dispose());
            gbc = SettingsDialog.makeGbc(0, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(closeButton, gbc);
            
            pack();
            setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            setLocationRelativeTo(parent);
            update();
        }
        
        private void update() {
            String a = StringUtil.prepareForSimilarityComparison(m1.getText());
            String b = StringUtil.prepareForSimilarityComparison(m2.getText());
            int similarity = Math.round(StringUtil.getSimilarity(a, b) * 100);
            int lengthSimilarity = Math.round(StringUtil.getLengthSimilarity(a, b) * 100);
            label.setText(String.format("Length Similarity: %s%% Text Similarity: %s%%", lengthSimilarity, similarity));
        }
        
    }
    
    public static void main(String[] args) {
        new TestSimilarity(null).setVisible(true);
    }
    
}
