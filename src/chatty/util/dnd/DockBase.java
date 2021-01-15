
package chatty.util.dnd;

import chatty.util.dnd.DockDropInfo.DropType;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Holds content or components that hold content (tabs).
 * 
 * @author tduva
 */
public class DockBase extends JPanel implements DockChild {
    
    private final DockOverlay overlay;
    private final DockManager manager;
    
    private DockChild child;
    
    public DockBase(DockManager m) {
        setLayout(new BorderLayout());
        child = new DockTabsContainer();
        child.setBase(this);
        child.setDockParent(this);
        add(child.getComponent(), BorderLayout.CENTER);
        overlay = new DockOverlay(this);
        this.manager = m;
    }
    
    public DockDropInfo findDrop(DockImportInfo info) {
        DockDropInfo childDrop = child.findDrop(info);
        if (childDrop != null && childDrop.location == DropType.INVALID) {
            return null;
        }
        if (childDrop != null) {
            return childDrop;
        }
        else {
            DropType location = DockDropInfo.determineLocation(this, info.getLocation(this), 20, 80, 0);
            if (location != null && location != DropType.CENTER) {
                return new DockDropInfo(this, location, DockDropInfo.makeRect(this, location, 20, 0, 80), -1);
            }
        }
        return childDrop;
    }
    
    @Override
    public void addContent(DockContent content) {
        child.addContent(content);
    }
    
    public void requestDrag(boolean allowPopout) {
        manager.requestDrag(allowPopout);
    }
    
    public void startDrag() {
        getRootPane().setGlassPane(overlay);
        overlay.setVisible(true);
        overlay.setOpaque(false);
    }
    
    /**
     * Informs the manager that the drag has stopped. The manager will then
     * inform all bases.
     * 
     * @param t 
     */
    public void requestStopDrag(DockTransferable t) {
        manager.requestStopDrag(t);
    }
    
    /**
     * Remove the drag overlay.
     */
    public void stopDrag() {
        overlay.setVisible(false);
        repaint();
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void split(DockDropInfo info, DockContent content) {
        if (child.isEmpty()) {
            child.addContent(content);
            return;
        }
        DockTabsContainer newCompTabs = new DockTabsContainer();
        DockSplit newChildSplit = DockUtil.createSplit(info, child, newCompTabs);
        if (newChildSplit != null) {
            // Configure new child
            applySettings(newChildSplit);
            newChildSplit.setBase(this);
            newChildSplit.setDockParent(this);
            // Configure tabs (basicially caused the split)
            applySettings(newCompTabs);
            newCompTabs.setBase(this);
            newCompTabs.setDockParent(newChildSplit);
            newCompTabs.addContent(content);
            // Configure old child (now component in new split)
            child.setDockParent(newChildSplit);
            
            // Exchange child
            remove(child.getComponent());
            child = newChildSplit;
            add(child.getComponent(), BorderLayout.CENTER);
            // Divider
            DockSplit split = newChildSplit;
            split.setDividerLocation(0.5);
            split.setResizeWeight(0.5);
            SwingUtilities.invokeLater(() -> {
                split.setDividerLocation(0.5);
                split.setResizeWeight(0.5);
            });
        }
    }

    @Override
    public void setBase(DockBase base) {
        // Not applicable, this is the base
    }
    
    @Override
    public void setDockParent(DockChild parent) {
        // Not applicable, this is the top component
    }
    
    @Override
    public DockChild getDockParent() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return child.isEmpty();
    }

    @Override
    public void removeContent(DockContent content) {
        child.removeContent(content);
        validate();
    }

    @Override
    public void drop(DockTransferInfo info) {
        // Once a drop was initiated by the user, should reset target path
        info.importInfo.content.setTargetPath(null);
        
        info.importInfo.source.removeContent(info.importInfo.content);
        split(info.dropInfo, info.importInfo.content);
    }

    @Override
    public void replace(DockChild old, DockChild replacement) {
        if (replacement == null) {
//            System.out.println("BASE EMPTY");
            manager.baseEmpty(this);
            return;
        }
        if (old == child && old != replacement) {
    //        System.out.println("BASE REPLACE "+old+"\nWITH "+replacement);
            remove(child.getComponent());
            replacement.setBase(this);
            replacement.setDockParent(this);
            child = replacement;
            add(child.getComponent(), BorderLayout.CENTER);
        }
    }

    @Override
    public List<DockContent> getContents() {
        return child.getContents();
    }
    
    /**
     * When a new tab is active. Also when a split gets removed.
     * 
     * @param tabs The tab pane the tab was changed in, may be null
     * @param content 
     */
    public void tabChanged(DockTabs tabs, DockContent content) {
        manager.changedActiveContent(content, false);
    }

    @Override
    public void setActiveContent(DockContent content) {
        child.setActiveContent(content);
    }

    @Override
    public boolean isContentVisible(DockContent content) {
        return child.isContentVisible(content);
    }

    @Override
    public List<DockContent> getContentsRelativeTo(DockContent content, int direction) {
        return child.getContentsRelativeTo(content, direction);
    }

    @Override
    public void setSetting(DockSetting.Type setting, Object value) {
        switch (setting) {
            case FILL_COLOR:
                overlay.setFillColor(DockSetting.getColor(value));
                break;
            case LINE_COLOR:
                overlay.setLineColor(DockSetting.getColor(value));
                break;
        }
        child.setSetting(setting, value);
    }
    
    /**
     * Apply all setting values stored in the manager to the given child.
     * 
     * @param child 
     */
    public void applySettings(DockChild child) {
        manager.applySettings(child);
    }

    @Override
    public DockPath getPath() {
        return buildPath(new DockPath(), null);
    }

    @Override
    public DockPath buildPath(DockPath path, DockChild child) {
        String popoutId = null;
        for (DockPopout popout : manager.getPopouts()) {
            if (popout.getBase() == this) {
                popoutId = popout.getId();
            }
        }
        path.addParent(DockPathEntry.createPopout(popoutId));
        return path;
    }
    
    @Override
    public String toString() {
        return "DockBase";
    }
    
}
