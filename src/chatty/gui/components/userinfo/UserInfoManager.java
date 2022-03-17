
package chatty.gui.components.userinfo;

import chatty.Room;
import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.Timestamp;
import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.TwitchApi;
import chatty.util.api.UserInfo;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 * @author tduva
 */
public class UserInfoManager {
    
    private final List<UserInfoDialog> dialogs = new ArrayList<>();
    private final ComponentListener closeListener;
    
    private final Window dummyWindow = new Window(null);
    private int x;
    private int y;
    private final Point temp2 = new Point();
            
    private final MainGui main;
    private final Settings settings;
    private final ContextMenuListener contextMenuListener;
    private final UserInfoListener userInfoListener;
    private final UserInfoRequester userInfoRequester;
    
    private String buttonsDef;
    private float fontSize;
    private Timestamp timestampFormat;
    
    public UserInfoManager(final MainGui owner, Settings settings,
            final ContextMenuListener contextMenuListener, TwitchApi api) {
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
        userInfoListener = new UserInfoListener() {

            @Override
            public void anonCustomCommand(Room room, CustomCommand command, Parameters parameters) {
                main.anonCustomCommand(room, command, parameters);
            }
        };
        userInfoRequester = new UserInfoRequester() {

            @Override
            public Follower getSingleFollower(String stream, String streamId, String user, String userId, boolean refresh) {
                return api.getSingleFollower(stream, streamId, user, userId, refresh);
            }

            @Override
            public UserInfo getCachedUserInfo(String channel, Consumer<UserInfo> result) {
                return api.getCachedUserInfo(channel, result);
            }
            
            @Override
            public void requestFollowerInfo(String stream) {
                api.getFollowers(stream);
            }

        };
    }
    
    public Window getDummyWindow() {
        return dummyWindow;
    }
    
    public void setUserDefinedButtonsDef(String def) {
        this.buttonsDef = def;
        for (UserInfoDialog dialog : dialogs) {
            dialog.setUserDefinedButtonsDef(def);
        }
    }
    
    public void update(User user, String localUsername) {
        for (UserInfoDialog dialog : dialogs) {
            dialog.update(user, localUsername);
        }
    }

    public void setFollowInfo(String stream, String user, TwitchApi.RequestResultCode result, Follower follow) {
        for (UserInfoDialog dialog : dialogs) {
            dialog.setFollowInfo(stream, user, follow, result);
        }
    }
    
    public void setFollowerInfo(FollowerInfo info) {
        for (UserInfoDialog dialog : dialogs) {
            dialog.setFollowerInfo(info);
        }
    }
    
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        for (UserInfoDialog dialog : dialogs) {
            dialog.setFontSize(fontSize);
        }
    }
    
    public void show(Component owner, User user, String msgId, String autoModMsgId, String localUsername, boolean keepPosition) {
        UserInfoDialog dialogToShow = getBestByUser(user);
        if (dialogToShow == null) {
            dialogToShow = getFirstUnpinned();
        }
        if (dialogToShow == null) {
            dialogToShow = createNew();
            setInitialLocationAndSize(dialogToShow);
            dialogs.add(dialogToShow);
            main.setWindowAttached(dialogToShow, true);
        }
        if (settings.getBoolean("openUserDialogByMouse") && !dialogToShow.isPinned() && !keepPosition) {
            GuiUtil.setLocationToMouse(dialogToShow);
        }
        saveLocationAndSize(dialogToShow);
        dialogToShow.show(owner, user, msgId, autoModMsgId, localUsername);
    }
    
    private UserInfoDialog getFirstUnpinned() {
        for (UserInfoDialog dialog : dialogs) {
            if (!dialog.isPinned()) {
                return dialog;
            }
        }
        return null;
    }
    
    private int numUnpinned() {
        int num = 0;
        for (UserInfoDialog dialog : dialogs) {
            if (!dialog.isPinned()) {
                num++;
            }
        }
        return num;
    }
    
    private UserInfoDialog getBestByUser(User user) {
        // First try to find existing one for user, if enabled
        if (settings.getBoolean("reuseUserDialog")) {
            for (UserInfoDialog dialog : dialogs) {
                if (dialog.getUser() == user && dialog.isVisible()) {
                    return dialog;
                }
            }
        }
        // Then try to find visible and unpinned one
        for (UserInfoDialog dialog : dialogs) {
            if (!dialog.isPinned() && dialog.isVisible()) {
                return dialog;
            }
        }
        return null;
    }
    
    private UserInfoDialog createNew() {
        UserInfoDialog dialog = new UserInfoDialog(main, userInfoListener, userInfoRequester, settings, contextMenuListener);
        dialog.setUserDefinedButtonsDef(buttonsDef);
        dialog.setFontSize(fontSize);
        dialog.setTimestampFormat(timestampFormat);
        return dialog;
    }
    
    private void setInitialLocationAndSize(UserInfoDialog dialog) {
        Point targetLocation = dummyWindow.getLocation();
        if (dummyWindow.getWidth() == 0) {
            // Since size (and location) has not been set from the settings,
            // move window according to actual dialog size
            targetLocation.translate(- dialog.getWidth() / 2, - dialog.getHeight() / 2);
        }
        if (isLocationUsed(targetLocation)) {
            targetLocation.translate(20, 20);
        }
        if (dummyWindow.getWidth() > 0) {
            dialog.setSize(dummyWindow.getSize());
        } else {
            // Default size
            dialog.setSize(400, 360);
        }
        dialog.setLocation(targetLocation);
        dialog.addComponentListener(closeListener);
    }
    
    private void handleClosed(Component c) {
        UserInfoDialog dialog = (UserInfoDialog)c;
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
        UserInfoDialog dialog = (UserInfoDialog)c;
        if (!dialog.isPinned()) {
            dummyWindow.setSize(dialog.getSize());
            dummyWindow.setLocation(dialog.getLocation());
        }
    }
    
    private boolean canRemove(UserInfoDialog dialog) {
        int numUnpinned = numUnpinned();
        if (!dialog.isPinned() && numUnpinned == 1) {
            return false;
        }
        return true;
    }
    
    private boolean isLocationUsed(Point location) {
        for (UserInfoDialog dialog : dialogs) {
            if (dialog.getLocation().equals(location)) {
                return true;
            }
        }
        return false;
    }
    
    public void aboutToSaveSettings() {
        UserInfoDialog dialog = getFirstUnpinned();
        if (dialog != null) {
            dummyWindow.setLocation(dialog.getLocation());
            dummyWindow.setSize(dialog.getSize());
        }
    }

    public void setTimestampFormat(Timestamp timestampFormat) {
        this.timestampFormat = timestampFormat;
        for (UserInfoDialog dialog : dialogs) {
            dialog.setTimestampFormat(timestampFormat);
        }
    }
    
}
