
package chatty.gui.components.menus;

import chatty.util.srl.Race;
import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class RaceContextMenu extends ContextMenu {
    
    private final ContextMenuListener listener;
    
    /**
     * Construct new Race context menu with options to open race pages and
     * stuff.
     *
     * @param race
     * @param listener
     * @param raceInfo If used within Race Info dialog, this can be set to obmit
     * any menu items that don't make sense
     */
    public RaceContextMenu(Race race, ContextMenuListener listener, boolean raceInfo) {
        this.listener = listener;
        
        if (!raceInfo) {
            addItem("raceInfo", "Open Race Info");
            addSeparator();
        }
        addItem("srlRacePage", "SpeedrunsLive.com");
        addItem("speedruntv", "Speedrun.tv");
        addSeparator();
        addItem("joinSrlChannel", "Join IRC");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        listener.menuItemClicked(e);
    }
    
}
