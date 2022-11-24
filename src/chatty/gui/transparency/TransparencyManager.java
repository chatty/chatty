
package chatty.gui.transparency;

import chatty.gui.Channels;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockPopout;
import chatty.util.MiscUtil;
import chatty.util.settings.Settings;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 *
 * @author tduva
 */
public class TransparencyManager {
    
    private static Channels channels;
    
    private static DockContent current;
    private static String currentId;
    private static Set<JComponent> currentNotOpaque;
    private static JFrame currentFrame;
    
    private static boolean clickThrough;
    private static int colorTransparency = 100;
    
    public static void setClickThrough(boolean enabled) {
        if (enabled == clickThrough) {
            return;
        }
        clickThrough = enabled;
        if (current != null && currentNotOpaque != null) {
            setWindowTransparent(currentFrame, false);
            setWindowTransparent(currentFrame, true);
        }
    }
    
    public static boolean getClickThrough() {
        return clickThrough;
    }
    
    public static void setColorTransparency(int transparency) {
        if (colorTransparency == transparency) {
            return;
        }
        colorTransparency = transparency;
        if (current != null && currentFrame != null) {
            findTransparencyComponent(current.getComponent()).setTransparent(transparency);
            if (!currentFrame.isUndecorated()) {
                return;
            }
            currentFrame.setBackground(new Color(0, 0, 0, transparencyToAlpha(colorTransparency)));
        }
    }
    
    public static int getColorTransparency() {
        return colorTransparency;
    }
    
    public static void init(Channels channels) {
        TransparencyManager.channels = channels;
    }
    
    public static void loadSettings(Settings settings) {
        setColorTransparency(settings.getInt("transparencyBackground"));
        setClickThrough(settings.getBoolean("transparencyClickThrough"));
        currentId = settings.getString("transparencyCurrentId");
    }
    
    public static void saveSettings(Settings settings) {
        settings.setLong("transparencyBackground", colorTransparency);
        settings.setBoolean("transparencyClickThrough", clickThrough);
        settings.setString("transparencyCurrentId", currentId);
    }
    
    public static void setTransparent(DockContent content) {
        if (current != content) {
            undoTransparent(current);
        }
        if (!isInPopout(content)) {
            channels.popout(content, true);
        }
        if (!isInPopout(content)) {
            return;
        }
        TransparencyComponent t = findTransparencyComponent(content.getComponent());
        if (t != null) {
            current = content;
            currentId = content.getId();
            currentNotOpaque = setNotOpaque((Container) t);
            currentFrame = getPopoutFrame(content);
            setWindowTransparent(currentFrame, true);
            channels.getDock().setWindowFixedAlwaysOnTop(currentFrame, true);
            currentFrame.setAlwaysOnTop(true);
            t.setTransparent(colorTransparency);
        }
    }
    
    public static void check() {
        if (current == null || currentFrame == null) {
            return;
        }
        // The DockContent is not in the expected popout anymore, so undo
        if (currentFrame != channels.getDock().getPopoutFromContent(current)) {
            removeTransparent(current);
        }
    }
    
    public static DockContent getCurrent() {
        return current;
    }
    
    public static void removeTransparent(DockContent content) {
        undoTransparent(content);
        current = null;
    }
    
    public static void undoTransparent(DockContent content) {
        if (content == null || content != current) {
            return;
        }
        if (currentFrame != null) {
            setWindowTransparent(currentFrame, false);
            channels.getDock().setWindowFixedAlwaysOnTop(currentFrame, false);
            currentFrame = null;
        }
        if (currentNotOpaque != null) {
            for (JComponent comp : currentNotOpaque) {
                comp.setOpaque(true);
            }
            currentNotOpaque = null;
        }
        findTransparencyComponent(current.getComponent()).setTransparent(0);
    }
    
    public static List<DockContent> getEligible() {
        List<DockContent> contents = channels.getDock().getContents();
        List<DockContent> result = new ArrayList<>();
        for (DockContent content : contents) {
            if (findTransparencyComponent(content.getComponent()) != null) {
                result.add(content);
            }
        }
        return result;
    }
    
    public static void toggleTransparent() {
        DockContent content = TransparencyDialog.selectedContent();
        if (content == null) {
            content = current;
        }
        if (content == null) {
            content = getEligibleById(currentId);
        }
        if (content == null) {
            return;
        }
        if (content != current || currentNotOpaque == null) {
            setTransparent(content);
        }
        else {
            undoTransparent(content);
        }
    }
    
    private static DockContent getEligibleById(String id) {
        if (id == null) {
            return null;
        }
        for (DockContent content : getEligible()) {
            if (content.getId().equals(id)) {
                return content;
            }
        }
        return null;
    }
    
    public static DockContent getCurrentById() {
        return getEligibleById(currentId);
    }
    
    public static String getCurrentId() {
        return currentId;
    }
    
    private static void setNotOpaque(Component container, Set<JComponent> notOpaque) {
        Container parent = container.getParent();
//        System.out.println(container.isOpaque() + " " + container + " -> " + parent);
        if (container instanceof JComponent) {
            JComponent comp = ((JComponent) container);
            if (parent != null && comp.isOpaque()) {
                comp.setOpaque(false);
                notOpaque.add(comp);
            }
        }
        if (parent != null) {
            setNotOpaque(parent, notOpaque);
        }
    }
    
    private static Set<JComponent> setNotOpaque(Component container) {
        Set<JComponent> notOpaque = new HashSet<>();
        setNotOpaque(container, notOpaque);
        return notOpaque;
    }
    
    public static TransparencyComponent findTransparencyComponent(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof TransparencyComponent) {
                return (TransparencyComponent) c;
            }
            else if (c instanceof Container) {
                TransparencyComponent result = findTransparencyComponent((Container) c);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
    
    public static void setWindowTransparent(JFrame frame, boolean transparent) {
        if (frame.isUndecorated() == transparent) {
            return;
        }
        // May not be visible if content removed from popout
        boolean frameVisible = frame.isVisible();
        frame.dispose();
        if (transparent) {
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 0, 0, transparencyToAlpha(colorTransparency)));
        }
        else {
            frame.setBackground(null);
            frame.setUndecorated(false);
        }
        frame.setVisible(true);
        if (MiscUtil.OS_WINDOWS) {
            // Always allow to be turned off
            if (!transparent || clickThrough) {
                setWindowTransparentNativeWin(frame, transparent);
            }
        }
        frame.setVisible(frameVisible);
    }
    
    private static JFrame getPopoutFrame(DockContent content) {
        DockPopout popout = channels.getDock().getPopoutFromContent(content);
        if (popout != null && popout.getWindow() instanceof JFrame) {
            return (JFrame) popout.getWindow();
        }
        return null;
    }
    
    private static boolean isInPopout(DockContent content) {
        return getPopoutFrame(content) != null;
    }

    /**
     * Not entirely sure what this does, but it makes the window completely
     * ignore mouse events.
     * 
     * @param w
     * @param transparent 
     */
    private static void setWindowTransparentNativeWin(Component w, boolean transparent) {
        WinDef.HWND hwnd = getHWnd(w);
        int wl = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
        if (transparent) {
            wl = wl | WinUser.WS_EX_LAYERED | WinUser.WS_EX_TRANSPARENT;
        }
        else {
            wl = wl & (~WinUser.WS_EX_LAYERED) & (~WinUser.WS_EX_TRANSPARENT);
        }
        User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, wl);
    }

    private static WinDef.HWND getHWnd(Component w) {
        WinDef.HWND hwnd = new WinDef.HWND();
        hwnd.setPointer(Native.getComponentPointer(w));
        return hwnd;
    }
    
    public static int transparencyToAlpha(int percentage) {
        if (percentage <= 0) {
            return 0;
        }
        return (int)(255 * (1 - percentage / 100.0));
    }
    
}
