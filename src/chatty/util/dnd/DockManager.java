
package chatty.util.dnd;

import chatty.util.Debugging;
import chatty.util.dnd.DockSetting.PopoutType;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

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
    private final PropertyChangeListener popoutParentPropertyListener;
    
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
        
        popoutParentPropertyListener = (PropertyChangeEvent evt) -> {
            if (evt.getPropertyName().equals("alwaysOnTop")) {
                updateWindowsAlwaysOnTop(popoutParent.isAlwaysOnTop());
            }
        };
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
        DockPopout popout = getPopoutFromContent(content);
        /**
         * Don't have to continue if the content is already active, both overall
         * and in it's respective popout (or main).
         */
        if (currentlyActive != content || active.get(popout) != content) {
            currentlyActive = content;
            active.put(popout, content);
            listener.activeContentChanged(popout, content, focusChange);
        }
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
    
    /**
     * Get the popout that contains the given content.
     * 
     * @param content
     * @return The popout, or null if the content is in main or not added at all
     */
    public DockPopout getPopoutFromContent(DockContent content) {
        Component c = content.getComponent();
        do {
            c = c.getParent();
            if (c instanceof DockBase) {
                for (DockPopout popout : popouts) {
                    if (popout.getBase() == c) {
                        return popout;
                    }
                }
                return null;
            }
        } while (c != null);
        return null;
    }
    
    /**
     * Return the currently active content. If no active content is currently
     * set, returns the first found content.
     * 
     * @return The active content, or null if no content is present at all
     */
    public DockContent getActiveContent() {
        DockContent result = currentlyActive;
        if (result == null) {
            List<DockContent> contents = getContents();
            if (!contents.isEmpty()) {
                result = contents.iterator().next();
            }
        }
        return result;
    }
    
    /**
     * Get the active content for a specific popout. If no active content is
     * currently set, returns the first found content in the specified popout.
     * 
     * @param popout The popout, or null for main
     * @return The active content, or null if no content is in the popout
     */
    public DockContent getActiveContent(DockPopout popout) {
        DockContent result = active.get(popout);
        if (result == null) {
            DockBase base = popout == null ? main : popout.getBase();
            List<DockContent> contents = base.getContents();
            if (!contents.isEmpty()) {
                result = contents.iterator().next();
            }
        }
        return result;
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
    
    /**
     * Get all open popouts.
     * 
     * @return A copy of the list of popouts
     */
    public Collection<DockPopout> getPopouts() {
        return new ArrayList<>(popouts);
    }
    
    public List<DockContent> getContents() {
        List<DockContent> result = new ArrayList<>();
        result.addAll(main.getContents());
        popouts.forEach(w -> result.addAll(w.getBase().getContents()));
        return result;
    }
    
    /**
     * Get the contents for a specific popout.
     * 
     * @param popout The popout, or null for main
     * @return A list of contents, could be empty (never null)
     */
    public List<DockContent> getContents(DockPopout popout) {
        DockBase base = popout == null ? main : popout.getBase();
        return new ArrayList<>(base.getContents());
    }
    
    /**
     * Get the content from all popouts (without main).
     * 
     * @return A list of contents, could be empty (never null)
     */
    public Collection<DockContent> getPopoutContents() {
        List<DockContent> result = new ArrayList<>();
        popouts.forEach(w -> result.addAll(w.getBase().getContents()));
        return result;
    }
    
    /**
     * Get a list of contents from the same tab pane as the given content.
     * 
     * @param content The content to base this on
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
    
    /**
     * Get the content contained in the tab relative to given content.
     * 
     * @param content The content to base this on
     * @param direction -1 for the next tab to the left, 1 for the next tab to
     * the right (wraps around if necessary)
     * @return The next tab, or null if none could be found
     */
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
    
    /**
     * Add content to the default location, which is the currently active tab
     * pane, unless the content's target path is already non-null. Once added,
     * the target path may be reset to null.
     * 
     * @param content 
     */
    public void addContent(DockContent content) {
        if (hasContent(content)) {
            return;
        }
        
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
        
        listener.contentAdded(content);
    }
    
    /**
     * Set the first content's target path to the path of the second content.
     * Will not set the target path if it is already non-null or the target does
     * not provide a non-null path.
     * 
     * @param content The content (must not be null)
     * @param target The target (may be null)
     */
    private void setTargetPath(DockContent content, DockContent target) {
        if (target == null || content.getTargetPath() != null) {
            return;
        }
        DockPath path = target.getPath();
        if (path != null) {
            content.setTargetPath(path);
        }
    }
    
    /**
     * Remove the given content.
     * 
     * @param content The content
     */
    public void removeContent(DockContent content) {
        if (!hasContent(content)) {
            return;
        }
        removeActive(content);
        main.removeContent(content);
        applyToPopoutsSafe(p -> p.getBase().removeContent(content));
        listener.contentRemoved(content);
    }
    
    /**
     * Check if the given content is currently added in main or any of the
     * popouts.
     * 
     * @param content
     * @return 
     */
    public boolean hasContent(DockContent content) {
        return getContents().contains(content);
    }
    
    /**
     * Switch to the given content, for example by changing tabs.
     * 
     * @param content 
     */
    public void setActiveContent(DockContent content) {
        main.setActiveContent(content);
        popouts.forEach(w -> w.getBase().setActiveContent(content));
        DockPopout popout = getPopoutFromContent(content);
        if (popout != null) {
            // TODO: Not sure if this would be annoying
//            popout.getWindow().toFront();
        }
        // This may already be called from a tab or focus change, however if the
        // active didn't change, it wouldn't have any affect
        changedActiveContent(content, false);
    }
    
    protected void baseEmpty(DockBase base) {
        applyToPopoutsSafe(p -> {
            if (p.getBase() == base) {
                DockManager.this.closePopout(p);
            }
        });
    }
    
    public boolean isMainEmpty() {
        return main.isEmpty();
    }
    
    public boolean closePopout() {
        if (popouts.isEmpty()) {
            return false;
        }
        DockManager.this.closePopout(popouts.iterator().next());
        return true;
    }
    
    protected void requestDrag(boolean allowPopout) {
        main.startDrag();
        if (allowPopout) {
            for (DockPopout window : popouts) {
                window.getBase().startDrag();
            }
        }
    }
    
    protected void requestStopDrag(DockTransferable t) {
        if (!DockUtil.isMouseOverWindow()
                && popoutTypeDrag != PopoutType.NONE
                && t != null
                && t.content != null) {
            // Manually changed location, so reset
            t.content.setTargetPath(null);
            // Popout from dragging outside window
            Point location = MouseInfo.getPointerInfo().getLocation();
            popout(t.content, popoutTypeDrag, new Point(location.x - 80, location.y - 10), null);
        }
        main.stopDrag();
        for (DockPopout window : popouts) {
            window.getBase().stopDrag();
        }
    }
    
    public void closePopout(DockPopout popout) {
        if (popout == null || !popouts.contains(popout)) {
            return;
        }
        
        // Remove first, so removing content doesn't trigger this method again
        popouts.remove(popout);
        
        // Add content to main (if present)
        List<DockContent> contents = popout.getBase().getContents();
        DockContent activeInPopout = getActiveContent(popout);
        DockContent activeInMain = active.get(null);
        for (DockContent c : contents) {
            popout.getBase().removeContent(c);
            setTargetPath(c, activeInMain);
            main.addContent(c);
        }
        if (activeInPopout != null) {
            main.setActiveContent(activeInPopout);
        }
        
        // Was still required to retrieve activeInPopout before this
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
        
        if (activeInPopout != null) {
            changedActiveContent(activeInPopout, false);
        }
        
        // Inform listeners
        listener.popoutClosed(popout, contents);
    }
    
    public DockPopout popout(DockContent content) {
        return popout(content, popoutType, null, null);
    }
    
    /**
     * Popout the given content into an extra window.
     * 
     * @param content The first content to add to the popout
     * @param type The type of popout
     * @param location The location to open the popout in (may be null to open
     * in default location)
     * @param size The size of the popout (may be null to use default size)
     * @return  
     */
    public DockPopout popout(DockContent content, PopoutType type, Point location, Dimension size) {
        if (type == PopoutType.NONE || !content.canPopout()) {
            return null;
        }
        
        // Remove content from previous location
        main.removeContent(content);
        applyToPopoutsSafe(p -> p.getBase().removeContent(content));
        
        // Get existing or create new popout
        DockPopout popout = getUnusedPopout(type);
        if (popout == null) {
            // Need to create a new one
            if (type == PopoutType.DIALOG) {
                popout = new DockPoputDialog(this, popoutParent);
            }
            else {
                popout = new DockPopoutFrame(this);
            }
            // Configure new popout
            applySettings(popout.getBase());
            if (popoutIcons != null) {
                popout.getWindow().setIconImages(popoutIcons);
            }
            DockPopout popout2 = popout;
            popout.getWindow().addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    // User closed window directly
                    listener.popoutClosing(popout2);
                }
            });
        }
        popout.getBase().addContent(content);
        
        // Configure window
        Window window = popout.getWindow();
        if (size != null) {
            window.setSize(size);
        } else {
            window.setSize(600, 400);
        }
        if (location != null) {
            window.setLocation(location);
        }
        else {
            window.setLocationByPlatform(true);
        }
        if (popoutParent != null) {
            window.setAlwaysOnTop(popoutParent.isAlwaysOnTop());
        }
        window.setVisible(true);
        SwingUtilities.invokeLater(() -> window.toFront());
        
        // Finish up
        popouts.add(popout);
        removeActive(content);
        changedActiveContent(content, false);
        listener.popoutOpened(popout, content);
        
        return popout;
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
    
    /**
     * Perform an action on a copy of popouts to prevent concurrent modification
     * when the action might also close the popout.
     * 
     * @param function 
     */
    private void applyToPopoutsSafe(Consumer<DockPopout> function) {
        for (DockPopout popout : new ArrayList<>(popouts)) {
            function.accept(popout);
        }
    }
    
    private void updateWindowsAlwaysOnTop(boolean onTop) {
        for (DockPopout p : popouts) {
            p.getWindow().setAlwaysOnTop(onTop);
        }
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
            if (popoutParent != value) {
                if (popoutParent != null) {
                    popoutParent.removePropertyChangeListener("alwaysOnTop", popoutParentPropertyListener);
                }
                popoutParent = (Frame) value;
                popoutParent.addPropertyChangeListener("alwaysOnTop", popoutParentPropertyListener);
            }
        }
        else {
            settings.put(setting, value);
            main.setSetting(setting, value);
            popouts.forEach(w -> w.getBase().setSetting(setting, value));
        }
    }
    
    /**
     * Apply all stored setting values to the given child.
     * 
     * @param child 
     */
    public void applySettings(DockChild child) {
        settings.forEach((type, value) -> child.setSetting(type, value));
    }
    
}
