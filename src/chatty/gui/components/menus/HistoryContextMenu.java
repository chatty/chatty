
package chatty.gui.components.menus;

import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class HistoryContextMenu extends ContextMenu {

    private static final String RANGE_MENU = "Range";
    
    public HistoryContextMenu() {
        addSubItem("range1h", "1 Hour",RANGE_MENU);
        addSubItem("range2h", "2 Hours",RANGE_MENU);
        addSubItem("range4h", "4 Hours",RANGE_MENU);
        addSubItem("range8h", "8 Hours",RANGE_MENU);
        addSubItem("range12h", "12 Hours",RANGE_MENU);
        addSeparator(RANGE_MENU);
        addSubItem("rangeAll", "All",RANGE_MENU);
        addItem("toggleShowFullVerticalRange", "Toggle Vertical");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        for (ContextMenuListener l : getContextMenuListeners()) {
            l.menuItemClicked(e);
        }
    }
}
