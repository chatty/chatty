
package chatty.util.commands;

import chatty.User;
import chatty.util.StringUtil;
import chatty.util.api.usericons.Usericon;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Allows adding values for use in Custom Commands replacements.
 * 
 * @author tduva
 */
public class Parameters {

    private final Map<String, String> parameters;
    private String[] args;
    private final Map<String, Object> objectParameters = new HashMap<>();

    public Parameters(Map<String, String> parameters) {
        this.parameters = parameters;
        updateArgs();
    }
    
    /**
     * Creates a copy containing the same String and Object parameters.
     * 
     * <p>
     * Useful if the same base parameters should be available to several
     * commands, but changes performed for one command should not affect
     * following commands.
     *
     * @return 
     */
    public synchronized Parameters copy() {
        Parameters result = new Parameters(new HashMap<>(parameters));
        result.objectParameters.putAll(objectParameters);
        return result;
    }

    /**
     * Get a parameter with the given key (this includes ones that are based on
     * objects such as "user" and "localUser", e.g. "display-nick").
     *
     * @param key Should be all-lowercase
     * @return The value associated with the key, or null if none exists
     */
    public synchronized String get(String key) {
        String result = parameters.get(key);
        if (result == null) {
            result = getUserParameter(key, (User)getObject("user"));
        }
        if (result == null && key.startsWith("my-")) {
            result = getUserParameter(key.substring("my-".length()),
                    (User) getObject("localUser"));
        }
        return result;
    }
    
    public synchronized boolean hasKey(String key) {
        return get(key) != null;
    }
    
    /**
     * Check that all of the given parameters are not null or empty (this
     * includes ones that are based on objects such as "user" and "localUser",
     * e.g. "display-nick").
     * 
     * @param keys
     * @return true if all parameters with the given keys are not null or empty
     */
    public synchronized boolean notEmpty(String... keys) {
        for (String key : keys) {
            if (StringUtil.isNullOrEmpty(get(key))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check that all of the given keys are not null or empty (this includes
     * ones that are based on objects such as "user" and "localUser", e.g.
     * "display-nick").
     *
     * @param keys
     * @return true if all given keys are not null or empty
     */
    public synchronized boolean notEmpty(Collection<String> keys) {
        for (String key : keys) {
            if (StringUtil.isNullOrEmpty(get(key))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get an object parameter with the given key.
     * 
     * @param key
     * @return The value associated with the key, or null if none exists
     */
    public synchronized Object getObject(String key) {
        return objectParameters.get(key);
    }
    
    /**
     * Set the parameter for the given key, if both key and value are non-null
     * and the value is non-empty. The key should be all-lowercase.
     * 
     * @param key
     * @param value 
     */
    public synchronized void put(String key, String value) {
        if (key != null && value != null && !value.isEmpty()) {
            parameters.put(key, value);
            if (key.equals("args")) {
                updateArgs();
            }
        }
    }
    
    /**
     * Remove a parameter. If the key doesn't exist in the current parameters
     * nothing happens.
     *
     * @param key The key of the parameter to remove (may be null)
     */
    public synchronized void remove(String key) {
        if (key != null) {
            parameters.remove(key);
            if (key.equals("args")) {
                updateArgs();
            }
        }
    }
    
    /**
     * Set the object parameter for the given key, if both key and value are
     * non-null. Object parameters may be used by function replacements, but
     * cannot be accessed directly.
     *
     * @param key
     * @param value 
     */
    public synchronized void putObject(String key, Object value) {
        if (key != null && value != null) {
            objectParameters.put(key, value);
        }
    }
    
    /**
     * Set new args, overwriting the old ones, even if args is null.
     * 
     * @param args 
     */
    public synchronized void putArgs(String args) {
        parameters.put("args", args);
        updateArgs();
    }
    
    /**
     * Return the args as the original String.
     * 
     * @return The args, can be null
     */
    public synchronized String getArgs() {
        return parameters.get("args");
    }
    
    private void updateArgs() {
        if (parameters.get("args") != null) {
            this.args = parameters.get("args").split(" ");
        } else {
            this.args = new String[0];
        }
    }

    public synchronized Collection<String> getRange(int startIndex, boolean toEnd) {
        if (startIndex > args.length - 1) {
            return null;
        }
        Collection<String> result = new LinkedList<>();
        if (!toEnd) {
            result.add(args[startIndex]);
        } else {
            for (int i = startIndex; i < args.length; i++) {
                result.add(args[i]);
            }
        }
        return result;
    }
    
    /**
     * Create Parameters with an args String (which is split by space and can be
     * accessed via the numeric replacements).
     *
     * @param args The args, can be null or empty
     * @return
     */
    public static Parameters create(String args) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("args", args);
        return new Parameters(parameters);
    }
    
    @Override
    public synchronized String toString() {
        return parameters.toString();
    }
    
    private static String getUserParameter(String name, User user) {
        if (user == null) {
            return null;
        }
        switch (name) {
            case "nick": return user.getRegularDisplayNick();
            case "user-id": return user.getId();
            case "display-nick": return user.getDisplayNick();
            case "custom-nick": return user.getCustomNick();
            case "full-nick": return user.getFullNick();
            case "special-nick": return !user.hasRegularDisplayNick() ? "true" : null;
        }
        
        if (user.hasRegularDisplayNick()) {
            switch (name) {
                case "display-nick2": return user.getDisplayNick();
                case "full-nick2": return user.getFullNick();
            }
        }
        else {
            // Special nick (with spaces or localized)
            switch (name) {
                case "display-nick2": return user.getDisplayNick()+" ("+user.getRegularDisplayNick()+")";
                case "full-nick2": return user.getFullNick()+" ("+user.getRegularDisplayNick()+")";
            }
        }
        
        if (user.getTwitchBadges() != null) {
            switch (name) {
                case "twitch-badge-info": return user.getTwitchBadges().toString();
                case "twitch-badges": return Usericon.makeBadgeInfo(user.getTwitchBadges());
            }
        }
        return null;
    }
    
}
