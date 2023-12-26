
package chatty;

import chatty.Chatty.PathType;
import chatty.gui.laf.LaFChanger;
import chatty.util.RingBuffer;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.*;

/**
 *
 * @author tduva
 */
public class Logging {
    
    public static final Level USERINFO = new UserinfoLevel();
    
    private static final String LOG_FILE = Chatty.getPath(PathType.DEBUG).toString()+File.separator+"debug%g.log";
    private static final String LOG_FILE_IRC = Chatty.getPath(PathType.DEBUG).toString()+File.separator+"debug_irc%g.log";
    
    /**
     * Maximum log file size in bytes.
     */
    private static final int MAX_LOG_SIZE = 1024*1024*2;
    
    /**
     * How many log files to rotate through when the file reaches maximum size.
     */
    private static final int MAX_LOG_FILES = 3;
    
    private final RingBuffer<LogRecord> lastMessages = new RingBuffer<>(8);
    
    private static TwitchClient client;
    
    public Logging(final TwitchClient client) {
        Logging.client = client;
        
        createLogDir();
        
        // Remove default handlers
        LogManager.getLogManager().reset();

        // Add console handler with custom formatter
        ConsoleHandler c = new ConsoleHandler();
        c.setFormatter(new TextFormatter());
        c.setFilter(new Filter() {

            @Override
            public boolean isLoggable(LogRecord record) {
                return record.getLevel() != USERINFO;
            }
        });
        Logger.getLogger("").addHandler(c);
        
        // Add file handler with custom formatter
        try {
            FileHandler file = new FileHandler(LOG_FILE,MAX_LOG_SIZE,MAX_LOG_FILES,true);
            file.setFormatter(new TextFormatter());
            file.setLevel(Level.INFO);
            file.setFilter(new FileFilter());
            Logger.getLogger("").addHandler(file);
        } catch (IOException | SecurityException ex) {
            fileWarning(ex);
        }
        
        // Add handler for the GUI (display errors, log into debug window)
        Handler guiHandler = new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() != USERINFO) {
                    client.debug(simpleFormatMessage(record));
                    // WebsocketClient/WebsocketManager
                    if (record.getMessage().startsWith("[FFZ-WS]")) {
                        client.debugFFZ(record.getMessage());
                    }
                    if (record.getMessage().startsWith("[PubSub]")) {
                        client.debugPubSub(record.getMessage());
                    }
                    if (record.getMessage().startsWith("[EventSub]")
                            || record.getMessage().contains("https://api.twitch.tv/helix/eventsub/subscriptions")) {
                        client.debugEventSub(record.getMessage());
                    }
                }
                if (record.getLevel() == Level.SEVERE) {
                    if (client.g != null) {
                        String msg = record.getMessage();
                        boolean flatLafError = msg.startsWith("FlatLaf: Failed to parse:");
                        if (flatLafError) {
                            LaFChanger.loggedFlatLookAndFeelError(msg+" "+record.getThrown().getLocalizedMessage());
                        }
                        else {
                            client.g.error(record, lastMessages.getItems());
                        }
                    }
                } else if (record.getLevel() == USERINFO) {
                    client.warning(record.getMessage());
                } else {
                    lastMessages.add(record);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        guiHandler.setLevel(Level.INFO);
        Logger.getLogger("").addHandler(guiHandler);
    }
    
    /**
     * Simple 1-line text formatter.
     */
    static class TextFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            return String.format("[%1$tF %1$tT/%1$tL %5$s] %2$s%6$s [%3$s/%4$s]\n",
                new Date(record.getMillis()),
                simpleFormatMessage(record),
                record.getSourceClassName(),
                record.getSourceMethodName(),
                record.getLevel().getName(),
                getStacktraceForLogging(record.getThrown()));
        }
        
    }
    
    public static String formatRecordCompact(LogRecord record) {
        return String.format("[%1$tT/%1$tL] %2$s%5$s [%3$s/%4$s]\n",
                new Date(record.getMillis()),
                simpleFormatMessage(record),
                record.getSourceClassName(),
                record.getSourceMethodName(),
                getStacktraceForLogging(record.getThrown()));
    }
    
    private static String simpleFormatMessage(LogRecord record) {
        // Probably just third-party code has records with parameters currently
        Object[] params = record.getParameters();
        if (params == null || params.length == 0) {
            return record.getMessage();
        }
        try {
            return MessageFormat.format(record.getMessage(), params);
        }
        catch (Exception ex) {
            return record.getMessage();
        }
    }
    
    public static String getStacktrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static String getStacktraceForLogging(Throwable t) {
        if (t != null) {
            try {
                return "\n:"+getStacktrace(t);
            } catch (Exception ex) {
                return "\n:Error getting stacktrace";
            }
        }
        return "";
    }
    
    static class FileFilter implements Filter {

        @Override
        public boolean isLoggable(LogRecord record) {
            if (record.getSourceClassName().equals("chatty.util.TwitchApiRequest")) {
                if (record.getLevel() == Level.INFO) {
                    return false;
                }
            }
            return true;
        }
        
    }
    
    private static class UserinfoLevel extends Level {
        
        private UserinfoLevel() {
            super("Userinfo", 950);
        }
        
    }
    
    public static FileHandler getIrcFileHandler() {
        createLogDir();
        try {
            FileHandler file = new FileHandler(LOG_FILE_IRC,MAX_LOG_SIZE*4,2,true);
            file.setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tF %1$tT/%1$tL] %2$s\n",
                            new Date(record.getMillis()),
                            record.getMessage());
                }
            });
            file.setLevel(Level.INFO);
            return file;
        } catch (IOException | SecurityException ex) {
            fileWarning(ex);
        }
        return null;
    }
    
    public static void createLogDir() {
        try {
            Files.createDirectories(Chatty.getPath(PathType.DEBUG));
        } catch (IOException ex) {
            fileWarning(ex);
        }
    }
    
    private static void fileWarning(Throwable ex) {
        if (client != null) {
            client.warning(String.format("Failed creating log files. Check that %s can be written to. (%s)",
                    Chatty.getPath(PathType.DEBUG),
                    ex));
        }
    }
    
}
