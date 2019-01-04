
package chatty.util.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class Parameters {

    private final Map<String, String> parameters;
    private String[] args;

    public Parameters(Map<String, String> parameters) {
        this.parameters = parameters;
        updateArgs();
    }

    public synchronized String get(String key) {
        return parameters.get(key);
    }
    
    /**
     * Set the parmaeter for the given key, if both key and value are non-null
     * and the value is non-empty.
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
     * Set different args, overwriting the old ones, even if args is null.
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
