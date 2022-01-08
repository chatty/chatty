
package chatty.gui.components;

import chatty.gui.components.completion.AutoCompletionServer;
import chatty.Room;
import chatty.User;
import chatty.gui.MainGui;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.ImageType;
import chatty.util.api.Emoticons;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;

/**
 *
 * @author tduva
 */
public class ChannelCompletion implements AutoCompletionServer {

    private final Channel channel;
    private final ChannelEditBox input;
    private final MainGui main;
    private final UserList users;

    public ChannelCompletion(Channel channel, MainGui main,
            ChannelEditBox input, UserList users) {
        this.channel = channel;
        this.main = main;
        this.input = input;
        this.users = users;
    }
    
    /**
     * Can contain case, will be completed to that.
     */
    private final Set<String> commands = new TreeSet<>(Arrays.asList(new String[]{
        "subscribers", "subscribersOff", "timeout", "ban", "unban", "host", "unhost", "raid", "unraid", "clear", "mods", "commercial",
        "join", "part", "close", "reconnect", "slow", "slowOff", "r9k", "r9koff", "emoteOnly", "emoteOnlyOff",
        "connection", "uptime", "appInfo", "releaseInfo",
        "dir", "wdir", "openDir", "openWdir", "showLogDir", "openLogDir",
        "showBackupDir", "openBackupDir", "showDebugDir", "openDebugDir",
        "showTempDir", "openTempDir", "showJavaDir", "openJavaDir",
        "showFallbackFontDir", "openFallbackFontDir", "customCompletion",
        "clearChat", "refresh", "changeToken", "testNotification", "server",
        "clearStreamChat", "getStreamChatSize", "setStreamChatSize", "streamChatTest", "openStreamChat",
        "customEmotes", "reloadCustomEmotes", "addStreamHighlight", "openStreamHighlights",
        "ignore", "unignore", "ignoreWhisper", "unignoreWhisper", "ignoreChat", "unignoreChat",
        "follow", "unfollow", "ffzws", "followers", "followersoff",
        "setcolor", "untimeout", "userinfo", "joinHosted", "favorite", "unfavorite",
        "popoutchannel", "setSize"
    }));
    
    private final Set<String> settingCommands = new TreeSet<>(Arrays.asList(new String[]{
        "set", "set2", "add", "add2", "addUnique", "addUnique2", "clearSetting",
        "remove", "remove2", "get", "reset"
    }));
    
    private boolean isSettingPrefix(String prefix) {
        for (String command : settingCommands) {
            if (prefix.equals("/"+StringUtil.toLowerCase(command)+" ")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Must be all lowercase for comparison.
     */
    private final Set<String> prefixesPreferUsernames = new HashSet<>(Arrays.asList(new String[]{
        "/ban ", "/to ", "/setname ", "/resetname ", "/timeout ", "/host ",
        "/unban ", "/ignore ", "/unignore ", "/ignorechat ", "/unignorechat ",
        "/ignorewhisper ", "/unignorewhisper ", "/follow ", "/unfollow ",
        "/untimeout ", "/favorite ", "/unfavorite ", "@@"
    }));
    
    private Font currentFont;
    private int currentEmoteScaling;
    private ImageType currentEmoteImageType;
    
    private Settings settings() {
        return main.getSettings();
    }

    private void updateSettings() {
        input.setCompletionMaxItemsShown((int) main.getSettings().getLong("completionMaxItemsShown"));
        input.setCompletionShowPopup(main.getSettings().getBoolean("completionShowPopup"));
        input.setCompleteToCommonPrefix(main.getSettings().getBoolean("completionCommonPrefix"));
        input.setCompletionAppendSpace(settings().getBoolean("completionSpace"));
        if (main.getSettings().getLong("emoteScaleDialog") != currentEmoteScaling
                || input.getFont() != currentFont) {
            currentEmoteScaling = (int)main.getSettings().getLong("emoteScaleDialog");
            currentFont = input.getFont();
            updateSizeSettings();
        }
        currentEmoteImageType = Emoticon.makeImageType(main.getSettings().getBoolean("animatedEmotes"));
    }
    
    private void updateSizeSettings() {
        int baseEmoteHeight = 32;
        int baseEmoteWidth = 30;
        int emoteScale = currentEmoteScaling;
        int scaledEmoteHeight = (baseEmoteHeight * emoteScale) / 100;
        int scaledEmoteWidth = (baseEmoteWidth * emoteScale) / 100;
        int fontHeight = input.getGraphics().getFontMetrics().getHeight();
        int cellHeight = Math.max(scaledEmoteHeight, fontHeight);
        
        String testText = "AbcdefghIJKLmnoPQRSTUVwxyz123456";
        int textWidth = input.getGraphics().getFontMetrics().stringWidth(testText);
        int cellWidth = textWidth + scaledEmoteWidth;
        
        input.setCompletionCellSize(cellWidth, cellHeight);
    }

    @Override
    public AutoCompletionServer.CompletionItems getCompletionItems(String type, String prefix, String search) {
        updateSettings();
        String searchLower = StringUtil.toLowerCase(search);
        prefix = StringUtil.toLowerCase(prefix);
        if (type == null || type.equals("auto")) {
            return getRegularCompletionItems(prefix, searchLower, search);
        } else if (type.equals("special")) {
            return getSpecialItems(prefix, searchLower, search);
        }
        return new AutoCompletionServer.CompletionItems();
    }

    /**
     * TAB
     *
     * @param prefix Lowercase prefix
     * @param search Lowercase search
     * @param searchCase Original search
     * @return
     */
    private AutoCompletionServer.CompletionItems getRegularCompletionItems(String prefix, String search, String searchCase) {
        List<String> items;
        if (prefix.startsWith("/") && isSettingPrefix(prefix)) {
                //--------------
            // Setting Names
            //--------------
            input.setCompleteToCommonPrefix(true);
            items = filterCompletionItems(main.getSettingNames(), search);
        } else if (prefix.equals("/")) {
                //--------------
            // Command Names
            //--------------
            List<String> c = new ArrayList<>();
            c.addAll(commands);
            c.addAll(settingCommands);
            items = filterCompletionItems(c, search);
            items.addAll(filterCompletionItems(main.getCustomCommandNames(), search));
        } else {
                //--------------------
            // Depending on Config
            //--------------------
            return getMainItems(main.getSettings().getString("completionTab"), prefix, search, searchCase);
        }
        return CompletionItems.createFromStrings(items, "");
    }

    /**
     * Shift-TAB
     *
     * @param prefix
     * @param search
     * @return
     */
    private AutoCompletionServer.CompletionItems getSpecialItems(String prefix, String search, String searchCase) {
        return getMainItems(main.getSettings().getString("completionTab2"), prefix, search, searchCase);
    }

    /**
     * Returns items that depend on current settings, but also some prefixes.
     *
     * @param setting
     * @param prefix Lowercase prefix
     * @param search
     * @return
     */
    private AutoCompletionServer.CompletionItems getMainItems(String setting, String prefix, String search, String searchCase) {
        boolean preferUsernames = prefixesPreferUsernames.contains(prefix)
                && main.getSettings().getBoolean("completionPreferUsernames");
        String emotePrefix = settings().getString("completionEmotePrefix");
        
        // Check prefixes and other non-setting dependant stuff first
        if (prefix.endsWith("@") || prefixesPreferUsernames.contains(prefix)) {
            return getCompletionItemsNames(search, preferUsernames);
        }
        if (prefix.endsWith(".")) {
            return AutoCompletionServer.CompletionItems.createFromStrings(getCustomCompletionItems(searchCase), ".");
        }
        if (prefix.endsWith(":")) {
            if (emotePrefix.equals(":")) {
                CompletionItems items;
                switch ((int)settings().getLong("completionMixed")) {
                    case 0:
                        items = getCompletionItemsEmoji(search);
                        items.append(getCompletionItemsEmotes(search, ":"));
                        sortMixed(items, search);
                        return items;
                    case 1:
                        items = getCompletionItemsEmoji(search);
                        items.append(getCompletionItemsEmotes(search, ":"));
                        return items;
                    case 2:
                        items = getCompletionItemsEmotes(search, ":");
                        items.append(getCompletionItemsEmoji(search));
                        return items;
                }
            }
            return getCompletionItemsEmoji(search);
        }
        if (!emotePrefix.isEmpty() && prefix.endsWith(emotePrefix)) {
            return getCompletionItemsEmotes(search, emotePrefix);
        }

        // Then check settings
        if (setting.equals("names")) {
            return getCompletionItemsNames(search, preferUsernames);
        }
        if (setting.equals("emotes")) {
            return getCompletionItemsEmotes(search, "");
        }
        if (setting.equals("custom")) {
            return AutoCompletionServer.CompletionItems.createFromStrings(getCustomCompletionItems(searchCase), "");
        }
        AutoCompletionServer.CompletionItems names = getCompletionItemsNames(search, preferUsernames);
        AutoCompletionServer.CompletionItems emotes = getCompletionItemsEmotes(search, "");
        if (setting.equals("both")) {
            names.append(emotes);
            return names;
        } else { // both2
            emotes.append(names);
            return emotes;
        }
    }

    private AutoCompletionServer.CompletionItems getCompletionItemsEmotes(String search, String prefix) {
        Collection<Emoticon> allEmotes = new LinkedList<>(main.getUsableGlobalEmotes());
        allEmotes.addAll(main.getUsableEmotesPerStream(channel.getStreamName()));
        List<Emoticon> result = filterCompletionItems(allEmotes, search, SORT_EMOTES_BY_NAME, item -> {
            return item.code;
        });
        List<CompletionItem> items = new ArrayList<>();
        for (Emoticon emote : result) {
            String code = Emoticons.toWriteable(emote.code);
            items.add(createEmoteItem(code, null, emote));
        }
        return new CompletionItems(items, prefix);
    }
    
    private CompletionItem createEmoteItem(String code, String info, Emoticon emote) {
        return new CompletionItem(code, info) {
            public ImageIcon getImage(Component c) {
                float scale = (float)(currentEmoteScaling / 100.0);
                ImageIcon icon = emote.getIcon(scale, 0, currentEmoteImageType, new Emoticon.EmoticonUser() {

                    @Override
                    public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
                        c.repaint();
                    }
                }).getImageIcon();
                return new ImageIcon(icon.getImage());
            }
        };
    }
    
    private static final Comparator<Emoticon> SORT_EMOTES_BY_NAME = new Comparator<Emoticon>() {

        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            return o1.code.compareToIgnoreCase(o2.code);
        }
    };

    private List<String> getCustomCompletionItems(String search) {
        String result = main.getCustomCompletionItem(search);
        List<String> list = new ArrayList<>();
        if (result != null) {
            list.add(result);
        }
        return list;
    }

    private AutoCompletionServer.CompletionItems getCompletionItemsEmoji(String search) {
        List<AutoCompletionServer.CompletionItem> result = new LinkedList<>();
        Collection<Emoticon> searchResult = new LinkedHashSet<>();
        findEmoji(searchResult, code -> code.startsWith(":" + search));
        if (searchResult.size() < 20) {
            findEmoji(searchResult, code -> code.contains("_" + search));
        }
        if (searchResult.size() < 20) {
            findEmoji(searchResult, code -> code.contains(search));
        }
        for (Emoticon emote : searchResult) {
            if (main.getSettings().getBoolean("emojiReplace")) {
                String alias = null;
                if (!emote.stringId.contains(search) && emote.stringIdAlias.contains(search)) {
                    alias = emote.stringIdAlias;
                }
                result.add(createEmoteItem(emote.stringId, alias, emote));
            } else {
                result.add(createEmoteItem(emote.code, null, emote));
            }
        }
        return new AutoCompletionServer.CompletionItems(result, ":");
    }
    
    private static final Comparator<Emoticon> EMOJI_SORTER = new Comparator<Emoticon>() {

        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            return o1.stringId.compareTo(o2.stringId);
        }
    };

    private void findEmoji(Collection<Emoticon> result, Function<String, Boolean> matcher) {
        // Find Emoji items
        List<Emoticon> searchResult = new LinkedList<>();
        for (Emoticon emote : main.emoticons.getEmoji()) {
            if (emote.stringId != null && matcher.apply(emote.stringId)) {
                searchResult.add(emote);
            } else if (emote.stringIdAlias != null && matcher.apply(emote.stringIdAlias)) {
                searchResult.add(emote);
            }
        }
        Collections.sort(searchResult, EMOJI_SORTER);
        result.addAll(searchResult);
    }

    //=================
    // General Purpose
    //=================
    /**
     *
     * @param data
     * @param search
     * @return
     */
    private List<String> filterCompletionItems(Collection<String> data,
            String search) {
        return filterCompletionItems(data, search, String.CASE_INSENSITIVE_ORDER,
                item -> {
                    return item;
                });
    }

    /**
     * Filter list of ready-to-use items based on the given search.
     *
     * @param data
     * @param search Should be all-lowercase
     * @return
     */
    private <T> List<T> filterCompletionItems(Collection<T> data,
            String search, Comparator<T> comparator,
            Function<T, String> getString) {
        List<T> containing = new ArrayList<>();
        List<T> matched = new ArrayList<>();
        Pattern cSearch = Pattern.compile(
                Pattern.quote(search.substring(0, 1).toUpperCase(Locale.ENGLISH))
                + "(?i)" + Pattern.quote(search.substring(1))
        );
        String searchMode = main.getSettings().getString("completionSearch");
        Set<String> added = new HashSet<>();
        for (T item : data) {
            String itemString = getString.apply(item);
            if (added.contains(itemString)) {
                continue;
            }
            String lc = StringUtil.toLowerCase(itemString);
            if (lc.startsWith(search)) {
                matched.add(item);
                added.add(itemString);
            } else if (searchMode.equals("words")
                    && !input.getCompleteToCommonPrefix()
                    && cSearch.matcher(itemString).find()) {
                containing.add(item);
                added.add(itemString);
            } else if (searchMode.equals("anywhere")
                    && lc.contains(search)) {
                containing.add(item);
                added.add(itemString);
            }
        }
        Collections.sort(matched, comparator);
        Collections.sort(containing, comparator);
        matched.addAll(containing);
        return matched;
    }
    
    private static void sortMixed(CompletionItems items, String search) {
        String emojiSearch = ":"+search;
        Collections.sort(items.items, new Comparator<CompletionItem>() {

            @Override
            public int compare(CompletionItem o1, CompletionItem o2) {
                String o1l = StringUtil.toLowerCase(o1.getCode());
                String o2l = StringUtil.toLowerCase(o2.getCode());
                boolean o1s = o1l.startsWith(search) || o1.getCode().startsWith(emojiSearch);
                boolean o2s = o2l.startsWith(search) || o2.getCode().startsWith(emojiSearch);
                if (o1s == o2s) {
                    return normalize(o1l).compareTo(normalize(o2l));
                }
                if (o1s) {
                    return -1;
                }
                return 1;
            }
            
            private String normalize(String input) {
                if (input.startsWith(":")) {
                    return input.substring(1);
                }
                return input;
            }
            
        });
    }

    //===========
    // Usernames
    //===========
    private final UserSorterNew userSorterNew = new UserSorterNew();
    private final UserSorterAlphabetic userSorterAlphabetical = new UserSorterAlphabetic();

    private AutoCompletionServer.CompletionItems getCompletionItemsNames(String search, boolean preferUsernames) {
        List<User> matchedUsers = new ArrayList<>();
        Set<User> regularMatched = new HashSet<>();
        Set<User> customMatched = new HashSet<>();
        Set<User> localizedMatched = new HashSet<>();
        for (User user : users.getData()) {
            matchUser(user, search, matchedUsers, regularMatched, localizedMatched, customMatched);
        }
        
        // Try to match current channel name if not matched yet
        if (channel.getRoom().hasStream()) {
            User channelUser = new User(channel.getStreamName(), Room.EMPTY);
            boolean channelUserMatched = false;
            for (User user : matchedUsers) {
                if (user.getName().equals(channelUser.getName())) {
                    channelUserMatched = true;
                }
            }
            if (!channelUserMatched) {
                matchUser(channelUser, search, matchedUsers, regularMatched, localizedMatched, customMatched);
            }
        }
        switch (main.getSettings().getString("completionSorting")) {
            case "predictive":
                Collections.sort(matchedUsers, userSorterNew);
                break;
            case "alphabetical":
                Collections.sort(matchedUsers, userSorterAlphabetical);
                break;
            default:
                Collections.sort(matchedUsers);
        }
        boolean includeAllNameTypes = main.getSettings().getBoolean("completionAllNameTypes");
        boolean includeAllNameTypesRestriction = main.getSettings().getBoolean("completionAllNameTypesRestriction");
        List<String> nicks = new ArrayList<>();
        Map<String, String> info = new HashMap<>();
        for (User user : matchedUsers) {
            if (includeAllNameTypes
                    && (!includeAllNameTypesRestriction || matchedUsers.size() <= 2)) {
                if (customMatched.contains(user) && !preferUsernames) {
                    nicks.add(user.getCustomNick());
                    if (!user.hasRegularDisplayNick()) {
                        nicks.add(user.getDisplayNick());
                    }
                    if (user.hasCustomNickSet() && !user.getCustomNick().equalsIgnoreCase(user.getRegularDisplayNick())) {
                        nicks.add(user.getRegularDisplayNick());
                    }
                } else if (localizedMatched.contains(user) && !preferUsernames) {
                    nicks.add(user.getDisplayNick());
                    if (user.hasCustomNickSet() && !user.getCustomNick().equalsIgnoreCase(user.getRegularDisplayNick())) {
                        nicks.add(user.getCustomNick());
                    }
                    nicks.add(user.getRegularDisplayNick());
                } else {
                    nicks.add(user.getRegularDisplayNick());
                    if (!user.hasRegularDisplayNick()) {
                        nicks.add(user.getDisplayNick());
                    }
                    if (user.hasCustomNickSet() && !user.getCustomNick().equalsIgnoreCase(user.getRegularDisplayNick())) {
                        nicks.add(user.getCustomNick());
                    }
                }
            } else {
                if (regularMatched.contains(user) || preferUsernames) {
                    nicks.add(user.getRegularDisplayNick());
                }
                if (localizedMatched.contains(user) && !preferUsernames) {
                    nicks.add(user.getDisplayNick());
                }
                if (customMatched.contains(user) && !preferUsernames) {
                    nicks.add(user.getCustomNick());
                }
            }

            if (!user.hasRegularDisplayNick()) {
                info.put(user.getDisplayNick(), user.getRegularDisplayNick());
                info.put(user.getRegularDisplayNick(), user.getDisplayNick());
            }
            if (user.hasCustomNickSet()) {
                info.put(user.getCustomNick(), user.getRegularDisplayNick());
                info.put(user.getRegularDisplayNick(), user.getCustomNick());
            }
        }
        List<CompletionItem> result = new ArrayList<>();
        for (String nick : nicks) {
            String nickInfo = info.get(nick);
            result.add(new CompletionItem(nick, nickInfo));
        }
        return new CompletionItems(result, "");
    }
    
    private static void matchUser(User user, String search,
            List<User> matchedUsers, Set<User> regularMatched,
            Set<User> localizedMatched, Set<User> customMatched) {
        boolean matched = false;
        if (user.getName().startsWith(search)) {
            matched = true;
            regularMatched.add(user);
        }
        if (!user.hasRegularDisplayNick() && StringUtil.toLowerCase(user.getDisplayNick()).startsWith(search)) {
            matched = true;
            localizedMatched.add(user);
        }
        if (user.hasCustomNickSet() && StringUtil.toLowerCase(user.getCustomNick()).startsWith(search)) {
            matched = true;
            customMatched.add(user);
        }

        if (matched) {
            matchedUsers.add(user);
        }
    }

    /**
     * Only auto-start if enabled and if it's either one of the fixed prefixes
     * or the value of "completionEmotePrefix". Also check that there is a space
     * in front (or nothing).
     * 
     * @param prefix The prefix is everything left to the cursor starting with
     * the first non-word character
     * @return 
     */
    @Override
    public boolean isAutostartPrefix(String prefix) {
        if (settings().getBoolean("completionAuto") && !prefix.isEmpty()) {
            boolean eligible = prefix.endsWith(":")
                    || prefix.endsWith("@")
                    || prefix.endsWith("@@")
                    || prefix.endsWith(settings().getString("completionEmotePrefix"));
            boolean beforeRequirement = prefix.length() == 1 || prefix.charAt(prefix.length() - 2) == ' ' || prefix.equals("@@");
            return eligible && beforeRequirement;
        }
        return false;
    }

    private class UserSorterNew implements Comparator<User> {

        @Override
        public int compare(User o1, User o2) {
            int s1 = o1.getActivityScore();
            int s2 = o2.getActivityScore();
            //System.out.println(o1+" "+s1+" "+o2+" "+s2);
            if (s1 == s2) {
                return o1.compareTo(o2);
            } else if (s1 > s2) {
                return -1;
            }
            return 1;
        }

    }

    private class UserSorterAlphabetic implements Comparator<User> {

        @Override
        public int compare(User o1, User o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }

    }

}
