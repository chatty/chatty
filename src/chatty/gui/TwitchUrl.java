
package chatty.gui;

import chatty.Helper;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon;
import java.awt.Component;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Helper class to build and open Twitch related URLs.
 * 
 * @author tduva
 */
public class TwitchUrl {
    
    public static void removeInvalidStreams(Collection<String> streams) {
        Iterator<String> it = streams.iterator();
        while (it.hasNext()) {
            if (!Helper.isValidChannel(it.next())) {
                it.remove();
            }
        }
    }
    
    public static String makeTwitchProfileUrl(String channel) {
        return "https://twitch.tv/" + StringUtil.toLowerCase(channel) + "/profile";
    }
    
    public static String makeTwitchStreamUrl(String channel) {
        return "https://twitch.tv/" + StringUtil.toLowerCase(channel);
    }
    
    public static String makeTwitchPlayerUrl(String channel) {
        return "https://player.twitch.tv/?channel="+StringUtil.toLowerCase(channel)+"&parent=twitch.tv";
    }
    
    public static String makeTwitchChatUrl(String channel) {
        return "https://www.twitch.tv/popout/"+StringUtil.toLowerCase(channel)+"/chat?popout=";
    }
    
    public static String makeTwitchTurboUrl() {
        return "https://twitch.tv/turbo";
    }
    
    public static String makeFFZUrl() {
        return "https://frankerfacez.com";
    }
    
    public static String makeFFZUserUrl(String user) {
        return "https://www.frankerfacez.com/emoticons/user/"+StringUtil.toLowerCase(user);
    }
    
    public static String makeBttvUrl() {
        return "https://www.nightdev.com/betterttv/";
    }
    
    public static String makeEmoteUrl(Emoticon.Type type, String id) {
        switch (type) {
            case FFZ: return "https://www.frankerfacez.com/emoticons/"+id;
            case TWITCH: return "https://twitchemotes.com/emote/"+id;
            case BTTV: return "https://betterttv.com/emotes/"+id;
        }
        return null;
    }
    
    public static final String MULTITWITCH = "http://multitwitch.tv/";
    public static final String SPEEDRUNTV = "http://speedrun.tv/";
    public static final String KADGAR = "http://kadgar.net/live/";
    
    public static void openMultitwitch(List<String> streams, Component parent, String type) {
        if (streams == null || streams.isEmpty()) {
            return;
        }
        UrlOpener.openUrlPrompt(parent, makeMultitwitchUrl(streams, type));
    }
    
    public static String makeMultitwitchUrl(List<String> streams, String type) {
        String streamsText = StringUtil.join(streams, "/");
        String url = type+streamsText;
        return url;
    }
    
    public static String makeSrlRaceLink(String id) {
        return "http://speedrunslive.com/race/?id="+id;
    }
    
    public static String makeSrtRaceLink(String id) {
        return "http://speedrun.tv/?race="+id;
    }
    
    public static String makeSrlIrcLink(String id) {
        return "irc://irc.speedrunslive.com/srl-"+id;
    }
}
