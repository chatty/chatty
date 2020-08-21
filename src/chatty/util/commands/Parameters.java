
package chatty.util.commands;

import chatty.util.StringUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
     * Get a parameter with the given key. The key should be all-lowercase.
     * 
     * @param key
     * @return The value associated with the key, or null if none exists
     */
    public synchronized String get(String key) {
        return parameters.get(key);
    }
    
    /**
     * Check that all of the given parameters are not null or empty.
     * 
     * @param keys
     * @return true if all parameters with the given keys are not null or empty
     */
    public synchronized boolean notEmpty(String... keys) {
        for (String key : keys) {
            if (StringUtil.isNullOrEmpty(parameters.get(key))) {
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
    
    public synchronized Set<String> getIdentifiers() {
        return new HashSet<>(parameters.keySet());
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
    
}
