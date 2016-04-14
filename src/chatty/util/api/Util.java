
package chatty.util.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class Util {
    
    private static final SimpleDateFormat PARSE_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    
    /**
     * Parses the time returned from the Twitch API.
     * 
     * @param time The time string
     * @return The timestamp
     * @throws java.text.ParseException if the time could not be parsed
     */
    public static long parseTime(String time) throws java.text.ParseException {
        Date parsed = PARSE_DATE.parse(time);
        return parsed.getTime();
    }
    
    public static final void main(String[] args) {
        try {
            Date test = PARSE_DATE.parse("2015-05-15T22:16:57+02");
            long time = test.getTime();
            System.out.println(System.currentTimeMillis() - time);
            System.out.println(parseTime("2015-05-15T17:27:16+00"));
            System.out.println(parseTime("2015-05-15T17:27:16Z"));
        } catch (java.text.ParseException ex) {
            Logger.getLogger(FollowerManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
