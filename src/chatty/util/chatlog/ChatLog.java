
package chatty.util.chatlog;

import chatty.Chatty;
import chatty.Helper;
import chatty.User;
import chatty.util.DateTime;
import chatty.util.DateTime.Formatting;
import chatty.util.StringUtil;
import chatty.util.api.ChannelInfo;
import chatty.util.api.StreamInfo.ViewerStats;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.settings.Settings;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Translates chat specific messages into loggable lines and sends them to the
 * LogManager.
 * 
 * @author tduva
 */
public class ChatLog {
    
    private static final Logger LOGGER = Logger.getLogger(ChatLog.class.getName());
    
    private SimpleDateFormat sdf;
    private CustomCommand messageTemplate;
    
    private final Map<String, Compact> compactForChannels;
    
    private final Settings settings;
    private final Path path;
    
    /**
     * Reference to the LogManager. This is null if the log path was invalid and
     * couldn't be created.
     */
    private final LogManager log;
    
    public ChatLog(Settings settings) {
        this.settings = settings;

        path = createPath();
        if (path == null) {
            log = null;
        } else {
            String logSplit = settings.getString("logSplit");
            boolean logSubdirectories = settings.getBoolean("logSubdirectories");
            boolean lockFiles = settings.getBoolean("logLockFiles");
            this.log = new LogManager(path, logSplit, logSubdirectories, lockFiles);
        }
        compactForChannels = new HashMap<>();
        try {
            String timestamp = settings.getString("logTimestamp");
            if (!timestamp.equals("off")) {
                sdf = new SimpleDateFormat(timestamp);
            }
        } catch (IllegalArgumentException ex) {
            sdf = null;
        }
        CustomCommand c = CustomCommand.parse(settings.getString("logMessageTemplate"));
        if (c.hasError()) {
            LOGGER.warning("Error in logMessageTemplate: "+c.getSingleLineError());
            c = CustomCommand.parse(settings.getStringDefault("logMessageTemplate"));
        }
        this.messageTemplate = c;
    }
    
    /**
     * Gets either the default log path, or the custom one from the settings, if
     * not empty.
     * 
     * @return The Path to write the log files to, or null if the path could not
     * be created
     */
    private Path createPath() {
        String pathToUse = Chatty.getUserDataDirectory()+"logs";
        String customPath = settings.getString("logPath");
        if (!customPath.isEmpty()) {
            pathToUse = customPath;
        }
        try {
            return Paths.get(pathToUse);
        } catch (InvalidPathException ex) {
            LOGGER.warning("Invalid path for chatlog: "+pathToUse);
            return null;
        }
    }
    
    /**
     * The base log path. Could be null if creating the path failed.
     * 
     * @return The path, or null if no valid path is set
     */
    public Path getPath() {
        return path;
    }
    
    public void start() {
        if (log != null) {
            log.start();
        }
    }
    
    public void bits(String channel, User user, int amount) {
        if (amount <= 0) {
            return;
        }
        if (isSettingEnabled("logBits") && isChanEnabled(channel)) {
            writeLine(channel, String.format("%sBITS: %s (%d)",
                    timestamp(),
                    user.getRegularDisplayNick(),
                    amount));
        }
    }
    
    public void message(String channel, User user, String message, boolean action) {
        if (isSettingEnabled("logMessage") && isChanEnabled(channel)) {
            Parameters param = messageParam(
                            user,
                            message,
                            action,
                            settings,
                            DateTime.currentTime(sdf));
            String line = messageTemplate.replace(param);
            if (line != null && !line.isEmpty()) {
                writeLine(channel, line);
            }
        }
    }
    
    public static Parameters messageParam(User user, String message, boolean action, Settings settings, String timestamp) {
        Parameters p = Parameters.create("");
        Helper.addUserParameters(user, null, null, p);
        p.put("msg", message);
        if (action) {
            p.put("action", "true");
        }
        p.put("timestamp", timestamp);
        p.putObject("settings", settings);
        return p;
    }

    public void info(String channel, String message) {
        if (isSettingEnabled("logInfo") && isChanEnabled(channel)) {
            writeLine(channel, timestamp()+message);
        }
    }
    
    public void modAction(ModeratorActionData data) {
        if (!Helper.isValidStream(data.stream)) {
            return;
        }
        String channel = Helper.toChannel(data.stream);
        if (isSettingEnabled("logModAction") && isChanEnabled(channel)) {
            writeLine(channel, timestamp()+String.format("MOD_ACTION: %s (%s%s)",
                    data.created_by,
                    data.moderation_action,
                    data.args.isEmpty() ? "" : " "+StringUtil.join(data.args, " ")));
        }
    }

    public void viewerstats(String channel, ViewerStats stats) {
        if (isSettingEnabled("logViewerstats") && isChanEnabled(channel)) {
            if (stats != null && stats.isValid()) {
                writeLine(channel, timestamp()+stats);
                //System.out.println(stats);
            }
        }
    }
    
    public void viewercount(String channel, int viewercount) {
        if (isSettingEnabled("logViewercount") && isChanEnabled(channel)) {
            writeLine(channel, timestamp()+"VIEWERS: "
                    +Helper.formatViewerCount(viewercount));
        }
    }
    
    public void system(String channel, String message) {
        if (isSettingEnabled("logSystem") && isChanEnabled(channel)) {
            writeLine(channel, timestamp()+message);
        }
    }
    
    private void writeLine(String channel, String message) {
        if (log != null) {
            compactClose(channel);
            log.writeLine(channel, message);
        }
    }
    
    public void userBanned(String channel, String nick, long duration,
            String reason, ChannelInfo info) {
        String text = nick;
        if (duration > 0) {
            text += " ("+duration+"s)";
        }
        if (reason != null && !reason.isEmpty()) {
            text += " ["+reason+"]";
        }
        if (info != null) {
            text += " {"+DateTime.formatAccountAge(info.createdAt, Formatting.COMPACT)+"}";
        }
        compact(channel, "BAN", text);
    }
    
    public void msgDeleted(User user, String msg) {
        compact(user.getRoom().getFilename(), "DELETED", user.getRegularDisplayNick()+" ("+msg+")");
    }

    public void compact(String channel, String type, String info) {
        if (isChanEnabled(channel)) {
            if (
                    ((type.equals("MOD") || type.equals("UNMOD")) && isSettingEnabled("logMod"))
                    || ((type.equals("JOIN") || type.equals("PART")) && isSettingEnabled("logJoinPart"))
                    || (type.equals("BAN") && isSettingEnabled("logBan")
                    || (type.equals("DELETED") && isSettingEnabled("logDeleted")))
                ) {
                compactAdd(channel, type, info);
            }
        }
    }
    
    private void compactAdd(String channel, String type, String info) {
        synchronized(compactForChannels) {
            getCompact(channel).add(type, info);
        }
    }
    
    private void compactClose(String channel) {
        synchronized(compactForChannels) {
            if (channel != null) {
                getCompact(channel).close();
            } else {
                for (Compact c : compactForChannels.values()) {
                    c.close();
                }
            }
        }
    }
    
    private Compact getCompact(String channel) {
        Compact c = compactForChannels.get(channel);
        if (c == null) {
            c = new Compact(channel);
            compactForChannels.put(channel, c);
        }
        return c;
    }
    
    private String timestamp() {
        if (sdf == null) {
            return "";
        }
        return DateTime.currentTime(sdf)+" ";
    }
    
    /**
     * Close chatlogging, which writes any remaining lines in the buffer closes
     * all files and stops the thread. This waits for the thread to finish, so
     * it can take some time.
     */
    public void close() {
        if (log != null) {
            compactClose(null);
            log.close();
        }
    }
    
    public void closeChannel(String channel) {
        if (log != null) {
            compactClose(channel);
            log.writeLine(channel, null);
        }
    }
    
    private boolean isChanEnabled(String channel) {
        if (log == null) {
            return false;
        }
        if (channel == null || channel.isEmpty()) {
            return false;
        }
        String mode = settings.getString("logMode");
        if (mode.equals("off")) {
            return false;
        }
        if (mode.equals("always")) {
            return true;
        }
        if (mode.equals("blacklist") && !settings.listContains("logBlacklist", channel)) {
            return true;
        }
        if (mode.equals("whitelist") && settings.listContains("logWhitelist", channel)) {
            return true;
        }
        return false;
    }
    
    private boolean isSettingEnabled(String setting) {
        return settings.getBoolean(setting);
    }

    private class Compact {
        
        private static final String SEPERATOR = ", ";
        private static final int MAX_LENGTH = 10;
        private static final int MAX_TIME = 2000;
        
        private final String channel;
        
        private StringBuilder text;
        private int length;
        private long start;
        private String mode;
        
        public Compact(String channel) {
            this.channel = channel;
        }
        
        /**
         * Prints something in compact mode, meaning that nick events of the
         * same type appear in the same line, for as long as possible.
         *
         * This is mainly used for a compact way of printing
         * joins/parts/mod/unmod.
         *
         * @param type
         * @param user
         */
        protected void add(String type, String info) {
            String seperator = SEPERATOR;
            if (start(type)) {
                // If compact mode has actually been started for this print,
                // print prefix first
                text = new StringBuilder();
                text.append(timestamp());
                text.append(type);
                text.append(": ");
                seperator = "";
            }
            text.append(seperator);
            text.append(info);

            length++;
            // If max number of compact prints happened, close compact mode to
            // start a new line
            if (length >= MAX_LENGTH) {
                close();
            }
        }

        /**
         * Enters compact mode, closes it first if necessary.
         *
         * @param type
         * @return
         */
        private boolean start(String type) {

            // Check if max time has passed, and if so close first
            long timePassed = System.currentTimeMillis() - start;
            if (timePassed > MAX_TIME) {
                close();
            }

            // If this is another type, close first
            if (!type.equals(mode)) {
                close();
            }

            // Only start if not already/still going
            if (mode == null) {
                mode = type;
                start = System.currentTimeMillis();
                length = 0;
                return true;
            }
            return false;
        }

        /**
         * Leaves compact mode (if necessary) and logs the buffered text.
         */
        protected void close() {
            if (mode != null && log != null) {
                log.writeLine(channel, text.toString());
                //System.out.println("Compact: "+text.toString());
                mode = null;
            }
        }
        
    }
    
 
}
