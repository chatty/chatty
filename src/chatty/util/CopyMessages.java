
package chatty.util;

import chatty.User;
import chatty.util.settings.Settings;

/**
 *
 * @author tduva
 */
public class CopyMessages {
    
    public static void copyMessage(Settings settings, User user, String message,
            boolean highlighted) {
        if (!settings.getBoolean("cmEnabled")) {
            return;
        }
        if (settings.getBoolean("cmHighlightedOnly") && !highlighted) {
            return;
        }
        String channel = settings.getString("cmChannel");
        if (!channel.trim().isEmpty() && !channel.equalsIgnoreCase(user.getChannel())) {
            return;
        }
        String text = settings.getString("cmTemplate");
        text = text.replaceFirst("\\{user\\}", user.getDisplayNick());
        text = text.replaceFirst("\\{message\\}", message);
        MiscUtil.copyToClipboard(text);
    }
    
}
