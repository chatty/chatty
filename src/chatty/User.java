
package chatty;

import chatty.gui.Highlighter;
import chatty.gui.colors.UsercolorManager;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonManager;
import chatty.util.colors.HtmlColors;
import chatty.gui.NamedColor;
import chatty.gui.components.textpane.ModLogInfo;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import chatty.util.api.pubsub.LowTrustUserMessageData;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.irc.IrcBadges;
import chatty.util.irc.MsgTags;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a single user on a specific channel.
 * 
 * @author tduva
 */
public class User implements Comparable<User> {

    private static final NamedColor[] defaultColors = {
        new NamedColor("Red", 255, 0, 0),
        new NamedColor("Blue", 0, 0, 255),
        new NamedColor("Green", 0, 255, 0),
        new NamedColor("FireBrick", 178, 34, 34),
        new NamedColor("Coral", 255, 127, 80),
        new NamedColor("YellowGreen", 154, 205, 50),
        new NamedColor("OrangeRed", 255, 69, 0),
        new NamedColor("SeaGreen", 46, 139, 87),
        new NamedColor("GoldenRod", 218, 165, 32),
        new NamedColor("Chocolate", 210, 105, 30),
        new NamedColor("CadetBlue", 95, 158, 160),
        new NamedColor("DodgerBlue", 30, 144, 255),
        new NamedColor("HotPink", 255, 105, 180),
        new NamedColor("BlueViolet", 138, 43, 226),
        new NamedColor("SpringGreen", 0, 255, 127)
    };
    
    public static volatile int MSG_ID;
    
    private UserSettings userSettings = UserSettings.EMPTY;
    
    //========
    // Basics
    //========
    private long firstSeen = -1;
    private volatile String id;
    private Room room;
    
    //===========
    // User Name
    //===========
    /**
     * The account name (all-lowercase).
     */
    private volatile String nick;
    
    /**
     * The nick, could contain different case or symbols.
     */
    private String displayNick;
    private boolean hasDisplayNickSet;
    
    /**
     * Custom nick, not from Twitch.
     */
    private String customNick;
    
    /**
     * The nick, with mode symbols, could contain different case.
     */
    private String fullNick;
    
    /**
     * True if the displayNick only differs in case from the username.
     */
    private boolean hasRegularDisplayNick;
    
    //===========
    // Usericons
    //===========
    /**
     * Current badges id/version. Map gets replaced, not modified.
     */
    private IrcBadges twitchBadges;
    private short subMonths;
    
    //===========
    // Usercolor
    //===========
    private Color color = HtmlColors.decode("");
    private Color correctedColor = HtmlColors.decode("");
    private boolean hasDefaultColor = true;
    private boolean hasCorrectedColor;
    private boolean hasCustomColor;
    
    //========
    // Status
    //========
    private boolean localUser;
    private boolean online;
    private boolean isModerator;
    private boolean isBroadcaster;
    private boolean isAdmin;
    private boolean isStaff;
    private boolean hasTurbo;
    private boolean isSubscriber;
    private boolean isBot;
    private boolean isVip;
    
    //==========
    // Messages
    //==========
    private List<Message> lines;

    private int numberOfMessages;
    private int numberOfLines;
    
    // Used for auto-completion score
    private long lastHighlight = -1;

    public static void createTestUser(String name, String channel) {
        User testUser = new User(name, name, Room.createRegular(channel));
        testUser = new User(name, name, Room.createRegular(channel));
        testUser.setColor(new Color(94, 0, 211));
        testUser.setColor(new Color(255, 255, 255));
        testUser.setModerator(true);
        testUser.setSubscriber(true);

    }
    public User(String nick, Room room) {
        this(nick, null, null, room);
    }
    
    public User(String nick, String displayNick, Room room) {
        this(nick, null, displayNick, room);
    }
    
    public User(String nick, String capitalizedNick, String displayNick, Room room) {
        // This should return the same string if no lowercasing is necessary
        this.nick = StringUtil.toLowerCase(nick);
        
        // Display Nick
        this.displayNick = displayNick;
        if (this.displayNick == null) {
            this.displayNick = capitalizedNick != null ? capitalizedNick : nick;
        }
        this.hasDisplayNickSet = displayNick != null;
        checkForRegularDisplayNick();
        
        this.room = room;
        setDefaultColor();
        updateFullNick();
    }
    
    public synchronized void setUserSettings(UserSettings settings) {
        if (settings != null) {
            this.userSettings = settings;
        }
    }
    
    /**
     * Set new badges. The set Map will be used directly, so it should not be
     * modified afterwards.
     * 
     * @param badges Map of badges
     * @return true if the User was changed, false if the badges are the same as
     * before
     */
    public synchronized boolean setTwitchBadges(IrcBadges badges) {
        if (!Objects.equals(badges, this.twitchBadges)) {
            this.twitchBadges = badges;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    /**
     * Get the ordered Map of Twitch Badge this user has. The returned Map must
     * not be modified.
     * 
     * @return 
     */
    public synchronized IrcBadges getTwitchBadges() {
        return twitchBadges;
    }
    
    /**
     * Check if the User currently has a Twitch Badge with the given id.
     * 
     * @param id The badge id
     * @return true if the user has a badge with that id (any version), false
     * otherwise
     */
    public synchronized boolean hasTwitchBadge(String id) {
        return twitchBadges != null && twitchBadges.hasId(id);
    }
    
    public synchronized boolean hasTwitchBadge(String id, String version) {
        return twitchBadges != null && twitchBadges.hasIdVersion(id, version);
    }
    
    public List<Usericon> getBadges(boolean botBadgeEnabled, MsgTags tags, User localUser, int channelLogoSize) {
        IrcBadges badges = getTwitchBadges();
        if (userSettings.iconManager != null) {
            return userSettings.iconManager.getBadges(badges, this, localUser, botBadgeEnabled, tags, channelLogoSize);
        }
        return null;
    }
    
    public UsericonManager getUsericonManager() {
        return userSettings.iconManager;
    }
    
    public synchronized void setSubMonths(short months) {
        this.subMonths = months;
    }
    
    public synchronized short getSubMonths() {
        return subMonths;
    }
    
    /**
     * Gets the categories from the addressbook for this user.
     * 
     * @return The categories or <tt>null</tt> if this user could not be found or if no
     * addressbook was specified.
     */
    public Set<String> getCategories() {
        if (userSettings.addressbook != null) {
            AddressbookEntry entry = userSettings.addressbook.get(nick);
            if (entry != null) {
                return entry.getCategories();
            }
        }
        return null;
    }
    
    public List<String> getPresetCategories() {
        if (userSettings.addressbook != null) {
            return userSettings.addressbook.getCategories();
        }
        return null;
    }
    
    public boolean hasCategory(String category) {
        return hasCategory(category, nick);
    }
    
    public boolean hasCategory(String category, String name) {
        if (userSettings.addressbook != null) {
            AddressbookEntry entry = userSettings.addressbook.get(name);
            if (entry != null) {
                return entry.hasCategory(category);
            }
        }
        return false;
    }
    
    public Addressbook getAddressbook() {
        return userSettings.addressbook;
    }
    
    public synchronized String getChannel() {
        return room.getChannel();
    }
    
    public synchronized String getOwnerChannel() {
        return room.getOwnerChannel();
    }
    
    public synchronized String getStream() {
        return room.getStream();
    }
    
    public synchronized Room getRoom() {
        return room;
    }
    
    public synchronized void setRoom(Room room) {
        if (room != this.room && room.sameChannel(this.room)) {
            this.room = room;
        }
    }
    
    public synchronized long getFirstSeen() {
        return firstSeen;
    }
    
    public synchronized void setFirstSeen() {
        if (firstSeen == -1) {
            firstSeen = System.currentTimeMillis();
        }
    }
    
    public synchronized int getNumberOfMessages() {
        return numberOfMessages;
    }
    
    public synchronized int getNumberOfLines() {
        return numberOfLines;
    }
    
    public synchronized int getMaxNumberOfLines() {
        return userSettings.maxLines;
    }
    
    /**
     * Returns true if clearing old lines is responsible for not all lines being
     * present.
     * 
     * @return 
     */
    public synchronized boolean linesCleared() {
        int numLines = lines != null ? lines.size() : 0;
        return numLines < userSettings.maxLines && numLines < numberOfLines;
    }
    
    /**
     * Returns true if the max number of lines per user is responsible for not
     * all lines being present (or would be responsible, if lines had not been
     * cleared before).
     * 
     * @return 
     */
    public synchronized boolean maxLinesExceeded() {
        int numLines = lines != null ? lines.size() : 0;
        return numLines == userSettings.maxLines && numLines < numberOfLines;
    }
    
    public synchronized void addMessage(String line, boolean action, String id) {
        addMessage(line, action, id, System.currentTimeMillis());
    }
    
    /**
     * Adds a single chatmessage with the current time.
     * 
     * @param line 
     * @param action 
     * @param id 
     * @param timestamp 
     */
    public synchronized void addMessage(String line, boolean action, String id, long timestamp) {
        if (timestamp == -1) {
            timestamp = System.currentTimeMillis();
        }
        setFirstSeen();
        addLine(new TextMessage(timestamp, line, action, id, null));
        replayCachedLowTrust();
        numberOfMessages++;
    }
    
    /**
     * Adds a single ban with the current time.
     * 
     * @param duration
     * @param reason
     * @param id
     */
    public synchronized void addBan(long duration, String reason, String id) {
        addLine(new BanMessage(System.currentTimeMillis(), duration, reason, id, null));
        replayCachedBanInfo();
    }
    
    public synchronized void addUnban(int type, String by) {
        addLine(new UnbanMessage(System.currentTimeMillis(), type, by));
    }
    
    public synchronized void addMsgDeleted(String targetMsgId, String msg) {
        addLine(new MsgDeleted(System.currentTimeMillis(), targetMsgId, msg, null));
        replayCachedBanInfo();
    }
    
    public synchronized void addSub(String message, String text, String id) {
        setFirstSeen();
        addLine(new SubMessage(System.currentTimeMillis(), message, text, id));
    }
    
    public synchronized void addInfo(String message, String fullText) {
        setFirstSeen();
        addLine(new InfoMessage(System.currentTimeMillis(), message, fullText));
    }
    
    public synchronized void addModAction(ModeratorActionData data) {
        setFirstSeen();
        addLine(new ModAction(System.currentTimeMillis(), data.moderation_action+" "+ModLogInfo.makeArgsText(data)));
    }
    
    private List<ModeratorActionData> cachedBanInfo;
    
    /**
     * Add ban info (by/reason) for this user. Must be for this user.
     * 
     * @param data 
     */
    public synchronized void addBanInfo(ModeratorActionData data) {
        if (!addBanInfoNow(data)) {
            // Adding failed, cache and wait to see if it works later
            Debugging.println("modlog", "[UserModLogInfo] Caching: %s", data.getCommandAndParameters());
            if (cachedBanInfo == null) {
                cachedBanInfo = new ArrayList<>();
            }
            cachedBanInfo.add(data);
        }
    }
    
    private static final int BAN_INFO_WAIT = 1000;
    
    private synchronized void replayCachedBanInfo() {
        if (cachedBanInfo == null) {
            return;
        }
        Debugging.println("modlog", "[UserModLogInfo] Replaying: %s", cachedBanInfo);
        Iterator<ModeratorActionData> it = cachedBanInfo.iterator();
        while (it.hasNext()) {
            ModeratorActionData data = it.next();
            if (System.currentTimeMillis() - data.created_at > BAN_INFO_WAIT) {
                it.remove();
                Debugging.println("modlog", "[UserModLogInfo] Abandoned: %s", data);
            } else {
                if (addBanInfoNow(data)) {
                    it.remove();
                    Debugging.println("modlog", "[UserModLogInfo] Added: %s", data);
                }
            }
        }
        if (cachedBanInfo.isEmpty()) {
            cachedBanInfo = null;
        }
    }
    
    private synchronized boolean addBanInfoNow(ModeratorActionData data) {
        if (lines == null) {
            return false;
        }
        String command = ModLogInfo.makeCommand(data);
        for (int i=lines.size() - 1; i>=0; i--) {
            Message m = lines.get(i);
            // Too old, abort (associated message might not be here yet)
            if (System.currentTimeMillis() - m.getTime() > BAN_INFO_WAIT) {
                return false;
            }
            /**
             * Note: Only set info if not already set, as to not overwrite
             * existing one while waiting for next message to show up (which
             * could happen with close together bans). In this each ban message
             * has it's own line, so appending several by strings shouldn't be
             * necessary.
             */
            if (m instanceof BanMessage) {
                BanMessage bm = (BanMessage)m;
                if (bm.by == null && command.equals(Helper.makeBanCommand(this, bm.duration, bm.id))) {
                    lines.set(i, bm.addModLogInfo(data.created_by, ModLogInfo.getReason(data)));
                    return true;
                }
            } else if (m instanceof MsgDeleted) {
                MsgDeleted md = (MsgDeleted)m;
                if (md.by == null && command.equals(Helper.makeBanCommand(this, -2, md.targetMsgId))) {
                    lines.set(i, md.addModLogInfo(data.created_by));
                    return true;
                }
            }
        }
        return false;
    }
    
    private List<LowTrustUserMessageData> cachedLowTrust;
    
    /**
     * Add ban info (by/reason) for this user. Must be for this user.
     * 
     * @param data 
     */
    public synchronized void addLowTrust(LowTrustUserMessageData data) {
        if (!addLowTrustNow(data)) {
            // Adding failed, cache and wait to see if it works later
            if (cachedLowTrust == null) {
                cachedLowTrust = new ArrayList<>();
            }
            cachedLowTrust.add(data);
        }
    }
    
    private synchronized void replayCachedLowTrust() {
        if (cachedLowTrust == null) {
            return;
        }
        Iterator<LowTrustUserMessageData> it = cachedLowTrust.iterator();
        while (it.hasNext()) {
            LowTrustUserMessageData data = it.next();
            if (System.currentTimeMillis() - data.created_at > BAN_INFO_WAIT) {
                it.remove();
            } else {
                if (addLowTrustNow(data)) {
                    it.remove();
                }
            }
        }
        if (cachedLowTrust.isEmpty()) {
            cachedLowTrust = null;
        }
    }
    
    private synchronized boolean addLowTrustNow(LowTrustUserMessageData data) {
        if (lines == null) {
            return false;
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            Message m = lines.get(i);
            // Too old, abort (associated message might not be here yet)
            if (System.currentTimeMillis() - m.getTime() > BAN_INFO_WAIT) {
                return false;
            }
            if (m instanceof TextMessage) {
                TextMessage tm = (TextMessage) m;
                if (tm.id != null && tm.id.equals(data.aboutMessageId)) {
                    lines.set(i, tm.addLowTrust(data));
                    return true;
                }
            }
        }
        return false;
    }
    
    public synchronized void addAutoModMessage(String line, String id, String reason) {
        addLine(new AutoModMessage(line, id, reason));
    }
    
    /**
     * Adds a Message.
     * 
     * @param line The Message object containig the data for this line.
     */
    private void addLine(Message line) {
        if (lines == null) {
            lines = new ArrayList<>(1);
        }
        lines.add(line);
        if (lines.size() > userSettings.maxLines) {
            lines.remove(0);
        }
        numberOfLines++;
    }
    
    /**
     * Returns a copy of the current messages (defensive copying because it
     * might be used while being modified concurrently).
     * 
     * @return 
     */
    public synchronized List<Message> getMessages() {
        if (lines == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(lines);
    }
    
    public synchronized int getNumberOfSimilarChatMessages(String compareMsg, int method, long timeframe, float minSimilarity, int minLen, char[] ignoredChars) {
        if (lines == null) {
            return 0;
        }
        compareMsg = StringUtil.prepareForSimilarityComparison(compareMsg, ignoredChars);
        int result = 0;
        long checkUntilTime = System.currentTimeMillis() - timeframe * 1000;
        for (int i=lines.size() - 1; i>=0; i--) {
            Message m = lines.get(i);
            if (m instanceof TextMessage) {
                TextMessage msg = (TextMessage)m;
                if (msg.getTime() < checkUntilTime) {
                    break;
                }
                if (msg.text.length() >= minLen) {
                    String text = StringUtil.prepareForSimilarityComparison(msg.text, ignoredChars);
                    if (StringUtil.checkSimilarity(compareMsg, text, minSimilarity, method) > 0) {
                        result++;
                    }
                }
            }
        }
        return result;
    }
    
    public synchronized int getMatchingMessages(Highlighter.HighlightItem item, int num, long time, boolean beforeTime) {
        if (lines == null) {
            return 0;
        }
        int result = 0;
        int numMsgs = 0;
        for (int i=lines.size() - 1; i>=0; i--) {
            Message m = lines.get(i);
            if (beforeTime) {
                if (m.time > time) {
                    continue;
                }
            }
            else {
                if (m.time < time) {
                    return result;
                }
            }
            if (m instanceof TextMessage) {
                TextMessage tm = (TextMessage) m;
                if (item.matchesTextOnly(tm.text, null)) {
                    result++;
                }
                numMsgs++;
                if (numMsgs == num) {
                    return result;
                }
            }
        }
        return result;
    }
    
    public synchronized int getNumberOfMessagesAfterBan() {
        if (lines == null) {
            return -1;
        }
        int msgsAfterBan = 0;
        for (int i=lines.size() - 1; i>=0; i--) {
            Message m = lines.get(i);
            if (m instanceof TextMessage) {
                msgsAfterBan++;
            }
            else if (m instanceof BanMessage) {
                return msgsAfterBan;
            }
        }
        return -1;
    }
    
    public synchronized TextMessage getMessage(String msgId) {
        if (msgId == null) {
            return null;
        }
        if (lines == null) {
            return null;
        }
        for (Message msg : lines) {
            if (msg instanceof TextMessage) {
                TextMessage textMsg = (TextMessage)msg;
                if (msgId.equals(textMsg.id)) {
                    return textMsg;
                }
            }
        }
        return null;
    }
    
    public synchronized SubMessage getSubMessage(String msgId) {
        if (msgId == null) {
            return null;
        }
        if (lines == null) {
            return null;
        }
        for (Message msg : lines) {
            if (msg instanceof SubMessage) {
                SubMessage textMsg = (SubMessage)msg;
                if (msgId.equals(textMsg.id)) {
                    return textMsg;
                }
            }
        }
        return null;
    }
    
    public String getMessageText(String msgId) {
        TextMessage msg = getMessage(msgId);
        if (msg != null) {
            return msg.text;
        }
        SubMessage subMsg = getSubMessage(msgId);
        if (subMsg != null) {
            return subMsg.attached_message;
        }
        return null;
    }
    
    public synchronized AutoModMessage getAutoModMessage(String msgId) {
        if (msgId == null) {
            return null;
        }
        if (lines == null) {
            return null;
        }
        for (Message msg : lines) {
            if (msg instanceof AutoModMessage) {
                AutoModMessage autoModMsg = (AutoModMessage) msg;
                if (msgId.equals(autoModMsg.id)) {
                    return autoModMsg;
                }
            }
        }
        return null;
    }
    
    public String getAutoModMessageText(String msgId) {
        AutoModMessage msg = getAutoModMessage(msgId);
        return msg != null ? msg.message : null;
    }
    
    public synchronized int clearLinesIfInactive(long duration) {
        if (lines != null
                && !lines.isEmpty()
                && System.currentTimeMillis() - getLastLineTime() >= duration) {
            int size = lines.size();
            lines = null;
            return size;
        }
        return 0;
    }
    
    public synchronized void clearLines() {
        lines = null;
        numberOfMessages = 0;
        numberOfLines = 0;
    }
    
    public synchronized void clearNumberOfMessages() {
        numberOfMessages = 0;
    }
    
    private long getLastLineTime() {
        if (lines != null && !lines.isEmpty()) {
            return lines.get(lines.size() - 1).time;
        }
        return -1;
    }
    
    public synchronized String getName() {
        return nick;
    }
    
    public synchronized String getId() {
        return id;
    }
    
    public synchronized void setId(String id) {
        if (id != null) {
            this.id = id;
        }
    }
    
    public synchronized String getDisplayNick() {
        return displayNick;
    }
    
    /**
     * Gets the display nick if it is equal to the username except for case, or
     * the username.
     * 
     * @return 
     */
    public synchronized String getRegularDisplayNick() {
        if (hasRegularDisplayNick) {
            return displayNick;
        }
        return nick;
    }
    
    /**
     * Whether this user has a display nick set that only differs from the
     * username by case.
     *
     * @return
     */
    public synchronized boolean hasRegularDisplayNick() {
        return hasRegularDisplayNick;
    }
    
    public synchronized boolean setDisplayNick(String newDisplayNick) {
        if (newDisplayNick == null || newDisplayNick.isEmpty()) {
            return false;
        }
        hasDisplayNickSet = true;
        if (displayNick != null && displayNick.equals(newDisplayNick)) {
            return false;
        }
        this.displayNick = newDisplayNick;
        updateFullNick();
        checkForRegularDisplayNick();
        return true;
    }
    
    private void checkForRegularDisplayNick() {
        hasRegularDisplayNick = displayNick != null && displayNick.equalsIgnoreCase(nick);
    }
    
    /**
     * Whether a display nick has been set. {@link getDisplayNick()} will still
     * return a default value even if this is false.
     * 
     * @return 
     */
    public synchronized boolean hasDisplayNickSet() {
        return hasDisplayNickSet;
    }
    
    /**
     * Gets the Custom Nick of this user, or the display nick of no custom nick
     * is set.
     * 
     * @return 
     */
    public synchronized String getCustomNick() {
        if (customNick != null) {
            return customNick;
        }
        return getDisplayNick();
    }
    
    public synchronized void setCustomNick(String nick) {
        this.customNick = nick;
        updateFullNick();
    }
    
    public synchronized boolean hasCustomNickSet() {
        return customNick != null;
    }
    
    /**
     * If a custom color is defined for this nick, then this returns the custom
     * color. Otherwise it returns the color received from Twitch Chat, or the
     * default color if none was received yet.
     * 
     * This does not return the corrected version of the color.
     * 
     * @return 
     */
    public synchronized Color getColor() {
        if (userSettings.colorManager != null) {
            /**
             * Should be fine to call within synchronized, if only called
             * through here.
             */
            Color result = userSettings.colorManager.getColor(this);
            if (result != null) {
                hasCustomColor = true;
                return result;
            } else {
                hasCustomColor = false;
            }
        }
        return color;
    }
    
    /**
     * If a custom color is defined for this user, then this returns the custom
     * color, otherwise it returns the original Twitch Chat color, possibly in
     * a corrected version (if it was corrected).
     * 
     * Custom colors are not corrected.
     * 
     * @return 
     */
    public synchronized Color getDisplayColor() {
        Color color = getColor();
        if (!hasCustomColor() && hasCorrectedColor()) {
            return correctedColor;
        }
        return color;
    }
    
    /**
     * Only return custom or corrected color.
     * 
     * @return The color, or null if no custom or corrected color is set
     */
    public synchronized Color getDisplayColor2() {
        Color color = getColor();
        if (hasCustomColor) {
            return color;
        }
        if (hasCorrectedColor) {
            return correctedColor;
        }
        return null;
    }
    
    /**
     * Returns the original Twitch Chat color, either the color received from
     * Twitch Chat, or the default color if none was received yet.
     * 
     * @return 
     */
    public synchronized Color getPlainColor() {
        return color;
    }
    
    /**
     * Whether this user has a custom color defined. The returned value is only
     * updated by calling getColor().
     * 
     * @return 
     */
    public synchronized boolean hasCustomColor() {
        return hasCustomColor;
    }
    
    /**
     * Returns the corrected color for this nick. This is the original Twitch
     * Chat color (either received from chat or default), which was corrected
     * for better readability against the current background. It may be the same
     * as the original color.
     * 
     * @return The corrected color or null if none was set
     */
    public synchronized Color getCorrectedColor() {
        return correctedColor;
    }
    
    /**
     * This merely means that the color has gone through color correction, it
     * may not be different from the original color.
     *
     * @return 
     */
    public synchronized boolean hasCorrectedColor() {
        return hasCorrectedColor;
    }
    
    public synchronized void setColor(Color color) {
        hasDefaultColor = false;
        this.color = color;
    }
    
    public synchronized void setColor(String htmlColor) {
        setColor(HtmlColors.decode(htmlColor));
    }
    
    public synchronized void setCorrectedColor(Color color) {
        correctedColor = color;
        hasCorrectedColor = true;
    }
    
    /**
     * Whether a Color has been set explicitely.
     * 
     * @return 
     */
    public synchronized boolean hasDefaultColor() {
        return hasDefaultColor;
    }
    
    /**
     * Sets the default color based on the nick. Based on what bGeorge posted.
     */
    private void setDefaultColor() {
        // Shouldn't happen expect for the stupid hack to get a User for submessage usericons
        if (nick.isEmpty()) {
            return;
        }
        String name = StringUtil.toLowerCase(nick);
        int n = name.codePointAt(0) + name.codePointAt(name.length() - 1);
        color = defaultColors[n % defaultColors.length];
        hasDefaultColor = true;
    }
    
    public synchronized boolean setOnline(boolean online) {
        if (online != this.online) {
            this.online = online;
            return true;
        }
        return false;
    }
    
    public synchronized boolean isOnline() {
        return online;
    }

    @Override
    public synchronized int compareTo(User u) {
        int broadcaster = 16;
        int admin = 8;
        int moderator = 4;
        int vip = 2;
        int subscriber = 1;
        
        int result = 0;
        if (this.isAdmin() || this.isStaff()) {
            result = result - admin;
        }
        if (u.isAdmin() || u.isStaff()) {
            result = result + admin;
        }
        if (this.isBroadcaster()) {
            result = result - broadcaster;
        }
        if (u.isBroadcaster()) {
            result = result + broadcaster;
        }
        if (this.isSubscriber()) {
            result = result - subscriber;
        }
        if (u.isSubscriber()) {
            result = result + subscriber;
        }
        if (this.isModerator()) {
            result = result - moderator;
        }
        if (u.isModerator()) {
            result = result + moderator;
        }
        if (this.isVip()) {
            result = result - vip;
        }
        if (u.isVip()) {
            result = result + vip;
        }
        if (result == 0) {
            return this.nick.compareTo(u.nick);
        }
        return result;
    }
    
    @Override
    public synchronized String toString() {
        return fullNick;
    }
    
    public synchronized String getFullNick() {
        return fullNick;
    }
    
    public synchronized void setMode(String mode) {
        if (mode.equals("o")) {
            setModerator(true);
        } else {
            setModerator(false);
        }
    }
    
    public synchronized boolean isLocalUser() {
        return localUser;
    }
    
    public synchronized boolean sameUser(User user) {
        return user != null && user.getChannel().equals(getChannel()) && user.getName().equals(nick);
    }
    
    /**
     * Returns true if this user has channel moderator rights, which includes
     * either being a Moderator or the Broadcaster.
     *
     * @return true if this user is a moderator or the broadcaster, false
     * otherwise
     */
    public synchronized boolean hasChannelModeratorRights() {
        return isModerator() || isBroadcaster();
    }
    
    /**
     * Returns true if this user has any kind of moderator rights, this includes
     * Moderator, Broadcaster, Global Mod, Admin and Staff.
     *
     * @return true if this user has moderator powers, false otherwise
     */
    public synchronized boolean hasModeratorRights() {
        return isAdmin() || isBroadcaster() || isGlobalMod() || isModerator()
                || isStaff();
    }

    /**
     * Returns true if this user is a channel moderator. This may not apply to
     * the broadcaster.
     * 
     * @return true if this user is a moderator, false otherwise
     */
    public synchronized boolean isModerator() {
        return isModerator;
    }
    
    /**
     * Always returns false, since global moderators have been removed from
     * Twitch. Just keeping this for now since it's used in some places.
     * 
     * @return false
     */
    public synchronized boolean isGlobalMod() {
        return false;
    }
    
    public synchronized boolean isAdmin() {
        return isAdmin;
    }
    
    public synchronized boolean isStaff() {
        return isStaff;
    }
    
    public synchronized boolean isBroadcaster() {
        return isBroadcaster;
    }
    
    public synchronized boolean isSubscriber() {
        return isSubscriber;
    }
    
    public synchronized boolean hasTurbo() {
        return hasTurbo;
    }
    
    public synchronized boolean isBot() {
        return isBot;
    }
    
    public synchronized boolean isVip() {
        return isVip;
    }
    
    public synchronized boolean setLocalUser(boolean localUser) {
        if (this.localUser != localUser) {
            this.localUser = localUser;
            return true;
        }
        return false;
    }
    
    public synchronized boolean setModerator(boolean mod) {
        if (isModerator != mod) {
            isModerator = mod;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setAdmin(boolean admin) {
        if (isAdmin != admin) {
            isAdmin = admin;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setStaff(boolean staff) {
        if (isStaff != staff) {
            isStaff = staff;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setTurbo(boolean turbo) {
        if (hasTurbo != turbo) {
            hasTurbo = turbo;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setSubscriber(boolean subscriber) {
        if (isSubscriber != subscriber) {
            isSubscriber = subscriber;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized void setBroadcaster(boolean broadcaster) {
        isBroadcaster = broadcaster;
        updateFullNick();
    }
    
    public synchronized boolean setBot(boolean bot) {
        if (isBot != bot) {
            isBot = bot;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setVip(boolean vip) {
        if (isVip != vip) {
            isVip = vip;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    private void updateFullNick() {
        fullNick = getModeSymbol() + getCustomNick();
        // Reuse existing String if possible
        if (fullNick.equals(displayNick)) {
            fullNick = displayNick;
        }
    }
    
    public synchronized String getModeSymbol() {
        String result = "";
        if (isVip()) {
            result += "!";
        }
        if (isSubscriber()) {
            result += "%";
        }
        if (hasTwitchBadge("bits")) {
            result += "$";
        }
        if (hasTurbo()) {
            result += "+";
        }
        if (isBot()) {
            result += "^";
        }
        if (isBroadcaster()) {
            return "~"+result;
        }
        if (isStaff() || isAdmin()) {
            return "&"+result;
        }
        if (isModerator()) {
            return "@"+result;
        }
        return result;
    }
    
    public synchronized int getActivityScore() {
        int score = 0;
        if (lastHighlight != -1) {
            score += makeScore(lastHighlight)*0.5;
        }
        long lastLine = getLastLineTime();
        if (lastLine != -1) {
            score += makeScore(lastLine);
        }
        return score;
    }
    
    private int makeScore(long time) {
        int ago = (int)(System.currentTimeMillis() - time) / 1000;
        int result = 1000 - ago / 120;
        return result < 0 ? 0 : result;
    }
    
    public synchronized void setHighlighted() {
        lastHighlight = System.currentTimeMillis();
    }
    
    public static class Message {
        
        private final long time;
        
        public Message(long time) {
            this.time = time;
        }
        
        public long getTime() {
            return time;
        }
    }
    
    public static class TextMessage extends Message {
        public final String text;
        public final boolean action;
        public final String id;
        public final LowTrustUserMessageData lowTrust;
        
        public TextMessage(long time, String message, boolean action, String id, LowTrustUserMessageData lowTrust) {
            super(time);
            this.text = message;
            this.action = action;
            this.id = id;
            this.lowTrust = lowTrust;
        }
        
        public String getText() {
            return text;
        }
        
        public boolean isAction() {
            return action;
        }
        
        public TextMessage addLowTrust(LowTrustUserMessageData data) {
            return new TextMessage(getTime(), text, action, id, data);
        }
        
    }
    
    public static class BanMessage extends Message {
        
        public final long duration;
        public final String reason;
        public final String id;
        public final String by;
        
        public BanMessage(long time, long duration, String reason, String id,
                String by) {
            super(time);
            this.duration = duration;
            this.reason = reason;
            this.id = id;
            this.by = by;
        }
        
        public BanMessage addModLogInfo(String by, String reason) {
            if (reason == null) {
                // Probably not set anyway, but just in case
                reason = this.reason;
            }
            return new BanMessage(getTime(), duration, reason, id, by);
        }
        
    }
    
    public static class UnbanMessage extends Message {
        
        public static final int TYPE_UNKNOWN = -1;
        public static final int TYPE_UNBAN = 0;
        public static final int TYPE_UNTIMEOUT = 1;
        
        public final int type;
        public final String by;
        
        public UnbanMessage(long time, int type, String by) {
            super(time);
            this.type = type;
            this.by = by;
        }
        
        public static int getType(String modAction) {
            switch (modAction) {
                case "unban": return TYPE_UNBAN;
                case "untimeout": return TYPE_UNTIMEOUT;
            }
            return TYPE_UNKNOWN;
        }
        
    }
    
    public static class MsgDeleted extends Message {
        
        public final String targetMsgId;
        public final String msg;
        public final String by;
        
        public MsgDeleted(long time, String targetMsgId, String msg, String by) {
            super(time);
            this.targetMsgId = targetMsgId;
            this.msg = msg;
            this.by = by;
        }
        
        public MsgDeleted addModLogInfo(String by) {
            return new MsgDeleted(getTime(), targetMsgId, msg, by);
        }
    }
    
    public static class SubMessage extends Message {
        
        public final String attached_message;
        public final String system_msg;
        public final String id;
        
        public SubMessage(long time, String message, String text, String id) {
            super(time);
            this.attached_message = message;
            this.system_msg = text;
            this.id = id;
        }
    }
    
    public static class InfoMessage extends Message {
        
        /**
         * The attached message (if any), currently not used.
         */
        public final String attached_message;
        
        /**
         * The full text of the info message (including type and attached
         * message). This full text is formatted by e.g. the UserNotice and may
         * work better for stuff like announcements.
         */
        public final String full_text;
        
        public InfoMessage(long time, String message, String full_text) {
            super(time);
            this.attached_message = message;
            this.full_text = full_text;
        }
    }
    
    public static class ModAction extends Message {

        /**
         * For display, may be formatted differently depending on the command.
         */
        public final String commandAndParameters;
        
        public ModAction(long time, String commandAndParameters) {
            super(time);
            this.commandAndParameters = commandAndParameters;
        }
        
    }
    
    public static class AutoModMessage extends Message {
        
        public final String message;
        public final String id;
        public final String reason;
        
        public AutoModMessage(String message, String id, String reason) {
            super(System.currentTimeMillis());
            this.message = message;
            this.id = id;
            this.reason = reason;
        }
        
    }
    
//    public static final void main(String[] args) {
//        ArrayList<User> list = new ArrayList<>();
//        for (int i=0;i<100000;i++) {
//            list.add(new User("nick"+i, ""));
//        }
//        try {
//            Thread.sleep(60000);
//        } catch (InterruptedException ex) {
//            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
    /**
     * Packs all the references to managers and settings a User needs access to
     * into one class, of which one instance can be used for most users, which
     * reduces memory usage.
     * 
     * An alternative might be to make all of this static values on the User
     * class, but that seems a bit messy, especially for testing.
     */
    public static class UserSettings {
        
        public static final UserSettings EMPTY = new UserSettings(100, null, null, null);
        
        private final int maxLines;
        private final UsercolorManager colorManager;
        private final Addressbook addressbook;
        private final UsericonManager iconManager;
        
        public UserSettings(int maxLines, UsercolorManager colorManager,
                            Addressbook addressbook, UsericonManager iconManager) {
            if (maxLines < 0) {
                maxLines = 100;
            }
            this.maxLines = maxLines;
            this.colorManager = colorManager;
            this.addressbook = addressbook;
            this.iconManager = iconManager;
        }
        
    }
    
}
