
package chatty.util;

import chatty.Chatty;
import chatty.Helper;
import chatty.User;
import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import chatty.util.settings.Settings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.logging.Logger;

/**
 * Writes currenty stream time to a file to help with creation of Stream
 * Highlights.
 *
 * @author tduva
 */
public class StreamHighlightHelper {
    
    private static final Logger LOGGER = Logger.getLogger(StreamHighlightHelper.class.getName());
    
    private final static Charset CHARSET = Charset.forName("UTF-8");
    
    private static final String FILE_NAME = "stream_highlights.txt";
    
    private final Settings settings;
    private final TwitchApi api;
    private final Path file;
    
    private long lastStreamStartWritten = -1;
    
    public StreamHighlightHelper(Settings settings, TwitchApi api) {
        this.settings = settings;
        this.api = api;
        // Chatty.getExportDirectory() should create the directory
        this.file = Paths.get(Chatty.getExportDirectory()+FILE_NAME);
    }
    
    /**
     * Check if the given user and message should trigger writing a stream
     * highlight.
     *
     * @param user The user who send the message
     * @param line The content of the message
     * @return A response to either echo or send to the channel
     */
    public String modCommand(User user, String line) {
        String channel = user.getOwnerChannel();
        String settingChannel = Helper.toChannel(settings.getString("streamHighlightChannel"));
        String command = StringUtil.toLowerCase(settings.getString("streamHighlightCommand"));
        if (command != null && !command.isEmpty()
                && settingChannel != null && settingChannel.equalsIgnoreCase(channel)
                && user.hasChannelModeratorRights()) {
            if (StringUtil.toLowerCase(line).startsWith(command)) {
                String comment = line.substring(command.length());
                //System.out.println(comment);
                return addHighlight(channel, "["+user.getDisplayNick()+"]"+comment);
            }
        }
        return null;
    }
    
    /**
     * Adds a highlight for the given channel with the given comment.
     * 
     * Thread-saftey: It should be safe to call this from several threads.
     * Instance variables and writing to the file is synchronized on this.
     * 
     * @param channel The channel to add the highlight for
     * @param comment The comment to add (can be null or empty for no comment)
     * @return A textual response to adding the highlight
     */
    public String addHighlight(String channel, String comment) {
        if (channel == null || channel.isEmpty() || !Helper.isRegularChannel(channel)) {
            return "Failed adding stream highlight (no channel).";
        }
        
        // Get StreamInfo
        StreamInfo streamInfo = api.getStreamInfo(Helper.toStream(channel), null);
        String streamTime = "Stream Time N/A";
        if (streamInfo.isValid() && streamInfo.getOnline()) {
            streamTime = DateTime.ago(streamInfo.getTimeStarted());
        }
        
        if (comment == null) {
            comment = "";
        }
        
        // Make the line to add to the file
        String line = String.format("%s %s [%s] %s",
                DateTime.fullDateTime(),
                channel,
                streamTime,
                comment);
        
        synchronized(this) {
            // Add seperator if probably new stream
            if (streamInfo.getTimeStarted() != lastStreamStartWritten) {
                addToFile("-");
            }

            // Add to file and make textual response
            boolean success = addToFile(line);
            if (success) {
                lastStreamStartWritten = streamInfo.getTimeStarted();
                String shortComment = "";
                if (!comment.isEmpty()) {
                    shortComment = "(" + StringUtil.shortenTo(comment, 30) + ")";
                }
                return "Added stream highlight for " + channel + " [" + streamTime + "] " + shortComment;
            }
            return "Failed adding stream highlight (write error).";
        }
    }
    
    /**
     * Write the given line to the stream highlights file.
     * 
     * @param line The line to write
     * @return true if writing succeeded, false otherwise
     */
    private boolean addToFile(String line) {
        try (BufferedWriter w = Files.newBufferedWriter(file, CHARSET, CREATE, WRITE, APPEND)) {
            w.append(line);
            w.newLine();
        } catch (IOException ex) {
            LOGGER.warning("Failed to write stream highlights: "+ex);
            return false;
        }
        return true;
    }
    
    /**
     * Open the stream highlights file, if present.
     * 
     * @return The result of trying to open the file in a textual form
     */
    public String openFile() {
        if (!file.toFile().exists()) {
            return "Error opening stream highlights file (not present).";
        }
        if (MiscUtil.openFolder(file.toFile(), null)) {
            return "Opened stream highlights file in default application.";
        }
        return "Error opening stream highlights file.";
    }
    
}
