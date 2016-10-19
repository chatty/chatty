
package chatty.util;

/**
 *
 * @author tduva
 */
public class RawMessageTest {
    
    public static String simulateIRC(String channel, String parameters) {
        String split[] = parameters.split(" ", 1);
        String type = split[0];
        String options = null;
        if (split.length == 2) {
            options = split[1];
        }
        
        if (type.equals("sub")) {
            return ":twitchnotify!twitchnotify@twitchnotify.tmi.twitch.tv PRIVMSG "+channel+" :USERNAME just subscribed!";
        }
        if (type.equals("resub")) {
            return "@badges=subscriber/1;color=;display-name=USERNAME;emotes=;id=123;login=username;mod=0;msg-id=resub;msg-param-months=4;subscriber=1;system-msg=USERNAME\\ssubscribed\\sfor\\s4\\smonths\\sin\\sa\\srow!;tmi-sent-ts=1475037717295;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :Hi strimmer are you gud strimmer";
        }
        return null;
    }
    
}
