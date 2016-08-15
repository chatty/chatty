
package chatty.util.api;

import chatty.gui.HtmlColors;
import java.awt.Color;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class CheersUtil {
    
    public static final Pattern PATTERN = Pattern.compile("(?<=^|\\s)cheer(\\d+)(?=$|\\s)");
    
    public enum CHEER {
        
        GRAY("gray", 1, HtmlColors.decode("#979797"), "1 or more bits"),
        PURPLE("purple", 100, HtmlColors.decode("#9c3ee8"), "100 or more bits"),
        GREEN("green", 1000, HtmlColors.decode("#1db2a5"), "1,000 or more bits"),
        BLUE("blue", 5000, HtmlColors.decode("#0099fe"), "5,000 or more bits"),
        RED("red", 10000, HtmlColors.decode("#f43021"), "10,000 or more bits");
        
        public int min;
        public Color color;
        public String image;
        public String info;
        
        CHEER(String image, int min, Color color, String info) {
            this.image = image;
            this.min = min;
            this.color = color;
            this.info = info;
        }
    }
    
    public static CHEER getCheerFromBits(int bits) {
        CHEER result = null;
        for (CHEER c : CHEER.values()) {
            if (bits < c.min) {
                break;
            }
            result = c;
        }
        return result;
    }
    
}
