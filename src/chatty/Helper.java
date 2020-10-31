
package chatty;

import chatty.gui.MainGui;
import chatty.gui.components.settings.MainSettings;
import chatty.gui.components.textpane.UserNotice;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.Replacer;
import chatty.util.StringUtil;
import chatty.util.api.usericons.Usericon;
import chatty.util.commands.Parameters;
import chatty.util.irc.MsgTags;
import chatty.util.settings.FileManager.SaveResult;
import java.awt.Dimension;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Some Chatty-specific static helper methods.
 * 
 * @author tduva
 */
public class Helper {
    
    private static final Logger LOGGER = Logger.getLogger(Helper.class.getName());
    
    public static final DecimalFormat VIEWERCOUNT_FORMAT = new DecimalFormat();
    
    public static String formatViewerCount(int viewerCount) {
        return VIEWERCOUNT_FORMAT.format(viewerCount);
    }
    
    /**
     * Parses comma-separated channels from a String.
     * 
     * @param channels The list channels to parse
     * @param prepend Whether to prepend # if necessary
     * @return Set of channels sorted as in the String
     */
    public static Set<String> parseChannelsFromString(String channels, boolean prepend) {
        String[] parts = channels.split(",");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            String channel = part.trim();
            if (isValidChannel(channel)) {
                if (prepend && !channel.startsWith("#")) {
                    channel = "#"+channel;
                }
                result.add(StringUtil.toLowerCase(channel));
            }
        }
        return result;
    }
    
    public static String[] parseChannels(String channels, boolean prepend) {
        return parseChannelsFromString(channels, prepend).toArray(new String[0]);
    }
    
    public static String[] parseChannels(String channels) {
        return parseChannels(channels, true);
    }
    
    /**
     * Takes a Set of Strings and builds a single comma-separated String of
     * streams out of it.
     * 
     * @param set
     * @return 
     */
    public static String buildStreamsString(Collection<String> set) {
        String result = "";
        String sep = "";
        for (String channel : set) {
            result += sep+channel.replace("#", "");
            sep = ", ";
        }
        return result;
    }
    
    public static final String USERNAME_REGEX = "[a-zA-Z0-9][a-zA-Z0-9_]+";
    public static final Pattern CHANNEL_PATTERN = Pattern.compile("(?i)^#?"+USERNAME_REGEX+"$");
    public static final Pattern CHATROOM_PATTERN = Pattern.compile("(?i)^#?chatrooms:[0-9a-z-:]+$");
    public static final Pattern STREAM_PATTERN = Pattern.compile("(?i)^"+USERNAME_REGEX+"$");
    
    /**
     * Kind of relaxed valiadation if a channel, which can have a leading # or
     * not, can also be a chatroom.
     * 
     * @param channel
     * @return 
     */
    public static boolean isValidChannel(String channel) {
        try {
            return CHANNEL_PATTERN.matcher(channel).matches()
                    || CHATROOM_PATTERN.matcher(channel).matches();
        } catch (PatternSyntaxException | NullPointerException ex) {
            return false;
        }
    }
    
    public static boolean isValidChannelStrict(String channel) {
        return isValidChannel(channel) && channel.startsWith("#");
    }
    
    /**
     * Checks if the given channel is a regular channel, which means it is valid
     * and is not a chatroom.
     *
     * @param channel
     * @return 
     */
    public static boolean isRegularChannel(String channel) {
        try {
            return CHANNEL_PATTERN.matcher(channel).matches();
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Checks if the given channel is a regular channel, which means it starts
     * with a #, is valid otherwise and is not a chatroom.
     * 
     * @param channel
     * @return 
     */
    public static boolean isRegularChannelStrict(String channel) {
        return isRegularChannel(channel) && channel.startsWith("#");
    }
    
    /**
     * Checks if the given name is a valid stream (no leading # and valid
     * otherwise, basicially just the username).
     * 
     * @param stream
     * @return 
     */
    public static boolean isValidStream(String stream) {
        try {
            return STREAM_PATTERN.matcher(stream).matches();
        } catch (PatternSyntaxException | NullPointerException ex) {
            return false;
        }
    }
    
    public static boolean isChatroomChannel(String channel) {
        try {
            return channel.startsWith("#") && CHATROOM_PATTERN.matcher(channel).matches();
        } catch (Exception ex) {
            return false;
        }
    }
    
    /**
     * Checks if the given stream/channel is valid and turns it into a channel
     * if necessary (leading # and all lowercase). Can also be a chatroom.
     *
     * @param channel The channel, valid or invalid, leading # or not.
     * @return The channelname with leading #, or null if channel was invalid.
     */
    public static String toValidChannel(String channel) {
        if (channel == null) {
            return null;
        }
        if (!isValidChannel(channel)) {
            return null;
        }
        if (!channel.startsWith("#")) {
            channel = "#"+channel;
        }
        return StringUtil.toLowerCase(channel);
    }
    
    /**
     * If this is a valid channel name, then it turns it into a channel (which
     * means adding the # in front if necessary). Otherwise it just returns the
     * input in all lowercase.
     * 
     * @param chan
     * @return 
     */
    public static String toChannel(String chan) {
        if (chan == null) {
            return null;
        }
        if (isValidChannel(chan) && !chan.startsWith("#")) {
            return StringUtil.toLowerCase("#"+chan);
        }
        return StringUtil.toLowerCase(chan);
    }
    
    /**
     * Removes a leading # from the channel, if present.
     * 
     * @param channel
     * @return 
     */
    public static String toStream(String channel) {
        if (channel == null) {
            return null;
        }
        if (channel.startsWith("#")) {
            return channel.substring(1);
        }
        return channel;
    }
    
    public static String toValidStream(String channel) {
        String stream = Helper.toStream(channel);
        if (!isValidStream(stream)) {
            return null;
        }
        return stream;
    }
    
    public static String[] toStream(String[] channels) {
        String[] result = new String[channels.length];
        for (int i=0;i<channels.length;i++) {
            result[i] = toStream(channels[i]);
        }
        return result;
    }
    
    /**
     * Makes a readable message out of the given reason code.
     * 
     * @param reason
     * @param reasonMessage
     * @return 
     */
    public static String makeDisconnectReason(int reason, String reasonMessage) {
        String result = "";
        
        switch (reason) {
            case Irc.ERROR_UNKNOWN_HOST:
                result = Language.getString("chat.error.unknownHost");
                break;
            case Irc.REQUESTED_DISCONNECT:
                result = "Requested";
                break;
            case Irc.ERROR_CONNECTION_CLOSED:
                result = "";
                break;
            case Irc.ERROR_REGISTRATION_FAILED:
                result = Language.getString("chat.error.loginFailed");
                break;
            case Irc.ERROR_SOCKET_TIMEOUT:
                result = Language.getString("chat.error.connectionTimeout");
                break;
            case Irc.SSL_ERROR:
                result = "Could not establish secure connection ("+reasonMessage+")";
                break;
            case Irc.ERROR_SOCKET_ERROR:
                result = reasonMessage;
                break;
        }
        
        if (!result.isEmpty()) {
            result = " ("+result+")";
        }
        
        return result;
    }
    

    /**
     * https://stackoverflow.com/questions/5609500/remove-jargon-but-keep-real-characters/5609532#5609532
     * 
     * Combining characters seem to affect performance sometimes. Opening the
     * User Info Dialog can take a noticeable amount of time to open if the
     * history contains these characters (or at least some of them).
     * 
     * Removing anything longer than 2 characters seemed to work well enough,
     * but keeps some legit stuff (or semi-legit stuff) intact.
     * 
     * Tests showed no clearly different performance compared to removing any
     * number of characters.
     */
    private static final Pattern COMBINING_CHARACTERS_STRICT
            = Pattern.compile("[\\u0300-\\u036f\\u0483-\\u0489\\u1dc0-\\u1dff\\u20d0-\\u20ff\\ufe20-\\ufe2f]{1,}");
    
    private static final Pattern COMBINING_CHARACTERS_LENIENT
            = Pattern.compile("[\\u0300-\\u036f\\u0483-\\u0489\\u1dc0-\\u1dff\\u20d0-\\u20ff\\ufe20-\\ufe2f]{3,}");
    
    public static final int FILTER_COMBINING_CHARACTERS_OFF = 0;
    public static final int FILTER_COMBINING_CHARACTERS_LENIENT = 1;
    public static final int FILTER_COMBINING_CHARACTERS_STRICT = 2;
    
    /**
     * Replaces combining characters in certain ranges with the given
     * replacement string.
     * 
     * @param text The input text
     * @param replaceWith The text to replace any matching characters with
     * @return The changed text
     */
    public static String filterCombiningCharacters(String text, String replaceWith, int mode) {
        if (mode == FILTER_COMBINING_CHARACTERS_STRICT) {
            return COMBINING_CHARACTERS_STRICT.matcher(text).replaceAll(replaceWith);
        } else if (mode == FILTER_COMBINING_CHARACTERS_LENIENT) {
            return COMBINING_CHARACTERS_LENIENT.matcher(text).replaceAll(replaceWith);
        }
        return text;
    }

    
    private static final Pattern ALL_UPERCASE_LETTERS = Pattern.compile("[A-Z]+");
    
    public static boolean isAllUppercaseLetters(String text) {
        return ALL_UPERCASE_LETTERS.matcher(text).matches();
    }
    
    private static final Replacer HTMLSPECIALCHARS_ENCODE;
    private static final Replacer HTMLSPECIALCHARS_DECODE;
    private static final Replacer TAGS_VALUE_DECODE;
    private static final Replacer TAGS_VALUE_ENCODE;
    
    static {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("&amp;", "&");
        replacements.put("&lt;", "<");
        replacements.put("&gt;", ">");
        replacements.put("&quot;", "\"");
        
        Map<String, String> replacementsReverse = new HashMap<>();
        for (String key : replacements.keySet()) {
            replacementsReverse.put(replacements.get(key), key);
        }
        HTMLSPECIALCHARS_ENCODE = new Replacer(replacementsReverse);
        HTMLSPECIALCHARS_DECODE = new Replacer(replacements);
        
        Map<String, String> replacements2 = new HashMap<>();
        replacements2.put("\\\\s", " ");
        replacements2.put("\\\\n", "\n");
        replacements2.put("\\\\r", "\r");
        replacements2.put("\\\\:", ";");
        replacements2.put("\\\\\\\\", "\\");
        
        Map<String, String> replacements2Reverse = new HashMap<>();
        replacements2Reverse.put("\\s", "\\s");
        replacements2Reverse.put("\n", "\\n");
        replacements2Reverse.put("\r", "\\r");
        replacements2Reverse.put(";", "\\:");
        replacements2Reverse.put("\\\\", "\\\\");
        
        TAGS_VALUE_ENCODE = new Replacer(replacements2Reverse);
        TAGS_VALUE_DECODE = new Replacer(replacements2);
    }
    
    public static String tagsvalue_decode(String s) {
        if (s == null) {
            return null;
        }
        return TAGS_VALUE_DECODE.replace(s);
    }
    
    public static String tagsvalue_encode(String s) {
        if (s == null) {
            return null;
        }
        return TAGS_VALUE_ENCODE.replace(s);
    }
    
    public static String htmlspecialchars_decode(String s) {
        if (s == null) {
            return null;
        }
        return HTMLSPECIALCHARS_DECODE.replace(s);
    }
    
    public static String htmlspecialchars_encode(String s) {
        if (s == null) {
            return null;
        }
        return HTMLSPECIALCHARS_ENCODE.replace(s);
    }
    
    public static String prepareForHtml(String s) {
        if (s == null) {
            return null;
        }
        return htmlspecialchars_encode(s).replaceAll(" ", "&nbsp;").replaceAll("\n", "<br />");
    }
    
    private static final Pattern EMOJI_VARIATION_SELECTOR = Pattern.compile("[\uFE0E\uFE0F]");
    
    /**
     * Remove both the text style and emoji style variation selector from the
     * input.
     * 
     * @param input
     * @return 
     */
    public static String removeEmojiVariationSelector(String input) {
        if (input == null) {
            return null;
        }
        return EMOJI_VARIATION_SELECTOR.matcher(input).replaceAll("");
    }
    
    private static final Pattern UNDERSCORE = Pattern.compile("_");
    
    public static String replaceUnderscoreWithSpace(String input) {
        return UNDERSCORE.matcher(input).replaceAll(" ");
    }
    
    
    
    public static <T> List<T> subList(List<T> list, int start, int end) {
        List<T> subList = new ArrayList<>();
        for (int i=start;i<end;i++) {
            if (list.size() > i) {
                subList.add(list.get(i));
            } else {
                break;
            }
        }
        return subList;
    }
    
    public static void unhandledException() {
        String[] a = new String[0];
        String b = a[1];
    }
    
    public static boolean arrayContainsInt(int[] array, int test) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == test) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Splits up a String in the format "Integer1,Integer2" and returns the
     * {@code Integer}s.
     *
     * @param input The input String
     * @return Both {@code Integer} values as a {@code IntegerPair} or
     * {@code null} if the format was invalid
     * @see IntegerPair
     */
    public static IntegerPair getNumbersFromString(String input) {
        String[] split = input.split(",");
        if (split.length != 2) {
            return null;
        }
        try {
            int a = Integer.parseInt(split[0]);
            int b = Integer.parseInt(split[1]);
            return new IntegerPair(a, b);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    /**
     * Gets two {@code Integer} values on creation, which can be accessed with
     * the {@code final} attributes {@code a} and {@code b}.
     */
    public static class IntegerPair {
        public final int a;
        public final int b;
        
        public IntegerPair(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }
    
    
    
    public static final void main(String[] args) {
//        System.out.println(htmlspecialchars_encode("< >"));
//        System.out.println(shortenTo("abcd", 0));
//        System.out.println(shortenTo("abcd", 1));
//        System.out.println(shortenTo("abcd", 2));
//        System.out.println(shortenTo("abcd", 3));
//        System.out.println(shortenTo("abcd", 4));
//        System.out.println(shortenTo("abcd", 5));
//        System.out.println(shortenTo("abcd", -2));
//        System.out.println(shortenTo("abcd", -3));
//        System.out.println(shortenTo("abcd", -4));
//        long start = System.currentTimeMillis();
//        for (int i=0;i<100000;i++) {
//            htmlspecialchars_encode("&");
//        }
//        System.out.println(System.currentTimeMillis() - start);
        
        System.out.println(Arrays.asList(parseChannels("b,a,b,c")));
        
        System.out.println(getServer("server"));
        System.out.println(getPort("server"));
        
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(1);
        System.out.println(nf.format(Math.round(74/30.0)*30/60.0));
    }
    
    /**
     * Checks if the id matches the given User. The id can be one of: $mod,
     * $sub, $turbo, $admin, $broadcaster, $staff, $bot. If the user has the
     * appropriate user status, this returns true. If the id is unknown or the
     * user doesn't have the required status, this returns false.
     * 
     * @param id The id that is required
     * @param user The User object to check against
     * @return true if the id is known and matches the User, false otherwise
     */
    public static boolean matchUserStatus(String id, User user) {
        if (id.equals("$mod")) {
            if (user.isModerator()) {
                return true;
            }
        } else if (id.equals("$sub")) {
            if (user.isSubscriber()) {
                return true;
            }
        } else if (id.equals("$turbo")) {
            if (user.hasTurbo()) {
                return true;
            }
        } else if (id.equals("$admin")) {
            if (user.isAdmin()) {
                return true;
            }
        } else if (id.equals("$broadcaster")) {
            if (user.isBroadcaster()) {
                return true;
            }
        } else if (id.equals("$staff")) {
            if (user.isStaff()) {
                return true;
            }
        } else if (id.equals("$bot")) {
            if (user.isBot()) {
                return true;
            }
        } else if (id.equals("$globalmod")) {
            if (user.isGlobalMod()) {
                return true;
            }
        } else if (id.equals("$anymod")) {
            if (user.isAdmin() || user.isBroadcaster() || user.isGlobalMod()
                    || user.isModerator() || user.isStaff()) {
                return true;
            }
        } else if (id.equals("$vip")) {
            if (user.hasTwitchBadge("vip")) {
                return true;
            }
        }
        return false;
    }
    
    public static String checkHttpUrl(String url) {
        if (url == null) {
            return null;
        }
        if (url.startsWith("//")) {
            url = "https:"+url;
        }
        return url;
    }
    
    public static String systemInfo() {
        return String.format("Java: %s (%s / %s) OS: %s (%s/%s) Locale: %s",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("java.home"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                Locale.getDefault());
    }
    
    /**
     * Top Level Domains (only relevant for URLs not starting with http or www).
     */
    private static final String TLD = "(?:tv|com|org|edu|gov|uk|net|ca|de|jp|fr|au|us|ru|ch|it|nl|se|no|es|me|gl|fm|io|gg|be)";
    
    private static final String MID = "[^\\s]";
    
    private static final String END = "[^:,.\\s]";
    
    /**
     * Start of the URL.
     */
    private static final String S1 = "(?:(?:https?)://|www\\.)";
    
    /**
     * Start of the URL (second possibility).
     */
    private static final String S2 = "(?:[A-Z0-9.-]+[A-Z0-9]\\."+TLD+"\\b)";
    
    /**
     * Complete URL.
     */
    private static final String T1 = "(?:(?:"+S1+"|"+S2+")"+MID+"*"+END+")";
    
    /**
     * Complete URL (only domain).
     */
    private static final String T2 = "(?:"+S2+")";
    
    /**
     * The regex String for finding URLs in messages.
     */
    private static final String URL_REGEX = "(?i)\\b"+T1+"|"+T2;
    
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEX);
    
    public static Pattern getUrlPattern() {
        return URL_PATTERN;
    }
    
    public static String buildUrlString(String scheme, String host, String path) {
        try {
            URI uri = new URI(scheme, host, path, null);
            return uri.toASCIIString();
        } catch (URISyntaxException ex) {
            LOGGER.warning("Error building URL: "+ex);
            return null;
        }
    }
    
    /**
     * Retrieve the server part out of a string formatted as "server:port".
     * 
     * @param serverAndPort
     * @return The server, or the entire string if no ":" was found
     */
    public static String getServer(String serverAndPort) {
        int p = serverAndPort.lastIndexOf(":");
        if (p == -1) {
            return serverAndPort;
        }
        return serverAndPort.substring(0, p);
    }
    
    /**
     * Retrieve the port part out of a string formatted as "server:port".
     * 
     * @param serverAndPort 
     * @return The parsed port, or -1 if invalid
     */
    public static int getPort(String serverAndPort) {
        int p = serverAndPort.lastIndexOf(":");
        if (p == -1) {
            return -1;
        }
        String port = serverAndPort.substring(p+1);
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
    
    private static String makeBanInfoDuration(long duration) {
        if (duration < 120) {
            return String.format("%ds", duration);
        }
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(1);
        
        if (duration < DateTime.HOUR*2) {
            return String.format("%sm", nf.format(Math.round(duration/30.0)*30/60.0));
        }
        duration = duration / 60;
        return String.format("%sh", nf.format(Math.round(duration/30.0)*30/60.0));
    }
    
    public static String makeBanInfo(long duration, String reason,
            boolean durationEnabled, boolean reasonEnabled, boolean includeBan) {
        String banInfo = "";
        if (durationEnabled) {
            if (duration > 0) {
                banInfo = String.format("(%s)", makeBanInfoDuration(duration));
            } else if (duration == -2) {
                banInfo = "(deleted)";
            } else if (includeBan) {
                banInfo = "(banned)";
            }
        }
        // Reason not via IRC anymore
//        if (reasonEnabled) {
//            if (reason != null && !reason.isEmpty()) {
//                banInfo = StringUtil.append(banInfo, " ", "[" + reason + "]");
//            }
//        }
        return banInfo;
    }
    
    public static String makeBanCommand(User user, long duration, String id) {
        if (duration > 0) {
            return StringUtil.concats("timeout", user.getName(), duration).trim();
        }
        if (duration == -2) {
            return StringUtil.concats("delete", id).trim();
        }
        return StringUtil.concats("ban", user.getName()).trim();
    }
    
    public static Dimension getDimensionFromParameter(String parameter) {
        if (parameter != null && !parameter.trim().isEmpty()) {
            String[] split = parameter.trim().split("x|\\s");
            if (split.length == 2) {
                try {
                    int width = Integer.parseInt(split[0]);
                    int height = Integer.parseInt(split[1]);
                    if (width > 0 && height > 0) {
                        return new Dimension(width, height);
                    }
                } catch (NumberFormatException ex) {
                    // Do nothing, will return null for invalid format
                }
            }
        }
        return null;
    }
    
    private static final Map<String, String> EMPTY_BADGES = Collections.unmodifiableMap(new LinkedHashMap<String, String>());
    
    /**
     * Parses the badges tag. The resulting map is unmodifiable.
     * 
     * @param data
     * @return 
     */
    public static Map<String, String> parseBadges(String data) {
        if (data == null || data.isEmpty()) {
            return EMPTY_BADGES;
        }
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        String[] badges = data.split(",");
        for (String badge : badges) {
            String[] split = badge.split("/");
            if (split.length == 2) {
                String id = split[0];
                String version = split[1];
                result.put(id, version);
            }
        }
        return Collections.unmodifiableMap(result);
    }
    
    public static short parseShort(String input, short defaultValue) {
        try {
            return Short.parseShort(input);
        }
        catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
    
    public static String makeDisplayNick(User user, long displayNamesMode) {
        if (user.hasCustomNickSet()) {
            return user.getFullNick();
        } else if (displayNamesMode == SettingsManager.DISPLAY_NAMES_MODE_BOTH) {
            if (user.hasRegularDisplayNick()) {
                return user.getFullNick();
            } else {
                return user.getFullNick() + " (" + user.getRegularDisplayNick() + ")";
            }
        } else if (displayNamesMode == SettingsManager.DISPLAY_NAMES_MODE_LOCALIZED) {
            return user.getFullNick();
        } else if (displayNamesMode == SettingsManager.DISPLAY_NAMES_MODE_CAPITALIZED) {
            return user.getModeSymbol() + user.getRegularDisplayNick();
        } else if (displayNamesMode == SettingsManager.DISPLAY_NAMES_MODE_USERNAME) {
            return user.getModeSymbol() + user.getName();
        }
        return user.getFullNick();
    }
    
    public static String encodeFilename(String input) {
        try {
            return URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unsupported encoding lol");
        }
    }
    
    public static String encodeFilename2(String input) {
        return input.replaceAll("[%\\.\"\\*/:<>\\?\\\\\\|\\+,\\.;=\\[\\]]", "_");
    }
    
    /**
     * Returns commands split up by '|' and trimmed for leading and trailing
     * whitespace. Only non-empty commands are included.
     * 
     * Use '||' to use the '|' character literally.
     * 
     * Example: '/chain /echo first | /echo second || third'
     * Returns: '/echo first' and '/echo second | third'
     * 
     * @param input
     * @return 
     */
    public static List<String> getChainedCommands(String input) {
        if (StringUtil.isNullOrEmpty(input)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        // A '|' not preceeded or followed by '|'
        String[] split = input.split("(?<!\\|)\\|(?!\\|)");
        for (String part : split) {
            // Remove first '|' from two or more '|' in a row
            part = part.trim().replaceAll("\\|(\\|+)", "$1");
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }
    
    public static void addUserParameters(User user, String msgId, String autoModMsgId, Parameters parameters) {
        parameters.put("nick", user.getRegularDisplayNick());
        if (msgId != null) {
            parameters.put("msg-id", msgId);
            parameters.put("msg", user.getMessageText(msgId));
        }
        if (autoModMsgId != null) {
            parameters.put("automod-msg-id", autoModMsgId);
            String autoModMsg = user.getAutoModMessageText(autoModMsgId);
            if (autoModMsg != null) {
                parameters.put("msg", autoModMsg);
            }
        }
        parameters.put("user-id", user.getId());
        if (user.getTwitchBadges() != null) {
            parameters.put("twitch-badge-info", user.getTwitchBadges().toString());
            parameters.put("twitch-badges", Usericon.makeBadgeInfo(user.getTwitchBadges()));
        }
        parameters.put("display-nick", user.getDisplayNick());
        parameters.put("custom-nick", user.getCustomNick());
        parameters.put("full-nick", user.getFullNick());
        if (!user.hasRegularDisplayNick()) {
            parameters.put("display-nick2", user.getDisplayNick()+" ("+user.getRegularDisplayNick()+")");
            parameters.put("full-nick2", user.getFullNick()+" ("+user.getRegularDisplayNick()+")");
            parameters.put("special-nick", "true");
        }
        else {
            parameters.put("display-nick2", user.getDisplayNick());
            parameters.put("full-nick2", user.getFullNick());
        }
        parameters.putObject("user", user);
    }
    
    private static final Map<UserNotice, javax.swing.Timer> pointsMerge = new HashMap<>();
    
    /**
     * Must be run in EDT.
     * 
     * @param newNotice
     * @param g 
     */
    public static void pointsMerge(UserNotice newNotice, MainGui g) {
        UserNotice result = findPointsMerge(newNotice);
        if (result == null) {
            javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
                pointsMerge.remove(newNotice);
                g.printUsernotice(newNotice.type, newNotice.user, newNotice.infoText, newNotice.attachedMessage, newNotice.tags);
            });
            timer.setRepeats(false);
            pointsMerge.put(newNotice, timer);
            timer.start();
        }
        else {
            g.printUsernotice(result.type, result.user, result.infoText, result.attachedMessage, result.tags);
        }
    }
    
    private static UserNotice findPointsMerge(UserNotice newNotice) {
        UserNotice found = null;
        for (Map.Entry<UserNotice, javax.swing.Timer> entry : pointsMerge.entrySet()) {
            UserNotice stored = entry.getKey();
            // Attached messages seem to be trimmed depending on source
            boolean sameAttachedMsg = Objects.equals(
                    StringUtil.trimAll(stored.attachedMessage),
                    StringUtil.trimAll(newNotice.attachedMessage));
            if (stored.user.sameUser(newNotice.user) && sameAttachedMsg) {
                found = stored;
                entry.getValue().stop();
            }
        }
        if (found != null) {
            pointsMerge.remove(found);
            UserNotice ps = found.tags.isFromPubSub() ? found : newNotice;
            UserNotice irc = found.tags.isFromPubSub() ? newNotice : found;
            // Use irc msg, since that would also have the emote tags
            return new UserNotice(ps.type, ps.user, ps.infoText, irc.attachedMessage, MsgTags.merge(found.tags, newNotice.tags));
        }
        return null;
    }
    
    public static void setDefaultTimezone(String input) {
        if (!StringUtil.isNullOrEmpty(input)) {
            MainSettings.DEFAULT_TIMEZONE = TimeZone.getDefault();
            TimeZone tz = TimeZone.getTimeZone(input);
            TimeZone.setDefault(tz);
            LOGGER.info(String.format("[Timezone] Set to %s [%s]", tz.getDisplayName(), input));
        }
    }
    
    public static String getErrorMessageWithCause(Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause != null) {
            return String.format("%s [%s]",
                    getErrorMessageCompact(ex),
                    getErrorMessageCompact(cause));
        }
        return getErrorMessageCompact(ex);
    }
    
    public static String getErrorMessageCompact(Throwable ex) {
        if (ex.getLocalizedMessage() != null) {
            return ex.getClass().getSimpleName()+": "+ex.getLocalizedMessage();
        }
        return ex.getClass().getSimpleName();
    }
    
    public static String makeSaveResultInfo(List<SaveResult> result) {
        StringBuilder b = new StringBuilder();
        int index = 0;
        for (SaveResult r : result) {
            if (r == null) {
                continue;
            }
            // Regular
            if (r.written) {
                b.append(String.format("* File written to %s\n",
                        r.filePath));
            }
            else if (r.writeError != null) {
                b.append(String.format("* Writing failed: %s\n",
                        getErrorMessageCompact(r.writeError)));
            }
            
            // Backup
            if (r.backupWritten) {
                b.append(String.format("* Backup written to %s\n",
                        r.backupPath));
            }
            else if (r.writeError != null) {
                b.append(String.format("* Backup failed: %s\n",
                        getErrorMessageCompact(r.backupError)));
            }
            else if (r.cancelReason == SaveResult.CancelReason.INVALID_CONTENT) {
                b.append("* Backup failed: Invalid content\n");
            }
            
            // Removed deprecated
            if (r.removed) {
                b.append("* Removed unused file\n");
            }
            
            // If anything was appended for this file, add header
            if (b.length() > index) {
                b.insert(index, String.format("[%s]\n", r.id));
                index = b.length();
            }
        }
        return b.toString();
    }
    
}
