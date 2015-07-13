
package chatty;

import chatty.util.RingBuffer;
import java.io.IOException;
import java.util.Date;
import java.util.logging.*;

/**
 *
 * @author tduva
 */
public class Logging {
    
    public static final Level USERINFO = new UserinfoLevel();
    
    private static final String LOG_FILE = Chatty.getUserDataDirectory()+"debug%g.log";
    private static final String LOG_FILE2 = Chatty.getUserDataDirectory()+"debug_session.log";
    private static final String LOG_FILE_IRC = Chatty.getUserDataDirectory()+"debug_irc%g.log";
    
    /**
     * Maximum log file size in bytes.
     */
    private static final int MAX_LOG_SIZE = 1024*1024*2;
    
    /**
     * How many log files to rotate through when the file reaches maximum size.
     */
    private static final int MAX_LOG_FILES = 3;
    
    private final RingBuffer<LogRecord> lastMessages = new RingBuffer<>(4);
    
    public Logging(final TwitchClient client) {
        
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
            
            FileHandler file2 = new FileHandler(LOG_FILE2);
            file2.setFormatter(new TextFormatter());
            file2.setLevel(Level.INFO);
            file2.setFilter(new FileFilter());
            Logger.getLogger("").addHandler(file2);
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(Logging.class.getName()).log(Level.WARNING, null, ex);
        }
        
        // Add handler for the GUI (display errors, log into debug window)
        Logger.getLogger("").addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() != USERINFO) {
                    client.debug(record.getMessage());
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
        });
    }
    
    /**
     * Simple 1-line text formatter.
     */
    static class TextFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            return String.format("[%1$tT %5$s] %2$s [%3$s/%4$s]\n",
                    new Date(record.getMillis()),
                    record.getMessage(),
                    record.getSourceClassName(),
                    record.getSourceMethodName(),
                    record.getLevel().getLocalizedName());
        }
        
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
        try {
            FileHandler file = new FileHandler(LOG_FILE_IRC,MAX_LOG_SIZE,2,true);
            file.setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tT] %2$s\n",
                            new Date(record.getMillis()),
                            record.getMessage());
                }
            });
            file.setLevel(Level.INFO);
            file.setFilter(new FileFilter());
            return file;
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(Logging.class.getName()).log(Level.WARNING, null, ex);
        }
        return null;
    }
    
}
