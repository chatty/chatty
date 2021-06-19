
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
    private int minLen;
    private float minSim;
    private int method;
    private char[] ignoredChars;
    
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
    
    /**
     * Must be used in EDT.
     * 
     * @param user
     * @param localUser
     * @param text
     * @param tags
     * @return 
     */
    public MsgTags check(User user, User localUser, String text, MsgTags tags) {
        if (matcher == null) {
            return tags;
        }
        if (text.length() < minLen) {
            return tags;
        }
        if (!matcher.matches(Highlighter.HighlightItem.Type.REGULAR, text, user, localUser, tags)) {
            return tags;
        }
        int repCount = user.getNumberOfSimilarChatMessages(text, method, time, minSim, minLen, ignoredChars) + 1;
        if (repCount >= minRep) {
            // Plus one count to include the current message
            return MsgTags.addTag(tags, TAGS_KEY, String.valueOf(repCount));
        }
        return tags;
    }
    
    /**
     * Must be used in EDT.
     * 
     * @param user
     * @param a
     * @param b
     * @return 
     */
    public int getPercentage(User user, String a, String b) {
        if (matcher == null) {
            return 0;
        }
        if (a.length() < minLen || b.length() < minLen) {
            return 0;
        }
        a = StringUtil.prepareForSimilarityComparison(a, ignoredChars);
        b = StringUtil.prepareForSimilarityComparison(b, ignoredChars);
        float sim = StringUtil.checkSimilarity(a, b, minSim, method);
        return (int)Math.floor(sim * 100);
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
            minLen = settings.getInt("repeatMsgLen");
            method = settings.getInt("repeatMsgMethod");
            ignoredChars = StringUtil.getCharsFromString(settings.getString("repeatMsgIgnored"));
            if (ignoredChars.length == 0) {
                ignoredChars = null;
            }
        }
        else {
            matcher = null;
        }
    }
    
}
