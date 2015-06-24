
package chatty.gui.components.menus;

import chatty.Helper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides some useful functions for several different context menus.
 * 
 * @author tduva
 */
public class ContextMenuHelper {

    public static boolean enableLivestreamer = true;
    public static String livestreamerQualities;
    public static String userCustomCommands;
    public static String channelCustomCommands;
    
    /**
     * Pattern for finding the qualities in the Livestreamer qualities setting
     */
    private static final Pattern LIVESTREAMER_PATTERN
            = Pattern.compile("(\\|)|([^,\\s]+)");
    
    /**
     * Pattern for finding commands/seperators in the custom commands setting
     */
    private static final Pattern CUSTOM_COMMANDS_PATTERN
            = Pattern.compile("(\\|)|(?:/?/?([^,\\s]+))");
    
    /**
     * Menu name to use for custom commands submenu
     */
    private static final String CUSTOM_COMMANDS_SUBMENU = "More..";
    
    /**
     * Adds menu items to the given ContextMenu that provide ways to do stream
     * related stuff.
     * 
     * @param m The menu to add the items to
     * @param numStreams How many streams this is for (labels the menu items
     * accordingly)
     * @see addStreamsOptions(ContextMenu, int, boolean)
     */
    protected static void addStreamsOptions(ContextMenu m, int numStreams) {
        addStreamsOptions(m, numStreams, true);
    }
    
    /**
     * Adds menu items to the given ContextMenu that provide ways to do stream
     * related stuff.
     * 
     * @param m The menu to add the items to
     * @param numStreams How many streams this is for (labels the menu items
     * accordingly)
     * @param join Whether to add a join channel menu item
     */
    protected static void addStreamsOptions(ContextMenu m, int numStreams, boolean join) {
        
        String count = "";
        String s = "";
        if (numStreams > 1) {
            s = "s";
            count = String.valueOf(numStreams)+" ";
        }
        String streamSubmenu = "Twitch Stream" + s;
        m.addItem("stream", "Normal", streamSubmenu);
        m.addItem("streamPopout", "Popout", streamSubmenu);
        m.addItem("streamsMultitwitchtv", "Multitwitch.tv", streamSubmenu);
        m.addItem("streamsSpeedruntv", "Speedrun.tv", streamSubmenu);
        m.addItem("streamsKadgar", "Kadgar.net", streamSubmenu);
        addLivestreamerOptions(m);
        m.addItem("profile", "Twitch Profile" + s);
        if (join) {
            m.addSeparator();
            m.addItem("join", "Join " + count + "channel" + s);
        }
    }
    
    /**
     * Parses the livestreamer qualities setting and adds the defined qualities
     * as menu items.
     * 
     * @param m 
     */
    public static void addLivestreamerOptions(ContextMenu m) {
        if (enableLivestreamer) {
            String livestreamerMenu = "Livestreamer";
            
            Matcher matcher = LIVESTREAMER_PATTERN.matcher(livestreamerQualities);
            boolean sep = false;
            while (matcher.find()) {
                String match = matcher.group();
                if (match.equals("|")) {
                    sep = true;
                } else {
                    if (sep) {
                        m.addSeparator(livestreamerMenu);
                    }
                    m.addItem("livestreamerQ"+match, match, livestreamerMenu);
                    sep = false;
                }
            }
        }
    }
    
    /**
     * Add custom commands intended for the User Context Menu.
     * 
     * @param m The ContextMenu to add the menu items to
     */
    public static void addCustomUserCommands(ContextMenu m) {
        if (userCustomCommands != null) {
            addCustomCommands(userCustomCommands, m);
        }
    }
    
    /**
     * Add custom commands intended for the Channel Context Menu.
     * 
     * @param m The ContextMenu to add the menu items to
     */
    public static void addCustomChannelCommands(ContextMenu m) {
        if (channelCustomCommands != null) {
            addCustomCommands(channelCustomCommands, m);
        }
    }
    
    /**
     * Parses the given commands setting and adds menu items to the given
     * ContextMenu.
     * 
     * @param commands The String containing the commands and stuff
     * @param m The context menu to add the menu items to
     */
    public static void addCustomCommands(String commands, ContextMenu m) {
        Matcher matcher = CUSTOM_COMMANDS_PATTERN.matcher(commands);
        boolean sep = false;
        while (matcher.find()) {
            String match = matcher.group();
            if (match.equals("|")) {
                sep = true;
            } else {
                String command = matcher.group(2);
                String submenu = null;
                if (match.startsWith("//")) {
                    submenu = CUSTOM_COMMANDS_SUBMENU;
                }
                if (sep) {
                    m.addSeparator(submenu);
                }
                m.addItem("command" + command, Helper.replaceUnderscoreWithSpace(command),
                        submenu);
                sep = false;
            }

        }
    }
    
}
