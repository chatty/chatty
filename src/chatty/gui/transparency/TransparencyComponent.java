
package chatty.gui.transparency;

/**
 * A component, like a ChannelTextPane, that acts as the base for transparency.
 * All components above are automatically made non-opaque.
 *
 * @author tduva
 */
public interface TransparencyComponent {
    
    public void setTransparent(int transparency);
    
}
