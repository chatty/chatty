
package chatty.util.api;

import chatty.util.MiscUtil;
import chatty.util.TwitchEmotes.Emoteset;
import chatty.util.TwitchEmotes.EmotesetInfo;
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
    
    private static final int MAX_NUMBER_OF_SETS_IN_REQUEST = 400;
    
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
    private final Set<Integer> pendingEmotesets = new HashSet<>();
    
    /**
     * Emotesets already requested, kept for possible refresh request.
     */
    private final Set<Integer> requestedEmotesets = new HashSet<>();
    
    /**
     * Stream names pending to be resolved into an emoteset.
     */
    private final Set<String> pendingStreams = new HashSet<>();
    
    /**
     * Stream names which were not able to be resolved, but kept in case
     * emotesetInfo is updated (to try again).
     */
    private final Set<String> backlogStreams = new HashSet<>();
    
    private final Set<Integer> erroredEmotesets = new HashSet<>();
    
    private long lastRequestTime;
    
    private int retryBackoff = 20;
    
    //--------------------------
    // Changing Data References
    //--------------------------
    /**
     * Used to resolve stream names to emotesets.
     */
    private EmotesetInfo emotesetInfo = EmotesetInfo.EMPTY;
    
    
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
    
    public synchronized void addStreams(Set<String> streams) {
        this.pendingStreams.addAll(streams);
    }
    
    public synchronized void addEmotesets(Set<Integer> emotesetsToAdd) {
        for (int emoteset : emotesetsToAdd) {
            addEmoteset(emoteset);
        }
    }
    
    /**
     * Emotesets of a request that didn't succeed. Will be scheduled for another
     * attempt.
     * 
     * @param emotesets 
     */
    public synchronized void addError(Set<Integer> emotesets) {
        erroredEmotesets.addAll(emotesets);
    }
    
    public synchronized void requestNow() {
        checkRequest();
    }
    
    public synchronized void refresh() {
        checkStreams();
        checkErrored();
        
        // Request both pending and already requested emotesets
        performRequest(true);
    }

    /**
     * Check if any emotesets should be requested, and request them.
     */
    private synchronized void checkRequest() {
        //System.out.println("checkRequest"+pendingEmotesets+" "+requestedEmotesets);
        checkStreams();
        checkErrored();
        
        if (!pendingEmotesets.isEmpty()) {
            performRequest(false);
        }
    }
    
    private synchronized void performRequest(boolean includeAll) {
        // Always add pending
        Set<Integer> toRequest = new HashSet<>(pendingEmotesets);
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
        
        for (Set<Integer> split : MiscUtil.splitSetByLimit(toRequest, MAX_NUMBER_OF_SETS_IN_REQUEST)) {
            requests.requestEmotesets(split);
        }
        lastRequestTime = System.currentTimeMillis();
    }
    
    //----------------------------
    // Stream -> Emoteset Related
    //----------------------------
    /**
     * Set new EmotesetInfo. Will only be set if non-null and different object,
     * and if so, try to resolve previously failed stream names again.
     * 
     * @param info 
     */
    public synchronized void setEmotesetInfo(EmotesetInfo info) {
        if (info != null && this.emotesetInfo != info) {
            this.emotesetInfo = info;
            pendingStreams.addAll(backlogStreams);
            backlogStreams.clear();
        }
    }
    
    /**
     * Resolve any pending stream names to emotesets and add them to be
     * requested.
     */
    private synchronized void checkStreams() {
        //System.out.println("checkStreams:"+pendingStreams+" "+backlogStreams);
        for (String stream : pendingStreams) {
            Set<Emoteset> sets = emotesetInfo.getEmotesetsByStream(stream);
            if (sets != null) {
                for (Emoteset set : sets) {
                    addEmoteset(set.emoteset_id);
                }
            } else {
                backlogStreams.add(stream);
            }
        }
        pendingStreams.clear();
    }
    
    //---------
    // Errored
    //---------
    private synchronized void checkErrored() {
        if (!erroredEmotesets.isEmpty()) {
            long ago = System.currentTimeMillis() - lastRequestTime;
            if (ago > 30*60*1000) {
                retryBackoff = 20;
            }
            if (ago > retryBackoff*1000) {
                LOGGER.info("Retrying requesting emotes: "+erroredEmotesets);
                pendingEmotesets.addAll(erroredEmotesets);
                erroredEmotesets.clear();
                retryBackoff *= 4;
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
    private void addEmoteset(int emoteset) {
        if (emoteset >= 0 && !requestedEmotesets.contains(emoteset)) {
            pendingEmotesets.add(emoteset);
        }
    }
    
}
