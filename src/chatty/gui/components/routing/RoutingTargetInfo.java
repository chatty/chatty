
package chatty.gui.components.routing;

/**
 *
 * @author tduva
 */
public class RoutingTargetInfo implements Comparable<RoutingTargetInfo> {

    public final String name;
    public final int messages;
    
    public RoutingTargetInfo(String name, int messages) {
        this.name = name;
        this.messages = messages;
    }

    @Override
    public int compareTo(RoutingTargetInfo o) {
        return name.compareToIgnoreCase(o.name);
    }
    
}
