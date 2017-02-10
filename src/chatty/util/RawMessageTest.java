
package chatty.util;

import chatty.util.api.CheerEmoticon;

/**
 *
 * @author tduva
 */
public class RawMessageTest {
    
    public static String simulateIRC(String channel, String parameters, String localUsername) {
        String split[] = parameters.split(" ", 2);
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
        if (type.equals("bits")) {
            return "@badges=bits/1000;bits=1;color=#FF7F50;display-name=tduvaTest;emotes=;id=123;mod=0;subscriber=0;turbo=0;user-type= :tduvatest!tduvatest@tduvatest.tmi.twitch.tv PRIVMSG "+channel+" :"+options;
        }
        if (type.equals("autohost")) {
            return ":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG "+localUsername+" :UserName is now auto hosting you.";
        }
        return null;
    }
    
}
