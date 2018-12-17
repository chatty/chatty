
package chatty.util.colors;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tduva
 */
public abstract class ColorCorrector {
    
    public abstract Color correctColor(Color foreground, Color background);
    
    public static final Map<String, ColorCorrector> TYPES = new LinkedHashMap<>();
    public static final List<String> ACTIVE_TYPES = new ArrayList<>();
    
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
        
        TYPES.put("gray", new ColorCorrector() {

            @Override
            public Color correctColor(Color foreground, Color background) {
                foreground = ColorCorrectionNew.correctReadability(foreground, background, 80);
                int brightness = ColorCorrection.getBrightness(foreground);
                return new Color(brightness, brightness, brightness);
            }
        });
        
        TYPES.put("off", new ColorCorrector() {

            @Override
            public Color correctColor(Color foreground, Color background) {
                return foreground;
            }
        });
        
        TYPES.keySet().forEach(type -> {
            if (!type.equals("off")) {
                ACTIVE_TYPES.add(type);
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
