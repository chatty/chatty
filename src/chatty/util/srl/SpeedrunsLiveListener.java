
package chatty.util.srl;

import java.util.List;

/**
 *
 * @author tduva
 */
public interface SpeedrunsLiveListener {
    
    /**
     * When a new list of races is received and no general error occured.
     * Changing the list of races is probably not a good idea (concurrency).
     * 
     * @param races The {@code List} of updated {@code Race} objects
     */
    public void racesReceived(List<Race> races);
    
    /**
     * When an error occured, e.g. the API couldn't be reached.
     * 
     * @param description A short description of the error.
     */
    public void error(String description);
    
}
