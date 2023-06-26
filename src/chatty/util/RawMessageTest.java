
package chatty.util;

import chatty.util.api.CheerEmoticon;
import java.util.concurrent.ThreadLocalRandom;

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
            return "@badges=staff/1,broadcaster/1,turbo/1;color=#008000;display-name=Tduvatest;emotes=;mod=0;msg-id=sub;msg-param-months=6;room-id=1337;subscriber=1;msg-param-sub-plan=Prime;msg-param-sub-plan-name=Channel\\sSubscription\\s(display_name);system-msg=Tduvatest\\shas\\ssubscribed\\sfor\\s6\\smonths!;login=tduvatest;turbo=1;user-id=1337;user-type=staff :tmi.twitch.tv USERNOTICE "+channel+" :"+(options != null ? options : "Great stream -- keep it up!");
        }
        if (type.equals("sub3")) {
            return "@badges=subscriber/0,bits/100;color=#B22222;display-name=TWITCH_UserName;emotes=;id=123;login=twitch_username;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=USER2;msg-param-recipient-id=123;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Abc;msg-param-sub-plan=1000;room-id=123;subscriber=1;system-msg=TWITCH_UserName\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sUSER2!;tmi-sent-ts=1520532381349;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("subgift")) {
            return "@badges=;color=;display-name=user1;emotes=;id=123;login=user1;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=user2;msg-param-recipient-id=44452165;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Channel\\sSubscription\\s(LIRIK);msg-param-sub-plan=1000;room-id=123;subscriber=0;system-msg=user1\\sgifted\\sa\\s$4.99\\ssub\\sto\\suser2!;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("subgift3m")) {
            // Gifted 3 months, not included in system-msg (just added msg-param-gift-months tag)
            return "@badges=;color=;display-name=user1;emotes=;id=123;login=user1;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=user2;msg-param-recipient-id=44452165;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Channel\\sSubscription\\s(LIRIK);msg-param-sub-plan=1000;msg-param-gift-months=3;room-id=123;subscriber=0;system-msg=user1\\sgifted\\sa\\s$4.99\\ssub\\sto\\suser2!;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("subgift3m2")) {
            // Gifted 3 months, included in system-msg (based on older message, but close enough)
            return "@badges=;color=;display-name=user1;emotes=;id=123;login=user1;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=user2;msg-param-recipient-id=44452165;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Channel\\sSubscription\\s(LIRIK);msg-param-sub-plan=1000;msg-param-gift-months=3;room-id=123;subscriber=0;system-msg=user1\\sgifted\\s3\\smonths\\sof\\s$4.99\\ssub\\sto\\suser2!;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("anonsubgift")) {
            return "@badges=;color=;emotes=;id=123;login=channame;mod=0;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name=user2;msg-param-recipient-id=44452165;msg-param-recipient-user-name=user2;msg-param-sub-plan-name=Channel\\sSubscription\\s(LIRIK);msg-param-sub-plan=1000;room-id=123;subscriber=0;system-msg=An\\sanonymous\\suser\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sabc!\\s;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("anonsubgift3m")) {
            return "@badge-info=;badges=;color=;display-name=AnAnonymousGifter;emotes=;flags=;id=1234;login=ananonymousgifter;mod=0;msg-id=subgift;msg-param-fun-string=FunStringTwo;msg-param-gift-months=3;msg-param-months=22;msg-param-origin-id=da\\s39\\sa3\\see\\s5e\\s6b\\s4b\\s0d\\s32\\s55\\sbf\\sef\\s95\\s60\\s18\\s90\\saf\\sd8\\s07\\s09;msg-param-recipient-display-name=USERNAME;msg-param-recipient-id=1234;msg-param-recipient-user-name=username;msg-param-sub-plan-name=StreamName\\sSub;msg-param-sub-plan=1000;room-id=1234;subscriber=0;system-msg=An\\sanonymous\\suser\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\sUSERNAME!\\s;tmi-sent-ts=1234;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("subgift2")) {
            return "@badge-info=subscriber/3;badges=moderator/1,subscriber/3;color=;display-name=USERNAME;emotes=;flags=;id=1234;login=username;mod=1;msg-id=resub;msg-param-anon-gift=false;msg-param-cumulative-months=3;msg-param-gift-month-being-redeemed=3;msg-param-gift-months=3;msg-param-gifter-id=12345;msg-param-gifter-login=gifterusername;msg-param-gifter-name=gifterUSERNAME;msg-param-months=0;msg-param-should-share-streak=0;msg-param-sub-plan-name=Channel\\sSubscription\\s(channel);msg-param-sub-plan=1000;msg-param-was-gifted=true;room-id=123;subscriber=1;system-msg=USERNAME\\ssubscribed\\sat\\sTier\\s1.\\sThey've\\ssubscribed\\sfor\\s3\\smonths!;tmi-sent-ts=12345;user-id=1234;user-type=mod :tmi.twitch.tv USERNOTICE "+channel+" :Thanks to @gifterUSERNAME for my sub gift!";
        }
        if (type.equals("primesub")) {
            return "@badge-info=subscriber/14;badges=subscriber/12;color=#00FF7F;display-name=USERNAME;emotes=;flags=;id=1234;login=username;mod=0;msg-id=resub;msg-param-cumulative-months=14;msg-param-months=0;msg-param-multimonth-duration=0;msg-param-multimonth-tenure=0;msg-param-should-share-streak=0;msg-param-sub-plan-name=StreamName\\sSub;msg-param-sub-plan=Prime;msg-param-was-gifted=false;room-id=1234;subscriber=1;system-msg=USERNAME\\ssubscribed\\swith\\sTwitch\\sPrime.\\sThey've\\ssubscribed\\sfor\\s14\\smonths!;tmi-sent-ts=1234;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :F1 subscription fee";
        }
        if (type.equals("newresub")) {
            // Without months in system-msg (even though it should have)
            return "@badges=moderator/1,subscriber/36,turbo/1;color=#0000FF;display-name=USER;emotes=;flags=;id=1234;login=user;mod=1;msg-id=resub;msg-param-cumulative-months=45;msg-param-cumulative-tenure-months=45;msg-param-months=0;msg-param-should-share-streak-tenure=false;msg-param-should-share-streak=0;msg-param-sub-plan-name=CHANNEL\\sSub;msg-param-sub-plan=Prime;room-id=123;subscriber=1;system-msg=USER\\sSubscribed\\swith\\sTwitch\\sPrime.;turbo=1;user-id=123;user-type=mod :tmi.twitch.tv USERNOTICE "+channel+" :Abc";
        }
        if (type.equals("newresub2")) {
            // With months in system-msg (theoretically)
            return "@badges=moderator/1,subscriber/36,turbo/1;color=#0000FF;display-name=USER;emotes=;flags=;id=1234;login=user;mod=1;msg-id=resub;msg-param-cumulative-months=45;msg-param-cumulative-tenure-months=45;msg-param-months=0;msg-param-should-share-streak-tenure=false;msg-param-should-share-streak=0;msg-param-sub-plan-name=CHANNEL\\sSub;msg-param-sub-plan=Prime;room-id=123;subscriber=1;system-msg=USER\\sSubscribed\\swith\\sTwitch\\sPrime.\\sThey've\\ssubscribed\\sfor\\s45\\smonthss!;turbo=1;user-id=123;user-type=mod :tmi.twitch.tv USERNOTICE "+channel+" :Abc";
        }
        if (type.equals("subextend")) {
            return "@badge-info=subscriber/1;badges=staff/1,subscriber/0,premium/1;color=;display-name=Test;emotes=;flags=;id=abc;login=test;mod=0;msg-id=extendsub;msg-param-sub-benefit-end-month=4;msg-param-sub-plan=1000;msg-param-cumulative-months=16;room-id=123;subscriber=1;system-msg=Test\\sextended\\stheir\\sTier\\s1\\ssubscription\\sthrough\\sApril!;tmi-sent-ts=123;user-id=123;user-type=staff :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("submsg")) {
            return "@badge-info=subscriber/31;badges=subscriber/24;color=;display-name=Test;emotes=;flags=;id=abc;mod=0;room-id=123;subscriber=1;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :abc blah";
        }
        if (type.equals("submsg2")) {
            // #cirno_tv
            return "@badge-info=subscriber/31;badges=subscriber/2024,bits/5000;client-nonce=abc;color=#4AADFF;display-name=Test;emotes=;flags=;id=abc;mod=0;room-id=123;subscriber=1;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :Abc";
        }
        if (type.equals("submultimonth")) {
            // Not sure what the tags would actually look like, this is just with the tags changed
            return "@badge-info=subscriber/1;badges=subscriber/1,premium/1;color=#5D12F3;display-name=USERNAME;emotes=;flags=;id=1234;login=username;mod=0;msg-id=sub;msg-param-cumulative-months=1;msg-param-months=0;msg-param-multimonth-duration=3;msg-param-multimonth-tenure=0;msg-param-should-share-streak=1;msg-param-streak-months=1;msg-param-sub-plan-name=Channel\\sSubscription\\s(channel);msg-param-sub-plan=1000;msg-param-was-gifted=false;room-id=1234;subscriber=1;system-msg=USERNAME\\ssubscribed\\sat\\sTier\\s1.;tmi-sent-ts=1234;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :Message";
        }
        if (type.equals("bits")) {
            return "@badges=bits/1000;bits=1;color=#FF7F50;display-name=tduvaTest;emotes=;id=123;mod=0;subscriber=0;turbo=0;user-type= :tduvatest!tduvatest@tduvatest.tmi.twitch.tv PRIVMSG "+channel+" :"+options;
        }
        if (type.equals("bitsbadgetier")) {
            return "@badges=subscriber/3,bits/1000;color=#FF0000;display-name=USERNAME;emotes=;flags=;id=123;login=username;mod=0;msg-id=bitsbadgetier;msg-param-threshold=1000;room-id=123;subscriber=1;system-msg=bits\\sbadge\\stier\\snotification;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :tmi.twitch.tv USERNOTICE "+channel;
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
        if (type.equals("anyemote")) {
            return "@badges=;color=#008000;display-name=Abc;emote-only=1;emotes="+options+":0-7;id=fwaef;mod=0;subscriber=0;tmi-sent-ts=1508516209239;turbo=0;user-type= :abc!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :DansGame ";
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
        if (type.startsWith("subbomb")) {
            return "@badges=moderator/1,subscriber/36,bits/100;color=#590094;display-name="+localUsername+";emotes=;id=123;login="+localUsername+";mod=1;msg-id=subgift;msg-param-months=1;msg-param-recipient-display-name="+options+";msg-param-recipient-id=1234;msg-param-recipient-user-name="+options+";msg-param-sender-count=0;msg-param-sub-plan-name=Channel\\sSubscription\\s("+channel+");msg-param-sub-plan=1000;room-id=12345;subscriber=1;system-msg="+localUsername+"\\sgifted\\sa\\sTier\\s1\\ssub\\sto\\s"+options+"!;tmi-sent-ts=1535312941402;turbo=0;user-id=123456;user-type=mod :tmi.twitch.tv USERNOTICE "+channel;
        }
        if (type.equals("badges")) {
            return "@badges="+options+";color=#008000;display-name=Abc;emote-only=1;emotes=33:0-7;id=fwaef;mod=0;subscriber=1;tmi-sent-ts=1508516209239;turbo=0;user-type= :abc!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :DansGame ";
        }
        if (type.equals("msg")) {
            return "@badges=;color=#008000;display-name=Abc;emote-only=1;emotes=33:0-7;id=1234;mod=0;subscriber=0;tmi-sent-ts=1508516209239;turbo=0;user-type= :abc!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :"+options;
        }
        if (type.equals("user")) {
            return "@badges=;color=#008000;display-name="+StringUtil.firstToUpperCase(options)+";emote-only=1;emotes=33:0-7;id=1234;mod=0;subscriber=0;tmi-sent-ts=1508516209239;turbo=0;user-type= :"+options+"!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :A message from "+options;
        }
        if (type.equals("del")) {
            return "@login=abc;target-msg-id=1234 :tmi.twitch.tv CLEARMSG "+channel+" :"+options;
        }
        if (type.equals("subonly")) {
            return "@msg-id=subs_on :tmi.twitch.tv NOTICE "+channel+" :This room is now in subscribers-only mode.";
        }
        // With several emote sets
        if (type.equals("userstate1")) {
            return "@badge-info=;badges=moderator/1,turbo/1;color=#0000FF;display-name=abctest;emote-sets=0,33,130,19194,19655,33563,1511296;mod=1;subscriber=0;user-type=mod :tmi.twitch.tv USERSTATE "+channel;
        }
        // With same emote sets, except one removed
        if (type.equals("userstate2")) {
            return "@badge-info=;badges=moderator/1,turbo/1;color=#0000FF;display-name=abctest;emote-sets=0,33,19194,19655,33563,1511296;mod=1;subscriber=0;user-type=mod :tmi.twitch.tv USERSTATE "+channel;
        }
        // With a great amount of emote sets
        if (type.equals("userstate3")) {
            return "@badge-info=;badges=moderator/1,turbo/1;color=#0000FF;display-name=abctest;emote-sets=0,849934,83973,143367,491534,335887,548868,1103900,20493,24589,239630,294921,24590,927745,2066,382998,178198,581660,217115,383007,583703,931884,22564,296993,57382,229416,368683,32815,835644,151600,120882,362550,960567,718897,927794,14399,815183,26692,839754,274499,532545,88138,317512,409675,118862,202834,202833,125010,86101,61528,825428,464989,22619,602199,223325,59487,737364,434278,893036,161888,553071,755820,18537,727138,866404,739425,1149055,749689,157814,391280,280690,315506,16506,233592,125,8318,716938,16513,407685,92292,141445,995457,544922,442514,630940,819353,10390,133268,657554,637072,839829,850066,481432,827564,360610,581792,907429,952487,10412,14510,16560,90291,334007,1058978,643258,16566,69815,878778,936116,1050794,258232,189,63677,538805,946352,14527,209084,193,108739,1038538,32967,387266,22728,184523,743617,123085,22733,57549,32974,452809,669914,65749,32981,106708,162007,354515,606431,215,176341,135386,522463,561362,16604,317656,800977,1077454,735444,887020,254176,968939,798954,231,233,579809,1108217,20717,538853,1048829,776420,176371,98549,98548,247,162043,334079,612595,164102,12549,20742,174346,176392,14604,641287,4368,1065216,583961,276758,682265,80148,1016088,532752,217370,817426,170269,74019,700716,1030436,102701,919842,108844,391477,22833,923967,809274,426289,172346,411966,833844,1095977,833846,833847,125242,1116461,317,2369,88387,141633,485696,63816,694595,63819,833856,1016157,604506,1012059,690515,160088,260447,20831,657770,131425,817519,182630,219494,868714,18795,479596,18798,303466,555367,2416,860537,274812,455038,164219,309630,280959,434554,446843,70015,215421,14722,506245,588170,16774,280962,352643,283011,627073,303496,555397,151948,18834,154000,20883,756120,278928,870809,608660,864656,123295,629143,971152,487847,668072,711084,31143,721312,764327,1354168,590247,1022369,168370,534968,522676,737727,342450,59832,248255,1073580,2494,125377,500167,12741,14789,248267,455116,975300,57803,111053,22989,186829,16850,78291,14805,895451,1579470,444881,674258,795095,160222,1358281,674261,641513,518629,207328,498148,186853,1024484,352745,401898,651749,68076,4591,762362,242161,631291,412149,174587,551409,1004021,76282,705009,12796,174591,231934,92671,231932,743947,174592,12805,330240,14854,696844,598542,121354,379400,18958,659972,14864,621080,12818,369175,1045018,21016,223771,864788,168478,1192462,150044,12832,21024,21026,6694,211496,320040,829984,133682,21043,4661,14903,178741,674354,137785,639538,573,666167,1063470,4671,205372,68161,23105,203330,711243,430661,633418,123460,434752,43592,318028,43593,68172,14926,381515,768580,1014336,1055327,16976,593,178769,412245,475732,68185,453214,547408,12889,68186,158297,426589,12896,113248,82533,143975,1131124,17000,866917,12906,658016,21099,356974,113261,121453,615013,12909,21101,420459,354932,1065573,377459,445040,477823,1051240,633,316029,957042,785035,547465,719497,1106579,678542,10885,19078,21126,993927,33417,121480,107146,150153,883331,180877,1387162,547480,227986,291477,322197,348822,696985,1051267,510610,422557,637591,1018513,268955,959121,199329,66210,160417,86695,66219,414379,17072,135859,240306,74418,78519,111289,160441,789175,479929,150205,21187,19142,283340,1063641,420552,144082,211667,19155,125650,90836,172759,1043161,318172,101080,357084,199385,559826,13019,418521,664298,625387,778985,987885,983782,473836,416491,525031,572135,436981,66294,531199,94969,70392,19194,387839,672497,539381,811762,109315,355078,533261,11018,578306,541444,316171,389898,897794,2832,21266,1078018,693017,19220,242452,6937,576275,285466,21279,15140,19236,131878,391969,396064,836388,326440,1061693,330539,375607,670520,1225505,72501,107317,21301,76596,279345,717631,1592109,422704,394033,459568,502576,1037112,4920,475960,11073,643912,230215,557902,299843,660290,1213274,15180,342859,62287,478024,971614,971615,150352,162645,574289,471900,701265,207710,570197,594772,297819,969583,84837,666479,179050,125800,711523,66411,105323,119659,670560,895846,752487,818017,852835,971616,19311,164723,439159,592760,473971,621437,2935,1512303,64383,1174416,990090,82822,107398,70536,414607,162697,678785,918405,19343,97169,551832,631705,158611,600987,969626,17301,9111,621470,947093,557972,609173,797584,19357,359320,19359,19363,172967,181159,119719,228266,742304,84909,1020832,588728,771001,795582,68530,15284,93109,381875,512959,9147,242620,525256,60354,756687,721870,431041,60360,533441,695235,918471,701376,170953,1029059,125900,691143,275409,173012,13271,818128,989,971731,191452,68574,154589,13281,13283,19427,578541,529390,547809,414700,15339,418794,986082,236531,19442,365559,494576,125945,33786,226297,1019,605170,699381,353285,107525,132103,488450,912394,19464,629761,1096730,498700,812032,33805,13326,953374,1020958,142358,168982,33813,80916,824345,388124,11292,107548,99358,246812,928814,318502,601131,1004589,341030,1088564,103460,543776,986151,568353,15403,1317945,1072,1121312,742462,95290,140350,556085,115774,707637,517189,11334,945224,1096,353357,486479,105544,296014,80970,107594,29772,486474,939072,652379,101458,429137,13402,533586,91233,99425,756843,472166,857196,3170,986220,607340,21607,326755,947305,3178,1033316,597093,140399,148589,273524,474229,789631,3190,81015,138357,441457,324732,930935,521341,121982,687242,15489,922765,308354,1146007,803979,107659,951452,900248,70809,615568,593045,60573,601236,902288,545942,212124,935057,15520,197798,210085,521377,21671,423073,574625,801957,1004735,29873,320694,603312,21691,3260,13500,216254,304314,224451,482503,29889,806088,169156,955593,726211,1105112,1653968,824513,70863,64719,273620,15569,236753,5332,603358,529618,664784,1076427,19677,324826,920811,660707,1094906,17644,111853,21742,60654,101615,318709,3314,900345,728317,658685,17657,267518,124154,793840,992499,406779,285946,324859,337159,611594,7428,357633,636172,138506,292110,361743,292111,1012995,394507,1049885,152845,683291,640282,292112,105748,593180,392467,572702,732435,1170696,118046,386341,486695,70947,1109300,21799,1320,183593,539939,5420,423208,392503,527674,585020,3381,470322,916788,949559,17722,390463,404797,939315,9535,177469,568631,826700,17730,146753,154945,1000780,419136,318797,867652,17738,21834,5451,109898,1045828,21838,165200,107860,89431,537950,523600,56665,410970,136543,17760,415078,118112,17762,126307,13667,970093,177510,263520,240998,93545,316781,505196,1074555,318824,101740,767333,169331,1121636,218490,249210,476542,89467,163192,19837,853363,931184,77184,19842,1168788,408960,32137,19852,17808,109969,11666,21907,19860,880025,413087,15770,32154,1015189,32155,109978,783760,19868,294296,1439,21920,75169,568745,239009,408996,564650,105893,724396,748972,898469,21929,21930,421290,1454,1103264,626104,1443236,419253,546236,191924,300467,202169,1062314,392638,1111469,366010,585161,3527,19911,105928,224714,196047,550341,19920,251346,992733,427474,732639,204245,290258,7639,607697,343518,546260,816592,11744,95713,394726,984558,89571,988653,1007084,81381,886249,1121780,60903,212459,1048038,118250,163305,337385,1518,155122,564734,239096,988659,595447,1471976,65023,812554,607745,595459,3595,441866,7693,853504,247309,810524,13842,71187,874014,13844,17941,613918,5658,167452,847405,511525,912937,110116,265772,996897,796195,151090,20024,1046071,1134121,13881,419388,134719,448071,585290,169537,22089,396876,1023557,646722,1612,15951,679493,56915,265820,1136203,1628,998995,540245,501337,222816,595565,499297,679532,85609,292462,108138,751200,20076,872033,73324,22126,503401,20079,122480,575099,781951,22133,183925,452209,710281,16004,300672,18056,601728,102027,196239,239246,386696,378507,7823,214668,253580,1040000,396951,628378,67220,1474189,104095,1697,26274,122533,847525,16043,306863,18096,108209,904892,292534,22195,216756,935609,810677,882356,540338,1724,757431,18110,634550,65220,489155,59079,396993,937673,1095383,157384,868039,22222,67281,519895,425684,558803,454360,515800,1054415,32484,771823,32485,87780,241382,71401,278253,132841,18157,18159,77550,16112,579320,67317,124662,12025,820981,128765,868081,20221,20222,216828,57090,294662,458500,771848,14088,16138,20234,771846,411403,960256,915225,763677,874263,98076,212765,704298,5921,917292,943919,972591,442148,859950,20260,401187,229156,1144630,98089,171816,208681,16171,861984,1705783,171821,16179,223031,978747,151348,20280,114488,20283,427837,83772,493370,10048,298816,532290,991044,431956,786271,788312,667487,118615,186196,270162,311132,675671,67423,1568631,374628,685931,16226,1015656,669539,929636,218984,249704,567139,501610,216941,259948,137074,747386,438130,786301,966520,405361,83832,524158,757617,563060,778103,3966,6015,276356,190342,61317,884615,710529,298888,20366,444296,853891,153490,477079,81810,298897,122782,427942,73635,20387,589739,14245,130987,862119,954276,14253,157612,913315,530360,985023,120755,137137,10164,18357,358323,128950,333746,16313,534449,14266,360382,612274,14272,18369,20417,18370,20418,4036,724942,892874,57288,1099739,18381,18382,956352,536519,8144,853981,952286,22481,98261,169943,92121,757719,92124,536534,968656,4063,139234,438247,20452,14310,73702,1343475,22504,389103,14315,129005,380905,20461,794593,149485,14321,20469,385008,450547,813049,270322,12281,180223,75775,364539;mod=1;subscriber=0;user-type=mod :tmi.twitch.tv USERSTATE "+channel;
        }
        if (type.equals("femote")) {
            return "@badge-info=subscriber/22;badges=subscriber/18;color=#420000;display-name=Test;emotes=300580752:0-7/300580752_HF:9-19;flags=;id=abc;mod=0;room-id=0;subscriber=1;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :cohhLick cohhLick_HF so tasty";
        }
        if (type.equals("validemoterange")) {
            return "@color=#420000;display-name=Test;emotes=425618:0-2;flags=;id=abc;mod=0;room-id=0;turbo=0;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :1234";
        }
        if (type.equals("invalidemoterange")) {
            return "@color=#420000;display-name=Test;emotes=425618:1-4;flags=;id=abc;mod=0;room-id=0;turbo=0;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :1234";
        }
        if (type.equals("invalidemoterange2")) {
            return "@color=#420000;display-name=Test;emotes=425618:0-4;flags=;id=abc;mod=0;room-id=0;turbo=0;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :1234";
        }
                if (type.equals("invalidemoterange3")) {
            return "@color=#420000;display-name=Test;emotes=425618:0-5;flags=;id=abc;mod=0;room-id=0;turbo=0;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :1234";
        }
        if (type.equals("hl")) {
            return "@badge-info=subscriber/19;badges=subscriber/12,premium/1;color=;display-name=Test;emotes=300737210:11-18/300737204:20-27;flags=;id=123;mod=0;msg-id=highlighted-message;room-id=123;subscriber=1;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :hello chat itmejpM1 itmejpM3 , just testing this highlight popup to see how glitzy this post will get";
        }
        if (type.equals("creward")) {
            return "@badge-info=;badges=vip/1,premium/1;color=#0000FF;custom-reward-id=r3ward-1d;display-name=Test;emotes=;flags=;id=123;mod=0;room-id=123;subscriber=0;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :Message text";
        }
        if (type.equals("vip")) {
            return "@badge-info=;badges=vip/1,premium/1;color=#0000FF;display-name=Test;emotes=;flags=;id=123;mod=0;room-id=123;subscriber=0;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :Message text";
        }
        if (type.equals("reply")) {
            return "@badge-info=subscriber/40;badges=broadcaster/1,subscriber/3024,partner/1;client-nonce=abc;color=#FF526F;display-name=TestUser;emotes=;flags=;id=abc;mod=0;reply-parent-display-name=OtherUser;reply-parent-msg-body=Test:\\sAbc;reply-parent-msg-id=abcd;reply-parent-user-id=123;reply-parent-user-login=otheruser;room-id=123;subscriber=1;tmi-sent-ts=123;turbo=0;user-type= :testuser!testuser@testuser.tmi.twitch.tv PRIVMSG "+channel+" :@OtherUser This is a reply!";
        }
        if (type.equals("founder")) {
            return "@badge-info=founder/29;badges=moderator/1,founder/0;client-nonce=1234;color=#0000FF;display-name=USERNAME;emotes=;flags=;id=1234;mod=1;room-id=1234;subscriber=0;tmi-sent-ts=1234;turbo=0;user-id=1234;user-type=mod :username!username@username.tmi.twitch.tv PRIVMSG " + channel + " :Ja bitte?";
        }
        if (type.equals("first")) {
            return "@color=;display-name=Test;id=123;first-msg=1;mod=0;room-id=123;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :hello";
        }
        if (type.equals("firstsub")) {
            return "@color=;badges=founder/0;display-name=Test;id=123;first-msg=1;mod=0;room-id=123;tmi-sent-ts=123;turbo=0;user-id=123;user-type= :test!test@test.tmi.twitch.tv PRIVMSG "+channel+" :hello";
        }
        if (type.equals("announcement")) {
            String color = "PRIMARY";
            if (!StringUtil.isNullOrEmpty(options)) {
                color = options;
            }
            return "@badge-info=;badges=broadcaster/1;color=#033700;display-name=ModName;emotes=;flags=;id=1234;login=modname;mod=0;msg-id=announcement;msg-param-color="+color+";room-id=1234;subscriber=0;system-msg=;tmi-sent-ts=1648758023469;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :This is an announcement https://chatty.github.io";
        }
        if (type.equals("announcement2")) {
            return "@badge-info=subscriber/28;badges=broadcaster/1,subscriber/0,premium/1;color=#0000FF;display-name=ModeratorName;emotes=emotesv2_bc0b18e802fb430ca03f0ad04efea2d1:0-6;flags=;id=1234;login=moderatorname;mod=0;msg-id=announcement;msg-param-color=PRIMARY;room-id=1234;subscriber=1;system-msg=;tmi-sent-ts=1648763597214;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :joshSss";
        }
        if (type.equals("announcement3")) {
            return "@badge-info=;badges=broadcaster/1;color=#033700;display-name=ModName;emotes=;flags=;id=1234;login=modname;mod=0;msg-id=announcement;room-id=1234;subscriber=0;system-msg=;tmi-sent-ts=1648758023469;user-id=1234;user-type= :tmi.twitch.tv USERNOTICE "+channel+" :"+options;
        }
        if (type.equals("hypechat")) {
            return "@badge-info=subscriber/9;badges=subscriber/9,bits/5000;color=#FF5700;display-name=USERNAME;emotes=;first-msg=0;flags=;id=1234;mod=0;pinned-chat-paid-amount=500;pinned-chat-paid-canonical-amount=500;pinned-chat-paid-currency=USD;pinned-chat-paid-exponent=2;pinned-chat-paid-is-system-message=0;pinned-chat-paid-level=TWO;returning-chatter=0;room-id=1234;subscriber=1;tmi-sent-ts=1687455824759;turbo=0;user-id=1234;user-type= :username!username@username.tmi.twitch.tv PRIVMSG "+channel+" :The message";
        }
        if (type.equals("hypechat2")) {
            return "@badge-info=;badges=;color=#00FF7F;display-name=USERNAME;emotes=;first-msg=0;flags=;id=1234;mod=0;pinned-chat-paid-amount=7500;pinned-chat-paid-canonical-amount=7500;pinned-chat-paid-currency=KRW;pinned-chat-paid-exponent=0;pinned-chat-paid-is-system-message=1;pinned-chat-paid-level=TWO;returning-chatter=0;room-id=1234;subscriber=0;tmi-sent-ts=1687458800209;turbo=0;user-id=1234;user-type= :username!username@username.tmi.twitch.tv PRIVMSG "+channel+" :User sent Hype Chat";
        }
        if (type.equals("custom")) {
            String[] parts = options.split("&");
            String badges = parts[0];
            String name = parts[1];
            String username = name.toLowerCase();
            String msg = parts[2];
            return "@badges="+badges+";color=#008000;display-name="+name+";emote-only=0;emotes=;id=fwaef"+ThreadLocalRandom.current().nextInt()+";mod=0;subscriber=1;tmi-sent-ts=1508516209239;turbo=0;user-type= :"+username+"!abc@abc.tmi.twitch.tv PRIVMSG "+channel+" :"+msg;
        }
        return null;
    }
    
}
