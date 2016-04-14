
package chatty.gui.components;

import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.TabContextMenu;
import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class TabComponent extends JPanel {
    
    public TabComponent(String text, ContextMenuListener listener) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel label = new JLabel(text);
        add(label);
        setOpaque(false);
        label.setComponentPopupMenu(new TabContextMenu(listener));
    }
}
