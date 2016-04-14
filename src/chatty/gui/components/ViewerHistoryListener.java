
package chatty.gui.components;

/**
 *
 * @author tduva
 */
public interface ViewerHistoryListener {

    public void itemSelected(int viewers, String status, String game);
    public void noItemSelected();
}