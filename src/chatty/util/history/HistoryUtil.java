
package chatty.util.history;

import chatty.gui.Highlighter;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;

/**
 *
 * @author tduva
 */
public class HistoryUtil {

    public static boolean checkAllowMatch(MsgTags tags,
                                            String type,
                                            Highlighter.HighlightItem hlItem,
                                            Settings settings) {
        if (tags == null || !tags.isHistoricMsg()) {
            return true;
        }
        if (settings.getBoolean("historyMessage" + type)) {
            return true;
        }
        return hlItem != null && hlItem.matchHistoric();
    }
    
}
