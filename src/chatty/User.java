
package chatty;

import chatty.gui.colors.UsercolorManager;
import chatty.util.api.usericons.Usericon;
import chatty.util.api.usericons.UsericonManager;
import chatty.util.colors.HtmlColors;
import chatty.gui.NamedColor;
import chatty.gui.components.textpane.ModLogInfo;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import chatty.util.api.pubsub.ModeratorActionData;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;

/**
 * Represents a single user on a specific channel.
 * 
 * @author tduva
 */
public class User implements Comparable {
    
    private static final Pattern SPLIT_EMOTESET = Pattern.compile("[^0-9]");
    
    private static final Set<Integer> EMPTY_EMOTESETS = new HashSet<>();
    
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
    
    private static final int MAXLINES = 100;
    
    private final Set<Integer> emoteSets = new HashSet<>();
    private final List<Message> messages = new ArrayList<>();
    
    /**
     * The nick, all-lowercase.
     */
    private volatile String nick;
    
    private volatile String id;
    
    /**
     * The nick, could contain different case.
     */
    private String displayNick;
    
    private String customNick;
    
    /**
     * The nick, with mode symbols, could contain different case.
     */
    private String fullNick;
    private boolean hasDisplayNickSet;
    
    /**
     * True if the displayNick only differs in case from the username.
     */
    private boolean hasRegularDisplayNick;
    private Room room;
    
    private volatile Addressbook addressbook;
    private volatile UsericonManager iconManager;
    
    private UsercolorManager colorManager;
    private Color color = HtmlColors.decode("");
    private Color correctedColor = HtmlColors.decode("");
    private boolean hasDefaultColor = true;
    private boolean hasCorrectedColor;
    private boolean hasCustomColor;
    
    private boolean online;
    private boolean isModerator;
    private boolean isGlobalMod;
    private boolean isBroadcaster;
    private boolean isAdmin;
    private boolean isStaff;
    private boolean hasTurbo;
    private boolean isSubscriber;
    private boolean isBot;
    
    private volatile Map<String, String> twitchBadges;

    private final long createdAt = System.currentTimeMillis();
    private int numberOfMessages;
    private int numberOfLines;
    
    // Used for auto-completion score
    private long lastMessage = -1;
    private long lastHighlight = -1;
    
    public User(String nick, Room room) {
        this(nick, null, room);
    }
    
    public User(String nick, String displayNick, Room room) {
        this.nick = StringUtil.toLowerCase(nick);
        this.displayNick = displayNick == null ? nick : displayNick;
        this.hasDisplayNickSet = displayNick != null;
        checkForRegularDisplayNick();
        this.room = room;
        setDefaultColor();
        updateFullNick();
    }
    
    public void setUsercolorManager(UsercolorManager manager) {
        this.colorManager = manager;
    }
    
    public void setUsericonManager(UsericonManager manager) {
        this.iconManager = manager;
    }
    
    public UsericonManager getUsericonManager() {
        return iconManager;
    }
    
    public Usericon getIcon(Usericon.Type type) {
        if (iconManager != null) {
            return iconManager.getIcon(type, null, null, this);
        }
        return null;
    }
    
    public boolean setTwitchBadges(Map<String, String> badges) {
        if (!Objects.equals(badges, this.twitchBadges)) {
            this.twitchBadges = badges;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    /**
     * Get the ordered Map of Twitch Badge this user has. The Map must not be
     * modified.
     * 
     * @return 
     */
    public Map<String, String> getTwitchBadges() {
        return twitchBadges;
    }
    
    /**
     * Check if the User currently has a Twitch Badge with the given id.
     * 
     * @param id The badge id
     * @return true if the user has a badge with that id (any version), false
     * otherwise
     */
    public boolean hasTwitchBadge(String id) {
        return twitchBadges != null && twitchBadges.containsKey(id);
    }
    
    public List<Usericon> getBadges(boolean botBadgeEnabled) {
        if (iconManager != null) {
            return iconManager.getBadges(twitchBadges, this, botBadgeEnabled);
        }
        return null;
    }
    
    public void setAddressbook(Addressbook addressbook) {
        this.addressbook = addressbook;
    }
    
    /**
     * Gets the categories from the addressbook for this user.
     * 
     * @return The categories or <tt>null</tt> if this user could not be found or if no
     * addressbook was specified.
     */
    public Set<String> getCategories() {
        if (addressbook != null) {
            AddressbookEntry entry = addressbook.get(nick);
            if (entry != null) {
                return entry.getCategories();
            }
        }
        return null;
    }
    
    public List<String> getPresetCategories() {
        if (addressbook != null) {
            return addressbook.getCategories();
        }
        return null;
    }
    
    public boolean hasCategory(String category) {
        return hasCategory(category, nick);
    }
    
    public boolean hasCategory(String category, String name) {
        if (addressbook != null) {
            AddressbookEntry entry = addressbook.get(name);
            if (entry != null) {
                return entry.hasCategory(category);
            }
        }
        return false;
    }
    
    public Addressbook getAddressbook() {
        return addressbook;
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
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public int getNumberOfMessages() {
        return numberOfMessages;
    }
    
    public int getMaxNumberOfLines() {
        return MAXLINES;
    }
    
    public boolean maxNumberOfLinesReached() {
        return numberOfLines > MAXLINES;
    }
    
    /**
     * Adds a single chatmessage with the current time.
     * 
     * @param line 
     * @param action 
     * @param id 
     */
    public synchronized void addMessage(String line, boolean action, String id) {
        addLine(new TextMessage(System.currentTimeMillis(), line, action, id));
        numberOfMessages++;
        lastMessage = System.currentTimeMillis();
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
    
    public synchronized void addMsgDeleted(String targetMsgId, String msg) {
        addLine(new MsgDeleted(System.currentTimeMillis(), targetMsgId, msg, null));
        replayCachedBanInfo();
    }
    
    public synchronized void addSub(String message, String text) {
        addLine(new SubMessage(System.currentTimeMillis(), message, text));
    }
    
    public synchronized void addInfo(String message, String text) {
        addLine(new InfoMessage(System.currentTimeMillis(), message, text));
    }
    
    public synchronized void addModAction(ModeratorActionData data) {
        addLine(new ModAction(System.currentTimeMillis(), data.getCommandAndParameters()));
    }
    
    private final List<ModeratorActionData> cachedBanInfo = new ArrayList<>();
    
    /**
     * Add ban info (by/reason) for this user. Must be for this user.
     * 
     * @param data 
     */
    public synchronized void addBanInfo(ModeratorActionData data) {
        if (!addBanInfoNow(data)) {
            // Adding failed, cache and wait to see if it works later
            Debugging.println("modlog", "[UserModLogInfo] Caching: %s", data.getCommandAndParameters());
            cachedBanInfo.add(data);
        }
    }
    
    private static final int BAN_INFO_WAIT = 500;
    
    private synchronized void replayCachedBanInfo() {
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
    }
    
    private synchronized boolean addBanInfoNow(ModeratorActionData data) {
        String command = ModLogInfo.makeCommand(data);
        for (int i=messages.size() - 1; i>=0; i--) {
            Message m = messages.get(i);
            // Too old, abort (associated message might not be here yet)
            if (System.currentTimeMillis() - m.getTime() > BAN_INFO_WAIT) {
                return false;
            }
            if (m instanceof BanMessage) {
                BanMessage bm = (BanMessage)m;
                if (command.equals(Helper.makeBanCommand(this, bm.duration, bm.id))) {
                    messages.set(i, bm.addModLogInfo(data.created_by, ModLogInfo.getReason(data)));
                    return true;
                }
            } else if (m instanceof MsgDeleted) {
                MsgDeleted dm = (MsgDeleted)m;
                if (command.equals(Helper.makeBanCommand(this, -2, dm.targetMsgId))) {
                    messages.set(i, dm.addModLogInfo(data.created_by));
                    return true;
                }
            }
        }
        return false;
    }
    
    public synchronized void addAutoModMessage(String line, String id) {
        addLine(new AutoModMessage(line, id));
    }
    
    /**
     * Adds a Message.
     * 
     * @param message The Message object containig the data for this line.
     */
    private void addLine(Message message) {
        messages.add(message);
        if (messages.size() > MAXLINES) {
            messages.remove(0);
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
        return new ArrayList<>(messages);
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
        if (colorManager != null) {
            Color result = colorManager.getColor(this);
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
    public synchronized int compareTo(Object o) {
        if (!(o instanceof User)) {
            return 0;
        }
        User u = (User)o;
        
        int broadcaster = 16;
        int admin = 8;
        int globalmod = 4;
        int moderator = 2;
        int subscriber = 1;
        
        int result = 0;
        if (this.isAdmin() || this.isStaff()) {
            result = result - admin;
        }
        if (u.isAdmin() || u.isStaff()) {
            result = result + admin;
        }
        if (this.isGlobalMod()) {
            result = result - globalmod;
        }
        if (u.isGlobalMod()) {
            result = result + globalmod;
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
    
    public synchronized boolean isGlobalMod() {
        return isGlobalMod;
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
    
    public synchronized boolean setModerator(boolean mod) {
        if (isModerator != mod) {
            isModerator = mod;
            updateFullNick();
            return true;
        }
        return false;
    }
    
    public synchronized boolean setGlobalMod(boolean globalMod) {
        if (globalMod != isGlobalMod) {
            isGlobalMod = globalMod;
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
    
    private void updateFullNick() {
        fullNick = getModeSymbol()+getCustomNick();
    }
    
    public synchronized String getModeSymbol() {
        String result = "";
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
        if (isAdmin()) {
            return "!"+result;
        }
        if (isStaff()) {
            return "&"+result;
        }
        if (isGlobalMod()) {
            return "*"+result;
        }
        if (isModerator()) {
            return "@"+result;
        }
        return result;
    }
    
    /**
     * Sets the set of emoticons available for this user.
     * 
     * Splits at any character that is not a number, but usually it should
     * be a string like: [1,5,39]
     * 
     * @param newEmoteSets 
     */
    public synchronized void setEmoteSets(String newEmoteSets) {
        //String[] split = newEmoteSets.split("[^0-9]");
        emoteSets.clear();
        if (newEmoteSets == null) {
            return;
        }
        String[] split = SPLIT_EMOTESET.split(newEmoteSets);
        for (String emoteSet : split) {
            if (!emoteSet.isEmpty()) {
                try {
                    emoteSets.add(Integer.parseInt(emoteSet));
                } catch (NumberFormatException ex) {
                    // Do nothing, invalid emoteset, just don't add it
                }
            }
        }
    }
    
    public synchronized void setEmoteSets(Set<Integer> emoteSets) {
        this.emoteSets.clear();
        this.emoteSets.addAll(emoteSets);
    }
    
    /**
     * Gets a Set of Integer containing the emotesets available to this user.
     * Defensive copying because it might be iterated over while being modified
     * concurrently.
     * 
     * @return 
     */
    public synchronized Set<Integer> getEmoteSet() {
        if (emoteSets.isEmpty()) {
            return EMPTY_EMOTESETS;
        }
        return new HashSet<>(emoteSets);
    }
    
    public synchronized int getActivityScore() {
        int score = 0;
        if (lastHighlight != -1) {
            score += makeScore(lastHighlight)*0.5;
//            int secondsAgo = getLastHighlightAgo();
//            return secondsAgo / 60;
//            if (System.currentTimeMillis() - lastHighlight < 300*1000) {
//                return 2;
//            } else {
//                lastHighlight = -1;
//            }
        }
        if (lastMessage != -1) {
            score += makeScore(lastMessage);
        }
//        if (lastMessage != -1) {
//            int secondsAgo = (int)(lastMessage) / 1000;
//            return secondsAgo / 60;
//        }
//        if (lastMessage != -1 && System.currentTimeMillis() - lastMessage < 300*1000) {
//            return 1;
//        }
        return score;
    }
    
    private int makeScore(long time) {
        int ago = (int)(System.currentTimeMillis() - time) / 1000;
        int result = 1000 - ago / 120;
        return result < 0 ? 0 : result;
    }
    
    private int getLastHighlightAgo() {
        return (int)(System.currentTimeMillis() - lastHighlight) / 1000;
    }
    
    public synchronized void setHighlighted() {
        lastHighlight = System.currentTimeMillis();
    }
    
    public static class Message {
        
        public static final int MESSAGE = 0;
        public static final int BAN = 1;
        public static final int SUB = 2;
        public static final int MOD_ACTION = 3;
        public static final int AUTO_MOD_MESSAGE = 4;
        public static final int INFO = 5;
        public static final int MSG_DELETED = 6;
        
        private final Long time;
        private final int type;
        
        public Message(int type, Long time) {
            this.time = time;
            this.type = type;
        }
        
        public int getType() {
            return type;
        }
        
        public long getTime() {
            return time;
        }
    }
    
    public static class TextMessage extends Message {
        public final String text;
        public final boolean action;
        public final String id;
        
        public TextMessage(Long time, String message, boolean action, String id) {
            super(MESSAGE, time);
            this.text = message;
            this.action = action;
            this.id = id;
        }
        
        public String getText() {
            return text;
        }
        
        public boolean isAction() {
            return action;
        }
    }
    
    public static class BanMessage extends Message {
        
        public final long duration;
        public final String reason;
        public final String id;
        public final String by;
        
        public BanMessage(Long time, long duration, String reason, String id,
                String by) {
            super(BAN, time);
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
    
    public static class MsgDeleted extends Message {
        
        public final String targetMsgId;
        public final String msg;
        public final String by;
        
        public MsgDeleted(Long time, String targetMsgId, String msg, String by) {
            super(MSG_DELETED, time);
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
        
        public SubMessage(Long time, String message, String text) {
            super(SUB, time);
            this.attached_message = message;
            this.system_msg = text;
        }
    }
    
    public static class InfoMessage extends Message {
        
        public final String attached_message;
        public final String system_msg;
        
        public InfoMessage(Long time, String message, String text) {
            super(INFO, time);
            this.attached_message = message;
            this.system_msg = text;
        }
    }
    
    public static class ModAction extends Message {

        public final String commandAndParameters;
        
        public ModAction(Long time, String commandAndParameters) {
            super(MOD_ACTION, time);
            this.commandAndParameters = commandAndParameters;
        }
        
    }
    
    public static class AutoModMessage extends Message {
        
        public final String message;
        public final String id;
        
        public AutoModMessage(String message, String id) {
            super(AUTO_MOD_MESSAGE, System.currentTimeMillis());
            this.message = message;
            this.id = id;
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
    
}
