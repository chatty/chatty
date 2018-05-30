
package chatty;

import static chatty.Logging.USERINFO;
import chatty.lang.Language;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * On a connection attempt a timer can be started that will join the channel
 * again, unless the timer is canceled, which can be done if the channel join
 * actually succeeds.
 * 
 * @author tduva
 */
public class JoinChecker {
    
    private static final Logger LOGGER = Logger.getLogger(JoinChecker.class.getName());
    
    /**
     * How long to wait before trying to join again in seconds (based on the
     * number of attempts per channel).
     */
    private static final int[] DELAY = new int[]{4, 30, 30, 30, 120, 120, 300};
    
    private final Irc irc;
    
    /**
     * Map of timers for channels.
     */
    private final HashMap<String, Timer> pendingChecks = new HashMap<>();
    private final Map<String, Integer> joinAttempts = new HashMap<>();
    
    public JoinChecker(Irc irc) {
        this.irc = irc;
    }
    
    /**
     * Starts a timer that will JOIN {@code channel} once it runs out.
     * 
     * @param channel The name of the channel to start the timer for
     */
    public synchronized void joinAttempt(final String channel) {
        int count = joinAttempts.containsKey(channel) ?
                joinAttempts.get(channel) + 1 : 1;
        int delay;
        if (count > DELAY.length) {
            delay = DELAY[DELAY.length - 1];
        } else {
            delay = DELAY[count - 1];
        }

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                LOGGER.warning("Join may have failed ("+channel+")");
                LOGGER.log(USERINFO, Language.getString("chat.error.joinFailed", channel));
                irc.joinChannel(channel);
            }
        }, delay*1000);
        
        pendingChecks.put(channel, timer);
        joinAttempts.put(channel, count);
    }
    
    /**
     * Cancels the timer for {@code channel} if one was running.
     * 
     * @param channel Then name of the channel to cancel the timer for
     */
    public synchronized void cancel(String channel) {
        Timer timer = pendingChecks.remove(channel);
        if (timer != null) {
            timer.cancel();
        }
        joinAttempts.remove(channel);
    }
    
    /**
     * Stops all timers.
     */
    public synchronized void cancelAll() {
        Set<String> toRemove = new HashSet<>(pendingChecks.keySet());
        for (String channel : toRemove) {
            cancel(channel);
        }
    }
}