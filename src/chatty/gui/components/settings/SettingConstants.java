
package chatty.gui.components.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class SettingConstants {
    
    protected static final Map<String,String> requirementOptions = new LinkedHashMap<>();
    
    static {
        requirementOptions.put("off","Off");
        requirementOptions.put("both", "When channel/app not active");
        requirementOptions.put("either", "When channel or app not active");
        requirementOptions.put("app", "When app not active");
        requirementOptions.put("channel", "When channel not active");
        requirementOptions.put("channelActive", "When channel active");
        requirementOptions.put("always", "Always");
    }
    
    protected static final String HTML_PREFIX = "<html><head><style>code { font-size: 1em; }</style></head><body style='width:320px;'>";
    
}
