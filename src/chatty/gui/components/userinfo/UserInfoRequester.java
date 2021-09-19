
package chatty.gui.components.userinfo;

import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.UserInfo;
import java.util.function.Consumer;

/**
 *
 * @author tduva
 */
public interface UserInfoRequester {

    Follower getSingleFollower(String stream, String streamId, String user, String userId, boolean refresh);

    UserInfo getCachedUserInfo(String channel, Consumer<UserInfo> result);
}
