
package chatty.util.colors;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author tduva
 */
public abstract class ColorCorrector {
    
    public abstract Color correctColor(Color foreground, Color background);
    
    public static final Map<String, ColorCorrector> TYPES = new LinkedHashMap<>();
    
    static {
        TYPES.put("normal", new ColorCorrector() {

            @Override
            public Color correctColor(Color foreground, Color background) {
                return ColorCorrectionNew.correctReadability(foreground, background, 60);
            }
        });
        
        TYPES.put("strong", new ColorCorrector() {

            @Override
            public Color correctColor(Color foreground, Color background) {
                return ColorCorrectionNew.correctReadability(foreground, background, 140);
            }
        });
        
        TYPES.put("old", new ColorCorrector() {

            @Override
            public Color correctColor(Color foreground, Color background) {
                return ColorCorrection.correctReadability(foreground, background);
            }
        });
        
        TYPES.put("test", new ColorCorrector() {

            @Override
            public Color correctColor(Color foreground, Color background) {
                foreground = ColorCorrectionNew.correctReadability(foreground, background, 80);
                int brightness = ColorCorrection.getBrightness(foreground);
                return new Color(brightness, brightness, brightness);
            }
        });
    }
    
    public static ColorCorrector get(String type) {
        if (!TYPES.containsKey(type)) {
            type = "normal";
        }
        return TYPES.get(type);
    }
    
}
