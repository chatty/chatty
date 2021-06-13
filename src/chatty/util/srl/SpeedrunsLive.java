
package chatty.util.srl;

import chatty.util.UrlRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Requests race data from the SRL API and sends the parsed result to the
 * registered listeners.
 * 
 * @author tduva
 */
public class SpeedrunsLive {
    
    private static final Logger LOGGER = Logger.getLogger(SpeedrunsLive.class.getName());
    
    private volatile boolean requestPending;
    private final List<SpeedrunsLiveListener> listeners = new ArrayList<>();
    
    /**
     * Request race data from the API, but only if no request is currently
     * pending.
     */
    public void requestRaces() {
        if (!requestPending) {
            requestPending = true;
            new SpeedrunsLiveRequest().async((result, responseCode) -> {
                result(result, responseCode);
            });
        }
    }
    
    /**
     * Adds a listener that receives the data and error messages.
     * 
     * @param listener 
     */
    public void addListener(SpeedrunsLiveListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Reacts to the result by parsing it and sending the data to the listeners.
     * 
     * @param result
     * @param responseCode 
     */
    private void result(String result, int responseCode) {
        requestPending = false;
        if (result == null || responseCode != 200) {
            error("Error receiving data.");
            return;
        }
        List<Race> races = parseRaces(result);
        if (races == null) {
            error("Error parsing data.");
        } else {
            for (SpeedrunsLiveListener listener : listeners) {
                listener.racesReceived(races);
            }
        }
    }
    
    /**
     * Passes the error message on to the listeners.
     * 
     * @param description 
     */
    private void error(String description) {
        for (SpeedrunsLiveListener listener : listeners) {
            listener.error(description);
        }
    }
    
    /**
     * Parses the races from the result.
     * 
     * @param json
     * @return A {@code List} of {@code Race} objects or {@code null} if a
     * parsing error occured
     */
    private List<Race> parseRaces(String json) {
        List<Race> result = new ArrayList<>();
        if (json == null) {
            return null;
        }
        JSONParser parser = new JSONParser();
        try {
            JSONObject root = (JSONObject)parser.parse(json.trim());
            JSONArray races = (JSONArray)root.get("races");
            for (Object o : races) {
                if (o instanceof JSONObject) {
                    Race race = parseRace((JSONObject)o);
                    if (race != null) {
                        result.add(race);
                    }
                }
            }
        } catch (ParseException | ClassCastException | NullPointerException ex) {
            LOGGER.warning("Error parsing SRL: "+ex);
            return null;
        }
        return result;
    }
    
    /**
     * Parses a single race.
     * 
     * @param o
     * @return The {@code Race} object or {@code null} if an error occured.
     */
    private Race parseRace(JSONObject o) {
        try {
            String id = (String)o.get("id");
            JSONObject gameInfo = (JSONObject)o.get("game");
            String game = (String)gameInfo.get("name");
            String goal = (String)o.get("goal");
            int state = ((Number)o.get("state")).intValue();
            String statetext = (String)o.get("statetext");
            long time = ((Number)o.get("time")).longValue();
            
            Race race = new Race(id, game, goal, state, statetext, time);
            JSONObject entrants = (JSONObject)o.get("entrants");
            parseEntrants(entrants, race);
            return race;
        } catch (ClassCastException | NullPointerException | NumberFormatException ex) {
            LOGGER.warning("Error parsing race: "+ex);
            return null;
        }
    }
    
    /**
     * Parses a single entrant. An error in this will pass the exception on to
     * the race parsing.
     * 
     * @param o
     * @param race 
     */
    private void parseEntrants(JSONObject o, Race race) {
        for (Object key : o.keySet()) {
            JSONObject entrant = (JSONObject)o.get(key);
            String name = (String)entrant.get("displayname");
            String statetext = (String)entrant.get("statetext");
            int place = ((Number)entrant.get("place")).intValue();
            long time = ((Number)entrant.get("time")).longValue();
            String twitch = (String)entrant.get("twitch");
            int points = Integer.parseInt((String)entrant.get("trueskill"));
            String message = (String)entrant.get("message");
            
            race.addEntrant(new Race.Entrant(name, statetext, place, time, twitch, points, message));
        }
    }
    
    private class SpeedrunsLiveRequest extends UrlRequest {

        private final static String URL = "https://api.speedrunslive.com/races";
        //private final static String URL = "http://127.0.0.1/twitch/races";
        
        /**
         * Creates a new request.
         * 
         * @param channel The channel this request is meant for.
         */
        private SpeedrunsLiveRequest() {
            super(URL);
        }
    }
    
    
    public static final void main(String[] args) {
        SpeedrunsLive srl = new SpeedrunsLive();
        srl.requestRaces();
    }
}
