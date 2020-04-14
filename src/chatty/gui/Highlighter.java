
package chatty.gui;

import chatty.util.colors.HtmlColors;
import chatty.Addressbook;
import chatty.Helper;
import chatty.Logging;
import chatty.User;
import chatty.util.Debugging;
import chatty.util.MiscUtil;
import chatty.util.Pair;
import chatty.util.StringUtil;
import chatty.util.api.usericons.BadgeType;
import chatty.util.irc.MsgTags;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Used for checking messages against stored items, including additional
 * settings such as a blacklist. Not only used for Highlighting, but that is
 * where the name originates.
 * 
 * @author tduva
 */
public class Highlighter {
    
    private static final Logger LOGGER = Logger.getLogger(Highlighter.class.getName());
    
    private static final int LAST_HIGHLIGHTED_TIMEOUT = 10*1000;
    
    private final Map<String, Long> lastHighlighted = new HashMap<>();
    private final Map<String, HighlightItem> lastHighlightedItem = new HashMap<>();
    private final List<HighlightItem> items = new ArrayList<>();
    private final List<HighlightItem> blacklistItems = new ArrayList<>();
    private HighlightItem usernameItem;
    private Color lastMatchColor;
    private Color lastMatchBackgroundColor;
    private boolean lastMatchNoNotification;
    private boolean lastMatchNoSound;
    private List<Match> lastTextMatches;
    private String lastReplacement;
    
    // Settings
    private boolean highlightUsername;
    private boolean highlightNextMessages;
    
    /**
     * Clear current items and load the new ones.
     * 
     * @param newItems 
     * @throws NullPointerException if newItems is null
     */
    public void update(List<String> newItems) {
        compile(newItems, items);
    }
    
    public void updateBlacklist(List<String> newItems) {
        compile(newItems, blacklistItems);
    }
    
    private void compile(List<String> newItems, List<HighlightItem> into) {
        into.clear();
        for (String item : newItems) {
            if (item != null && !item.isEmpty()) {
                HighlightItem compiled = new HighlightItem(item);
                if (!compiled.hasError()) {
                    into.add(compiled);
                }
            }
        }
    }
    
    /**
     * Sets the current username.
     * 
     * @param username 
     */
    public void setUsername(String username) {
        if (username == null) {
            usernameItem = null;
        }
        else {
            HighlightItem newItem = new HighlightItem("w:"+username);
            if (!newItem.hasError()) {
                usernameItem = newItem;
            } else {
                usernameItem = null;
            }
        }
    }
    
    /**
     * Sets whether the username should be highlighted.
     * 
     * @param highlighted 
     */
    public void setHighlightUsername(boolean highlighted) {
        this.highlightUsername = highlighted;
    }
    
    public void setHighlightNextMessages(boolean highlight) {
        this.highlightNextMessages = highlight;
    }
    
    /**
     * Returns the color for the last match, which can be used to make the
     * highlight appear in the appropriate custom color.
     * 
     * @return The {@code Color} or {@code null} if no color was specified
     */
    public Color getLastMatchColor() {
        return lastMatchColor;
    }
    
    public Color getLastMatchBackgroundColor() {
        return lastMatchBackgroundColor;
    }
    
    public boolean getLastMatchNoNotification() {
        return lastMatchNoNotification;
    }
    
    public boolean getLastMatchNoSound() {
        return lastMatchNoSound;
    }
    
    /**
     * Get all text matches from the last match (only from the item that caused
     * the match).
     * 
     * @return 
     */
    public List<Match> getLastTextMatches() {
        return lastTextMatches;
    }
    
    public String getLastReplacement() {
        return lastReplacement;
    }
    
    /**
     * Check if this matches as a REGULAR message, getting all additional data
     * from the User. See  for more.
     * 
     * @param user The User associated with this message
     * @param text The message text
     * @return true if the message matches, false otherwise
     * @see #check(HighlightItem.Type, String, String, Addressbook, User)
     */
    public boolean check(User user, String text) {
        return check(HighlightItem.Type.REGULAR, text, null, null, user, null, MsgTags.EMPTY);
    }
    
    /**
     * Check if the message with the given data matches the stored items and a
     * match is not prevented by the blacklist.
     * <p>
     * The channel, Addressbook and User can be null, in which case any
     * associated requirements are ignored. If User is not null, then channel
     * and Addressbook, if null, will be retrieved from User.
     * <p>
     * Use {@link #update(List)} and {@link #updateBlacklist(List)} and other
     * setting methods to define what is being matched by this Highlighter.
     * 
     * @param type What kind of message this is, REGULAR, INFO or ANY (which
     * means the type is ignored)
     * @param text The text of the message to check
     * @param channel The channel of this message
     * @param ab The Addressbook for checking channel category
     * @param user The User associated with this message, for checking username
     * and user Addressbook category
     * @return true if the message matches, false otherwise
     */
    public boolean check(HighlightItem.Type type, String text, String channel,
            Addressbook ab, User user, User localUser, MsgTags tags) {
        Blacklist blacklist = null;
        if (!blacklistItems.isEmpty()) {
            blacklist = new Blacklist(type, text, channel, ab, user, localUser, tags, blacklistItems);
        }
        
        /**
         * All last match variables filled in case of match, except for text
         * matches, so reset here.
         */
        lastTextMatches = null;
        
        // Try to match own name first (if enabled)
        if (highlightUsername && usernameItem != null &&
                usernameItem.matches(type, text, blacklist,
                        channel, ab, user, localUser, tags)) {
            fillLastMatchVariables(usernameItem, text);
            addMatch(user, usernameItem);
            return true;
        }
        
        // Then try to match against the items
        for (HighlightItem item : items) {
            if (item.matches(type, text, blacklist, channel, ab, user, localUser, tags)) {
                fillLastMatchVariables(item, text);
                addMatch(user, item);
                return true;
            }
        }
        
        // Then see if there is a recent match ("Highlight follow-up")
        if (highlightNextMessages && user != null && hasRecentMatch(user.getName())) {
            fillLastMatchVariables(lastHighlightedItem.get(user.getName()), null);
            return true;
        }
        return false;
    }
    
    private void fillLastMatchVariables(HighlightItem item, String text) {
        lastMatchColor = item.getColor();
        lastMatchBackgroundColor = item.getBackgroundColor();
        lastMatchNoNotification = item.noNotification();
        lastMatchNoSound = item.noSound();
        lastReplacement = item.getReplacement();
        if (text != null) {
            lastTextMatches = item.getTextMatches(text);
        }
    }
    
    /**
     * This should not be necessary if the state is only checked after a match
     * (since variables will be set correctly then), but it may be useful in
     * some other situations.
     */
    public void resetLastMatchVariables() {
        lastMatchColor = null;
        lastMatchBackgroundColor = null;
        lastMatchNoNotification = false;
        lastMatchNoSound = false;
        lastReplacement = null;
        lastTextMatches = null;
    }
    
    private void addMatch(User user, HighlightItem item) {
        if (highlightNextMessages && user != null) {
            String username = user.getName();
            lastHighlighted.put(username, MiscUtil.ems());
            lastHighlightedItem.put(username, item);
        }
    }
    
    private boolean hasRecentMatch(String fromUsername) {
        clearRecentMatches();
        return lastHighlighted.containsKey(fromUsername);
    }
    
    private void clearRecentMatches() {
        Iterator<Map.Entry<String, Long>> it = lastHighlighted.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (MiscUtil.ems() - entry.getValue() > LAST_HIGHLIGHTED_TIMEOUT) {
                it.remove();
                lastHighlightedItem.remove(entry.getKey());
            }
        }
    }
    
    /**
     * A single Highlight item that parses the item string and prepares it for
     * matching. It provides methods to check if an error occured parsing, as
     * well as methods to check if the item matches a message or text, to
     * retrieve all match indices and some meta information.
     */
    public static class HighlightItem {
        
        public enum Type {
            REGULAR("Regular chat messages"),
            INFO("Info messages"),
            ANY("Any type of message"),
            TEXT_MATCH_TEST("Only match text, any message type");
            
            public final String description;
            
            Type(String description) {
                this.description = description;
            }
        }
        
        private static abstract class Item {
            
            /**
             * For info output.
             */
            private final String info;
            
            /**
             * Data mainly used in the matches() method for this item, for info
             * output.
             */
            private final Object infoData;
            
            private final boolean matchesOnText;
            
            private Item(String info, Object infoData, boolean matchesOnText) {
                this.info = info;
                this.infoData = infoData;
                this.matchesOnText = matchesOnText;
            }
            
            private Item(String info, Object infoData) {
                this(info, infoData, false);
            }
            
            @Override
            public String toString() {
                if (infoData != null) {
                    return info+": "+infoData;
                }
                return info;
            }
            
            abstract public boolean matches(String text, Blacklist blacklist,
                                            String channel, Addressbook ab,
                                            User user, User localUser,
                                            MsgTags tags);
        }
        
        private void addUserItem(String info, Object infoData, Function<User, Boolean> m) {
            Item item = new Item(info, infoData) {

                @Override
                public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                    return user != null && m.apply(user);
                }
            };
            matchItems.add(item);
        }
        
        private void addLocalUserItem(String info, Object infoData, Function<User, Boolean> m) {
            Item item = new Item(info, infoData) {

                @Override
                public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                    return localUser != null && m.apply(localUser);
                }
            };
            matchItems.add(item);
        }
        
        private void addChanItem(String info, Object infoData, Function<String, Boolean> m) {
            Item item = new Item(info, infoData) {

                @Override
                public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                    return channel != null && m.apply(channel);
                }
            };
            matchItems.add(item);
        }
        
        private void addTagsItem(String info, Object infoData, Function<MsgTags, Boolean> m) {
            Item item = new Item(info, infoData) {

                @Override
                public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                    return tags != null && m.apply(tags);
                }
            };
            matchItems.add(item);
        }
        
        /**
         * A regex that will never match.
         */
        private static final Pattern NO_MATCH = Pattern.compile("(?!)");
        private static final Item NO_MATCH_ITEM = new Item("Never Match", null) {

            @Override
            public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                return false;
            }
        };
        
        private final String raw;
        private final List<Item> matchItems = new ArrayList<>();
        private Pattern pattern;
        private List<HighlightItem> localBlacklistItems;
        private Color color;
        private Color backgroundColor;
        private boolean noNotification;
        private boolean noSound;
        private Type appliesToType = Type.REGULAR;
        // Replacement string for filtering parts of a message
        private String replacement;
        private Item failedItem;
        
        private String error;
        private boolean patternWarning;
        private String textWithoutPrefix = "";
        private String mainPrefix;
        
        private boolean invalidRegexLog;
        
        private enum Status {
            MOD("m"), SUBSCRIBER("s"), BROADCASTER("b"), ADMIN("a"), STAFF("f"),
            TURBO("t"), ANY_MOD("M"), GLOBAL_MOD("g"), BOT("r"), VIP("v");
            
            private final String id;
            
            Status(String id) {
                this.id = id;
            }
        }
        
        /**
         * Contains all the text matching prefixes that are turned into a Regex
         * and their associated pattern builder functions.
         */
        private static final Map<String, Function<String, String>> patternPrefixes = new HashMap<>();
        
        static {
            /**
             * Add text matching prefixes and their pattern builder functions
             * (with possible aliases).
             */
            addPatternPrefix(text -> text, "re*:", "reg:");
            addPatternPrefix(text -> "(?iu)"+text, "regi:");
            addPatternPrefix(text -> "\\b(?:"+text+")\\b", "regw:");
            addPatternPrefix(text -> "(?iu)\\b(?:"+text+")\\b", "regwi:");
            addPatternPrefix(text -> "^(?:"+text+")$", "re:", "regm:");
            addPatternPrefix(text -> "(?iu)^(?:"+text+")$", "regmi:");
            addPatternPrefix(text -> "(?iu)\\b"+Pattern.quote(text)+"\\b", "w:");
            addPatternPrefix(text -> "\\b"+Pattern.quote(text)+"\\b", "wcs:");
            addPatternPrefix(text -> Pattern.quote(text), "cs:");
            addPatternPrefix(text -> "(?iu)^"+Pattern.quote(text), "start:");
            addPatternPrefix(text -> "(?iu)" + Pattern.quote(text), "text:");
        }
        
        /**
         * Create a new item to match messages against.
         * 
         * @param item The string containing the match requirements
         * @param invalidRegexLog Whether to add invalid regex warnings to the
         * debug log (this can be useful to disable for editing regex, where it
         * might otherwise spam a lot of debug messages)
         */
        public HighlightItem(String item, boolean invalidRegexLog) {
            raw = item;
            this.invalidRegexLog = invalidRegexLog;
            prepare(item);
        }
        
        public HighlightItem(String item) {
            // By default, log invalid regex warnings
            this(item, true);
        }
        
        /**
         * Prepare an item for matching by checking for prefixes and handling
         * the different types accordingly.
         * 
         * @param item 
         */
        private void prepare(String item) {
            item = item.trim();
            if (!findPatternPrefixAndCompile(item)) {
                // If not a text matching prefix, search for other prefixes
                
                //--------------------------
                // User prefixes
                //--------------------------
                if (item.startsWith("cat:")) {
                    List<String> categories = parseStringListPrefix(item, "cat:", s -> s);
                    addUserItem("Any of Addressbook Categories", categories, user -> {
                        for (String category : categories) {
                            if (user.hasCategory(category)) {
                                return true;
                            }
                        }
                        return false;
                    });
                }
                else if (item.startsWith("!cat:")) {
                    List<String> categories = parseStringListPrefix(item, "!cat:", s -> s);
                    addUserItem("Not any of Addressbook Categories", categories, user -> {
                        for (String category : categories) {
                            if (!user.hasCategory(category)) {
                                return true;
                            }
                        }
                        return false;
                    });
                }
                else if (item.startsWith("user:")) {
                    Pattern p = compilePattern(Pattern.quote(parsePrefix(item, "user:").toLowerCase(Locale.ENGLISH)));
                    addUserItem("Username", p, user -> {
                        return p.matcher(user.getName()).matches();
                    });
                }
                else if (item.startsWith("reuser:")) {
                    Pattern p = compilePattern(parsePrefix(item, "reuser:").toLowerCase(Locale.ENGLISH));
                    addUserItem("Username (Regex)", p, user -> {
                        return p.matcher(user.getName()).matches();
                    });
                }
                else if (item.startsWith("status:")) {
                    Set<Status> s = parseStatus(parsePrefix(item, "status:"));
                    addUserItem("User Status", s, user -> {
                        return checkStatus(user, s, true);
                    });
                }
                else if (item.startsWith("!status:")) {
                    Set<Status> s = parseStatus(parsePrefix(item, "!status:"));
                    addUserItem("Not User Status", s, user -> {
                        return checkStatus(user, s, false);
                    });
                }
                else if (item.startsWith("mystatus:")) {
                    Set<Status> s = parseStatus(parsePrefix(item, "mystatus:"));
                    addLocalUserItem("My User Status", s, user -> {
                        return checkStatus(user, s, true);
                    });
                }
                else if (item.startsWith("!mystatus:")) {
                    Set<Status> s = parseStatus(parsePrefix(item, "!mystatus:"));
                    addLocalUserItem("Not My User Status", s, user -> {
                        return checkStatus(user, s, false);
                    });
                }
                //--------------------------
                // Channel Prefixes
                //--------------------------
                else if (item.startsWith("chan:")) {
                    List<String> chans = parseStringListPrefix(item, "chan:",
                                                               c -> Helper.toChannel(c));
                    addChanItem("One of channels", chans, chan -> {
                        return chans.contains(chan);
                    });
                }
                else if (item.startsWith("!chan:")) {
                    List<String> chans = parseStringListPrefix(item, "!chan:",
                                                               c -> Helper.toChannel(c));
                    addChanItem("Not one of channels", chans, chan -> {
                        return !chans.contains(chan);
                    });
                }
                else if (item.startsWith("chanCat:")) {
                    List<String> cats = parseStringListPrefix(item, "chanCat:", s -> s);
                    matchItems.add(new Item("Channel Addressbook Category", cats) {

                        @Override
                        public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                            if (channel == null || ab == null) {
                                return false;
                            }
                            for (String cat : cats) {
                                if (ab.hasCategory(channel, cat)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }
                else if (item.startsWith("!chanCat:")) {
                    List<String> cats = parseStringListPrefix(item, "!chanCat:", s -> s);
                    matchItems.add(new Item("Not Channel Addressbook Category", cats) {

                        @Override
                        public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                            if (channel == null || ab == null) {
                                return false;
                            }
                            for (String cat : cats) {
                                if (!ab.hasCategory(channel, cat)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
                }
                //--------------------------
                // Behaviour Prefixes
                //--------------------------
                else if (item.startsWith("color:")) {
                    color = HtmlColors.decode(parsePrefix(item, "color:"));
                }
                else if (item.startsWith("bgcolor:")) {
                    backgroundColor = HtmlColors.decode(parsePrefix(item, "bgcolor:"));
                }
                else if (item.startsWith("replacement:")) {
                    replacement = parsePrefix(item, "replacement:");
                }
                //--------------------------
                // Mixed Prefixes
                //--------------------------
                else if (item.startsWith("config:")) {
                    List<String> list = parseStringListPrefix(item, "config:", s -> s);
                    list.forEach(part -> {
                        if (part.equals("silent")) {
                            noSound = true;
                        }
                        else if (part.equals("!notify")) {
                            noNotification = true;
                        }
                        else if (part.equals("info")) {
                            appliesToType = Type.INFO;
                        }
                        else if (part.equals("any")) {
                            appliesToType = Type.ANY;
                        }
                        else if (part.equals("firstmsg")) {
                            addUserItem("First Message of User", null, user -> {
                                return user.getNumberOfMessages() == 0;
                            });
                        }
                        else if (part.equals("hl")) {
                            addTagsItem("Highlighted by channel points", null, t -> {
                                return t.isHighlightedMessage();
                            });
                        }
                        else if (part.equals("url")) {
                            matchItems.add(new Item("Contains URL", null, true) {
                                
                                @Override
                                public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                                    return matchesPattern(text, Helper.getUrlPattern(), blacklist);
                                }
                            });
                        }
                    });
                    parseBadges(list);
                    parseTags(list);
                }
                else if (item.startsWith("blacklist:")) {
                    List<String> list = parseStringListPrefix(item, "blacklist:", s -> s);
                    List<HighlightItem> blItems = new ArrayList<>();
                    for (String entry : list) {
                        HighlightItem hlItem = new HighlightItem(entry, invalidRegexLog);
                        if (!hlItem.hasError()) {
                            blItems.add(hlItem);
                            if (hlItem.patternThrowsError()) {
                                patternWarning = true;
                            }
                        }
                        else {
                            error = hlItem.getError();
                        }
                    }
                    if (!blItems.isEmpty()) {
                        if (localBlacklistItems == null) {
                            localBlacklistItems = blItems;
                        }
                        else {
                            localBlacklistItems.addAll(blItems);
                        }
                    }
                }
                //--------------------------
                // No prefix
                //--------------------------
                else {
                    textWithoutPrefix = item;
                    pattern = compilePattern("(?iu)" + Pattern.quote(item));
                }
            }
        }
        
        /**
         * Receives all list items from a single "config:" prefix, which may or
         * may not contain badge items. Only one of the resulting badge items
         * has to match in the resulting match item.
         * 
         * @param list The "config:" prefix items
         */
        private void parseBadges(List<String> list) {
            List<BadgeType> badges = new ArrayList<>();
            list.forEach(part -> {
                if (part.startsWith("b|") && part.length() > 2) {
                    badges.add(BadgeType.parse(part.substring(2)));
                }
            });
            if (!badges.isEmpty()) {
                addUserItem("Any of Twitch Badge", badges, user -> {
                    for (BadgeType type : badges) {
                        if (type.version == null) {
                            if (user.hasTwitchBadge(type.id)) {
                                return true;
                            }
                        }
                        else {
                            if (user.hasTwitchBadge(type.id, type.version)) {
                                return true;
                            }
                        }
                    }
                    return false;
                });
            }
        }
        
        /**
         * Receives all list items from a single "config:" prefix, which may or
         * may not contain tag items. Only of of the resulting tag items has to
         * match in the resulting match item.
         * 
         * @param list The "config:" prefix items
         */
        private void parseTags(List<String> list) {
            List<Pair<String, Pattern>> items = new ArrayList<>();
            list.forEach(part -> {
                if (part.startsWith("t|") && part.length() > 2) {
                    String tag = part.substring(2);
                    String[] split = tag.split("=", 2);
                    if (split.length == 2) {
                        String value = split[1];
                        Pattern p;
                        if (value.startsWith("reg:")) {
                            p = compilePattern(split[1].substring("reg:".length()));
                        }
                        else {
                            p = compilePattern(Pattern.quote(split[1]));
                        }
                        items.add(new Pair(split[0], p));
                    }
                    else {
                        items.add(new Pair(split[0], null));
                    }
                }
            });
            if (!items.isEmpty()) {
                addTagsItem("Any of Message Tags", items, tags -> {
                    for (Pair<String, Pattern> item : items) {
                        if (tags.containsKey(item.key)) {
                            if (item.value != null) {
                                if (item.value.matcher(tags.get(item.key)).matches()) {
                                    return true;
                                }
                            }
                            else {
                                return true;
                            }
                        }
                    }
                    return false;
                });
            }
        }
        
        /**
         * Find status ids in the status: or !status: prefix and save the ones
         * that were found as requirement. Characters that do not represent a
         * status id are ignored.
         * 
         * @param status The String containing the status ids
         * @param shouldBe Whether this is a requirement where the status should
         * be there or should NOT be there (status:/!status:)
         */
        private Set<Status> parseStatus(String status) {
            Set<Status> result = new HashSet<>();
            for (Status s : Status.values()) {
                if (status.contains(s.id)) {
                    result.add(s);
                }
            }
            return result;
        }
        
        /**
         * Parse a prefix with a parameter, also prepare() following items (if
         * present).
         * 
         * Uses StringUtil.split() to find the first non-quoted/escaped space,
         * with the returned prefix value cleared of quote/escape characters
         * (but the value handed to prepare() not).
         * 
         * @param item The input to parse the stuff from
         * @param prefix The name of the prefix, used to remove the prefix to
         * get the value
         * @return The value of the prefix
         */
        private String parsePrefix(String item, String prefix) {
            List<String> split = StringUtil.split(item, ' ', '"', '"', 2, 1);
            if (split.size() == 2) {
                prepare(split.get(1));
            }
            return split.get(0).substring(prefix.length());
        }
        
        private void parseListPrefixSingle(String item, String prefix, Consumer<String> p) {
            /**
             * Don't clear quote/escape characters from prefix value, since the
             * list parsing will still need them. It's a bit weird using the
             * same "level" of quote/escape characters for different "levels" of
             * parsing, but it should work well enough for this.
             */
            List<String> split = StringUtil.split(item, ' ', '"', '"', 2, 0);
            if (split.size() == 2) {
                prepare(split.get(1));
            }
            parseList(split.get(0).substring(prefix.length()), p);
        }
        
        /**
         * Parses a string list from the prefix value (items separated by ",").
         * 
         * @param item
         * @param prefix
         * @param c Applied to each list entries, for example to modify it
         * @return 
         */
        private List<String> parseStringListPrefix(String item, String prefix, Function<String, String> c) {
            List<String> result = new ArrayList<>();
            parseListPrefixSingle(item, prefix, p -> result.add(c.apply(p)));
            return result;
        }
        
        /**
         * Split input by comma and send each non-empty item to the given
         * consumer.
         * 
         * Since StringUtil.split() is used, quoting and escaping can be used
         * for ignoring commas.
         * 
         * @param list The String containing the comma-separated list
         * @param p The consumer
         */
        private static void parseList(String list, Consumer<String> p) {
            List<String> split = StringUtil.split(list, ',', '"', '"', 0, 1);
            for (String part : split) {
                if (!part.isEmpty()) {
                    p.accept(part);
                }
            }
        }
        
        /**
         * Based on the pre-defined static pattern map, look if the given input
         * contains a text matching prefix and compile the associated pattern.
         * 
         * @param input The input string
         * @return true if a text matching prefix was found, false otherwise
         */
        private boolean findPatternPrefixAndCompile(String input) {
            for (String prefix : patternPrefixes.keySet()) {
                // Check for prefix and that there is more text after it
                if (findAdditionalPatternPrefix(input, "+", prefix)) {
                    return true;
                }
                else if (findAdditionalPatternPrefix(input, "+!", prefix)) {
                    return true;
                }
                else if (findAdditionalPatternPrefix(input, "!", prefix)) {
                    return true;
                }
                else if (input.startsWith(prefix) && input.length() > prefix.length()) {
                    String withoutPrefix = input.substring(prefix.length());
                    String completePattern = patternPrefixes.get(prefix).apply(withoutPrefix);
                    textWithoutPrefix = withoutPrefix;
                    mainPrefix = prefix;
                    this.pattern = compilePattern(completePattern);
                    return true;
                }
            }
            return false;
        }
        
        private boolean findAdditionalPatternPrefix(String input, String type, String prefix) {
            String fullPrefix = type+prefix;
            if (input.startsWith(fullPrefix) && input.length() > fullPrefix.length()) {
                String value;
                if (type.startsWith("+")) {
                    // Also continues parsing other prefixes
                    value = parsePrefix(input, fullPrefix);
                }
                else {
                    // Take entire remaining text
                    value = input.substring(fullPrefix.length());
                    mainPrefix = fullPrefix;
                }
                String completePattern = patternPrefixes.get(prefix).apply(value);
                Pattern compiled = compilePattern(completePattern);
                if (type.equals("+")) {
                    matchItems.add(new Item("Additional regex (" + prefix.substring(0, prefix.length() - 1) + ")", compiled, true) {

                        @Override
                        public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                            return matchesPattern(text, compiled, blacklist);
                        }
                    });
                }
                else {
                    matchItems.add(new Item("Not matching regex (" + prefix.substring(0, prefix.length() - 1) + ")", compiled, true) {

                        @Override
                        public boolean matches(String text, Blacklist blacklist, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
                            return !matchesPattern(text, compiled, null);
                        }
                    });
                }
                
                return true;
            }
            return false;
        }
        
        /**
         * Adds a prefix and associated pattern builder to the static map.
         * 
         * @param patternBuilder
         * @param prefixes 
         */
        private static void addPatternPrefix(Function<String, String> patternBuilder, String... prefixes) {
            for (String prefix : prefixes) {
                patternPrefixes.put(prefix, patternBuilder);
            }
        }

        private Pattern compilePattern(String patternString) {
            try {
                Pattern pattern = Pattern.compile(patternString);
                if (patternThrowsError(pattern)) {
                    patternWarning = true;
                }
                return pattern;
            } catch (PatternSyntaxException ex) {
                error = ex.getDescription();
                if (invalidRegexLog) {
                    LOGGER.warning("Invalid regex: " + ex);
                }
                return NO_MATCH;
            }
        }
        
        /**
         * Check if the given text matches the text matching pattern,
         * disregarding matches that are blacklisted.
         *
         * @param text The input text to find the match in
         * @param blacklist The blacklist for the same input text
         * @return true if matches taking account the blacklist, false otherwise
         */
        private boolean matchesPattern(String text, Pattern pattern, Blacklist blacklist) {
            if (pattern == null) {
                return true;
            }
            try {
                Matcher m = pattern.matcher(text);
                while (m.find()) {
                    if (blacklist == null || !blacklist.isBlacklisted(m.start(), m.end())) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                /**
                 * Catch error since there seems to be a rare case where some
                 * regex matching on some text may trigger an exception:
                 * 
                 * Excerpt:
                 * java.lang.StringIndexOutOfBoundsException: String index out of range: 21
                 *   at java.lang.String.charAt(String.java:658)
                 *   at java.lang.Character.codePointAt(Character.java:4866)
                 *   at java.util.regex.Pattern$CIBackRef.match(Pattern.java:4948)
                 *   at java.util.regex.Pattern$BmpCharProperty.match(Pattern.java:3800)
                 *   at java.util.regex.Pattern$GroupTail.match(Pattern.java:4719)
                 *   at java.util.regex.Pattern$CharProperty.match(Pattern.java:3779)
                 *   at java.util.regex.Pattern$Curly.match0(Pattern.java:4274)
                 *   at java.util.regex.Pattern$Curly.match0(Pattern.java:4265)
                 *   at java.util.regex.Pattern$Curly.match(Pattern.java:4236)
                 *   at java.util.regex.Pattern$CharProperty.match(Pattern.java:3779)
                 *   at java.util.regex.Pattern$GroupHead.match(Pattern.java:4660)
                 *   at java.util.regex.Pattern$GroupTail.match(Pattern.java:4719)
                 *   at java.util.regex.Pattern$BranchConn.match(Pattern.java:4570)
                 *   at java.util.regex.Pattern$Begin.match(Pattern.java:3527)
                 *   at java.util.regex.Pattern$Branch.match(Pattern.java:4606)
                 *   at java.util.regex.Pattern$GroupHead.match(Pattern.java:4660)
                 *   at java.util.regex.Pattern$Start.match(Pattern.java:3463)
                 *   at java.util.regex.Matcher.search(Matcher.java:1248)
                 *   at java.util.regex.Matcher.find(Matcher.java:637)
                 *   at chatty.gui.Highlighter$HighlightItem.matchesPattern(Highlighter.java:526)
                 * 
                 * Possibly related: https://stackoverflow.com/q/16008974
                 * 
                 * Example for problematic item: "reg:(?i)(.)\1{2,}"
                 */
                if (Debugging.millisecondsElapsed("HighlighterRegexError", 5000)) {
                    /**
                     * Some delay to not spam too much as well as preventing
                     * possible infinite loop (since this outputs an info
                     * message which would in turn trigger an error again,
                     * however unlikely).
                     */
                    LOGGER.log(Logging.USERINFO,
                            String.format("Error: Regex '%s' failed with %s",
                                    pattern, ex));
                    LOGGER.warning(
                        String.format("Error: Regex '%s' failed on '%s' with %s",
                        pattern, text, Debugging.getStacktrace(ex)));
                }
            }
            return false;
        }
        
        /**
         * Returns all matches by the text pattern, or null if this item has no
         * text pattern.
         * 
         * @param text The string to look for matches in
         * @return List of Match objects, or null if no text pattern is set
         */
        public List<Match> getTextMatches(String text) {
            if (pattern == null) {
                return null;
            }
            List<Match> result = new ArrayList<>();
            try {
                Matcher m = pattern.matcher(text);
                while (m.find()) {
                    result.add(new Match(m.start(), m.end()));
                }
            } catch (Exception ex) {
                // See matchesPattern() for explanation
                if (Debugging.millisecondsElapsed("HighlighterRegexError", 5000)) {
                    LOGGER.log(Logging.USERINFO,
                            String.format("Error: Regex '%s' failed with %s",
                                    pattern, ex));
                    LOGGER.warning(
                        String.format("Error: Regex '%s' failed on '%s' with %s",
                        pattern, text, Debugging.getStacktrace(ex)));
                }
            }
            return result;
        }
        
        /**
         * Test for possible bug. See matchesPattern() for explanation.
         * 
         * @return true if an error is thrown on the test text, false otherwise
         */
        private static boolean patternThrowsError(Pattern pattern) {
            if (pattern != null) {
                try {
                    pattern.matcher("💕💕💕").find();
                } catch (Exception ex) {
                    return true;
                }
            }
            return false;
        }
        
        public boolean patternThrowsError() {
            return patternWarning;
        }
        
        /**
         * Get the main text part of this item, without any prefixes.
         * 
         * @return The text, may be empty
         */
        public String getTextWithoutPrefix() {
            return textWithoutPrefix;
        }
        
        /**
         * Get the main prefix for this item (such as "reg:" or "!reg:"). Could
         * be null.
         * 
         * @return 
         */
        public String getMainPrefix() {
            return mainPrefix;
        }
        
        public String getMatchInfo() {
            StringBuilder result = new StringBuilder();
            result.append("Applies to: ").append(appliesToType.description).append("\n");
            if (pattern != null) {
                result.append("Main regex: ").append(pattern).append("\n");
                addPatternWarning(result, pattern);
            }
            for (Item item : matchItems) {
                result.append(item.toString());
                result.append("\n");
                addPatternWarning(result, item.infoData);
            }
            if (localBlacklistItems != null) {
                result.append("Local blacklist:\n");
                for (HighlightItem item : localBlacklistItems) {
                    result.append("-- ").append(item.pattern).append("\n");
                    addPatternWarning(result, item.pattern);
                }
            }
            return result.toString();
        }
        
        private static void addPatternWarning(StringBuilder b, Object pattern) {
            if (pattern instanceof Pattern) {
                if (patternThrowsError((Pattern) pattern)) {
                    b.append("-- [!] The above regex may throw an error on some texts (due to a bug in the Java Regex implementation).\n");
                }
            }
        }
        
        public boolean matchesAny(String text, Blacklist blacklist) {
            return matches(Type.ANY, text, blacklist, null, null);
        }
        
        public boolean matchesTest(String text, Blacklist blacklist) {
            return matches(Type.TEXT_MATCH_TEST, text, blacklist, null, null);
        }
        
        public boolean matches(Type type, String text, User user, User localUser, MsgTags tags) {
            return matches(type, text, null, null, null, user, localUser, tags);
        }
        
        public boolean matches(Type type, String text, Blacklist blacklist,
                User user, User localUser) {
            return matches(type, text, blacklist, null, null, user, localUser, MsgTags.EMPTY);
        }
        
        public boolean matches(Type type, String text, String channel, Addressbook ab) {
            return matches(type, text, null, channel, ab, null, null, MsgTags.EMPTY);
        }
        
        /**
         * Check whether a message matches this item.
         * 
         * The type of the message can be ANY to disregard what type this item
         * applies to, otherwise the type has to be equal, unless the item
         * itself applies to ANY.
         * 
         * The channel, Addressbook and User can be null, in which case any
         * associated requirements are ignored. If User is not null, then
         * channel and Addressbook, if null, will be retrieved from User.
         * 
         * @param type The type of this message
         * @param text The text as received
         * @param blacklist The blacklist, can be null
         * @param channel The channel, can be null
         * @param ab The Addressbook, can be null
         * @param user The User object, can be null
         * @param localUser The local User object, can be null
         * @param tags MsgTags, can be null
         * @return true if it matches, false otherwise
         */
        public boolean matches(Type type, String text, Blacklist blacklist,
                String channel, Addressbook ab, User user, User localUser,
                MsgTags tags) {
            failedItem = null;
            
            if (localBlacklistItems != null) {
                blacklist = Blacklist.addMatches(blacklist, text, localBlacklistItems);
            }
            //------
            // Type
            //------
            if (type != appliesToType && appliesToType != Type.ANY
                    && type != Type.ANY && type != Type.TEXT_MATCH_TEST) {
                return false;
            }
            
            //------
            // Text
            //------
            if (pattern != null && !matchesPattern(text, pattern, blacklist)) {
                return false;
            }
            
            //-----------
            // Variables
            //-----------
            if (user != null) {
                if (channel == null) {
                    channel = user.getChannel();
                }
                if (ab == null) {
                    ab = user.getAddressbook();
                }
            }
            if (tags == null) {
                tags = MsgTags.EMPTY;
            }
            
//            System.out.println(raw);
            for (Item item : matchItems) {
                if (type == Type.TEXT_MATCH_TEST && !item.matchesOnText) {
                    continue;
                }
                boolean match = item.matches(text, blacklist, channel, ab, user, localUser, tags);
//                System.out.println(item);
                if (!match) {
                    failedItem = item;
                    return false;
                }
            }
            
            // If all the requirements didn't make it fail, this matches
            return true;
        }
        
        /**
         * Check if the status of a User matches the given requirements. It is
         * valid to either give the statusReq (requires status to BE there) or
         * statusReqNot (requires status to NOT BE there) set of requirements.
         * The set may contain only some or even no requirements.
         * 
         * @param user The user to check against
         * @param req The set of status requirements
         * @return true if the requirements match, false otherwise (depending
         * on which set of requirements was given, statusReq or statusReqNot,
         * only one requirement has to match or all have to match)
         */
        private static boolean checkStatus(User user, Set<Status> req, boolean positive) {
            // No requirement, so always matching
            if (req.isEmpty()) {
                return true;
            }
            if (user == null) {
                return false;
            }
            /**
             * If this checks the requirements that SHOULD be there, then this
             * is an OR relation (only one of the available requirements has to
             * match, so it will return true at the first matching requirement,
             * false otherwise).
             * 
             * Otherwise, then this is an AND relation (all of the available
             * requirements have to match, so it will return false at the first
             * requirement that doesn't match, true if all match).
             */
            boolean or = positive;
            if (req.contains(Status.MOD) && user.isModerator()) {
                return or;
            }
            if (req.contains(Status.SUBSCRIBER) && user.isSubscriber()) {
                return or;
            }
            if (req.contains(Status.ADMIN) && user.isAdmin()) {
                return or;
            }
            if (req.contains(Status.STAFF) && user.isStaff()) {
                return or;
            }
            if (req.contains(Status.BROADCASTER) && user.isBroadcaster()) {
                return or;
            }
            if (req.contains(Status.TURBO) && user.hasTurbo()) {
                return or;
            }
            if (req.contains(Status.GLOBAL_MOD) && user.isGlobalMod()) {
                return or;
            }
            if (req.contains(Status.BOT) && user.isBot()) {
                return or;
            }
            if (req.contains(Status.ANY_MOD) && user.hasModeratorRights()) {
                return or;
            }
            if (req.contains(Status.VIP) && user.isVip()) {
                return or;
            }
            return !or;
        }
        
        /**
         * Get the color defined for this entry, if any.
         * 
         * @return The Color or null if none was defined for this entry
         */
        public Color getColor() {
            return color;
        }
        
        public Color getBackgroundColor() {
            return backgroundColor;
        }
        
        public boolean noNotification() {
            return noNotification;
        }
        
        public boolean noSound() {
            return noSound;
        }
        
        public String getFailedReason() {
            if (failedItem != null) {
                return failedItem.toString();
            }
            return null;
        }
        
        public boolean hasError() {
            return error != null;
        }
        
        public String getError() {
            return error;
        }
        
        public String getReplacement() {
            return replacement;
        }
        
    }
    
    public static class Blacklist {
        
        private final Collection<Match> blacklisted;
        
        /**
         * Creates the blacklist for a specific message, using the stored
         * blacklist items and the message data to get all relevant matches.
         * 
         * @param type The type of the message to create the Blacklist for
         * @param text
         * @param channel
         * @param ab
         * @param user
         * @param items The HighlightItem objects that the Blacklist is based on
         */
        public Blacklist(HighlightItem.Type type, String text, String channel,
                Addressbook ab, User user, User localUser, MsgTags tags, Collection<HighlightItem> items) {
            blacklisted = new ArrayList<>();
            for (HighlightItem item : items) {
                if (item.matches(type, text, null, channel, ab, user, localUser, tags)) {
                    List<Match> matches = item.getTextMatches(text);
                    if (matches != null) {
                        blacklisted.addAll(matches);
                    } else {
                        blacklisted.add(null);
                    }
                }
            }
        }
        
        public Blacklist(Collection<Match> blacklisted) {
            this.blacklisted = blacklisted;
        }
        
        public boolean isBlacklisted(int start, int end) {
            for (Match section : blacklisted) {
                if (section == null || section.spans(start, end)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public String toString() {
            return blacklisted.toString();
        }
        
        /**
         * Add text matches (and only text matches) from the given HighlightItem
         * objects to the given Blacklist's matches, as a new Blacklist.
         * 
         * @param blacklist The Blacklist to take existing matches from (can be
         * null)
         * @param text The text to check against
         * @param items The HighlightItem objects to get more text matches from
         * @return A new Blacklist with the previous and new matches (if any)
         */
        public static Blacklist addMatches(Blacklist blacklist,
                                           String text,
                                           Collection<HighlightItem> items) {
            Collection<Match> matches = new ArrayList<>();
            if (blacklist != null) {
                matches.addAll(blacklist.blacklisted);
            }
            if (items != null) {
                for (HighlightItem item : items) {
                    List<Match> m = item.getTextMatches(text);
                    matches.addAll(m);
                }
            }
            return new Blacklist(matches);
        }

    }
    
    public static class Match {

        public final int start;
        public final int end;

        private Match(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public boolean spans(int start, int end) {
            return this.start <= start && this.end >= end;
        }
        
        @Override
        public String toString() {
            return start+"-"+end;
        }
        
        /**
         * Add to the index of all Match objects in the given list of matches.
         * Resulting negative indices are set to 0, if start and end are equal
         * the match is not added to the result.
         * 
         * @param input The input list
         * @param shift How much to add to the indices
         * @return A new list with new Match objects, or the same as input if it
         * was null or empty
         */
        public static List<Match> shiftMatchList(List<Match> input, int shift) {
            if (input == null || input.isEmpty()) {
                return input;
            }
            List<Match> result = new ArrayList<>();
            for (Match m : input) {
                int start = Math.max(m.start + shift, 0);
                int end = Math.max(m.end + shift, 0);
                if (start != end) {
                    result.add(new Match(start, end));
                }
            }
            return result;
        }

    }
    
}
