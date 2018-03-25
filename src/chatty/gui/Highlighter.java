
package chatty.gui;

import chatty.Helper;
import chatty.User;
import chatty.util.StringUtil;
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
 * Checks if a given String matches the saved highlight items.
 * 
 * @author tduva
 */
public class Highlighter {
    
    private static final Logger LOGGER = Logger.getLogger(Highlighter.class.getName());
    
    private static final int LAST_HIGHLIGHTED_TIMEOUT = 10*1000;
    
    private final Map<String, Long> lastHighlighted = new HashMap<>();
    private final List<HighlightItem> items = new ArrayList<>();
    private final List<HighlightItem> blacklistItems = new ArrayList<>();
    private HighlightItem usernameItem;
    private Color lastMatchColor;
    private boolean lastMatchNoNotification;
    private boolean lastMatchNoSound;
    
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
    
    public boolean check(User fromUser, String text) {
        if (checkMatch(fromUser, text)) {
            if (fromUser != null) {
                addMatch(fromUser.getName());
            }
            return true;
        }
        return false;
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
    
    public boolean getLastMatchNoNotification() {
        return lastMatchNoNotification;
    }
    
    public boolean getLastMatchNoSound() {
        return lastMatchNoSound;
    }
    
    /**
     * Checks whether the given message consisting of username and text should
     * be highlighted.
     * 
     * @param userName The name of the user who send the message
     * @param text The text of the message
     * @return true if the message should be highlighted, false otherwise
     */
    private boolean checkMatch(User user, String text) {
        
        Blacklist blacklist = new Blacklist(user, text, blacklistItems, false);
        
        lastMatchColor = null;
        lastMatchNoNotification = false;
        lastMatchNoSound = false;
        
        // Try to match own name first (if enabled)
        if (highlightUsername && usernameItem != null &&
                usernameItem.matches(user, text, true, blacklist)) {
            return true;
        }
        
        // Then try to match against the items
        for (HighlightItem item : items) {
            if (item.matches(user, text, false, blacklist)) {
                lastMatchColor = item.getColor();
                lastMatchNoNotification = item.noNotification();
                lastMatchNoSound = item.noSound();
                return true;
            }
        }
        
        // Then see if there is a recent match
        if (highlightNextMessages && user != null && hasRecentMatch(user.getName())) {
            return true;
        }
        return false;
    }
    
    private void addMatch(String fromUsername) {
        lastHighlighted.put(fromUsername, System.currentTimeMillis());
    }
    
    private boolean hasRecentMatch(String fromUsername) {
        clearRecentMatches();
        return lastHighlighted.containsKey(fromUsername);
    }
    
    private void clearRecentMatches() {
        Iterator<Map.Entry<String, Long>> it = lastHighlighted.entrySet().iterator();
        while (it.hasNext()) {
            if (System.currentTimeMillis() - it.next().getValue() > LAST_HIGHLIGHTED_TIMEOUT) {
                it.remove();
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
        private boolean noNotification;
        private boolean noSound;
        private boolean appliesToInfo;
        
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
                } else if (item.startsWith("status:")) {
                    String status = parsePrefix(item, "status:");
                    parseStatus(status, true);
                } else if (item.startsWith("!status:")) {
                    String status = parsePrefix(item, "!status:");
                    parseStatus(status, false);
                } else if (item.startsWith("config:")) {
                    parseListPrefix(item, "config:");
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
         * Parses a comma-seperated list of a prefix.
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
                            appliesToInfo = true;
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
        
        public boolean matches(String text) {
            return matches(null, text, true, null);
        }
        
        public boolean matches(User user, String text) {
            return matches(user, text, false, null);
        }
        
        /**
         * Check whether a message matches this item.
         * 
         * @param user The User object, or null if this message has none
         * @param text The text as received
         * @param noUserRequired Will continue matching when no User object is
         * given, even without config:info
         * @param blacklist
         * @return true if it matches, false otherwise
         */
        public boolean matches(User user, String text, boolean noUserRequired,
                Blacklist blacklist) {
            /**
             * Check text matching, if present.
             */
            if (pattern != null && !matchesPattern(text, blacklist)) {
                return false;
            }
            /**
             * This was called without User object, so only match if either
             * "config:info" was present or wanted by the caller (e.g. if only
             * applied to one message type).
             */
            if (user == null) {
                return appliesToInfo || noUserRequired;
            }
            /**
             * If a User object was supplied and "config:info" was present, then
             * this shouldn't be matched, because it can't be an info message.
             * 
             * TODO: If message types should be matched more reliably, there
             * should probably be an extra message type parameter instead of
             * reyling on whether an User object was supplied.
             */
            if (user != null && appliesToInfo) {
                return false;
            }
            if (username != null && !username.equals(user.getName())) {
                return false;
            }
            if (usernamePattern != null && !usernamePattern.matcher(user.getName()).matches()) {
                return false;
            }
            if (category != null && !user.hasCategory(category)) {
                return false;
            }
            if (categoryNot != null && user.hasCategory(categoryNot)) {
                return false;
            }
            if (!channels.isEmpty() && !channels.contains(user.getChannel())) {
                return false;
            }
            if (!notChannels.isEmpty() && notChannels.contains(user.getChannel())) {
                return false;
            }
            if (channelCategory != null && !user.hasCategory(channelCategory, user.getChannel())) {
                return false;
            }
            if (channelCategoryNot != null && user.hasCategory(channelCategoryNot, user.getChannel())) {
                return false;
            }
            if (!checkStatus(user, statusReq)) {
                return false;
            }
            if (!checkStatus(user, statusReqNot)) {
                return false;
            }
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
        
    }
    
    public static class Blacklist {
        
        private final Collection<Match> blacklisted;
        
        public Blacklist(User user, String text, Collection<HighlightItem> items, boolean noUserReq) {
            blacklisted = new ArrayList<>();
            for (HighlightItem item : items) {
                if (item.matches(user, text, noUserReq, null)) {
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

    }
    
}
