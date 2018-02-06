
package chatty.gui;

import chatty.Helper;
import chatty.util.StringUtil;
import java.awt.Component;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Helper class to build and open Twitch related URLs.
 * 
 * @author tduva
 */
public class TwitchUrl {
    
    public static void removeInvalidStreams(Collection<String> streams) {
        Iterator<String> it = streams.iterator();
        while (it.hasNext()) {
            if (!Helper.validateChannel(it.next())) {
                it.remove();
            }
        }
    }
    
    public static void openTwitchProfile(String nick, Component parent) {
        if (nick == null) {
            JOptionPane.showMessageDialog(parent, "Unable to open Twitch Profile URL (Not on a channel)",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String url = makeTwitchProfileUrl(nick);
            UrlOpener.openUrlPrompt(parent, url);
        }
    }
    
    public static void openTwitchStream(String nick, Component parent) {
        openTwitchStream(nick, false, parent);
    }
    
    public static void openTwitchStream(String nick, boolean popout, Component parent) {
        if (nick == null) {
            JOptionPane.showMessageDialog(parent, "Unable to open Twitch Stream URL (Not on a channel)",
                    "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            String url = makeTwitchStreamUrl(nick, popout);
            UrlOpener.openUrlPrompt(parent, url);
        }
    }
    
    public static String makeTwitchProfileUrl(String channel) {
        return "http://twitch.tv/" + StringUtil.toLowerCase(channel) + "/profile";
    }
    
    public static String makeTwitchStreamUrl(String channel, boolean popout) {
        String url = "http://twitch.tv/" + StringUtil.toLowerCase(channel) + "";
        if (popout) {
            url += "/popout";
        }
        return url;
    }
    
    public static String makeTwitchPlayerUrl(String channel) {
        return "http://player.twitch.tv/?channel="+StringUtil.toLowerCase(channel);
    }
    
    public static String makeTwitchTurboUrl() {
        return "http://twitch.tv/turbo";
    }
    
    public static String makeFFZUrl() {
        return "http://frankerfacez.com";
    }
    
    public static String makeFFZUrl(int id) {
        return "http://www.frankerfacez.com/emoticons/"+id;
    }
    
    public static String makeFFZUrl(String user) {
        return "http://www.frankerfacez.com/emoticons/user/"+StringUtil.toLowerCase(user);
    }
    
    public static String makeBttvUrl() {
        return "http://www.nightdev.com/betterttv/";
    }
    
    public static String makeTwitchemotesUrl(int id) {
        return "https://twitchemotes.com/emote/"+id;
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
