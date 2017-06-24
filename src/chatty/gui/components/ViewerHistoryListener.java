
package chatty.gui.components;

import chatty.util.api.StreamInfoHistoryItem;

/**
 *
 * @author tduva
 */
public interface ViewerHistoryListener {

    public void itemSelected(StreamInfoHistoryItem item);
    public void noItemSelected();
}