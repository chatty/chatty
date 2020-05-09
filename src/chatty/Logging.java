
package chatty;

import chatty.util.RingBuffer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.*;

/**
 *
 * @author tduva
 */
public class Logging {
    
    public static final Level USERINFO = new UserinfoLevel();
    
    private static final String LOG_FILE = Chatty.getDebugLogDirectory()+"debug%g.log";
    private static final String LOG_FILE_SESSION = Chatty.getDebugLogDirectory()+"debug_session.log";
    private static final String LOG_FILE_IRC = Chatty.getDebugLogDirectory()+"debug_irc%g.log";
    
    /**
     * Maximum log file size in bytes.
     */
    private static final int MAX_LOG_SIZE = 1024*1024*2;
    
    private static final int MAX_SESSION_LOG_SIZE = 1024*1024*20;
    
    /**
     * How many log files to rotate through when the file reaches maximum size.
     */
    private static final int MAX_LOG_FILES = 3;
    
    private final RingBuffer<LogRecord> lastMessages = new RingBuffer<>(8);
    
    public Logging(final TwitchClient client) {
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
            
            FileHandler fileSession = new FileHandler(LOG_FILE_SESSION, MAX_SESSION_LOG_SIZE, 1);
            fileSession.setFormatter(new TextFormatter());
            fileSession.setLevel(Level.INFO);
            fileSession.setFilter(new FileFilter());
            Logger.getLogger("").addHandler(fileSession);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(Logging.class.getName()).log(Level.WARNING, null, ex);
        }
        
        // Add handler for the GUI (display errors, log into debug window)
        Handler guiHandler = new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() != USERINFO) {
                    client.debug(record.getMessage());
                    // WebsocketClient/WebsocketManager
                    if (record.getMessage().startsWith("[FFZ-WS]")) {
                        client.debugFFZ(record.getMessage());
                    }
                    if (record.getMessage().startsWith("[PubSub]")) {
                        client.debugPubSub(record.getMessage());
                    }
                }
                if (record.getLevel() == Level.SEVERE) {
                    if (client.g != null) {
                        client.g.error(record, lastMessages.getItems());
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
            return formatRecord(record);
        }
        
    }
    
    public static String formatRecord(LogRecord record) {
        return String.format("[%1$tF %1$tT/%1$tL %5$s] %2$s%6$s [%3$s/%4$s]\n",
                new Date(record.getMillis()),
                record.getMessage(),
                record.getSourceClassName(),
                record.getSourceMethodName(),
                record.getLevel().getName(),
                getStacktraceForLogging(record.getThrown()));
    }
    
    public static String formatRecordCompact(LogRecord record) {
        return String.format("[%1$tT/%1$tL] %2$s%5$s [%3$s/%4$s]\n",
                new Date(record.getMillis()),
                record.getMessage(),
                record.getSourceClassName(),
                record.getSourceMethodName(),
                getStacktraceForLogging(record.getThrown()));
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
            Logger.getLogger(Logging.class.getName()).log(Level.WARNING, null, ex);
        }
        return null;
    }
    
    public static void createLogDir() {
        try {
            Files.createDirectories(Paths.get(Chatty.getDebugLogDirectory()));
        } catch (IOException ex) {
            Logger.getLogger(Logging.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
