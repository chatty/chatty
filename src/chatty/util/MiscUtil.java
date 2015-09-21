
package chatty.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * General purpose static methods.
 * 
 * @author tduva
 */
public class MiscUtil {

    /**
     * Copy the given text to the clipboard.
     * 
     * @param text 
     */
    public static void copyToClipboard(String text) {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(text), null);
    }
    
    public static boolean openFolder(File folder, Component parent) {
        try {
            Desktop.getDesktop().open(folder);
        } catch (Exception ex) {
            if (parent != null) {
                JOptionPane.showMessageDialog(parent, "Opening folder failed.");
            }
            return false;
        }
        return true;
    }
    
    /**
     * Parses the command line arguments from the main method into a Map.
     * Arguments that start with a dash "-" are interpreted as key, everything
     * after as value (until the next key or end of the arguments). This means
     * that argument values can contain spaces, but they can not contain an
     * argument starting with "-" (which would be interpreted as the next key).
     * If a key occurs more than once, the value of the last one is used.
     * 
     * Example:
     * -cd -channel test -channel zmaskm, sirstendec -connect
     * 
     * Returns the Map:
     * {cd="",
     *  channel="zmaskm, sirstendec",
     *  connect=""
     * }
     * 
     * @param args The commandline arguments from the main method
     * @return The map with argument keys and values
     */
    public static Map<String, String> parseArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        String key = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                // Put key in result, but also remember for next iteration
                key = arg.substring(1);
                // Overwrites possibly existing key, so only last one with this
                // name is saved
                result.put(key, "");
            } else if (key != null) {
                // Append current value (not a key) to last found key
                // Trim in case previous value was empty
                String newValue = (result.get(key)+" "+arg).trim();
                result.put(key, newValue);
            }
        }
        return result;
    }
    
    
}
