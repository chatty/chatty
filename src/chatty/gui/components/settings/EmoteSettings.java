
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JCheckBox;
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
        
        JPanel main = addTitledPanel("General Settings", 0);
        
        GridBagConstraints gbc;

        main.add(
                d.addSimpleBooleanSetting("emoticonsEnabled", "Show emoticons",
                        "Whether to show emotes as icons.\n"
                        + "Changing this only affects new lines."),
                d.makeGbcCloser(0, 0, 2, 1, GridBagConstraints.WEST));
        
        //---------
        // FFZ/BTTV
        //---------
        main.add(d.addSimpleBooleanSetting("bttvEmotes",
                "Enable BetterTTV Emotes",
                "Show BetterTTV emoticons"),
                d.makeGbcCloser(2, 0, 3, 1, GridBagConstraints.WEST));

        final JCheckBox ffz = d.addSimpleBooleanSetting(
                "ffz",
                "Enable FrankerFaceZ (FFZ)",
                "Retrieve custom emotes and possibly mod icon.");
        main.add(ffz,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzMod = d.addSimpleBooleanSetting(
                "ffzModIcon",
                "Enable FFZ Mod Icon",
                "Show custom mod icon for some channels (only works if FFZ is enabled).");
        main.add(ffzMod,
                d.makeGbcSub(0, 2, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzEvent = d.addSimpleBooleanSetting(
                "ffzEvent",
                "Enable FFZ Featured Emotes",
                "Show Featured Emotes available in some Event channels (like Speedrunning Marathons)");
        main.add(ffzEvent,
                d.makeGbcCloser(2, 2, 3, 1, GridBagConstraints.WEST));
        
        main.add(d.addSimpleBooleanSetting(
                "showAnimatedEmotes",
                "Allow animated emotes",
                "Show animated emotes (currently only BTTV has GIF emotes)"),
                d.makeGbc(2, 3, 3, 1, GridBagConstraints.WEST));
        
        //-----------
        // Emote Size
        //-----------
        main.add(new JLabel("Maximum Height:"),
                d.makeGbc(2, 4, 1, 1, GridBagConstraints.WEST));
        main.add(d.addSimpleLongSetting("emoteMaxHeight", 3, true),
                d.makeGbc(3, 4, 1, 1, GridBagConstraints.WEST));
        main.add(new JLabel("pixels"),
                d.makeGbc(4, 4, 1, 1, GridBagConstraints.WEST));
        
        main.add(new JLabel("Scale:"), d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        
        final Map<Long, String> scaleDef = new LinkedHashMap<>();
        for (int i=50;i<=200;i += 10) {
            if (i == 10) {
                scaleDef.put((long)i, "Normal");
            } else {
                scaleDef.put((long)i, (i)+"%");
            }
        }
        ComboLongSetting emoteScale = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScale", emoteScale);
        main.add(emoteScale, d.makeGbc(1, 3, 1, 1, GridBagConstraints.CENTER));
        
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
        
        Map<String, String> emojiSetDef = new LinkedHashMap<>();
        emojiSetDef.put("twemoji", "Twitter Emoji");
        emojiSetDef.put("e1", "Emoji One");
        emojiSetDef.put("none", "None");
        ComboStringSetting emojiSet = new ComboStringSetting(emojiSetDef);
        
        d.addStringSetting("emoji", emojiSet);
        
        main.add(new JLabel("Emoji Set:"),
                d.makeGbc(0, 6, 1, 1, GridBagConstraints.WEST));
        main.add(emojiSet,
                d.makeGbc(1, 6, 1, 1, GridBagConstraints.WEST));
        
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
                d.makeGbc(2, 6, 1, 1, GridBagConstraints.CENTER));
        main.add(cheersType,
                d.makeGbc(3, 6, 2, 1, GridBagConstraints.EAST));
        
        //===============
        // Ignored Emotes
        //===============
        
        JPanel ignored = addTitledPanel("Ignored Emotes", 1, true);
        
        gbc = d.makeGbc(0, 0, 1, 1);
        ignored.add(d.addListSetting("ignoredEmotes", 150, 130, false, true), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.anchor = GridBagConstraints.NORTH;
        ignored.add(new JLabel(IGNORED_INFO), gbc);
        
    }
    
}
