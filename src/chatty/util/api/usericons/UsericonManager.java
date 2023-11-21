
package chatty.util.api.usericons;

import chatty.Helper;
import chatty.User;
import chatty.gui.GuiUtil;
import chatty.util.api.usericons.Usericon.Type;
import chatty.gui.MainGui;
import chatty.util.Pair;
import chatty.util.StringUtil;
import chatty.util.irc.IrcBadges;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class UsericonManager {
    
    private static final Logger LOGGER = Logger.getLogger(UsericonManager.class.getName());
    
    private static final String SETTING_NAME = "customUsericons";
    private static final String SETTING_NAME_HIDDEN_BADGES = "hiddenUsericons";
    
    /**
     * The default icons are the fallback ones loaded from the JAR and the ones
     * requested from the Twitch API and FFZ. Those are sorted so that FFZ >
     * Twitch > Fallback.
     */
    private final TreeSet<Usericon> defaultIcons = new TreeSet<>();
    
    /**
     * The custom icons which are defined by the user. Those are sorted manually
     * by the user and are also loaded/saved from/to the settings.
     */
    private final List<Usericon> customIcons = new ArrayList<>();
    
    private final List<Usericon> hiddenBadges = new ArrayList<>();
    
    private final List<Usericon> thirdParty = new ArrayList<>();
    
    private final Map<Pair<String, Integer>, Usericon> channelLogos = new HashMap<>();
    private final Map<String, String> channelLogoUrls = new HashMap<>();

    private final Settings settings;
    
    public UsericonManager(Settings settings) {
        this.settings = settings;
    }
    
    public synchronized void addDefaultIcons(List<Usericon> icons) {
        for (Usericon icon : icons) {
            addDefaultIcon(icon);
        }
//        debug();
    }
    
    public synchronized void addDefaultIcon(Usericon icon) {
        if (icon != null) {
            // Remove icon if it already exists, so it can actually be
            // updated if necessary.
            if (defaultIcons.contains(icon)) {
                defaultIcons.remove(icon);
            }
            defaultIcons.add(icon);
        }
    }
    
    public synchronized void setThirdPartyIcons(List<Usericon> icons) {
        LOGGER.info(String.format(Locale.ROOT, "Added %d third-party badges", icons.size()));
        this.thirdParty.clear();
        this.thirdParty.addAll(icons);
    }
    
    /**
     * Add or update (if the size doesn't match) the channel logo for the given
     * channel. If the size is not valid, all logos are removed.
     * 
     * The logos are currently only used for the Stream Chat.
     *
     * @param channel Must not be null
     * @param url Must not be null
     */
    public synchronized void addChannelLogoUrl(String channel, String url) {
        channelLogoUrls.put(channel, url);
    }
    
    private synchronized Usericon getChannelLogo(String channel, int size) {
        Pair<String, Integer> key = new Pair<>(channel, size);
        Usericon icon = channelLogos.get(key);
        String url = channelLogoUrls.get(channel);
        if (icon == null && url != null) {
            icon = UsericonFactory.createChannelLogo(channel, url, size);
            if (icon != null) {
                LOGGER.info("Added StreamChat channel logo: "+icon);
                channelLogos.put(key, icon);
            }
        }
        return icon;
    }
    
    /**
     * Run in EDT to avoid very obscure bug where all window contents would be
     * white and/or unresponsive, when using addColor() when creating fallback
     * icons.
     * 
     * This may not affect the creation of Twitch icons, because that is done
     * when the GUI is already created. (Possibly, it's a strange bug.)
     */
    public void init() {
        GuiUtil.edt(() -> {
            addFallbackIcons();
            loadFromSettings();
            loadHiddenBadgesFromSettings();
        });
    }
    
    /**
     * Adds default icons from inside the JAR, which can be displayed if other
     * stuff didn't load (yet).
     */
    private synchronized void addFallbackIcons() {
        addFallbackIcon(Usericon.Type.MOD, "icon_mod.png");
        addFallbackIcon(Usericon.Type.ADMIN, "icon_admin.png");
        addFallbackIcon(Usericon.Type.STAFF, "icon_staff.png");
        addFallbackIcon(Usericon.Type.BROADCASTER, "icon_broadcaster.png");
        addFallbackIcon(Usericon.Type.SUB, "icon_sub.png");
        addFallbackIcon(Usericon.Type.TURBO, "icon_turbo.png");
        addFallbackIcon(Usericon.Type.GLOBAL_MOD, "icon_globalmod.png");
        addFallbackIcon(Usericon.Type.BOT, "icon_bot.png");
        addFallbackIcon(Usericon.Type.HL, "icon_hl.png");
        addFallbackIcon(Usericon.Type.FIRSTMSG, "icon_firstmsg.png");
//        addFallbackIcon(Usericon.Type.RESUB, "icon_sub.png");
//        addFallbackIcon(Usericon.Type.NEWSUB, "icon_sub.png");
//        List<Usericon> test = new ArrayList<>();
//        Usericon blah = UsericonFactory.createTwitchBadge("premium", "1",
//                "https://static-cdn.jtvnw.net/badges/v1/a1dd5073-19c3-4911-8cb4-c464a7bc1510/1",
//                null);
//        System.out.println(blah);
//        test.add(blah);
//        addDefaultIcons(test);
    }
    
    private void addFallbackIcon(Usericon.Type type, String fileName) {
        defaultIcons.add(UsericonFactory.createFallbackIcon(type,
                MainGui.class.getResource(fileName)));
    }
    
    public synchronized List<Usericon> getCustomData() {
        return new ArrayList<>(customIcons);
    }
    
    public synchronized List<Usericon> getHiddenBadgesData() {
        return new ArrayList<>(hiddenBadges);
    }
    
    public synchronized void setCustomData(List<Usericon> data) {
        customIcons.clear();
        customIcons.addAll(data);
        saveToSettings();
    }
    
    public synchronized void setHiddenBadgesData(List<Usericon> data) {
        hiddenBadges.clear();
        hiddenBadges.addAll(data);
        saveHiddenBadgesToSettings();
    }
    
    public synchronized Set<String> getTwitchBadgeTypes() {
        Set<String> result = new TreeSet<>();
        for (Usericon icon : defaultIcons) {
            if (icon.type == Usericon.Type.TWITCH) {
                result.add(icon.badgeType.id);
                result.add(icon.badgeType.toString());
            }
        }
        return result;
    }
    
    public synchronized List<Usericon> getBadges(IrcBadges badgesDef,
            User user, User localUser, boolean botBadgeEnabled, MsgTags tags, int channelLogoSize) {
        List<Usericon> icons = getTwitchBadges(badgesDef, user, tags);
        if (user.isBot() && botBadgeEnabled) {
            Usericon icon = getIcon(Usericon.Type.BOT, null, null, user, tags);
            if (icon != null) {
                icons.add(icon);
            }
        }
        addThirdPartyIcons(icons, user, tags);
        addAddonIcons(icons, user, tags);
        if (tags != null && tags.isHighlightedMessage()) {
            Usericon icon = getIcon(Usericon.Type.HL, null, null, user, tags);
            if (icon != null) {
                icons.add(0, icon);
            }
        }
        if (tags != null && tags.isTrue("first-msg")
                && localUser != null && localUser.hasChannelModeratorRights()) {
            Usericon icon = getIcon(Usericon.Type.FIRSTMSG, null, null, user, tags);
            if (icon != null) {
                icons.add(0, icon);
            }
        }
        Usericon channelIcon = getChannelIcon(user, channelLogoSize);
        if (channelIcon != null) {
            icons.add(0, channelIcon);
        }
        return icons;
    }
    
    public synchronized Usericon getChannelIcon(User user, int channelLogoSize) {
        if (channelLogoSize > 0) {
            return getChannelLogo(user.getChannel(), channelLogoSize);
        }
        return null;
    }

    private List<Usericon> getTwitchBadges(IrcBadges badgesDef, User user, MsgTags tags) {
        if (badgesDef == null || badgesDef.isEmpty()) {
            return new ArrayList<>();
        }
        List<Usericon> result = new ArrayList<>();
        for (int i=0; i<badgesDef.size(); i++) {
            String id = badgesDef.getId(i);
            String value = badgesDef.getVersion(i);
            Usericon icon = getIcon(Type.TWITCH, id, value, user, tags);
            if (icon != null) {
                result.add(icon);
            }
        }
        return result;
    }
    
    /**
     * Returns the first matching {@code ImageIcon} for the given type and user.
     * It first checks if there are any matching custom icons, then checks the
     * default icons. It also only returns an icon that actually has an image
     * loaded, otherwise it continues searching.
     * 
     * <p>Should always return an icon, since fallback icons are added.</p>
     * 
     * <p>The returned icon, if non-null, will always have a valid image, unless
     * the removeBadge-property is true.</p>
     * 
     * @param type The type to retrieve
     * @param id
     * @param version
     * @param user The user the returned icon has to match
     * @param tags The MsgTags (some tags are user-specific)
     * @return The matching icon or {@code null} if none matched
     */
    public synchronized Usericon getIcon(Usericon.Type type,
            String id, String version, User user, MsgTags tags) {
        if (customUsericonsEnabled()) {
            for (Usericon icon : customIcons) {
                //System.out.println("A:"+" "+type+" "+icon.type+" "+iconsMatchesAdvancedType(icon, type, id, version)+" "+icon);
                if (iconsMatchesAdvancedType(icon, type, id, version) && iconMatchesUser(icon, user, tags)) {
                    if (icon.removeBadge) {
                        return icon;
                    } else if (icon.hasRegularImage) {
                        return icon;
                    } else if (icon.fileName.equalsIgnoreCase("$ffz")) {
                        // If fileName is a reference, then check if an icon
                        // for that exists (only really applicable for FFZ Mod
                        // Icon at the moment)
                        Usericon refIcon = getDefaultIcon(type, id, version, user, Usericon.SOURCE_FFZ);
                        if (refIcon != null) {
                            return refIcon;
                        }
                    } else if (icon.fileName.equalsIgnoreCase("$twitch")) {
                        // This is a very special case, which only applies if
                        // the Twitch Icon wasn't loaded or not loaded yet, and
                        // it should be replaced only when that happens.
                        
                        Usericon refIcon = getDefaultIcon(type, id, version, user, Usericon.SOURCE_TWITCH2);
                        if (refIcon != null) {
                            return refIcon;
                        }
                    } else if (icon.fileName.equalsIgnoreCase("$default")) {
                        Usericon refIcon = getDefaultIcon(type, id, version, user, Usericon.SOURCE_ANY);
                        if (refIcon != null) {
                            return refIcon;
                        }
                    }
                }
            }
        }
        for (Usericon icon : hiddenBadges) {
            if (iconsMatchesAdvancedType(icon, type, id, version)) {
                return icon;
            }
        }
        return getDefaultIcon(type, id, version, user, Usericon.SOURCE_ANY);
    }
    
    /**
     * Get a default icon for the given type and user, with {@code source}.
     * 
     * @param type The type
     * @param user The user it has to match
     * @param source The source, can be -1 to match any source
     * @return The {@code ImageIcon} or {@code null} if none was found
     */
    private Usericon getDefaultIcon(Usericon.Type type, String id, String version, User user, int source) {
        for (Usericon icon : defaultIcons) {
            Usericon checked = checkIcon(icon, type, id, version, user, source);
            if (checked != null) {
                return checked;
            }
        }
        for (Usericon icon : thirdParty) {
            Usericon checked = checkIcon(icon, type, id, version, user, source);
            if (checked != null) {
                return checked;
            }
        }
        return null;
    }
    
    private Usericon checkIcon(Usericon icon, Usericon.Type type, String id, String version, User user, int source) {
        if (iconsMatchesAdvancedType(icon, type, id, version) && iconMatchesUser(icon, user, null)
                && (source == Usericon.SOURCE_ANY || icon.source == source)) {
            // Skip FFZ if disabled
            if (icon.source == Usericon.SOURCE_FFZ && !settings.getBoolean("ffzModIcon")) {
                return null;
            }
            return icon;
        }
        return null;
    }
    
    private boolean customUsericonsEnabled() {
        return settings.getBoolean("customUsericonsEnabled");
    }
    
    private void addAddonIcons(List<Usericon> icons, User user, MsgTags tags) {
        if (customUsericonsEnabled()) {
            for (Usericon icon : customIcons) {
                if (icon.type == Type.ADDON && iconMatchesUser(icon, user, tags)
                        && icon.hasRegularImage) {
                    insert(icons, icon);
                    if (icon.stop) {
                        break;
                    }
                }
            }
        }
    }
    
    private void addThirdPartyIcons(List<Usericon> icons, User user, MsgTags tags) {
        for (Usericon icon : thirdParty) {
            /**
             * Need to check eligibility here first, since a Custom Icon
             * matching this icon's type wouldn't have the same restrictions
             * (normal Twitch badges are requested via getIcon() based on which
             * types the user actually has access to, so that's already a
             * different starting position).
             */
            if (iconMatchesUser(icon, user, tags)) {
                // This may or may not return the same icon, depending on
                // whether Custom Usericons replace it
                Usericon transformed = getIcon(Type.OTHER, icon.badgeType.id, icon.badgeType.version, user, tags);
                if (transformed != null) {
                    insert(icons, transformed);
                }
            }
        }
    }
    
    /**
     * Insert icon according to it's position value, if present, otherwise
     * simply at the end.
     * 
     * @param icons
     * @param icon 
     */
    private void insert(List<Usericon> icons, Usericon icon) {
        if (icon.position == null) {
            icons.add(icon);
        } else {
            int insertIndex = -1;
            for (int i=0;i<icons.size();i++) {
                if (icon.position.insertHere(icons.get(i))) {
                    insertIndex = i;
                    break;
                }
            }
            if (insertIndex != -1) {
                icons.add(insertIndex, icon);
            } else {
                icons.add(icon);
            }
        }
    }
    
    /**
     * Checks whether the given {@code Usericon} matches the given {@code User}.
     * This checks if the channel restriction and the other restrictions ($sub,
     * $mod etc.) match.
     * 
     * @param icon The icon to check
     * @param user The user to check against
     * @return {@code true} if this icon matches the user, {@code false}
     * otherwise
     */
    private boolean iconMatchesUser(Usericon icon, User user, MsgTags tags) {
        if (icon.badgeTypeRestriction.id != null) {
            IrcBadges badges = user.getTwitchBadges();
            String id = icon.badgeTypeRestriction.id;
            String version = icon.badgeTypeRestriction.version;
            if (badges == null) {
                return false;
            }
            if (!badges.hasId(id)) {
                return false;
            }
            if (version != null && !badges.get(id).equals(version)) {
                return false;
            }
        }
        
        // If channelRestriction doesn't match, don't have to continue
        if (!icon.channel.isEmpty()) {
            if (icon.channel.equalsIgnoreCase(user.getOwnerChannel())) {
                if (icon.channelInverse) {
                    return false;
                }
            } else {
                if (!icon.channelInverse) {
                    return false;
                }
            }
        }
        // Username/id restriction (can fail only if non-null)
        boolean usernameR = icon.usernames == null || (user.getName() != null && icon.usernames.contains(user.getName()));
        boolean useridR = icon.userids == null || (user.getId() != null  && icon.userids.contains(user.getId()));
        if (icon.usernames != null && icon.userids != null) {
            // Both are set, so either one may match to not fail
            if (!usernameR && !useridR) {
                return false;
            }
        }
        else if (!usernameR || !useridR) {
            // Only one set, which also failed
            return false;
        }
        
        // Now check for the other restriction
        if (icon.restriction == null) {
            return true;
        } else if (icon.matchType == Usericon.MatchType.ALL) {
            return true;
        } else if (icon.matchType == Usericon.MatchType.NAME) {
            if (icon.restrictionValue.equalsIgnoreCase(user.getName())) {
                return true;
            }
        } else if (icon.matchType == Usericon.MatchType.CATEGORY) {
            if (user.hasCategory(icon.category)) {
                return true;
            }
        } else if (icon.matchType == Usericon.MatchType.MATCH) {
            if (icon.match.matches(user, tags)) {
                return true;
            }
        } else if (icon.matchType == Usericon.MatchType.STATUS) {
            if (Helper.matchUserStatus(icon.restrictionValue, user)) {
                return true;
            }
        } else if (icon.matchType == Usericon.MatchType.COLOR) {
            if (user.getColor().equals(icon.colorRestriction)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean iconsMatchesAdvancedType(Usericon icon,
            Usericon.Type requestedType, String id, String version) {
        if (icon.type == Type.ALL) {
            return true;
        }
        else if (icon.type == requestedType) {
            if (icon.badgeType.matchesLenient(id, version)) {
                return true;
            }
        }
        
        /**
         * If we're looking for a TWITCH badge (id/version), but this is an icon
         * with a regular type (MOD, ..), then check if and when there is a
         * mapping for the badgeId for this type.
         * 
         * This is so that icons with a type of e.g. MOD can still match a
         * TWITCH badge with mod/1 (for custom and fallback icons).
         */
        else if (requestedType == Usericon.Type.TWITCH && icon.type.badgeId != null) {
            if (icon.type == Usericon.typeFromBadgeId(id) && icon.badgeType.equals(null, null)) {
                return true;
            }
        }
        return false;
    }
    
    private synchronized void loadFromSettings() {
        List<List> entriesToLoad = settings.getList(SETTING_NAME);
        customIcons.clear();
        int count = 0;
        for (List entryToLoad : entriesToLoad) {
            Usericon icon = listToEntry(entryToLoad);
            if (icon != null) {
                customIcons.add(icon);
                count++;
            } else {
                LOGGER.warning("Usericons: Couldn't load entry "+entryToLoad);
            }
        }
        LOGGER.info("Usericons: Loaded "+count+"/"+entriesToLoad.size());
    }
    
    private synchronized void loadHiddenBadgesFromSettings() {
        List<List> entriesToLoad = settings.getList(SETTING_NAME_HIDDEN_BADGES);
        hiddenBadges.clear();
        int count = 0;
        for (List entryToLoad : entriesToLoad) {
            Usericon icon = listToEntryHiddenBadges(entryToLoad);
            if (icon != null) {
                hiddenBadges.add(icon);
                count++;
            } else {
                LOGGER.warning("Hidden Usericons: Couldn't load entry "+entryToLoad);
            }
        }
        LOGGER.info("Hidden Usericons: Loaded "+count+"/"+entriesToLoad.size());
    }
    
    private synchronized void saveToSettings() {
        List<List> entriesToSave = new ArrayList<>();
        for (Usericon iconToSave : customIcons) {
            entriesToSave.add(entryToList(iconToSave));
        }
        settings.putList(SETTING_NAME, entriesToSave);
    }
    
    private synchronized void saveHiddenBadgesToSettings() {
        List<List> entriesToSave = new ArrayList<>();
        for (Usericon iconToSave : hiddenBadges) {
            entriesToSave.add(entryToListHiddenBadges(iconToSave));
        }
        settings.putList(SETTING_NAME_HIDDEN_BADGES, entriesToSave);
    }
    
    private List entryToList(Usericon icon) {
        List list = new ArrayList();
        list.add(icon.type.id);
        list.add(icon.restriction);
        list.add(icon.fileName);
        list.add(icon.channelRestriction);
        list.add(icon.getIdAndVersion());
        list.add(icon.positionValue);
        return list;
    }
    
    private List entryToListHiddenBadges(Usericon icon) {
        List list = new ArrayList();
        list.add(icon.type.id);
        list.add(icon.getIdAndVersion());
        return list;
    }
    
    private Usericon listToEntry(List list) {
        try {
            Type type = Type.getTypeFromId(((Number)list.get(0)).intValue());
            String restriction = (String)list.get(1);
            String fileName = (String)list.get(2);
            String channel = (String)list.get(3);
            String idVersion = null;
            if (list.size() > 4) {
                idVersion = (String)list.get(4);
            }
            String position = null;
            if (list.size() > 5) {
                position = (String)list.get(5);
            }
            return UsericonFactory.createCustomIcon(type, idVersion, restriction, fileName, channel, position);
        } catch (ClassCastException | IndexOutOfBoundsException ex) {
            return null;
        }
    }
    
    private Usericon listToEntryHiddenBadges(List list) {
        Type type = Type.getTypeFromId(((Number)list.get(0)).intValue());
        String idVersion = (String)list.get(1);
        return UsericonFactory.createCustomIcon(type, idVersion, "", "", "", "");
    }
 
    public synchronized void debug() {
        LOGGER.info(String.format(Locale.ROOT, "Default usericons (%d): %s",
                defaultIcons.size(), defaultIcons));
        LOGGER.info(String.format(Locale.ROOT, "Custom usericons (%d): %s",
                customIcons.size(), customIcons));
    }

    public synchronized boolean hideBadge(Usericon usericon) {
        boolean alreadyHidden = false;
        for (Usericon icon : hiddenBadges) {
            if (iconsMatchesAdvancedType(icon, usericon.type, usericon.badgeType.id, usericon.badgeType.version)) {
                alreadyHidden = true;
            }
        }
        if (!alreadyHidden) {
            Usericon customUsericon = UsericonFactory.createCustomIcon(usericon.type, usericon.badgeType.id, "", "", "", "");
            hiddenBadges.add(0, customUsericon);
            saveHiddenBadgesToSettings();
            return true;
        }
        return false;
    }
    
}
