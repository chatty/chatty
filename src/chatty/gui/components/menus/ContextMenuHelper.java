
package chatty.gui.components.menus;

import chatty.gui.components.help.About;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    
    // This shouldn't usually be used before it is set, but just to be sure set
    // it to an empty String instead of null by default
    public static String livestreamerQualities = "";
    public static Settings settings;
    
    /**
     * Pattern for finding the qualities in the Livestreamer qualities setting
     */
    private static final Pattern LIVESTREAMER_PATTERN
            = Pattern.compile("(\\|)|(?:\\{(?<name>[^:}]+:)?(?<qualities>.+?)\\})|(?<basic>[^,\\s]+)");
    
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
        String streamSubmenu = "Twitch Stream";
        String miscSubmenu = Language.getString("channelCm.menu.misc");
        m.setSubMenuIcon(streamSubmenu, ICON_SPACING);
        m.addItem("stream", "Normal", streamSubmenu);
        m.addItem("streamPopout", "Popout", streamSubmenu);
        m.addItem("streamChat", "Chat", streamSubmenu);
        m.addSeparator(streamSubmenu);
        m.addItem("streamsMultitwitchtv", "Multitwitch.tv", streamSubmenu);
        m.addItem("streamsSpeedruntv", "Speedrun.tv", streamSubmenu);
        m.addItem("streamsKadgar", "Kadgar.net", streamSubmenu);
        addLivestreamerOptions(m);
        if (join) {
            m.addSeparator();
            m.addItem("join", Language.getString("channelCm.join", numStreams));
            m.addSeparator();
            m.addItem("hostchannel", Language.getString("channelCm.hostChannel"), miscSubmenu);
            m.addSeparator(miscSubmenu);
            m.addItem("copy", Language.getString("channelCm.copyStreamname"), miscSubmenu);
            m.addSeparator(miscSubmenu);
            m.addItem("favoriteChannel", Language.getString("channelCm.favorite"), miscSubmenu);
            m.addItem("unfavoriteChannel", Language.getString("channelCm.unfavorite"), miscSubmenu);
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
            String livestreamerMenu = "Streamlink";
            m.setSubMenuIcon(livestreamerMenu, ICON_COMMANDLINE);
            List<Quality> qualities = parseLivestreamerQualities(livestreamerQualities);
            for (Quality q : qualities) {
                if (q == null) {
                    m.addSeparator(livestreamerMenu);
                } else {
                    m.addItem("livestreamerQ"+q.qualities, q.displayName, livestreamerMenu);
                }
            }
        }
    }

    static List<Quality> parseLivestreamerQualities(String input) {
        List<Quality> result = new ArrayList<>();
        Matcher matcher = LIVESTREAMER_PATTERN.matcher(input);
        boolean sep = false;
        while (matcher.find()) {
            if (matcher.group().equals("|")) {
                // Separator found, add before next quality
                sep = true;
            } else {
                // Quality found
                if (sep) {
                    result.add(null);
                }
                String match;
                String displayName;

                // Quality
                if (matcher.group("qualities") != null) {
                    // Advanced format { }
                    match = matcher.group("qualities");
                } else {
                    // Basic format
                    match = matcher.group();
                }

                // Display name
                if (matcher.group("name") != null) {
                    displayName = matcher.group("name").replace(":", "");
                } else {
                    displayName = match;
                }

                result.add(new Quality(displayName, match));
                sep = false;
            }
        }
        return result;
    }
    
    static class Quality {
        String displayName;
        String qualities;
        
        Quality(String displayName, String qualities) {
            this.displayName = displayName;
            this.qualities = qualities;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Quality other = (Quality) obj;
            if (!Objects.equals(this.displayName, other.displayName)) {
                return false;
            }
            if (!Objects.equals(this.qualities, other.qualities)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.displayName);
            hash = 83 * hash + Objects.hashCode(this.qualities);
            return hash;
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
