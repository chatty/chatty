
package chatty.util.api.eventsub;

import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author tduva
 */
public class RaidTopicManager {

    private static final int UPDATE_DELAY = 30*1000;
    
    private final EventSubManager eventSub;
    private final TwitchApi api;
    
    private final Object LOCK = new Object();
    private final Set<String> usernames;
    private final Set<String> listenedUsernames;
    private final List<Runnable> actionQueue;
    
    public RaidTopicManager(EventSubManager eventSub, TwitchApi api) {
        this.eventSub = eventSub;
        this.api = api;
        this.usernames = new HashSet<>();
        this.listenedUsernames = new HashSet<>();
        this.actionQueue = new ArrayList<>();
        Timer timer = new Timer("RaidTopicManager", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                update();
            }
        }, UPDATE_DELAY, UPDATE_DELAY);
    }
    
    protected void listen(String username, boolean isLocalUser) {
        synchronized (LOCK) {
            if (isLocalUser) {
                actionQueue.add(() -> eventSub.listenRaidInternal(username));
            }
            else {
                usernames.add(username);
            }
        }
        runQueue();
    }
    
    protected void unlisten(String username) {
        synchronized (LOCK) {
            usernames.remove(username);
            listenedUsernames.remove(username);
            // No condition, so that it can also unlisten for local username
            actionQueue.add(() -> eventSub.unlistenRaidInternal(username));
        }
        runQueue();
    }
    
    private void update() {
        synchronized (LOCK) {
            for (String username : usernames) {
                if (!listenedUsernames.contains(username)) {
                    if (isEligible(username)) {
                        listenedUsernames.add(username);
                        actionQueue.add(() -> eventSub.listenRaidInternal(username));
                    }
                }
                else {
                    if (!isEligibleEnough(username)) {
                        listenedUsernames.remove(username);
                        actionQueue.add(() -> eventSub.unlistenRaidInternal(username));
                    }
                }
            }
        }
        runQueue();
    }
    
    private void runQueue() {
        List<Runnable> toExecute;
        synchronized (LOCK) {
            if (actionQueue.isEmpty()) {
                return;
            }
            toExecute = new ArrayList<>(actionQueue);
            actionQueue.clear();
        }
        for (Runnable runnable : toExecute) {
            runnable.run();
        }
    }
    
    private boolean isEligible(String username) {
        StreamInfo info = api.getCachedStreamInfo(username);
        return info != null && info.getLastOnlineAgoSecs() < 600;
    }
    
    /**
     * For unlistening, allow invalid StreamInfo as well, so that when errors
     * occur (e.g. connection issues), nothing is changed, since in that case
     * it's likely that nothing can be changed anyway.
     * 
     * @param username
     * @return 
     */
    private boolean isEligibleEnough(String username) {
        StreamInfo info = api.getCachedStreamInfo(username);
        return info != null && (!info.isValid() || info.getLastOnlineAgoSecs() < 600);
    }
    
}
