
package chatty.util;

import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;

/**
 *
 * @author tduva
 */
public class RepeatMsgHelper {
    
    private static final String TAGS_KEY = "chatty-repeatmsg-count";

    private final Settings settings;
    
    private Highlighter.HighlightItem matcher;
    private long time;
    private int minRep;
    private float minSim;
    
    public RepeatMsgHelper(Settings settings) {
        this.settings = settings;
        settings.addSettingChangeListener((setting, type, value) -> {
            if (setting.startsWith("repeatMsg")) {
                GuiUtil.edt(() -> {
                    loadSettings();
                });
            }
        });
    }
    
    public MsgTags check(User user, User localUser, String text, MsgTags tags) {
        if (matcher == null) {
            return tags;
        }
        if (!matcher.matches(Highlighter.HighlightItem.Type.REGULAR, text, user, localUser, tags)) {
            return tags;
        }
        int repCount = user.getNumberOfSimilarChatMessages(text, time, minSim) + 1;
        if (repCount >= minRep) {
            // Plus one count to include the current message
            return MsgTags.addTag(tags, TAGS_KEY, String.valueOf(repCount));
        }
        return tags;
    }
    
    public static int getRepeatMsg(MsgTags tags) {
        String value = tags.get(TAGS_KEY);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException ex) {
                return 0;
            }
        }
        return 0;
    }
    
    public void loadSettings() {
        if (settings.getBoolean("repeatMsg")) {
            matcher = new HighlightItem(settings.getString("repeatMsgMatch"));
            time = settings.getLong("repeatMsgTime");
            minRep = settings.getInt("repeatMsgRep");
            minSim = settings.getLong("repeatMsgSim") / 100f;
        }
        else {
            matcher = null;
        }
    }
    
}
