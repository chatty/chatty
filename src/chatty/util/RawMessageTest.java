
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
        if (type.equals("resub2")) {
            return "@badges=subscriber/1;color=;display-name=USERNAME;emotes=;id=123;login=username;mod=0;msg-id=resub;msg-param-months=4;subscriber=1;system-msg=USERNAME\\ssubscribed\\sfor\\s4\\smonths\\sin\\sa\\srow!;tmi-sent-ts=1475037717295;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("sub2")) {
            return "@badges=staff/1,broadcaster/1,turbo/1;color=#008000;display-name=TWITCH_UserName;emotes=;mod=0;msg-id=sub;msg-param-months=6;room-id=1337;subscriber=1;msg-param-sub-plan=Prime;msg-param-sub-plan-name=Channel\\sSubscription\\s(display_name);system-msg=TWITCH_UserName\\shas\\ssubscribed\\sfor\\s6\\smonths!;login=twitch_username;turbo=1;user-id=1337;user-type=staff :tmi.twitch.tv USERNOTICE "+channel+" :Great stream -- keep it up!";
        }
        if (type.equals("sub3")) {
            return "@badges=subscriber/0,bits/100;color=#B22222;display-name=TWITCH_UserName;emotes=;id=123;login=twitch_username;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=USER2;msg-param-recipient-id=123;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Abc;msg-param-sub-plan=1000;room-id=123;subscriber=1;system-msg=TWITCH_UserName\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sUSER2!;tmi-sent-ts=1520532381349;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("subgift")) {
            return "@badges=;color=;display-name=user1;emotes=;id=123;login=user1;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=user2;msg-param-recipient-id=44452165;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Channel\\sSubscription\\s(LIRIK);msg-param-sub-plan=1000;room-id=123;subscriber=0;system-msg=user1\\sgifted\\sa\\s$4.99\\ssub\\sto\\suser2!;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("bits")) {
            return "@badges=bits/1000;bits=1;color=#FF7F50;display-name=tduvaTest;emotes=;id=123;mod=0;subscriber=0;turbo=0;user-type= :tduvatest!tduvatest@tduvatest.tmi.twitch.tv PRIVMSG "+channel+" :"+options;
        }
        if (type.equals("autohost")) {
            return ":jtv!jtv@jtv.tmi.twitch.tv PRIVMSG "+localUsername+" :UserName is now auto hosting you.";
        }
        if (type.equals("whisper")) {
            return "@badges=turbo/1;color=#0000FF;display-name=tduva;emotes=;message-id=161;turbo=1;user-id=36194025;user-type= :tduva!tduva@tduva.tmi.twitch.tv WHISPER "+localUsername+" :abc";
        }
        if (type.equals("charity")) {
            return "@badges=partner/1;color=#8A2BE2;display-name=Twitch;emotes=;id=f4ab0ee7-90bd-434c-9120-03952d84fdf8;login=twitch;mod=0;msg-id=charity;room-id=1337;subscriber=0;system-msg=$14,332\\stotal\\sraised\\sso\\sfar\\sfor\\sExtra\\sLife!\\s12\\smore\\sdays\\sto\\sshow\\ssupport.\\sCheer\\sand\\sinclude\\s#charity.\\sLearn\\sMore\\sat\\slink.twitch.tv/cheer4kids;tmi-sent-ts=1504738162878;turbo=0;user-id=12826;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("messagewithemotes")) {
            return "@badges=;color=#008000;display-name=Abc;emote-only=1;emotes=33:0-7;id=fwaef;mod=0;subscriber=0;tmi-sent-ts=1508516209239;turbo=0;user-type= :abc!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :DansGame ";
        }
        if (type.equals("raid")) {
            return "@badges=turbo/1;color=#9ACD32;display-name=TestChannel;emotes=;id=3d830f12-795c-447d-af3c-ea05e40fbddb;login=testchannel;mod=0;msg-id=raid;msg-param-displayName=TestChannel;msg-param-login=testchannel;msg-param-viewerCount=15;room-id=56379257;subscriber=0;system-msg=15\\sraiders\\sfrom\\sTestChannel\\shave\\sjoined\\n!;tmi-sent-ts=1507246572675;tmi-sent-ts=1507246572675;turbo=1;user-id=123456;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("raid2")) {
            return "@badges=turbo/1;color=#9ACD32;display-name=TestChannel;emotes=;id=3d830f12-795c-447d-af3c-ea05e40fbddb;login=testchannel;mod=0;msg-id=raid;msg-param-displayName=TestChannel;msg-param-login=testchannel;msg-param-viewerCount=15;room-id=56379257;subscriber=0;system-msg=15\\sraiders\\sfrom\\sTestChannel\\shave\\sjoined\\n!;tmi-sent-ts=1507246572675;tmi-sent-ts=1507246572675;turbo=1;user-id=123456;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :Hyyyype! \\o/";
        }
        if (type.equals("message")) {
            return "@badges=;color=#008000;display-name=Abc;emote-only=1;emotes=33:0-7;id=fwaef;mod=0;subscriber=0;tmi-sent-ts=1508516209239;turbo=0;user-type= :abc!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :"+options;
        }
        if (type.equals("hello")) {
            return "@badges=premium/1;color=#000000;display-name=USER;emotes=30259:0-6;id=ID;login=user;mod=0;msg-id=ritual;msg-param-ritual-name=new_chatter;room-id=1234;subscriber=0;system-msg=@USER\\sis\\snew\\shere.\\sSay\\shello!;tmi-sent-ts=12345;turbo=0;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :HeyGuys";
        }
        return null;
    }
    
}
