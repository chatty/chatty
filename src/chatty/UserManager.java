
package chatty;

import chatty.gui.colors.UsercolorManager;
import chatty.util.api.usericons.UsericonManager;
import chatty.util.BotNameManager;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.util.Map.Entry;
import java.util.*;
import java.util.logging.Logger;

/**
 * Provides methods to get a (maybe new) User object for a channel/username 
 * combination and search for User objects by channel, username etc.
 * 
 * With IRCv3 this doesn't save a lot of state anymore, because every message
 * has the user type etc. it is only saved directly in the User objects.
 * Although it could be useful to add some caching again (e.g. for showing
 * user type in userlist before the user said something).
 * 
 * @author tduva
 */
public class UserManager {

    private static final Logger LOGGER = Logger.getLogger(UserManager.class.getName());
    
    private static final int CLEAR_MESSAGES_TIMER = 1*60*60*1000;
    
    private final Set<UserManagerListener> listeners = new HashSet<>();
    
    private volatile String localUsername;
    public final User specialUser = new User("[specialUser]", Room.createRegular("[nochannel]"));
    
    private final HashMap<String, HashMap<String, User>> users = new HashMap<>();
    private final HashMap<String, String> cachedColors = new HashMap<>();
    private boolean capitalizedNames = false;
    
    private final User errorUser = new User("[Error]", Room.createRegular("#[error]"));
    
    // Stupid hack to get Usericons in ChannelTextPane without a user (twitchnotify messages)
    public final User dummyUser = new User("", Room.createRegular("#[error]"));

    private CustomNames customNamesManager;
    private UsericonManager usericonManager;
    private UsercolorManager usercolorManager;
    private Addressbook addressbook;
    private BotNameManager botNameManager;
    private Settings settings;
    
    public UserManager() {
        Timer clearMessageTimer = new Timer("Clear User Messages", true);
        clearMessageTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                clearMessagesOfInactiveUsers();
            }
        }, CLEAR_MESSAGES_TIMER, CLEAR_MESSAGES_TIMER);
    }
    
    public void setLocalUsername(String username) {
        this.localUsername = username;
    }
    
    public void setBotNameManager(BotNameManager m) {
        this.botNameManager = m;
        m.addListener(new BotNameManager.BotNameListener() {

            private void setUserBot(User user) {
                if (user.setBot(true)) {
                    userUpdated(user);
                }
            }
            
            @Override
            public void botNameAdded(String channel, String botName) {
                if (channel == null) {
                    for (User user : getUsersByName(botName)) {
                        setUserBot(user);
                    }
                } else {
                    User user = getUserIfExists(channel, botName);
                    if (user != null) {
                        setUserBot(user);
                    }
                }
            }
        });
    }
    
    public void addListener(UserManagerListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    private void userUpdated(User user) {
        for (UserManagerListener listener : listeners) {
            listener.userUpdated(user);
        }
    }
    
    public  void updateRoom(Room room) {
        Map<String, User> data = getUsersByChannel(room.getChannel());
        for (User user : data.values()) {
            user.setRoom(room);
        }
    }
    
    public void setSettings(Settings settings) {
        this.settings = settings;
    }
    
    public void setCapitalizedNames(boolean capitalized) {
        capitalizedNames = capitalized;
    }
    
    public void setUsericonManager(UsericonManager manager) {
        usericonManager = manager;
        dummyUser.setUsericonManager(manager);
    }
    
    public void setUsercolorManager(UsercolorManager manager) {
        usercolorManager = manager;
    }
    
    public void setAddressbook(Addressbook addressbook) {
        this.addressbook = addressbook;
    }
    
    public void setCustomNamesManager(CustomNames m) {
        if (m != null) {
            this.customNamesManager = m;
            m.addListener(new CustomNames.CustomNamesListener() {

                @Override
                public void setName(String name, String customNick) {
                    List<User> users = getUsersByName(name);
                    for (User user : users) {
                        user.setCustomNick(customNick);
                        userUpdated(user);
                    }
                }
            });
        }
    }
    
    /**
     * Gets a Map of all User objects in the given channel.
     * 
     * @param channel
     * @return 
     */
    public synchronized HashMap<String, User> getUsersByChannel(String channel) {
        HashMap<String, User> result = users.get(channel);
        if (result == null) {
            result = new HashMap<>();
            users.put(channel, result);
        }
        return result;
    }

    /**
     * Searches all channels for the given username and returns a List of all
     * the associated User objects. Does not create User object, only return
     * existing ones.
     * 
     * @param name The username to search for
     * @return The List of User-objects.
     */
    public synchronized List<User> getUsersByName(String name) {
        name = StringUtil.toLowerCase(name);
        List<User> result = new ArrayList<>();
        Iterator<HashMap<String, User>> it = users.values().iterator();
        while (it.hasNext()) {
            HashMap<String, User> channelUsers = it.next();
            User user = channelUsers.get(name);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    /**
     * Returns the user for the given channel and name, but only if an object
     * already exists.
     * 
     * @param channel
     * @param name
     * @return The {@code User} object or null if none exists
     */
    public synchronized User getUserIfExists(String channel, String name) {
        return getUsersByChannel(channel).get(name);
    }
    
    /**
     * Returns the User with the given name or creates a new User object if none
     * exists for this name.
     *
     * @param channel
     * @param name The name of the user
     * @return The matching User object
     * @see User
     */
    public synchronized User getUser(Room room, String name) {
        // Not sure if this makes sense
        if (name == null || name.isEmpty()) {
            return errorUser;
        }
        name = StringUtil.toLowerCase(name);
        User user = getUserIfExists(room.getChannel(), name);
        if (user == null) {
            // Capitalize name if enabled (might still be overwritten by setting
            // displayNick from tags)
            String capitalizedName = name;
            if (capitalizedNames) {
                capitalizedName = name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
            }
            user = new User(capitalizedName, room);
            user.setUsercolorManager(usercolorManager);
            user.setAddressbook(addressbook);
            user.setUsericonManager(usericonManager);
            if (customNamesManager != null) {
                user.setCustomNick(customNamesManager.getCustomName(name));
            }
            if (botNameManager != null && botNameManager.isBotName(room.getOwnerChannel(), name)) {
                user.setBot(true);
            }
            // Initialize some values if present for this name
            if (cachedColors.containsKey(name)) {
                user.setColor(cachedColors.get(name));
            }
            if (name.equals(localUsername)) {
                /**
                 * Set initial data for local user that is globally valid. This
                 * data would have been received from the GLOBALUSERSTATE
                 * command which may not be send after every join or sent
                 * message.
                 */
                user.setAdmin(specialUser.isAdmin());
                user.setStaff(specialUser.isStaff());
                user.setTurbo(specialUser.hasTurbo());
                user.setId(specialUser.getId());
                user.setLocalUser(true);
                if (!specialUser.hasDefaultColor()) {
                    user.setColor(specialUser.getPlainColor());
                }
                if (specialUser.hasDisplayNickSet()) {
                    user.setDisplayNick(specialUser.getDisplayNick());
                }
            }
            // Put User into the map for the channel
            getUsersByChannel(room.getChannel()).put(name, user);
            
            // Set history length
            int userDialogMessageLimit = settings.getInt("userDialogMessageLimit");
            if (userDialogMessageLimit >= 0) {
                user.setMaxNumberOfLines(userDialogMessageLimit);
            }
        }
        return user;
    }
    
    /**
     * Searches all channels for the given username and returns a Map with
     * all channels the username was found in and the associated User objects.
     * 
     * @param name The username to be searched for
     * @return A Map with channel->User association
     */
    public synchronized HashMap<String,User> getChannelsAndUsersByUserName(String name) {
        String lowercaseName = StringUtil.toLowerCase(name);
        HashMap<String,User> result = new HashMap<>();
        
        Iterator<Entry<String, HashMap<String, User>>> it = users.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, HashMap<String, User>> channel = it.next();
            
            String channelName = channel.getKey();
            HashMap<String,User> channelUsers = channel.getValue();
            
            User user = channelUsers.get(lowercaseName);
            if (user != null) {
                result.put(channelName,user);
            }
        }
        return result;
    }
    
    /**
     * Remove all users.
     */
    public synchronized void clear() {
        users.clear();
    }
    
    /**
     * Remove all users of the given channel.
     * 
     * @param channel 
     */
    public synchronized void clear(String channel) {
        getUsersByChannel(channel).clear();
    }
    
    public synchronized void clearMessagesOfInactiveUsers() {
        if (settings == null) {
            return;
        }
        long clearUserMessages = settings.getLong("clearUserMessages");
        if (clearUserMessages >= 0) {
            int numRemoved = 0;
            for (Map<String, User> chan : users.values()) {
                for (User user : chan.values()) {
                    numRemoved += user.clearMessagesIfInactive(clearUserMessages*60*60*1000);
                }
            }
            LOGGER.info("Cleared "+numRemoved+" user messages");
        }
    }
    
    /**
     * Set all users offline.
     */
    public synchronized void setAllOffline() {
        Iterator<HashMap<String,User>> it = users.values().iterator();
        while (it.hasNext()) {
            setAllOffline(it.next());
        }
    }
    
    /**
     * Set all users of the given channel offline.
     * 
     * @param channel 
     */
    public synchronized void setAllOffline(String channel) {
        if (channel == null) {
            setAllOffline();
        }
        Map<String, User> usersInChannel = users.get(channel);
        if (usersInChannel != null) {
            setAllOffline(usersInChannel);
        }
    }
    
    /**
     * Set all given users offline. Helper method.
     * 
     * @param usersInChannel 
     */
    private void setAllOffline(Map<String, User> usersInChannel) {
        for (User user : usersInChannel.values()) {
            user.setOnline(false);
        }
    }
    
    /**
     * Sets the color of a user across all channels.
     * 
     * @param userName String The name of the user
     * @param color String The color as a string representation
     */
    protected synchronized void setColorForUsername(String userName, String color) {
        userName = StringUtil.toLowerCase(userName);
        cachedColors.put(userName,color);
        
        List<User> userAllChans = getUsersByName(userName);
        for (User user : userAllChans) {
            user.setColor(color);
        }
    }
    
    /**
     * The list of mods received with channel context, set the containing names
     * as mod. Returns the changed users so they can be updated in the GUI.
     * 
     * @param channel
     * @param modsList
     * @return 
     */
    protected synchronized List<User> modsListReceived(Room room, List<String> modsList) {
        // Demod everyone on the channel
        Map<String,User> usersToDemod = getUsersByChannel(room.getChannel());
        for (User user : usersToDemod.values()) {
            user.setModerator(false);
        }
        // Mod everyone in the list
        LOGGER.info("Setting users as mod for "+room.getChannel()+": "+modsList);
        List<User> changedUsers = new ArrayList<>();
        for (String userName : modsList) {
            if (Helper.isValidChannel(userName)) {
                User user = getUser(room, userName);
                if (user.setModerator(true)) {
                    userUpdated(user);
                }
                changedUsers.add(user);
            }
        }
        return changedUsers;
    }
    
    public static interface UserManagerListener {
        public void userUpdated(User user);
    }
    
}
