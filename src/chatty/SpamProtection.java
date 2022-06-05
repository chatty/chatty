
package chatty;

import chatty.util.TimedCounter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Tracks when lines where send to the server to prevent spam.
 * 
 * @author tduva
 */
public class SpamProtection {
    
    private final TimedCounter counter = new TimedCounter(30*1000);
    
    private boolean enabled = false;
    private int lines;
    private int seconds;

    /**
     * Changes the lines per seconds. If either lines or seconds is 0, then the
     * spam protection is disabled altogether.
     * 
     * @param lines
     * @param seconds 
     */
    public synchronized void setLinesPerSeconds(int lines, int seconds) {
        enabled = lines > 0 && seconds > 0;
        this.lines = lines;
        this.seconds = seconds;
        counter.setInterval(TimeUnit.SECONDS.toMillis(seconds));
    }
    
    /**
     * Sets the lines per seconds as a String in the format "lines/seconds".
     * Invalid values are just ignored.
     * 
     * @param linesPerSeconds 
     */
    public void setLinesPerSeconds(String linesPerSeconds) {
        String split[] = linesPerSeconds.split("/");
        if (split.length == 2) {
            try {
                Integer lines = Integer.parseInt(split[0]);
                Integer seconds = Integer.parseInt(split[1]);
                setLinesPerSeconds(lines, seconds);
            } catch (NumberFormatException ex) {
                // Do nothing
            }
        }
    }
    
    public boolean check() {
        return getAllowance() > 0;
    }
    
    public synchronized int getAllowance() {
        if (!enabled) {
            return 1;
        }
        return lines - counter.getCount(true);
    }
    
    public synchronized void increase() {
        counter.increase();
    }
    
    public synchronized boolean tryMessage() {
        if (check()) {
            increase();
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%d lines in %d seconds, %d left",
                lines, seconds, getAllowance());
    }
    
}
