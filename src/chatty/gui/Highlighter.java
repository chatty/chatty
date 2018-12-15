
package chatty.gui;

import chatty.util.colors.HtmlColors;
import chatty.Addressbook;
import chatty.Helper;
import chatty.User;
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
        return Highlighter.this.check(HighlightItem.Type.REGULAR, text, null, null, user);
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
            Addressbook ab, User user) {
        Blacklist blacklist = null;
        if (!blacklistItems.isEmpty()) {
            blacklist = new Blacklist(type, text, channel, ab, user, blacklistItems);
        }
        
        // Only reset matches, since the other variables are filled anyway,
        // except for "follow-up", where they should stay the same
        lastTextMatches = null;
        
        // Try to match own name first (if enabled)
        if (highlightUsername && usernameItem != null &&
                usernameItem.matches(type, text, blacklist,
                        channel, ab, user)) {
            fillLastMatchVariables(usernameItem, text);
            addMatch(user, usernameItem);
            return true;
        }
        
        // Then try to match against the items
        for (HighlightItem item : items) {
            if (item.matches(type, text, blacklist, channel, ab, user)) {
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
    
    private void addMatch(User user, HighlightItem item) {
        if (highlightNextMessages && user != null) {
            String username = user.getName();
            lastHighlighted.put(username, System.currentTimeMillis());
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
            if (System.currentTimeMillis() - entry.getValue() > LAST_HIGHLIGHTED_TIMEOUT) {
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
            REGULAR, INFO, ANY
        }
        
        /**
         * A regex that will never match.
         */
        private static final Pattern NO_MATCH = Pattern.compile("(?!)");
        
        private String username;
        private Pattern usernamePattern;
        private Pattern pattern;
        private String category;
        private final Set<String> notChannels = new HashSet<>();
        private final Set<String> channels = new HashSet<>();
        private String channelCategory;
        private String channelCategoryNot;
        private String categoryNot;
        private Color color;
        private Color backgroundColor;
        private boolean noNotification;
        private boolean noSound;
        private Type appliesToType = Type.REGULAR;
        private boolean firstMsg;
        // Replacement string for filtering parts of a message
        private String replacement;
        
        private String error;
        private String textWithoutPrefix = "";
        
        private final Set<Status> statusReq = new HashSet<>();
        private final Set<Status> statusReqNot = new HashSet<>();
        
        private enum Status {
            MOD("m"), SUBSCRIBER("s"), BROADCASTER("b"), ADMIN("a"), STAFF("f"),
            TURBO("t"), ANY_MOD("M"), GLOBAL_MOD("g"), BOT("r");
            
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
        }
        
        public HighlightItem(String item) {
            prepare(item);
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
                if (item.startsWith("cat:")) {
                    category = parsePrefix(item, "cat:");
                } else if (item.startsWith("!cat:")) {
                    categoryNot = parsePrefix(item, "!cat:");
                } else if (item.startsWith("user:")) {
                    username = parsePrefix(item, "user:").toLowerCase(Locale.ENGLISH);
                } else if (item.startsWith("reuser:")) {
                    String regex = parsePrefix(item, "reuser:").toLowerCase(Locale.ENGLISH);
                    compileUsernamePattern(regex);
                } else if (item.startsWith("chan:")) {
                    parseListPrefix(item, "chan:");
                } else if (item.startsWith("!chan:")) {
                    parseListPrefix(item, "!chan:");
                } else if (item.startsWith("chanCat:")) {
                    channelCategory = parsePrefix(item, "chanCat:");
                } else if (item.startsWith("!chanCat:")) {
                    channelCategoryNot = parsePrefix(item, "!chanCat:");
                } else if (item.startsWith("color:")) {
                    color = HtmlColors.decode(parsePrefix(item, "color:"));
                } else if (item.startsWith("bgcolor:")) {
                    backgroundColor = HtmlColors.decode(parsePrefix(item, "bgcolor:"));
                } else if (item.startsWith("status:")) {
                    String status = parsePrefix(item, "status:");
                    parseStatus(status, true);
                } else if (item.startsWith("!status:")) {
                    String status = parsePrefix(item, "!status:");
                    parseStatus(status, false);
                } else if (item.startsWith("config:")) {
                    parseListPrefix(item, "config:");
                } else if (item.startsWith("replacement:")) {
                    replacement = parsePrefix(item, "replacement:");
                } else {
                    textWithoutPrefix = item;
                    compilePattern("(?iu)" + Pattern.quote(item));
                }
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
        private void parseStatus(String status, boolean shouldBe) {
            for (Status s : Status.values()) {
                if (status.contains(s.id)) {
                    if (shouldBe) {
                        statusReq.add(s);
                    } else {
                        statusReqNot.add(s);
                    }
                }
            }
        }
        
        /**
         * Parse a prefix with a parameter, also prepare() following items (if
         * present).
         * 
         * @param item The input to parse the stuff from
         * @param prefix The name of the prefix, used to remove the prefix to
         * get the value
         * @return The value of the prefix
         */
        private String parsePrefix(String item, String prefix) {
            String[] split = item.split(" ", 2);
            if (split.length == 2) {
                // There is something after this prefix, so prepare that just
                // like another item (but of course added to this object).
                prepare(split[1]);
            }
            return split[0].substring(prefix.length());
        }
        
        private void parseListPrefix(String item, String prefix) {
            parseList(parsePrefix(item, prefix), prefix);
        }
        
        /**
         * Parses a comma-separated list of a prefix.
         * 
         * @param list The String containing the list
         * @param prefix The prefix for this list, used to determine what to do
         * with the found list items
         */
        private void parseList(String list, String prefix) {
            String[] split2 = list.split(",");
            for (String part : split2) {
                if (!part.isEmpty()) {
                    if (prefix.equals("chan:")) {
                        channels.add(Helper.toChannel(part));
                    } else if (prefix.equals("!chan:")) {
                        notChannels.add(Helper.toChannel(part));
                    } else if (prefix.equals("config:")) {
                        if (part.equals("silent")) {
                            noSound = true;
                        } else if (part.equals("!notify")) {
                            noNotification = true;
                        } else if (part.equals("info")) {
                            appliesToType = Type.INFO;
                        } else if (part.equals("any")) {
                            appliesToType = Type.ANY;
                        } else if (part.equals("firstmsg")) {
                            firstMsg = true;
                        }
                    }
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
                if (input.startsWith(prefix) && input.length() > prefix.length()) {
                    String withoutPrefix = input.substring(prefix.length());
                    String pattern = patternPrefixes.get(prefix).apply(withoutPrefix);
                    textWithoutPrefix = withoutPrefix;
                    compilePattern(pattern);
                    return true;
                }
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
        
        /**
         * Compiles a pattern (regex) and sets it as pattern.
         * 
         * @param patternString 
         */
        private void compilePattern(String patternString) {
            try {
                pattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException ex) {
                error = ex.getDescription();
                pattern = NO_MATCH;
                LOGGER.warning("Invalid regex: " + ex);
            }
        }
        
        private void compileUsernamePattern(String patternString) {
            try {
                usernamePattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException ex) {
                error = ex.getDescription();
                pattern = NO_MATCH;
                LOGGER.warning("Invalid username regex: " + ex);
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
        private boolean matchesPattern(String text, Blacklist blacklist) {
            if (pattern == null) {
                return true;
            }
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                if (blacklist == null || !blacklist.isBlacklisted(m.start(), m.end())) {
                    return true;
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
            Matcher m = pattern.matcher(text);
            while (m.find()) {
                result.add(new Match(m.start(), m.end()));
            }
            return result;
        }
        
        /**
         * Get the text part of this item, without any prefixes.
         * 
         * @return The text, may be empty
         */
        public String getTextWithoutPrefix() {
            return textWithoutPrefix;
        }
        
        public String getPatternText() {
            if (pattern != null) {
                return pattern.pattern();
            }
            return null;
        }
        
        public boolean matchesAny(String text, Blacklist blacklist) {
            return matches(Type.ANY, text, blacklist, null);
        }
        
        public boolean matches(Type type, String text, User user) {
            return matches(type, text, null, null, null, user);
        }
        
        public boolean matches(Type type, String text, Blacklist blacklist,
                User user) {
            return matches(type, text, blacklist, null, null, user);
        }
        
        public boolean matches(Type type, String text, String channel, Addressbook ab) {
            return matches(type, text, null, channel, ab, null);
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
         * @return true if it matches, false otherwise
         */
        public boolean matches(Type type, String text, Blacklist blacklist,
                String channel, Addressbook ab, User user) {
            //------
            // Type
            //------
            if (type != appliesToType && appliesToType != Type.ANY
                    && type != Type.ANY) {
                return false;
            }
            
            //------
            // Text
            //------
            if (pattern != null && !matchesPattern(text, blacklist)) {
                return false;
            }
            
            //---------
            // Channel
            //---------
            if (channel == null && user != null) {
                channel = user.getChannel();
            }
            if (user != null) {
                if (channel == null) {
                    channel = user.getChannel();
                }
                if (ab == null) {
                    ab = user.getAddressbook();
                }
            }
            if (!channels.isEmpty() && channel != null
                    && !channels.contains(channel)) {
                return false;
            }
            if (!notChannels.isEmpty() && channel != null
                    && notChannels.contains(channel)) {
                return false;
            }
            if (channelCategory != null && ab != null && channel != null
                    && !ab.hasCategory(channel, channelCategory)) {
                return false;
            }
            if (channelCategoryNot != null && ab != null && channel != null
                    && ab.hasCategory(channel, channelCategoryNot)) {
                return false;
            }

            //------
            // User
            //------
            if (username != null && user != null
                    && !username.equals(user.getName())) {
                return false;
            }
            if (usernamePattern != null && user != null
                    && !usernamePattern.matcher(user.getName()).matches()) {
                return false;
            }
            if (category != null && user != null
                    && !user.hasCategory(category)) {
                return false;
            }
            if (categoryNot != null && user != null
                    && user.hasCategory(categoryNot)) {
                return false;
            }
            if (!checkStatus(user, statusReq)) {
                return false;
            }
            if (!checkStatus(user, statusReqNot)) {
                return false;
            }
            // Message count is updated after printing message, so it checks 0
            if (firstMsg && user != null
                    && user.getNumberOfMessages() > 0) {
                return false;
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
        private boolean checkStatus(User user, Set<Status> req) {
            // No requirement, so always matching
            if (req.isEmpty()) {
                return true;
            }
            if (user == null) {
                return true;
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
            boolean or = req == statusReq;
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
                Addressbook ab, User user, Collection<HighlightItem> items) {
            blacklisted = new ArrayList<>();
            for (HighlightItem item : items) {
                if (item.matches(type, text, null, channel, ab, user)) {
                    List<Match> matches = item.getTextMatches(text);
                    if (matches != null) {
                        blacklisted.addAll(matches);
                    } else {
                        blacklisted.add(null);
                    }
                }
            }
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

    }
    
}
