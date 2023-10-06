
package chatty.util;

import chatty.util.settings.Settings;

/**
 *
 * @author tduva
 */
public class MacAwtOptions {

    /**
     * Added in own class, so it doesn't load any AWT stuff yet. The options
     * have to be set before that.
     * 
     * @param settings 
     */
    public static void setMacLookSettings(Settings settings) {
        System.setProperty("apple.awt.application.name", "Chatty");
        if (settings.getBoolean("macScreenMenuBar")) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }
        if (settings.getBoolean("macSystemAppearance")) {
            System.setProperty("apple.awt.application.appearance", "system");
        }
    }
    
}
