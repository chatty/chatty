
package chatty.util;

import chatty.Helper;
import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import chatty.util.settings.SettingChangeListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes stream info to a file. Does not request stream info itself (except
 * when a setting changes, for testing), but instead can get stream infos that
 * are requested anyway (so basicially you have to have the channel already open
 * or followed).
 * 
 * Setting in the format (one file per line):
 * streamname filename content
 * 
 * Example:
 * joshimuz title %title
 * joshimuz game %game
 * joshimuz status %title %game
 * 
 * 
 * Since info is written as it comes in (streamStatus()), having more than one
 * stream written to the same file doesn't really make much sense, because you
 * can't guarantee a precedence. Maybe polling info would work better for that
 * case, but then info may not be written immediately as it comes in. But
 * usually one stream -> one file should be enough anyway. For that reason, for
 * now only one file/[online/offline] combination is allowed.
 * 
 * @author tduva
 */
public class StreamStatusWriter implements SettingChangeListener {
    
    private static final Logger LOGGER = Logger.getLogger(StreamStatusWriter.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final Pattern p = Pattern.compile("(\\w+) ([0-9_A-Za-z\\.]+)( online| offline)?( [^\\n]+)?");
    
    private boolean enabled;
    
    /**
     * Using Set so only one file/[online/offline] combination is allowed.
     */
    private final Set<Item> items = new HashSet<>();
    
    /**
     * Save list of streams used in the items, so they can be pulled for
     * testing.
     */
    private final Set<String> streams = new HashSet<>();
    
    /**
     * The base path for writing the files.
     */
    private final Path path;
    private final TwitchApi api;
    
    /**
     * Creates a new instance.
     * 
     * @param path The path to use as base for the file to write the info to
     * @param api A reference to the TwitchApi to request info from for testing
     */
    public StreamStatusWriter(Path path, TwitchApi api) {
        this.path = path;
        this.api = api;
    }
    
    /**
     * Enable or disable writing the stream info altogether.
     * 
     * @param enabled 
     */
    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Sets the current setting to a new one, parsing the information and
     * turning it into easier to use Item objects.
     * 
     * @param setting 
     */
    public synchronized void setSetting(String setting) {
        items.clear();
        streams.clear();
        Matcher m = p.matcher(setting);
        while (m.find()) {
            String stream = StringUtil.toLowerCase(m.group(1));
            String file = m.group(2);
            String online = StringUtil.trim(m.group(3));
            boolean forOnline = true;
            if ("offline".equals(online)) {
                forOnline = false;
            }
            String content = StringUtil.trim(m.group(4));
            content = content == null ? "" : content;
            items.add(new Item(stream, file, content, forOnline));
            streams.add(stream);
        }
        //System.out.println(items);
    }
    
    /**
     * If stream info writing is enabled altogether, request every stream info
     * once to test the info writing.
     */
    public synchronized void test() {
        if (enabled) {
            for (String stream : streams) {
                streamStatus(api.getStreamInfo(stream, null));
            }
        }
    }
    
    /**
     * Writes the given stream info if it is online and an Item exists for it.
     * 
     * @param info 
     */
    public synchronized void streamStatus(StreamInfo info) {
        if (enabled && info.isValid()) {
            for (Item item : items) {
                checkItemAndWrite(info, item);
            }
        }
    }
    
    /**
     * Check if the given StreamInfo matches the Item's requirements, and if so,
     * then write it.
     * 
     * @param info The StreamInfo to get the info from
     * @param item The Item to check if the StreamInfo should be used
     * @return true if a file was written, false if the requirements didn't
     * match
     */
    private boolean checkItemAndWrite(StreamInfo info, Item item) {
        if (item.stream.equalsIgnoreCase(info.getStream())) {
            if ((item.forOnline && info.getOnline())
                    || !item.forOnline && !info.getOnline()) {
                write(item.file, makeContent(info, item.content));
                return true;
            }
        }
        return false;
    }
    
    /**
     * Makes the text that will be written to the file.
     * 
     * @param info The StreamInfo to replace the parameters with
     * @param content The content, which can contain parameters to be replaced
     * @return The content, with the parameter replaced
     */
    private String makeContent(StreamInfo info, String content) {
        content = content.replace("%title", info.getTitle());
        content = content.replace("%game", info.getGame());
        content = content.replace("%viewersf", Helper.formatViewerCount(info.getViewers()));
        content = content.replace("%followersf", Helper.formatViewerCount(info.getFollowerCount()));
        content = content.replace("%subscribersf", Helper.formatViewerCount(info.getSubscriberCount()));
        content = content.replace("%viewers", String.valueOf(info.getViewers()));
        content = content.replace("%followers", String.valueOf(info.getFollowerCount()));
        content = content.replace("%subscribers", String.valueOf(info.getSubscriberCount()));
        return content;
    }
    
    /**
     * Write the given content to a file.
     * 
     * @param fileName
     * @param content 
     */
    private void write(String fileName, String content) {
        Path file = path.resolve(fileName);
        try {
            Files.createDirectories(path);
            try (BufferedWriter writer = Files.newBufferedWriter(file, CHARSET)) {
                writer.write(content);
            }
        } catch (IOException ex) {
            LOGGER.warning("Error writing status: " + ex);
        }
    }

    /**
     * Listen to setting changes and update accordingly.
     * 
     * @param setting
     * @param type
     * @param value 
     */
    @Override
    public void settingChanged(String setting, int type, Object value) {
        if (setting.equals("statusWriter")) {
            setSetting((String) value);
            test();
        } else if (setting.equals("enableStatusWriter")) {
            enabled = (Boolean)value;
            test();
        }
    }
    
    private static class Item {
        
        public final String stream;
        public final String file;
        public final String content;
        public final boolean forOnline;
        
        public Item(String stream, String file, String content, boolean forOnline) {
            this.stream = stream;
            this.file = file;
            this.content = content;
            this.forOnline = forOnline;
        }
        
        @Override
        public String toString() {
            return stream+"/"+file+"/"+forOnline+"/"+content;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 17 * hash + Objects.hashCode(this.file);
            hash = 17 * hash + (this.forOnline ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Item other = (Item) obj;
            if (!Objects.equals(this.file, other.file)) {
                return false;
            }
            if (this.forOnline != other.forOnline) {
                return false;
            }
            return true;
        }
        
    }
    
}
