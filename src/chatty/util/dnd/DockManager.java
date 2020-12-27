
package chatty.util.dnd;

import chatty.util.Debugging;
import chatty.util.dnd.DockSetting.PopoutType;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JComponent;

/**
 * The main class acting as the interface for external classes to add content,
 * change settings and retrieve information. Holds the main DockBase, which is
 * a JPanel that the calling program must add to it's GUI and that holds the
 * content. Also handles popouts, which provide additional DockBase instances
 * that can also hold content.
 * 
 * <pre>DockManager
 * DockBase (main)
 * - DockChild (several can be nested)
 * -- DockContent
 * DockBase (popout)
 * - DockBase (several can be nested)
 * -- DockChild
 * 
 * @author tduva
 */
public class DockManager {
    
    private final DockBase main;
    private final DockListener listener;
    
    //--------------------------
    // Popouts
    //--------------------------
    private final Set<DockPopout> popouts = new HashSet<>();
    private final LinkedList<DockPopoutFrame> unusedFrames = new LinkedList<>();
    private final LinkedList<DockPoputDialog> unusedDialogs = new LinkedList<>();
    
    //--------------------------
    // Active content
    //--------------------------
    private DockContent currentlyActive;
    private final Map<DockPopout, DockContent> active = new HashMap<>();
    
    //--------------------------
    // Settings
    //--------------------------
    private final Map<DockSetting.Type, Object> settings = new HashMap<>();
    
    private DockSetting.PopoutType popoutType = DockSetting.PopoutType.DIALOG;
    private DockSetting.PopoutType popoutTypeDrag = DockSetting.PopoutType.DIALOG;
    private List<Image> popoutIcons;
    private Frame popoutParent;
    
    public DockManager(DockListener listener) {
        main = new DockBase(this);
        this.listener = listener;
        
        // Track focus
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", e -> {
            Object o = e.getNewValue();
            if (o != null && o instanceof Component) {
                checkFocus((Component)o);
            }
        });
    }
    
    /**
     * Used to track focus. Finds the first Dock related class to find the
     * content the focus changed to.
     * 
     * @param c 
     */
    private void checkFocus(Component c) {
//        System.out.println("Focus gain: "+c+" "+c.getClass());
        if (c instanceof DockContent) {
            changedActiveContent((DockContent)c, true);
        }
        else if (c instanceof DockTabs) {
            DockTabs t = (DockTabs)c;
            DockContent content = t.getCurrentContent();
            if (content != null) {
                changedActiveContent(content, true);
            }
            else if (t.getDockParent() instanceof DockTabsContainer) {
                changedActiveContent(((DockTabsContainer) t.getDockParent()).getCurrentContent(), true);
            }
        }
        else if (c instanceof DockTabsContainer) {
            changedActiveContent(((DockTabsContainer) c).getCurrentContent(), true);
        }
        else if (c instanceof DockPopout) {
            List<DockContent> contents = ((DockPopout)c).getBase().getContents();
            if (contents.size() == 1) {
                changedActiveContent(contents.get(0), true);
            }
        }
        else {
            if (c.getParent() != null) {
                checkFocus(c.getParent());
            }
        }
    }
    
    protected void changedActiveContent(DockContent content, boolean focusChange) {
        Debugging.println("dnda", "Changed active (Focus: %s): %s", focusChange, content);
        if (content == null) {
            return;
        }
        if (content == currentlyActive) {
            return;
        }
        currentlyActive = content;
        DockPopout popout = getDockWindowFromContent(content);
        active.put(popout, content);
        listener.activeContentChanged(popout, content, focusChange);
    }
    
    private void removeActive(DockContent content) {
        Debugging.println("dnda", "Remove active: %s", content);
        if (currentlyActive == content) {
            currentlyActive = null;
        }
        Iterator<Map.Entry<DockPopout, DockContent>> it = active.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue() == content) {
                it.remove();
            }
        }
    }
    
    private DockPopout getDockWindowFromContent(DockContent content) {
        Component c = content.getComponent();
        do {
            c = c.getParent();
            if (c instanceof DockBase) {
                for (DockPopout window : popouts) {
                    if (window.getBase() == c) {
                        return window;
                    }
                }
                return null;
            }
        } while (c != null);
        return null;
    }
    
    public DockContent getActiveContent() {
        return currentlyActive;
    }
    
    /**
     * Get all active contents. The main DockBase is included with the null
     * key.
     * 
     * @return 
     */
    public Map<DockPopout, DockContent> getAllActive() {
        return active;
    }
    
    /**
     * Get the main base that holds content. This component should be added
     * to the layout so that content is visible.
     * 
     * @return 
     */
    public JComponent getBase() {
        return main;
    }
    
    public Collection<DockPopout> getPopouts() {
        return popouts;
    }
    
    public List<DockContent> getContents() {
        List<DockContent> result = new ArrayList<>();
        result.addAll(main.getContents());
        popouts.forEach(w -> result.addAll(w.getBase().getContents()));
        return result;
    }
    
    public Collection<DockContent> getPopoutContents() {
        List<DockContent> result = new ArrayList<>();
        popouts.forEach(w -> result.addAll(w.getBase().getContents()));
        return result;
    }
    
    /**
     * 
     * @param content
     * @param direction -1 for tabs to the left, 1 for tabs to the right and 0
     * for tabs in both directions
     * @return List of components in the given direction, except the given
     * center one, or empty if no components are found
     */
    public List<DockContent> getContentsRelativeTo(DockContent content, int direction) {
        List<DockContent> result = new ArrayList<>();
        result.addAll(main.getContentsRelativeTo(content, direction));
        popouts.forEach(w -> result.addAll(w.getBase().getContentsRelativeTo(content, direction)));
        return result;
    }
    
    public DockContent getContentTab(DockContent content, int direction) {
        List<DockContent> c = getContentsRelativeTo(content, direction);
        if (!c.isEmpty()) {
            return c.get(0);
        }
        c = getContentsRelativeTo(content, -direction);
        if (!c.isEmpty()) {
            return c.get(c.size() - 1);
        }
        return null;
    }
    
    public void addContent(DockContent content) {
        if (currentlyActive == null) {
            changedActiveContent(content, false);
        }
        else {
            setTargetPath(content, currentlyActive);
        }
        
        // Add content
        DockPath target = content.getTargetPath();
        if (target == null || target.getPopoutId() == null) {
            main.addContent(content);
        }
        else {
            for (DockPopout p : popouts) {
                if (p.getId().equals(target.getPopoutId())) {
                    p.getBase().addContent(content);
                }
            }
        }
    }
    
    private void setTargetPath(DockContent content, DockContent target) {
        if (target == null || content.getTargetPath() != null) {
            return;
        }
        DockPath path = target.getPath();
        if (path != null) {
            content.setTargetPath(path);
        }
    }
    
    public void removeContent(DockContent content) {
        removeActive(content);
        main.removeContent(content);
        for (DockPopout w : popouts) {
            w.getBase().removeContent(content);
        }
    }
    
    public boolean hasContent(DockContent content) {
        return getContents().contains(content);
    }
    
    public void setActiveContent(DockContent content) {
        main.setActiveContent(content);
        popouts.forEach(w -> w.getBase().setActiveContent(content));
    }
    
    protected void baseEmpty(DockBase base) {
        for (DockPopout w : popouts) {
            if (w.getBase() == base) {
                w.getWindow().setVisible(false);
            }
        }
    }
    
    public boolean isMainEmpty() {
        return main.isEmpty();
    }
    
    public boolean closeWindow() {
        if (popouts.isEmpty()) {
            return false;
        }
        popouts.iterator().next().getWindow().setVisible(false);
        return true;
    }
    
    protected void requestDrag() {
        main.startDrag();
        for (DockPopout window : popouts) {
            window.getBase().startDrag();
        }
    }
    
    protected void requestStopDrag(DockTransferable t) {
        if (!DockUtil.isMouseOverWindow()
                && popoutTypeDrag != PopoutType.NONE) {
            // Manually changed location, so reset
            t.content.setTargetPath(null);
            // Popout from dragging outside window
            Point location = MouseInfo.getPointerInfo().getLocation();
            DockContent c = t != null ? t.content : currentlyActive;
            popout(c, popoutTypeDrag).getWindow().setLocation(location.x - 80, location.y - 10);
        }
        main.stopDrag();
        for (DockPopout window : popouts) {
            window.getBase().stopDrag();
        }
    }
    
    private void closeWindow(DockPopout popout) {
        // Add content to main (if present)
        List<DockContent> contents = popout.getBase().getContents();
        DockContent activeInPopout = active.get(popout);
        DockContent activeInMain = active.get(null);
        for (DockContent c : contents) {
            popout.getBase().removeContent(c);
            setTargetPath(c, activeInMain);
            main.addContent(c);
        }
        if (activeInPopout != null) {
            main.setActiveContent(activeInPopout);
        }
        
        // Remove popout from stuff
        popouts.remove(popout);
        active.remove(popout);
        popout.getWindow().setVisible(false);
        
        // Store unused popouts
        if (popout instanceof DockPoputDialog) {
            if (!unusedDialogs.contains((DockPoputDialog) popout)) {
                unusedDialogs.add((DockPoputDialog)popout);
            }
        }
        else if (popout instanceof DockPopoutFrame) {
            if (!unusedFrames.contains((DockPopoutFrame) popout)) {
                unusedFrames.add((DockPopoutFrame)popout);
            }
        }
        
        // Inform listeners
        listener.popoutClosed(popout, contents);
    }
    
    public DockPopout popout(DockContent content) {
        return popout(content, popoutType);
    }
    
    /**
     * Popout the given content into an extra window.
     * 
     * @param content 
     * @param type 
     * @return  
     */
    public DockPopout popout(DockContent content, PopoutType type) {
        if (type == PopoutType.NONE) {
            return null;
        }
        main.removeContent(content);
        popouts.forEach(w -> w.getBase().removeContent(content));
        DockPopout window = getUnusedPopout(type);
        if (window == null) {
            // Need to create a new one
            if (type == PopoutType.DIALOG) {
                window = new DockPoputDialog(this, popoutParent);
            }
            else {
                window = new DockPopoutFrame(this);
            }
            // Configure new popout
            applySettings(window.getBase());
            if (popoutIcons != null) {
                window.getWindow().setIconImages(popoutIcons);
            }
            DockPopout window2 = window;
            window.getWindow().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    closeWindow(window2);
                }
            });
        }
        window.getBase().addContent(content);
        window.getWindow().setSize(600, 400);
        window.getWindow().setLocationByPlatform(true);
        window.getWindow().setVisible(true);
        popouts.add(window);
        changedActiveContent(content, false);
        listener.popoutOpened(window, content);
        return window;
    }
    
    private DockPopout getUnusedPopout(PopoutType type) {
        if (type == PopoutType.DIALOG) {
            return unusedDialogs.poll();
        }
        else if (type == PopoutType.FRAME) {
            return unusedFrames.poll();
        }
        return null;
    }
    
    public boolean isContentVisible(DockContent content) {
        if (main.isContentVisible(content)) {
            return true;
        }
        for (DockPopout w : popouts) {
            if (w.getBase().isContentVisible(content)) {
                return true;
            }
        }
        return false;
    }
    
    public void setSetting(DockSetting.Type setting, Object value) {
        if (setting == DockSetting.Type.POPOUT_TYPE) {
            popoutType = (DockSetting.PopoutType) value;
        }
        else if (setting == DockSetting.Type.POPOUT_TYPE_DRAG) {
            popoutTypeDrag = (DockSetting.PopoutType) value;
        }
        else if (setting == DockSetting.Type.POPOUT_ICONS) {
            popoutIcons = (List<Image>) value;
        }
        else if (setting == DockSetting.Type.POPOUT_PARENT) {
            popoutParent = (Frame) value;
        }
        else {
            settings.put(setting, value);
            main.setSetting(setting, value);
            popouts.forEach(w -> w.getBase().setSetting(setting, value));
        }
    }
    
    public void applySettings(DockChild child) {
        settings.forEach((type, value) -> child.setSetting(type, value));
    }
    
}
