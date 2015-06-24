
package chatty.util;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
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
    
    
}
