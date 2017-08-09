
package chatty.gui;

import chatty.Helper;
import chatty.User;
import chatty.util.StringUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
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
    private Pattern usernamePattern;
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
        items.clear();
        for (String item : newItems) {
            if (item != null && !item.isEmpty()) {
                items.add(new HighlightItem(item));
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
            usernamePattern = null;
        }
        else {
            // Create pattern to match username on word boundaries
            try {
                usernamePattern = Pattern.compile("(?i).*\\b"+username+"\\b.*");
            } catch (PatternSyntaxException ex) {
                LOGGER.warning("Invalid regex for username: " + ex.getLocalizedMessage());
                usernamePattern = null;
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
        
        lastMatchColor = null;
        lastMatchNoNotification = false;
        lastMatchNoSound = false;
        
        String lowercaseText = text.toLowerCase();
        
        // Try to match own name first (if enabled)
        if (highlightUsername && usernamePattern != null &&
                usernamePattern.matcher(text).matches()) {
            return true;
        }
        
        // Then try to match against the items
        for (HighlightItem item : items) {
            if (item.matches(user, text, lowercaseText)) {
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
     * A single item that itself parses the item String and prepares it for
     * matching. The item can be asked whether it matches a message.
     * 
     * A message matches the item if the message text contains the text of this
     * item (case-insensitive).
     * 
     * Prefixes that change this behaviour:
     * user: - to match the exact username the message is from
     * cs: - to match the following term case-sensitive
     * re: - to match as regex
     * chan: - to match when the user is on this channel (can be a
     * comma-seperated list)
     * !chan: - same as chan: but inverted
     * status:m
     * 
     * An item can be prefixed with a user:username, so the username as well
     * as the item after it has to match.
     */
    public static class HighlightItem {
        
        private String username;
        private Pattern usernamePattern;
        private Pattern pattern;
        private Pattern pattern2;
        private String caseSensitive;
        private String caseInsensitive;
        private String startsWith;
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
        
        private boolean error;
        
        private final Set<Status> statusReq = new HashSet<>();
        private final Set<Status> statusReqNot = new HashSet<>();
        
        enum Status {
            MOD("m"), SUBSCRIBER("s"), BROADCASTER("b"), ADMIN("a"), STAFF("f"),
            TURBO("t"), ANY_MOD("M"), GLOBAL_MOD("g"), BOT("r");
            
            private final String id;
            
            Status(String id) {
                this.id = id;
            }
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
            if (item.startsWith("re:") && item.length() > 3) {
                compilePattern(item.substring(3));
            } else if (item.startsWith("re*:") && item.length() > 4) {
                compilePattern2(item.substring(4));
            } else if (item.startsWith("w:") && item.length() > 2) {
                compilePattern("(?i).*\\b"+item.substring(2)+"\\b.*");
            } else if (item.startsWith("wcs:") && item.length() > 4) {
                compilePattern(".*\\b"+item.substring(4)+"\\b.*");
            } else if (item.startsWith("cs:") && item.length() > 3) {
                caseSensitive = item.substring(3);
            } else if (item.startsWith("start:") && item.length() > 6) {
                startsWith = item.substring(6).toLowerCase();
            } else if (item.startsWith("cat:")) {
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
                caseInsensitive = item.toLowerCase();
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
         * Compiles a pattern (regex) and sets it as pattern.
         * 
         * @param patternString 
         */
        private void compilePattern(String patternString) {
            try {
                pattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException ex) {
                error = true;
                LOGGER.warning("Invalid regex: " + ex);
            }
        }
        
        private void compilePattern2(String patternString) {
            try {
                pattern2 = Pattern.compile(patternString);
            } catch (PatternSyntaxException ex) {
                error = true;
                LOGGER.warning("Invalid regex2: " + ex);
            }
        }
        
        private void compileUsernamePattern(String patternString) {
            try {
                usernamePattern = Pattern.compile(patternString);
            } catch (PatternSyntaxException ex) {
                error = true;
                LOGGER.warning("Invalid username regex: " + ex);
            }
        }
        
        public boolean matches(String text) {
            return matches(null, text, StringUtil.toLowerCase(text), true);
        }
        
        public boolean matches(String text, String lowercaseText) {
            return matches(null, text, lowercaseText, true);
        }
        
        public boolean matches(User user, String text, String lowercaseText) {
            return matches(user, text, lowercaseText, false);
        }
        
        /**
         * Check whether a message matches this item.
         * 
         * @param user The User object, or null if this message has none
         * @param text The text as received
         * @param lowercaseText The text in lowercase (minor optimization, so
         *  it doesn't have to be made lowercase for every item)
         * @param noUserRequired Will continue matching when no User object is
         * given, even without config:info
         * @return true if it matches, false otherwise
         */
        public boolean matches(User user, String text, String lowercaseText,
                boolean noUserRequired) {
            
            if (pattern != null && !pattern.matcher(text).matches()) {
                return false;
            }
            if (pattern2 != null && !pattern2.matcher(text).find()) {
                return false;
            }
            if (caseSensitive != null && !text.contains(caseSensitive)) {
                return false;
            }
            if (caseInsensitive != null && !lowercaseText.contains(caseInsensitive)) {
                return false;
            }
            if (startsWith != null && !lowercaseText.startsWith(startsWith)) {
                return false;
            }
            if (user == null) {
                return appliesToInfo || noUserRequired;
            }
            // Everything else from here is user-based
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
            return error;
        }
        
    }
    
}
