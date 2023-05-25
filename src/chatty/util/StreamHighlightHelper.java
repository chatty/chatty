
package chatty.util;

import chatty.Chatty;
import chatty.Chatty.PathType;
import chatty.Helper;
import chatty.Logging;
import chatty.User;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Settings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.HashMap;
import java.util.Map;
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
    
    private final Map<String, ElapsedTime> highlightLastAdded = new HashMap<>();
    
    public StreamHighlightHelper(Settings settings, TwitchApi api) {
        this.settings = settings;
        this.api = api;
        this.file = Chatty.getPathCreate(PathType.EXPORT).resolve(FILE_NAME);
    }
    
    /**
     * Check if the given user and message should trigger writing a stream
     * highlight.
     *
     * @param user The user who send the message
     * @param line The content of the message
     * @return A response to either echo or send to the channel
     */
    public String modCommand(User user, String line, MsgTags tags) {
        //---------
        // Channel
        //---------
        String channel = user.getOwnerChannel();
        String settingChannel = Helper.toChannel(settings.getString("streamHighlightChannel"));
        if (settingChannel == null || !settingChannel.equalsIgnoreCase(channel)) {
            return null;
        }
        //---------
        // Command
        //---------
        String command = StringUtil.toLowerCase(settings.getString("streamHighlightCommand"));
        if (command == null || command.trim().isEmpty()) {
            return null;
        }
        String lcLine = StringUtil.toLowerCase(line);
        if (!lcLine.startsWith(command+" ") && !lcLine.equals(command)) {
            return null;
        }
        //--------
        // Access
        //--------
        String match = settings.getString("streamHighlightMatch");
        // An empty HighlightItem would match everything, so check before
        if (match == null || match.trim().isEmpty()) {
            return null;
        }
        HighlightItem item = new Highlighter.HighlightItem(match);
        if (!item.matches(HighlightItem.Type.REGULAR, line, user, null, tags)) {
            return null;
        }
        //---------------------------------------
        // All challenges successfully completed
        //---------------------------------------
        String comment = line.substring(command.length()).trim();
        return addHighlight(channel, comment, user);
    }
    
    /**
     * Adds a highlight for the given channel with the given comment.
     * 
     * Thread-saftey: It should be safe to call this from several threads.
     * Instance variables and writing to the file is synchronized on this.
     * 
     * @param channel The channel to add the highlight for
     * @param comment The comment to add (can be null or empty for no comment)
     * @param chatUser The user that executed the chat command (or null)
     * @return A textual response to adding the highlight
     */
    public String addHighlight(String channel, String comment, User chatUser) {
        if (channel == null || channel.isEmpty() || !Helper.isRegularChannel(channel)) {
            return "Failed adding stream highlight (no channel).";
        }
        if (!checkCooldown(channel)) {
            return "Stream highlight not added (cooldown).";
        }
        
        String rawComment = comment;
        if (comment != null && chatUser != null) {
            comment = "["+chatUser.getDisplayNick()+"] "+comment;
        }
        
        boolean createdMarker = addStreamMarker(channel, comment);
        String createdMarkerText = createdMarker ? " (Created Stream Marker)" : "";
        
        // Get StreamInfo
        StreamInfo streamInfo = api.getStreamInfo(Helper.toStream(channel), null);
        String streamTime = "Stream Time N/A";
        if (streamInfo.isValid() && streamInfo.getOnline()) {
            streamTime = DateTime.ago(streamInfo.getTimeStarted());
        }
        
        if (comment == null) {
            comment = "";
        }
        comment = comment.trim();
        
        // Make the line to add to the file
        String line = String.format("%s %s [%s]%s%s",
                DateTime.fullDateTime(),
                channel,
                streamTime,
                !comment.isEmpty() ? " "+comment : "",
                createdMarkerText);
        
        String shortComment = "";
        if (!comment.isEmpty()) {
            shortComment = "(" + StringUtil.shortenTo(comment, 30) + ")";
        }
        
        // Parameters
        Parameters params = Parameters.create("");
        params.put("added", "highlight" + (createdMarker ? "/marker" : ""));
        params.put("addedmarker", createdMarker ? "true" : "");
        params.put("chan", channel);
        params.put("uptime", streamTime);
        params.put("timestamp", DateTime.fullDateTime());
        params.put("comment", shortComment);
        params.put("rawcomment", rawComment);
        params.put("fullcomment", comment.isEmpty() ? "" : "(" + comment + ")");
        if (chatUser != null) {
            params.put("chatuser", chatUser.getRegularDisplayNick());
            params.putObject("user", chatUser);
        }
        if (streamInfo.isValid()) {
            params.putObject("streamInfo", streamInfo);
        }
        
        if (settings.getBoolean("streamHighlightCustomEnabled")) {
            CustomCommand cc = CustomCommand.parse(settings.getString("streamHighlightCustom"));
            if (cc.hasError()) {
                /**
                 * Fail in case of error (don't fall back to default because the
                 * user wouldn't immediately notice what is written to file).
                 */
                return "Failed adding stream highlight (error in custom format)."+createdMarkerText;
            }
            String ccResult = cc.replace(params);
            if (StringUtil.isNullOrEmpty(ccResult)) {
                return "Failed adding stream highlight (empty)."+createdMarkerText;
            }
            line = ccResult;
        }
        
        synchronized(this) {
            // Add seperator if probably new stream
            if (streamInfo.getTimeStarted() != lastStreamStartWritten
                    && settings.getBoolean("streamHighlightExtra")) {
                addToFile("-");
            }

            // Add to file and make textual response
            boolean success = addToFile(line);
            if (success) {
                lastStreamStartWritten = streamInfo.getTimeStarted();
                cooldownSetAdded(channel);
                
                // Command
                String template = settings.getString("streamHighlightResponseMsg");
                CustomCommand cc = CustomCommand.parse(template);
                String result = null;
                if (!cc.hasError()) {
                    result = cc.replace(params);
                }
                if (StringUtil.isNullOrEmpty(StringUtil.trim(result))) {
                    /**
                     * Use default in case of error or empty command (the user
                     * should notice the result in chat, so it should be fine to
                     * fall back to default).
                     */
                    result = CustomCommand.parse(settings.getStringDefault("streamHighlightResponseMsg")).replace(params);
                }
                return result;
            }
            return "Failed adding stream highlight (write error)."+createdMarkerText;
        }
    }
    
    private boolean checkCooldown(String channel) {
        int cooldownSeconds = settings.getInt("streamHighlightCooldown");
        if (cooldownSeconds <= 0) {
            return true;
        }
        if (!highlightLastAdded.containsKey(channel)) {
            highlightLastAdded.put(channel, new ElapsedTime());
        }
        return highlightLastAdded.get(channel).secondsElapsedSync(cooldownSeconds);
    }
    
    private void cooldownSetAdded(String channel) {
        if (settings.getInt("streamHighlightCooldown") <= 0
                || !highlightLastAdded.containsKey(channel)) {
            return;
        }
        highlightLastAdded.get(channel).setSync();
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
        if (MiscUtil.openFile(file.toFile(), null)) {
            return "Opened stream highlights file in default application.";
        }
        return "Error opening stream highlights file.";
    }
    
    private boolean addStreamMarker(String channel, String description) {
        if (settings.getBoolean("streamHighlightMarker")) {
            api.createStreamMarker(Helper.toStream(channel), description, e -> {
                if (e != null) {
                    String errorMessage = String.format("Error adding stream marker for %s (%s)",
                                    channel, e);
                    LOGGER.log(Logging.USERINFO, errorMessage);
                    if (settings.getBoolean("streamHighlightExtra")) {
                        synchronized (this) {
                            addToFile(DateTime.fullDateTime() + " " + errorMessage);
                        }
                    }
                }
            });
            return true;
        }
        return false;
    }
    
}
