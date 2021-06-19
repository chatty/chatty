
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
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
    
    protected EmoteSettings(SettingsDialog d) {
        
        //==================
        // General Settings
        //==================
        JPanel main = addTitledPanel(Language.getString("settings.section.emoticons"), 0);

        main.add(
                d.addSimpleBooleanSetting("emoticonsEnabled"),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        IgnoredEmotesDialog ignoredEmotesDialog = new IgnoredEmotesDialog(d);
        JButton ignoredEmotesButton = new JButton(Language.getString("settings.emoticons.button.editIgnored"));
        ignoredEmotesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        ignoredEmotesButton.addActionListener(e -> {
            ignoredEmotesDialog.setLocationRelativeTo(d);
            ignoredEmotesDialog.setVisible(true);
        });
        main.add(ignoredEmotesButton,
                d.makeGbc(2, 0, 3, 1, GridBagConstraints.EAST));
        
        //------------
        // Emote Size
        //------------
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
        main.add(new JLabel(Language.getString("settings.emoticons.chatScale")),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScale = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScale", emoteScale);
        main.add(emoteScale, d.makeGbc(1, 3, 1, 1, GridBagConstraints.CENTER));
        
        // Maximum Emote Height (Chat)
        main.add(new JLabel(Language.getString("settings.emoticons.maxHeight")),
                d.makeGbc(2, 3, 1, 1, GridBagConstraints.EAST));
        main.add(d.addSimpleLongSetting("emoteMaxHeight", 3, true),
                d.makeGbc(3, 3, 1, 1, GridBagConstraints.WEST));
        main.add(new JLabel(Language.getString("settings.emoticons.maxHeightPixels")),
                d.makeGbc(4, 3, 1, 1, GridBagConstraints.WEST));
        
        // Emotes Dialog Emote Scale
        main.add(new JLabel(Language.getString("settings.emoticons.dialogScale")),
                d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScaleDialog = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScaleDialog", emoteScaleDialog);
        main.add(emoteScaleDialog, d.makeGbc(1, 4, 1, 1, GridBagConstraints.CENTER));
        
        //-------
        // Other
        //-------
        main.add(d.addSimpleBooleanSetting("closeEmoteDialogOnDoubleClick"),
                d.makeGbc(0, 5, 3, 1));
        
        //----------
        // Cheering
        //----------
        Map<String, String> cheeringTypeDef = new LinkedHashMap<>();
        cheeringTypeDef.put("none", Language.getString("settings.emoticons.cheers.option.text"));
        cheeringTypeDef.put("static", Language.getString("settings.emoticons.cheers.option.static"));
        cheeringTypeDef.put("animated", Language.getString("settings.emoticons.cheers.option.animated"));
        ComboStringSetting cheersType = new ComboStringSetting(cheeringTypeDef);
        
        d.addStringSetting("cheersType", cheersType);
        
        main.add(new JLabel(Language.getString("settings.emoticons.cheers")),
                d.makeGbc(0, 6, 1, 1, GridBagConstraints.CENTER));
        main.add(cheersType,
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.WEST));
        
        //--------------------------
        // Animated
        //--------------------------
        main.add(d.addSimpleBooleanSetting("animatedEmotes"),
                d.makeGbc(0, 7, 3, 1, GridBagConstraints.WEST));
        
        //==========
        // FFZ/BTTV
        //==========
        
        JPanel other = addTitledPanel(Language.getString("settings.section.3rdPartyEmotes"), 1);
        other.add(d.addSimpleBooleanSetting("bttvEmotes"),
                d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("showAnimatedEmotes"),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));

        final JCheckBox ffz = d.addSimpleBooleanSetting("ffz");
        other.add(ffz,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzMod = d.addSimpleBooleanSetting("ffzModIcon");
        other.add(ffzMod,
                d.makeGbcSub(0, 2, 1, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzEvent = d.addSimpleBooleanSetting("ffzEvent");
        other.add(ffzEvent,
                d.makeGbcCloser(1, 2, 1, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(ffz, ffzMod, ffzEvent);

        //=======
        // Emoji
        //=======
        JPanel emojiSettings = addTitledPanel(Language.getString("settings.section.emoji"), 2);
        
        Map<String, String> emojiSetDef = new LinkedHashMap<>();
        emojiSetDef.put("twemoji", "Twemoji (Twitter)");
        emojiSetDef.put("e1", "Emoji One [outdated]");
        emojiSetDef.put("none", Language.getString("settings.emoji.option.none"));
        ComboStringSetting emojiSet = new ComboStringSetting(emojiSetDef);
        
        d.addStringSetting("emoji", emojiSet);
        
        emojiSettings.add(new JLabel(Language.getString("settings.emoji.set")),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        emojiSettings.add(emojiSet,
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        emojiSettings.add(d.addSimpleBooleanSetting("emojiReplace"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
    }
    
    private static class IgnoredEmotesDialog extends JDialog {
        
        private IgnoredEmotesDialog(SettingsDialog d) {
            super(d);
            setTitle(Language.getString("settings.ignoredEmotes.title"));
            setModal(true);
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(d.addListSetting("ignoredEmotes", "Ignored Emote", 180, 220, false, true), gbc);
            
            gbc = d.makeGbc(1, 0, 1, 1);
            gbc.anchor = GridBagConstraints.NORTH;
            add(new JLabel("<html><body style='width:160px;'>"
                    + "<p style='padding:5px;'>"+Language.getString("settings.ignoredEmotes.info1")+"</p>"
                    + "<p style='padding:5px;'>"+Language.getString("settings.ignoredEmotes.info2")+"</p>"), gbc);
            
            JButton closeButton = new JButton(Language.getString("dialog.button.close"));
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
