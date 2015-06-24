
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
        
        JPanel main = addTitledPanel("General Settings", 0);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbcCloser(0, 0, 2, 1, GridBagConstraints.WEST);
        main.add(
                d.addSimpleBooleanSetting("emoticonsEnabled", "Show emoticons",
                        "Whether to show emotes as icons.\n"
                        + "Changing this only affects new lines."),
                gbc);
        
        gbc = d.makeGbcCloser(2, 0, 3, 1, GridBagConstraints.WEST);
        main.add(
                d.addSimpleBooleanSetting("bttvEmotes", "Enable BetterTTV Emotes",
                        "Show BetterTTV emoticons (most of them at least)"),
                gbc);

        
        gbc = d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST);
        final JCheckBox ffz = d.addSimpleBooleanSetting("ffz","Enable FrankerFaceZ (FFZ)",
                "Retrieve custom emotes and possibly mod icon.");
        main.add(ffz,
                gbc);
        
        
        final JCheckBox ffzMod = d.addSimpleBooleanSetting("ffzModIcon","Enable FFZ Mod Icon",
                "Show custom mod icon for some channels (only works if FFZ is enabled).");
        gbc = d.makeGbc(2, 1, 3, 1, GridBagConstraints.WEST);
        main.add(ffzMod,
                gbc);
        
        main.add(new JLabel("Maximum Height:"), d.makeGbc(2, 2, 1, 1, GridBagConstraints.WEST));
        main.add(d.addSimpleLongSetting("emoteMaxHeight", 3, true), d.makeGbc(3, 2, 1, 1, GridBagConstraints.WEST));
        main.add(new JLabel("pixels"), d.makeGbc(4, 2, 1, 1, GridBagConstraints.WEST));
        //main.add(new JLabel("(Max height of 0 means no max height.)"), d.makeGbc(2, 3, 3, 1));
        
        main.add(new JLabel("Scale:"), d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        
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
        main.add(emoteScale, d.makeGbc(1, 2, 1, 1, GridBagConstraints.CENTER));
        
        main.add(new JLabel("Emotes Dialog:"), d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        
        ComboLongSetting emoteScaleDialog = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScaleDialog", emoteScaleDialog);
        main.add(emoteScaleDialog, d.makeGbc(1, 3, 1, 1, GridBagConstraints.CENTER));
        
        ffzMod.setEnabled(false);
        ffz.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                ffzMod.setEnabled(ffz.isSelected());
            }
        });
        
        
        JPanel ignored = addTitledPanel("Ignored Emotes", 1);
        
        gbc = d.makeGbc(0, 0, 1, 1);
        ignored.add(d.addListSetting("ignoredEmotes", 150, 130, false), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.anchor = GridBagConstraints.NORTH;
        ignored.add(new JLabel(IGNORED_INFO), gbc);
        
    }
    
}
