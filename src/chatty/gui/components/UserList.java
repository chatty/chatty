
package chatty.gui.components;

import chatty.User;
import chatty.gui.UserListener;
import chatty.gui.UserlistModel;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.UserContextMenu;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.JList;

/**
 *
 * @author tduva
 */
public class UserList extends JList<User> {
    
    private final UserlistModel<User> data;
    private final ContextMenuListener contextMenuListener;
    private final UserListener userListener;
    
    public UserList(ContextMenuListener contextMenuListener,
            UserListener userListener) {
        data = new UserlistModel<>();
        this.setModel(data);
        this.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    userSelected(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                openContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        this.contextMenuListener = contextMenuListener;
        this.userListener = userListener;
    }
    
    public void addUser(User user) {
        data.add(user);
    }
    
    public void removeUser(User user) {
        data.remove(user);
    }
    
    public void updateUser(User user) {
        data.remove(user);
        data.add(user);
        //TODO: this didnt sort the user correctly after opping, maybe it can be fixed?
        //userlistData.updated(user);
    }
    
    public void resort() {
        data.sort();
    }
    
    public void clearUsers() {
        data.clear();
    }
    
    public int getNumUsers() {
        return data.getSize();
    }
    
    public ArrayList<User> getData() {
        return data.getData();
    }
    
    /**
     * Open context menu for this user, if the event points at one.
     * 
     * @param e 
     */
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            User user = getUser(e);
            if (user != null) {
                UserContextMenu m = new UserContextMenu(user, contextMenuListener);
                m.show(this, e.getX(), e.getY());
            }
        }
    }
    
    /**
     * Gets the user from the item this mouse event points to.
     * 
     * @param e
     * @return The user or null if there is none
     */
    private User getUser(MouseEvent e) {
        int index = locationToIndex(e.getPoint());
        Rectangle bounds = getCellBounds(index, index);
        if (bounds != null && bounds.contains(e.getPoint())) {
            setSelectedIndex(index);
            return getSelectedValue();
        }
        return null;
    }
    
    /**
     * Called when a user is double-clicked to tell the GUI to perform the
     * User-selected action (open the User Info dialog).
     * 
     * @param e 
     */
    private void userSelected(MouseEvent e) {
        User user = getUser(e);
        if (user != null) {
            userListener.userClicked(user, e);
        }
    }
    
    
    
}
