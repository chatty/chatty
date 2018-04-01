
package chatty.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * General purpose static methods.
 * 
 * @author tduva
 */
public class MiscUtil {
    
    private static final Logger LOGGER = Logger.getLogger(MiscUtil.class.getName());

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
    
    /**
     * Attempt to move the file atomically, and if that fails try regular file
     * replacing.
     * 
     * @param from The file to move
     * @param to The target filename, which will be overwritten if it already
     * exists
     * @throws java.io.IOException
     */
    public static void moveFile(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, ATOMIC_MOVE);
        } catch (IOException ex) {
            // Based on the Files.move() docs it may throw an IOException when
            // the target file already exists (implementation specific), so try
            // alternate move on that instead of AtomicMoveNotSupportedException
            LOGGER.info("ATOMIC_MOVE failed: "+ex);
            Files.move(from, to, REPLACE_EXISTING);
        }
    }
    
    /**
     * Returns the StackTrace of the given Throwable as a String.
     * 
     * @param e
     * @return 
     */
    public static String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static final boolean OS_WINDOWS = checkOS("Windows");
    public static final boolean OS_LINUX = checkOS("Linux");
    public static final boolean OS_MAC = checkOS("Mac");
    
    private static boolean checkOS(String check) {
        String os = System.getProperty("os.name");
        return os.startsWith(check);
    }
}
