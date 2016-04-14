    
package chatty;

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

    
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e == null && t != null) {
            LOGGER.severe("Unknown exception in thread "+t.toString());
            return;
        }
        if (e == null && t == null) {
            LOGGER.severe("Unknown exception");
            return;
        }
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stacktrace = sw.toString();
            LOGGER.severe(stacktrace);
        } catch (Throwable ex) {
            LOGGER.severe("Exception "+ex+"\n\toccured during logging of uncaught exception: "+e.getClass().getName()+" ["+t.toString()+"]");
        }
    }
    
}
