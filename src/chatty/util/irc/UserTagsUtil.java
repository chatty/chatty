
package chatty.util.irc;

import chatty.Helper;
import chatty.User;
import chatty.util.StringUtil;

/**
 *
 * @author tduva
 */
public class UserTagsUtil {

    public static boolean updateUserFromTags(User user, MsgTags tags) {
        if (tags.isEmpty()) {
            return false;
        }
        /**
         * Any and all tag values may be null, so account for that when checking
         * against them.
         */
        // Whether anything in the user changed to warrant an update
        boolean changed = false;

        IrcBadges badges = IrcBadges.parse(tags.get("badges"));
        if (user.setTwitchBadges(badges)) {
            changed = true;
        }

        IrcBadges badgeInfo = IrcBadges.parse(tags.get("badge-info"));
        String subMonths = badgeInfo.get("subscriber");
        if (subMonths == null) {
            subMonths = badgeInfo.get("founder");
        }
        if (subMonths != null) {
            user.setSubMonths(Helper.parseShort(subMonths, (short) 0));
        }
        
        if (user.setDisplayNick(StringUtil.trim(tags.get("display-name")))) {
            changed = true;
        }

        // Update color
        String color = tags.get("color");
        if (color != null && !color.isEmpty()) {
            user.setColor(color);
        }

        // Update user status
        boolean turbo = tags.isTrue("turbo") || badges.hasId("turbo") || badges.hasId("premium");
        if (user.setTurbo(turbo)) {
            changed = true;
        }
        boolean subscriber = badges.hasId("subscriber") || badges.hasId("founder");
        if (user.setSubscriber(subscriber)) {
            changed = true;
        }
        if (user.setVip(badges.hasId("vip"))) {
            changed = true;
        }
        if (user.setModerator(badges.hasId("moderator"))) {
            changed = true;
        }
        if (user.setAdmin(badges.hasId("admin"))) {
            changed = true;
        }
        if (user.setStaff(badges.hasId("staff"))) {
            changed = true;
        }
        
        user.setId(tags.get("user-id"));
        return changed;
    }
    
}
