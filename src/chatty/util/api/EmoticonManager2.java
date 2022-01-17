
package chatty.util.api;

import chatty.util.MiscUtil;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Request emotes for only some emotesets (instead of all >250k emotes at once).
 * Also request emotes for a certain stream, if stream name can be resolved.
 * 
 * @author tduva
 */
public class EmoticonManager2 {
    
    private static final Logger LOGGER = Logger.getLogger(EmoticonManager2.class.getName());
    
    private static final int MAX_NUMBER_OF_SETS_IN_REQUEST = 20;
    
    /**
     * How often to try to make a request. This is used to "bunch up" requests
     * a bit.
     */
    private static final long REQUEST_CHECK_INTERVAL = 5*1000;
    
    //------------
    // References
    //------------
    /**
     * Reference to the Requests object to make API requests.
     */
    private final Requests requests;
    
    //-------
    // State
    //-------
    /**
     * Emotesets to be included in the next request.
     */
    private final Set<String> pendingEmotesets = new HashSet<>();
    
    /**
     * Emotesets already requested, kept for possible refresh request.
     */
    private final Set<String> requestedEmotesets = new HashSet<>();
    
    private final Set<String> erroredEmotesets = new HashSet<>();
    
    private long lastRequestTime;
    
    private int errorRetrySeconds = 20;
    
    public EmoticonManager2(TwitchApiResultListener listener, Requests requests) {
        this.requests = requests;
        
        Timer timer = new Timer("RequestEmotesets");
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                checkRequest();
            }
        },
                REQUEST_CHECK_INTERVAL, REQUEST_CHECK_INTERVAL);
    }
    
    //-------------------
    // Request Emotesets
    //-------------------
    
    public synchronized void addEmotesets(Set<String> emotesetsToAdd) {
        for (String emoteset : emotesetsToAdd) {
            addEmoteset(emoteset);
        }
    }
    
    /**
     * Emotesets of a request that didn't succeed. Will be scheduled for another
     * attempt.
     * 
     * @param emotesets 
     */
    public synchronized void addError(Set<String> emotesets) {
        erroredEmotesets.addAll(emotesets);
    }
    
    /**
     * Adding to already requested emotesets, which means they are not requested
     * by this, except when refreshing.
     * 
     * @param emotesets 
     */
    public synchronized void addRequested(Set<String> emotesets) {
        requestedEmotesets.addAll(emotesets);
        // Remove from error for now (they may be added again if requested for
        // some reason, but at least now they shouldn't be done again)
        erroredEmotesets.removeAll(emotesets);
    }
    
    public synchronized void requestNow() {
        checkRequest();
    }
    
    public synchronized void refresh() {
        checkErrored();
        
        // Request both pending and already requested emotesets
        performRequest(true);
    }
    
    public synchronized void refresh(Set<String> emotesets) {
        requestedEmotesets.addAll(emotesets);
        for (Set<String> split : MiscUtil.splitSetByLimit(emotesets, MAX_NUMBER_OF_SETS_IN_REQUEST)) {
            requests.requestEmotesetsNew(split);
        }
    }

    /**
     * Check if any emotesets should be requested, and request them.
     */
    private synchronized void checkRequest() {
        //System.out.println("checkRequest"+pendingEmotesets+" "+requestedEmotesets);
        checkErrored();
        
        if (!pendingEmotesets.isEmpty()) {
            performRequest(false);
        }
    }
    
    private synchronized void performRequest(boolean includeAll) {
        // Always add pending
        Set<String> toRequest = new HashSet<>(pendingEmotesets);
        pendingEmotesets.clear();
        
        // Add errored if available
        toRequest.addAll(erroredEmotesets);
        erroredEmotesets.clear();
        
        // Include already requested if required
        if (includeAll) {
            toRequest.addAll(requestedEmotesets);
        }
        
        // All of these are being requested, so remember as such
        requestedEmotesets.addAll(toRequest);
        
        for (Set<String> split : MiscUtil.splitSetByLimit(toRequest, MAX_NUMBER_OF_SETS_IN_REQUEST)) {
            requests.requestEmotesetsNew(split);
        }
        lastRequestTime = System.currentTimeMillis();
    }
    
    //---------
    // Errored
    //---------
    private synchronized void checkErrored() {
        if (!erroredEmotesets.isEmpty()) {
            long ago = System.currentTimeMillis() - lastRequestTime;
            if (ago > errorRetrySeconds*1000) {
                LOGGER.info("Retrying requesting emotes: "+erroredEmotesets);
                pendingEmotesets.addAll(erroredEmotesets);
                erroredEmotesets.clear();
                errorRetrySeconds *= 4;
                errorRetrySeconds = Math.min(errorRetrySeconds, 60*60);
            }
        }
    }
    
    //--------------
    // Other Helper
    //--------------
    /**
     * Add emoteset to be requested, but only if it seems valid and hasn't
     * already been requested.
     * 
     * @param emoteset 
     */
    private void addEmoteset(String emoteset) {
        if (emoteset != null && !emoteset.isEmpty() && !requestedEmotesets.contains(emoteset)) {
            pendingEmotesets.add(emoteset);
        }
    }
    
}
