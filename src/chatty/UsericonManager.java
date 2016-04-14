
package chatty;

import chatty.Usericon.Type;
import chatty.gui.MainGui;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class UsericonManager {
    
    private static final Logger LOGGER = Logger.getLogger(UsericonManager.class.getName());
    
    private static final String SETTING_NAME = "customUsericons";
    
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

    private final Settings settings;
    
    public UsericonManager(Settings settings) {
        init();
        this.settings = settings;
        loadFromSettings();
    }
    
    public synchronized void addDefaultIcons(List<Usericon> icons) {
        for (Usericon icon : icons) {
            if (icon != null && icon.image != null) {
                // Remove icon if it already exists, so it can actually be
                // updated if necessary.
                if (defaultIcons.contains(icon)) {
                    defaultIcons.remove(icon);
                }
                defaultIcons.add(icon);
            }
        }
    }
    
    /**
     * Run in EDT to avoid very obscure bug where all window contents would be
     * white and/or unresponsive, when using addColor() when creating fallback
     * icons.
     * 
     * This may not affect the creation of Twitch icons, because that is done
     * when the GUI is already created. (Possibly, it's a strange bug.)
     */
    private void init() {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                addFallbackIcons();
            }
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
    }
    
    private void addFallbackIcon(Usericon.Type type, String fileName) {
        defaultIcons.add(Usericon.createFallbackIcon(type,
                MainGui.class.getResource(fileName)));
    }
    
    public synchronized List<Usericon> getData() {
        return new ArrayList<>(customIcons);
    }
    
    public synchronized void setData(List<Usericon> data) {
        customIcons.clear();
        customIcons.addAll(data);
        saveToSettings();
    }
    
    /**
     * Returns the first matching {@code ImageIcon} for the given type and user.
     * It first checks if there are any matching custom icons, then checks the
     * default icons. It also only returns an icon that actually has an image
     * loaded, otherwise it continues searching.
     * 
     * <p>Should always return an icon, since fallback icons are added.</p>
     * 
     * @param type The type to retrieve
     * @param user The user the returned icon has to match
     * @return The matching icon or {@code null} if none matched
     */
    public synchronized ImageIcon getIcon(Usericon.Type type, User user) {
        if (customUsericonsEnabled()) {
            for (Usericon icon : customIcons) {
                if (icon.type == type && iconMatchesUser(icon, user)) {
                    if (icon.image != null) {
                        return icon.image;
                    } else if (icon.fileName.equalsIgnoreCase("$ffz")) {
                        // If fileName is a reference, then check if an icon
                        // for that exists (only really applicable for FFZ Mod
                        // Icon at the moment)
                        ImageIcon refImage = getDefaultIcon(type, user, Usericon.SOURCE_FFZ);
                        if (refImage != null) {
                            return refImage;
                        }
                    } else if (icon.fileName.equalsIgnoreCase("$twitch")) {
                        // This is a very special case, which only applies if
                        // the Twitch Icon wasn't loaded or not loaded yet, and
                        // it should be replaced only when that happens.
                        ImageIcon refImage = getDefaultIcon(type, user, Usericon.SOURCE_TWITCH);
                        if (refImage != null) {
                            return refImage;
                        }
                    }
                }
            }
        }
        return getDefaultIcon(type, user, -1);
    }
    
    /**
     * Get a default icon for the given type and user, with {@code source}.
     * 
     * @param type The type
     * @param user The user it has to match
     * @param source The source, can be -1 to match any source
     * @return The {@code ImageIcon} or {@code null} if none was found
     */
    private ImageIcon getDefaultIcon(Usericon.Type type, User user, int source) {
        for (Usericon icon : defaultIcons) {
            if (icon.type == type && iconMatchesUser(icon, user) && (source == -1 || icon.source == source)) {
                // Skip FFZ if disabled
                if (icon.source == Usericon.SOURCE_FFZ && !settings.getBoolean("ffzModIcon")) {
                    continue;
                }
                return icon.image;
            }
        }
        return null;
    }
    
    private boolean customUsericonsEnabled() {
        return settings.getBoolean("customUsericonsEnabled");
    }
    
    /**
     * Gets all icons with the given type that match the given {@code User}.
     * 
     * @param type The type of the icon
     * @param user The user the returned icons have to match
     * @return A {@code List} of {@code Usericon} objects, can be empty if no
     * icon matched or custom icons are disabled
     */
    public synchronized List<ImageIcon> getCustomIcons(Usericon.Type type, User user, boolean first) {
        List<ImageIcon> result = new ArrayList<>();
        if (customUsericonsEnabled()) {
            for (Usericon icon : customIcons) {
                if (icon.type == type && iconMatchesUser(icon, user)
                        && icon.image != null && first == icon.first) {
                    result.add(icon.image);
                    if (icon.stop) {
                        break;
                    }
                }
            }
        }
        return result;
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
    private boolean iconMatchesUser(Usericon icon, User user) {
        
        // If channelRestriction doesn't match, don't have to continue
        if (!icon.channel.isEmpty()) {
            if (icon.channel.equalsIgnoreCase(user.getChannel())) {
                if (icon.channelInverse) {
                    return false;
                }
            } else {
                if (!icon.channelInverse) {
                    return false;
                }
            }
        }
        // Now check for the other restriction
        if (icon.restriction == null) {
            return true;
        } else if (icon.matchType == Usericon.MatchType.ALL) {
            return true;
        } else if (icon.matchType == Usericon.MatchType.NAME) {
            if (icon.restrictionValue.equalsIgnoreCase(user.getNick())) {
                return true;
            }
        } else if (icon.matchType == Usericon.MatchType.CATEGORY) {
            if (user.hasCategory(icon.category)) {
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
    
    private synchronized void saveToSettings() {
        List<List> entriesToSave = new ArrayList<>();
        for (Usericon iconToSave : customIcons) {
            entriesToSave.add(entryToList(iconToSave));
        }
        settings.putList(SETTING_NAME, entriesToSave);
    }
    
    private List entryToList(Usericon icon) {
        List list = new ArrayList();
        list.add(icon.type.id);
        list.add(icon.restriction);
        list.add(icon.fileName);
        list.add(icon.channelRestriction);
        return list;
    }
    
    private Usericon listToEntry(List list) {
        try {
            Type type = Type.getTypeFromId(((Number)list.get(0)).intValue());
            String id = (String)list.get(1);
            String fileName = (String)list.get(2);
            String channel = (String)list.get(3);
            return Usericon.createCustomIcon(type, id, fileName, channel);
        } catch (ClassCastException | IndexOutOfBoundsException ex) {
            return null;
        }
    }
 
    public synchronized void debug() {
        LOGGER.info("Default usericons: "+defaultIcons);
        LOGGER.info("Custom usericons: "+customIcons);
    }
    
}
