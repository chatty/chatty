
package chatty.gui.components.menus;

import chatty.Helper;
import chatty.gui.components.help.About;
import chatty.util.settings.Settings;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;

/**
 * Provides some useful functions for several different context menus.
 * 
 * @author tduva
 */
public class ContextMenuHelper {
    
    protected static final ImageIcon ICON_SPACING = new ImageIcon(ContextMenuHelper.class.getResource("transparent-10x1.png"));
    protected static final ImageIcon ICON_WEB = new ImageIcon(About.class.getResource("go-web.png"));
    protected static final ImageIcon ICON_IMAGE = new ImageIcon(ContextMenuHelper.class.getResource("image-icon.png"));
    protected static final ImageIcon ICON_COMMANDLINE = new ImageIcon(ContextMenuHelper.class.getResource("commandline.png"));
    protected static final ImageIcon ICON_COPY = new ImageIcon(ContextMenuHelper.class.getResource("edit-copy.png"));

    public static boolean enableLivestreamer = true;
    public static String livestreamerQualities;
    public static Settings settings;
    
    /**
     * Pattern for finding the qualities in the Livestreamer qualities setting
     */
    private static final Pattern LIVESTREAMER_PATTERN
            = Pattern.compile("(\\|)|(?:\\{(.+?)\\})|([^,\\s]+)");
    
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
        String streamSubmenu = "Twitch Stream";
        String miscSubmenu = "Miscellaneous";
        m.setSubMenuIcon(streamSubmenu, ICON_SPACING);
        m.addItem("stream", "Normal", streamSubmenu);
        m.addItem("streamPopout", "Popout", streamSubmenu);
        m.addSeparator(streamSubmenu);
        m.addItem("streamsMultitwitchtv", "Multitwitch.tv", streamSubmenu);
        m.addItem("streamsSpeedruntv", "Speedrun.tv", streamSubmenu);
        m.addItem("streamsKadgar", "Kadgar.net", streamSubmenu);
        addLivestreamerOptions(m);
        if (join) {
            m.addSeparator();
            m.addItem("join", "Join " + count + "channel" + s);
            m.addSeparator();
            m.addItem("hostchannel", "Host Channel", miscSubmenu);
            m.addSeparator(miscSubmenu);
            m.addItem("copy", "Copy Stream Name", miscSubmenu);
            m.addSeparator(miscSubmenu);
            m.addItem("follow", "Follow Channel", miscSubmenu);
            m.addItem("unfollow", "Unfollow Channel", miscSubmenu);
        }
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.STREAMS, m);
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
            m.setSubMenuIcon(livestreamerMenu, ICON_COMMANDLINE);
            
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
                    if (match.charAt(0) == '{')
                    	match = livestreamerQualities.substring(matcher.start()+1, matcher.end()-1);
                    m.addItem("livestreamerQ"+match, match, livestreamerMenu);
                    sep = false;
                }
            }
        }
    }
    
    public static void addIgnore(ContextMenu m, String name, String submenu, boolean whisper) {
        if (settings != null) {
            String setting = whisper ? "ignoredUsersWhisper" : "ignoredUsers";
            String label = whisper ? "(whisper)" : "(chat)";
            String item = whisper ? "Whisper" : "";
            if (settings.listContains(setting, name)) {
                m.addItem("unignore"+item, "Unignore "+label, submenu);
            } else {
                m.addItem("ignore"+item, "Ignore "+label, submenu);
            }
        }
    }
    
}
