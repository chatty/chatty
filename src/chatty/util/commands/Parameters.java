
package chatty.util.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class Parameters {

    private final Map<String, String> parameters;
    private final String[] args;

    public Parameters(Map<String, String> parameters) {
        this.parameters = parameters;
        if (parameters.get("args") != null) {
            this.args = parameters.get("args").split(" ");
        } else {
            this.args = new String[0];
        }
    }

    public String get(String key) {
        return parameters.get(key);
    }
    
    public void put(String key, String value) {
        parameters.put(key, value);
    }

    public Collection<String> getRange(int startIndex, boolean toEnd) {
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
    
    public static Parameters create(String args) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("args", args);
        return new Parameters(parameters);
    }

}
