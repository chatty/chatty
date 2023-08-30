
package chatty.gui.components.routing;

import chatty.gui.Highlighter.HighlightItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class RoutingTargets {

    private LinkedHashMap<String, HighlightItem> resultTargets;
    
    public void add(HighlightItem hlItem) {
        List<String> targets = hlItem.getRoutingTargets();
        if (targets != null && !targets.isEmpty()) {
            if (resultTargets == null) {
                resultTargets = new LinkedHashMap<>();
            }
            for (String target : targets) {
                // Only save the first hlItem
                if (!resultTargets.containsKey(target)) {
                    resultTargets.put(target, hlItem);
                }
            }
        }
    }
    
    public Map<String, HighlightItem> getResultTargets() {
        return resultTargets;
    }
    
    public boolean hasTargets() {
        return resultTargets != null && !resultTargets.isEmpty();
    }
    
    public void removeAllExceptFirst() {
        if (resultTargets == null || resultTargets.size() == 1) {
            return;
        }
        Map.Entry<String, HighlightItem> entry = resultTargets.entrySet().iterator().next();
        resultTargets = new LinkedHashMap<>();
        resultTargets.put(entry.getKey(), entry.getValue());
    }
    
}
