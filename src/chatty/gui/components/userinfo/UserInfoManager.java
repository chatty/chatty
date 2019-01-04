
package chatty.gui.components.userinfo;

import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.TwitchApi;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class UserInfoManager {
    
    private final List<UserInfo> dialogs = new ArrayList<>();
    private final ComponentListener closeListener;
    
    private final Window dummyWindow = new Window(null);
    private int x;
    private int y;
    private final Point temp2 = new Point();
            
    private final MainGui main;
    private final Settings settings;
    private final ContextMenuListener contextMenuListener;
    
    private String buttonsDef;
    private float fontSize;
    
    public UserInfoManager(final MainGui owner, Settings settings,
            final ContextMenuListener contextMenuListener) {
        this.main = owner;
        this.settings = settings;
        this.contextMenuListener = contextMenuListener;
        closeListener = new ComponentAdapter() {
            
            @Override
            public void componentHidden(ComponentEvent e) {
                handleClosed(e.getComponent());
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {
//                handleChanged(e.getComponent());
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
//                handleChanged(e.getComponent());
            }
        };
    }
    
    public Window getDummyWindow() {
        return dummyWindow;
    }
    
    public void setUserDefinedButtonsDef(String def) {
        this.buttonsDef = def;
        for (UserInfo dialog : dialogs) {
            dialog.setUserDefinedButtonsDef(def);
        }
    }
    
    public void update(User user, String localUsername) {
        for (UserInfo dialog : dialogs) {
            dialog.update(user, localUsername);
        }
    }

    public void setChannelInfo(ChannelInfo info) {
        for (UserInfo dialog : dialogs) {
            dialog.setChannelInfo(info);
        }
    }

    public void setFollowInfo(String stream, String user, TwitchApi.RequestResultCode result, Follower follow) {
        for (UserInfo dialog : dialogs) {
            dialog.setFollowInfo(stream, user, follow, result);
        }
    }
    
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        for (UserInfo dialog : dialogs) {
            dialog.setFontSize(fontSize);
        }
    }
    
    public void show(Component owner, User user, String msgId, String autoModMsgId, String localUsername) {
        UserInfo dialogToShow = getBestByUser(user);
        if (dialogToShow == null) {
            dialogToShow = getFirstUnpinned();
        }
        if (dialogToShow == null) {
            dialogToShow = createNew();
            setInitialLocationAndSize(dialogToShow);
            dialogs.add(dialogToShow);
            main.setWindowAttached(dialogToShow, true);
        }
        if (settings.getBoolean("openUserDialogByMouse") && !dialogToShow.isPinned()) {
            GuiUtil.setLocationToMouse(dialogToShow);
        }
        saveLocationAndSize(dialogToShow);
        dialogToShow.show(owner, user, msgId, autoModMsgId, localUsername);
    }
    
    private UserInfo getFirstUnpinned() {
        for (UserInfo dialog : dialogs) {
            if (!dialog.isPinned()) {
                return dialog;
            }
        }
        return null;
    }
    
    private int numUnpinned() {
        int num = 0;
        for (UserInfo dialog : dialogs) {
            if (!dialog.isPinned()) {
                num++;
            }
        }
        return num;
    }
    
    private UserInfo getBestByUser(User user) {
        for (UserInfo dialog : dialogs) {
//            if (dialog.getUser() == user && dialog.isVisible()) {
//                return dialog;
//            }
            if (!dialog.isPinned() && dialog.isVisible()) {
                return dialog;
            }
        }
        return null;
    }
    
    private UserInfo createNew() {
        UserInfo dialog = new UserInfo(main, settings, contextMenuListener);
        dialog.setUserDefinedButtonsDef(buttonsDef);
        dialog.setFontSize(fontSize);
        return dialog;
    }
    
    private void setInitialLocationAndSize(UserInfo dialog) {
        Point targetLocation = dummyWindow.getLocation();
        if (dummyWindow.getWidth() == 0) {
            // Since size (and location) has not been set from the settings,
            // move window according to actual dialog size
            targetLocation.translate(- dialog.getWidth() / 2, - dialog.getHeight() / 2);
        }
        if (isLocationUsed(targetLocation)) {
            targetLocation.translate(20, 20);
        }
        dialog.setSize(dummyWindow.getSize());
        dialog.setLocation(targetLocation);
        dialog.addComponentListener(closeListener);
    }
    
    private void handleClosed(Component c) {
        UserInfo dialog = (UserInfo)c;
        if (canRemove(dialog)) {
            dialogs.remove(dialog);
            main.setWindowAttached(dialog, false);
        } else {
            dialog.setPinned(false);
        }
        if (!dialog.isPinned() && numUnpinned() == 1) {
            saveLocationAndSize(dialog);
        }
    }
    
    private void saveLocationAndSize(Component c) {
        UserInfo dialog = (UserInfo)c;
        if (!dialog.isPinned()) {
            dummyWindow.setSize(dialog.getSize());
            dummyWindow.setLocation(dialog.getLocation());
        }
    }
    
    private boolean canRemove(UserInfo dialog) {
        int numUnpinned = numUnpinned();
        if (!dialog.isPinned() && numUnpinned == 1) {
            return false;
        }
        return true;
    }
    
    private boolean isLocationUsed(Point location) {
        for (UserInfo dialog : dialogs) {
            if (dialog.getLocation().equals(location)) {
                return true;
            }
        }
        return false;
    }
    
    public void aboutToSaveSettings() {
        UserInfo dialog = getFirstUnpinned();
        if (dialog != null) {
            dummyWindow.setLocation(dialog.getLocation());
            dummyWindow.setSize(dialog.getSize());
        }
    }
    
}
