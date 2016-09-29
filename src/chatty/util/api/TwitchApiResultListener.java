
package chatty.util.api;

import chatty.util.api.usericons.Usericon;
import chatty.util.api.TwitchApi.RequestResult;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Interface definition for API response results.
 * 
 * @author tduva
 */
public interface TwitchApiResultListener {
    void receivedEmoticons(Set<Emoticon> emoticons);
    void receivedUsericons(List<Usericon> icons);
    void gameSearchResult(Set<String> games);
    void tokenVerified(String token, TokenInfo tokenInfo);
    void runCommercialResult(String stream, String text, RequestResult result);
    void putChannelInfoResult(RequestResult result);
    void receivedChannelInfo(String channel, ChannelInfo info, RequestResult result);
    void accessDenied();
    void receivedFollowers(FollowerInfo followerInfo);
    void newFollowers(FollowerInfo followerInfo);
    void receivedSubscribers(FollowerInfo info);
    
    /**
     * The correctly capitalized name for a user.
     * 
     * @param name All-lowercase name
     * @param displayName Correctly capitalized name
     */
    void receivedDisplayName(String name, String displayName);
    
    void receivedServer(String channel, String server);
    
    /**
     * Info retrieved from chat properties.
     * 
     * @param chatInfo Can be null if an error occured
     */
    void receivedChatInfo(ChatInfo chatInfo);
    
    /**
     * Human-readable result message.
     * 
     * @param message 
     */
    void followResult(String message);
}
