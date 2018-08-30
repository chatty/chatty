
package chatty.util;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class Debugging {
    
    private static final Set<String> enabled = new HashSet<>();
    
    public static String command(String parameter) {
        if (parameter == null) {
            return "Invalid parameter";
        }
        for (String id : parameter.split(" ")) {
            if (id.startsWith("+")) {
                enabled.add(id.substring(1));
            } else if (id.startsWith("-")) {
                enabled.remove(id.substring(1));
            } else {
                if (enabled.contains(id)) {
                    enabled.remove(id);
                } else {
                    enabled.add(id);
                }
            }
        }
        return "Now: "+enabled;
    }
    
    public static boolean isEnabled(String id) {
        return !enabled.isEmpty() && enabled.contains(id);
    }
    
    /**
     * For filtering debug output that may contain commandline parameters
     * containing a token (not commonly used, but possible).
     * 
     * @param input
     * @return 
     */
    public static String filterToken(String input) {
        return input.replaceAll("(-set:token|-token|-password) \\w+", "$1 <token>");
    }
    
    // For testing
    public static void main(String[] args) {
        System.out.println(filterToken("-d \"G:\\chatty settings\" -set:token abc -token abc -password -password abc -connect"));
    }
    
}
