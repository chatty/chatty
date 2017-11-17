
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class EmoteSettings extends SettingsPanel {
    
    private static final String IGNORED_INFO = "<html><body style='width:160px;'>"
            + "<p style='padding:5px;'>Ignored emotes are shown as just the emote code and not turned "
            + "into an image.</p>"
            + "<p style='padding:5px;'>It is recommended to use the emote context menu (right-click on "
            + "an emote in chat) to add emotes to this list.</p>";
    
    protected EmoteSettings(SettingsDialog d) {
        
        //=================
        // General Settings
        //=================
        
        JPanel main = addTitledPanel("General Emoticon Settings", 0);

        main.add(
                d.addSimpleBooleanSetting("emoticonsEnabled", "Show emoticons",
                        "Whether to show emotes as icons. "
                        + "Changing this only affects new lines."),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        IgnoredEmotesDialog ignoredEmotesDialog = new IgnoredEmotesDialog(d);
        JButton ignoredEmotesButton = new JButton("Edit Ignored Emotes");
        ignoredEmotesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        ignoredEmotesButton.addActionListener(e -> {
            ignoredEmotesDialog.setLocationRelativeTo(d);
            ignoredEmotesDialog.setVisible(true);
        });
        main.add(ignoredEmotesButton,
                d.makeGbc(2, 0, 3, 1, GridBagConstraints.EAST));
        
        //---------
        // FFZ/BTTV
        //---------
        
        JPanel other = addTitledPanel("Third-Party Emoticons", 1);
        other.add(d.addSimpleBooleanSetting("bttvEmotes",
                "Enable BetterTTV Emotes",
                "Show BetterTTV emoticons"),
                d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting(
                "showAnimatedEmotes",
                "Allow animated emotes",
                "Show animated emotes (currently only BTTV has GIF emotes)"),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));

        final JCheckBox ffz = d.addSimpleBooleanSetting(
                "ffz",
                "Enable FrankerFaceZ (FFZ)",
                "Retrieve custom emotes and possibly mod icon.");
        other.add(ffz,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzMod = d.addSimpleBooleanSetting(
                "ffzModIcon",
                "Enable FFZ Mod Icon",
                "Show custom mod icon for some channels (only works if FFZ is enabled).");
        other.add(ffzMod,
                d.makeGbcSub(0, 2, 1, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzEvent = d.addSimpleBooleanSetting(
                "ffzEvent",
                "Enable FFZ Featured Emotes",
                "Show Featured Emotes available in some Event channels (like Speedrunning Marathons)");
        other.add(ffzEvent,
                d.makeGbcCloser(1, 2, 1, 1, GridBagConstraints.WEST));

        //-----------
        // Emote Size
        //-----------

        // Emote Scale Defintion, used for both Chat and Emotes Dialog
        final Map<Long, String> scaleDef = new LinkedHashMap<>();
        for (int i=50;i<=200;i += 10) {
            if (i == 10) {
                scaleDef.put((long)i, "Normal");
            } else {
                scaleDef.put((long)i, (i)+"%");
            }
        }
        
        // Chat Emote Scale
        main.add(new JLabel("Scale (Chat):"), d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScale = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScale", emoteScale);
        main.add(emoteScale, d.makeGbc(1, 3, 1, 1, GridBagConstraints.CENTER));
        
        // Maximum Emote Height (Chat)
        main.add(new JLabel("Maximum Height:"),
                d.makeGbc(2, 3, 1, 1, GridBagConstraints.WEST));
        main.add(d.addSimpleLongSetting("emoteMaxHeight", 3, true),
                d.makeGbc(3, 3, 1, 1, GridBagConstraints.WEST));
        main.add(new JLabel("pixels"),
                d.makeGbc(4, 3, 1, 1, GridBagConstraints.WEST));
        
        // Emotes Dialog Emote Scale
        main.add(new JLabel("Emotes Dialog:"), d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScaleDialog = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScaleDialog", emoteScaleDialog);
        main.add(emoteScaleDialog, d.makeGbc(1, 4, 1, 1, GridBagConstraints.CENTER));
        
        //------
        // Other
        //------
        main.add(d.addSimpleBooleanSetting(
                "closeEmoteDialogOnDoubleClick",
                "Double-click on emote closes Emote Dialog",
                "Double-clicking on an emote in the Emotes Dialog closes the Dialog"),
                d.makeGbc(0, 5, 3, 1));
        
        // Checkbox status
        ffzMod.setEnabled(false);
        ffzEvent.setEnabled(false);
        ffz.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                ffzMod.setEnabled(ffz.isSelected());
                ffzEvent.setEnabled(ffz.isSelected());
            }
        });
        
        //------
        // Emoji
        //------
        JPanel emojiSettings = addTitledPanel("Emoji", 2);
        
        Map<String, String> emojiSetDef = new LinkedHashMap<>();
        emojiSetDef.put("twemoji", "Twitter Emoji");
        emojiSetDef.put("e1", "Emoji One");
        emojiSetDef.put("none", "None");
        ComboStringSetting emojiSet = new ComboStringSetting(emojiSetDef);
        
        d.addStringSetting("emoji", emojiSet);
        
        emojiSettings.add(new JLabel("Set:"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        emojiSettings.add(emojiSet,
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        emojiSettings.add(d.addSimpleBooleanSetting("emojiReplace",
                "Replace Emoji codes in entered text",
                "Codes like :joy: entered into the inputbox are turned into the"
                        + " corresponding Emoji (Tip: Use TAB-Completion)"),
                d.makeGbcSub(2, 0, 1, 1, GridBagConstraints.WEST));
        
        //---------
        // Cheering
        //---------
        
        Map<String, String> cheeringTypeDef = new LinkedHashMap<>();
        cheeringTypeDef.put("none", "Text Only");
        cheeringTypeDef.put("static", "Static Images");
        cheeringTypeDef.put("animated", "Animated");
        ComboStringSetting cheersType = new ComboStringSetting(cheeringTypeDef);
        
        d.addStringSetting("cheersType", cheersType);
        
        main.add(new JLabel("Cheers (Bits):"),
                d.makeGbc(0, 6, 1, 1, GridBagConstraints.CENTER));
        main.add(cheersType,
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.WEST));
    }
    
    private static class IgnoredEmotesDialog extends JDialog {
        
        private IgnoredEmotesDialog(SettingsDialog d) {
            super(d);
            setTitle("Ignored Emotes");
            setModal(true);
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(d.addListSetting("ignoredEmotes", 180, 220, false, true), gbc);
            
            gbc = d.makeGbc(1, 0, 1, 1);
            gbc.anchor = GridBagConstraints.NORTH;
            add(new JLabel(IGNORED_INFO), gbc);
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> {
                setVisible(false);
            });
            gbc = d.makeGbc(0, 1, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(closeButton, gbc);
            
            pack();
            GuiUtil.installEscapeCloseOperation(this);
        }
        
    }
    
}
