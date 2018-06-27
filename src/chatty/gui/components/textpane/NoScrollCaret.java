
package chatty.gui.components.textpane;

import java.awt.Rectangle;
import javax.swing.text.DefaultCaret;

/**
 * A caret that will not scroll (or do anything) when adjustVisibility() is
 * called.
 *
 * @author tduva
 */
public class NoScrollCaret extends DefaultCaret {
    
    @Override
    protected void adjustVisibility(Rectangle nloc) {
        // Don't do anything
    }
    
}
