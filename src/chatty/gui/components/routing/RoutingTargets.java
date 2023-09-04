
package chatty.gui.components.routing;

import chatty.gui.Highlighter.HighlightItem;
import chatty.util.Pair;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tduva
 */
public class RoutingTargets {

    private LinkedHashMap<String, Pair<String, HighlightItem>> resultTargets;
    
    public void add(HighlightItem hlItem) {
        List<String> targets = hlItem.getRoutingTargets();
        if (targets != null && !targets.isEmpty()) {
            if (resultTargets == null) {
                resultTargets = new LinkedHashMap<>();
            }
            for (String target : targets) {
                String id = RoutingManager.toId(target);
                // Only save the first hlItem
                if (!resultTargets.containsKey(id)) {
                    resultTargets.put(id, new Pair<>(target, hlItem));
                }
            }
        }
    }
    
    public Map<String, Pair<String, HighlightItem>> getResultTargets() {
        return resultTargets;
    }
    
    public boolean hasTargets() {
        return resultTargets != null && !resultTargets.isEmpty();
    }
    
    public void removeAllExceptFirst() {
        if (resultTargets == null || resultTargets.size() == 1) {
            return;
        }
        Map.Entry<String, Pair<String, HighlightItem>> entry = resultTargets.entrySet().iterator().next();
        resultTargets = new LinkedHashMap<>();
        resultTargets.put(entry.getKey(), entry.getValue());
    }
    
}
