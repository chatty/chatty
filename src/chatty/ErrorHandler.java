    
package chatty;

import chatty.util.TimedCounter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Logger;

/**
 * Writes the stacktrace of an uncaught exception into logging.
 * 
 * @author tduva
 */
public class ErrorHandler implements UncaughtExceptionHandler {

    private final static Logger LOGGER = Logger.getLogger(ErrorHandler.class.getName());

    private final TimedCounter counter = new TimedCounter(60*1000);
    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        counter.increase();
        if (counter.getCount(false) > 1000) {
            LOGGER.warning("Over 1000 errors in a minute, exiting application.");
            System.exit(1);
        }
        if (e == null && t != null) {
            LOGGER.severe("Unknown exception in thread "+t.toString());
            return;
        }
        if (e == null && t == null) {
            LOGGER.severe("Unknown exception");
            return;
        }
        try {
            String stacktrace = Logging.getStacktrace(e);
            if (t != null && t.getName() != null && t.getName().startsWith("JKeyMaster-")) {
                // Output as warning, but also show directly to user
                LOGGER.warning(String.format("[%s/%s][%s][%s]\n%s",
                        e.getClass(), e.getLocalizedMessage(), e.getCause(), Thread.currentThread(), stacktrace));
                LOGGER.log(Logging.USERINFO, "A global hotkey error occured. Check debug logs for more details.");
            }
            else {
                LOGGER.severe(String.format("[%s/%s][%s][%s]\n%s",
                        e.getClass(), e.getLocalizedMessage(), e.getCause(), Thread.currentThread(), stacktrace));
            }
        } catch (Throwable ex) {
            LOGGER.severe("Exception "+ex+"\n\toccured during logging of uncaught exception: "+e.getClass().getName()+" ["+t.toString()+"]");
        }
    }
    
}
