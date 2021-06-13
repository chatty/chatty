
package chatty.gui.components;

import chatty.Helper;
import chatty.SettingsManager;
import chatty.User;
import chatty.gui.UserListener;
import chatty.gui.UserlistModel;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.UserContextMenu;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

/**
 *
 * @author tduva
 */
public class UserList extends JList<User> {
    
    private final UserlistModel<User> data;
    private final ContextMenuListener contextMenuListener;
    private final UserListener userListener;

    private long displayNamesMode = SettingsManager.DISPLAY_NAMES_MODE_CAPITALIZED;
    
    public UserList(ContextMenuListener contextMenuListener,
                    UserListener userListener,
                    Settings settings) {
        data = new UserlistModel<>();
        this.setModel(data);
        this.setCellRenderer(new DefaultListCellRenderer() {
            
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                /**
                 * In rare cases apparently value can be null (even if that
                 * shouldn't be possible).
                 */
                if (value == null) {
                    setText("");
                    setToolTipText("error");
                    return this;
                }
                // Configure some default stuff like colors
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                
                User user = (User)value;
                setText(Helper.makeDisplayNick(user, displayNamesMode));

                if (!isSelected && settings.getBoolean("displayColoredNamesInUserlist")) {
                    Color userColor = user.getDisplayColor2();
                    if (userColor != null) {
                        setForeground(userColor);
                    }
                }

                return this;
            }
            
        });
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
    
    public void setDisplayNamesMode(long mode) {
        this.displayNamesMode = mode;
        data.update();
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
                UserContextMenu m = new UserContextMenu(user, null, null, contextMenuListener);
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
            userListener.userClicked(user, null, null, e);
        }
    }
    
    
    
}
