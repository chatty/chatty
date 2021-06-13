
package chatty.gui.components.userinfo;

import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;

/**
 *
 * @author tduva
 */
public interface UserInfoRequester {

    Follower getSingleFollower(String stream, String streamId, String user, String userId, boolean refresh);

    ChannelInfo getCachedChannelInfo(String channel, String id);
}
