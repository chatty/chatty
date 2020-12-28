
package chatty.util.dnd;

import chatty.util.Debugging;
import chatty.util.dnd.DockDropInfo.DropType;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

/**
 * Shows two components side by side (horizontal or vertical).
 * 
 * @author tduva
 */
public class DockSplit extends JSplitPane implements DockChild {
    
    private DockBase base;
    private DockChild parent;
    
    private DockChild left;
    private DockChild right;
    
    public DockSplit(int orientation, DockChild left, DockChild right) {
        super(orientation, left.getComponent(), right.getComponent());
//        if (left instanceof JSplitPane) {
//            ((JSplitPane)left).setBorder(null);
//        }
//        if (right instanceof JSplitPane) {
//            ((JSplitPane)right).setBorder(null);
//        }
        this.left = left;
        this.right = right;
    }

    @Override
    public JComponent getComponent() {
        return this;
    }
    
    public DockChild getLeftChild() {
        return left;
    }
    
    public DockChild getRightChild() {
        return right;
    }

    @Override
    public void split(DockDropInfo info, DockContent content) {
        //        System.out.println("LEFT: "+left+" DROP: "+info.dropComponent+" RIGHT: "+right);
        DockChild presentComp = null;
        if (checkComponent(left, info)) {
            presentComp = left;
        }
        else if (checkComponent(right, info)) {
            presentComp = right;
        }
        if (presentComp == null) {
            return;
        }
        DockTabsContainer newCompTabs = new DockTabsContainer();
        DockSplit newChildSplit = DockUtil.createSplit(info, presentComp, newCompTabs);
        if (newChildSplit != null) {
            // Configure child first
            base.applySettings(newChildSplit);
            newChildSplit.setBase(base);
            newChildSplit.setDockParent(this);
            // Configure tabs (new left or right component)
            base.applySettings(newCompTabs);
            newCompTabs.setBase(base);
            newCompTabs.setDockParent(newChildSplit);
            newCompTabs.addContent(content);
            // Configure present comp
            presentComp.setDockParent(newChildSplit);
            
            // Exchange left or right component
            if (checkComponent(left, info)) {
                setLeftComponent(newChildSplit);
                left = newChildSplit;
            }
            else if (checkComponent(right, info)) {
                setRightComponent(newChildSplit);
                right = newChildSplit;
            }
            // Divider
            DockSplit split = newChildSplit;
            split.setDividerLocation(0.5);
            split.setResizeWeight(0.5);
            SwingUtilities.invokeLater(() -> {
                split.setDividerLocation(0.5);
                split.setResizeWeight(0.5);
            });
            DockUtil.preserveDividerLocation(this);
        }
    }
    
    private boolean checkComponent(DockChild parent, DockDropInfo info) {
        if (parent == info.dropComponent) {
            return true;
        }
        return parent == info.dropComponent.getComponent().getParent();
    }

    @Override
    public void addContent(DockContent content) {
        DockPathEntry next = DockUtil.getNext(content, this);
        if (next != null) {
            Debugging.println("dndp", "%s -> %s", this, next);
            if (next.type == DockPathEntry.Type.SPLIT
                    && (next.location == DropType.RIGHT || next.location == DropType.BOTTOM)) {
                right.addContent(content);
            }
            else {
                left.addContent(content);
            }
        }
        else {
            left.addContent(content);
        }
    }
    
    @Override
    public void removeContent(DockContent content) {
        left.removeContent(content);
        right.removeContent(content);
    }

    @Override
    public void setBase(DockBase base) {
        this.base = base;
    }

    @Override
    public DockDropInfo findDrop(DockImportInfo info) {
//        System.out.println(info.getLocation(this)+" "+getLeftComponent().getBounds()+" "+getRightComponent().getBounds());
        Rectangle leftBounds = getLeftComponent().getBounds();
        Point p = info.getLocation(this);
        if (leftBounds.contains(p)) {
            return left.findDrop(info);
        }
        else if (getRightComponent().getBounds().contains(p)) {
            return right.findDrop(info);
        }
        return null;
    }
    
    @Override
    public boolean isEmpty() {
        // If the split exists, it should probably always contain content, but
        // not entirely sure
        return false;
    }

    @Override
    public void drop(DockTransferInfo info) {
        // This component does not receive drops
    }

    @Override
    public void setDockParent(DockChild parent) {
        this.parent = parent;
    }
    
    @Override
    public DockChild getDockParent() {
        return parent;
    }
    
    @Override
    public String toString() {
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            return String.format("%s || %s", left, right);
        }
        else {
            return String.format("%s -- %s", left, right);
        }
    }

    @Override
    public void replace(DockChild old, DockChild replacement) {
        if (old == left) {
            if (replacement == null) {
                // Remove "left" side (close this split, replace with "right")
                parent.replace(this, right);
                DockUtil.activeTabAfterJoin(right, base);
            }
            else {
                // Exchange "left" side (keep this split open)
                setLeftComponent(replacement.getComponent());
                left = replacement;
                replacement.setDockParent(this);
                DockUtil.preserveDividerLocation(this);
            }
        }
        else if (old == right) {
            if (replacement == null) {
                // Remove "right" side (close this split, replace with "left")
                parent.replace(this, left);
                DockUtil.activeTabAfterJoin(left, base);
            }
            else {
                // Exchange "right" side (keep this split open)
                setRightComponent(replacement.getComponent());
                right = replacement;
                replacement.setDockParent(this);
                DockUtil.preserveDividerLocation(this);
            }
        }
    }

    @Override
    public List<DockContent> getContents() {
        List<DockContent> result = new ArrayList<>();
        result.addAll(left.getContents());
        result.addAll(right.getContents());
        return result;
    }

    @Override
    public void setActiveContent(DockContent content) {
        left.setActiveContent(content);
        right.setActiveContent(content);
    }

    @Override
    public boolean isContentVisible(DockContent content) {
        return left.isContentVisible(content) || right.isContentVisible(content);
    }

    @Override
    public List<DockContent> getContentsRelativeTo(DockContent content, int direction) {
        if (left.getContents().contains(content)) {
            return left.getContentsRelativeTo(content, direction);
        }
        else {
            return right.getContentsRelativeTo(content, direction);
        }
    }

    @Override
    public void setSetting(DockSetting.Type setting, Object value) {
        left.setSetting(setting, value);
        right.setSetting(setting, value);
        
        if (setting == DockSetting.Type.DEBUG) {
            setBorder(value == Boolean.TRUE ? BorderFactory.createLineBorder(Color.BLUE, 4) : null);
        }
        if (setting == DockSetting.Type.DIVIDER_SIZE) {
            setDividerSize(((Number)value).intValue());
        }
    }

    @Override
    public DockPath getPath() {
        return parent.buildPath(new DockPath(), this);
    }

    @Override
    public DockPath buildPath(DockPath path, DockChild child) {
        DropType type = null;
        if (child == left) {
            if (getOrientation() == HORIZONTAL_SPLIT) {
                type = DropType.LEFT;
            }
            else {
                type = DropType.TOP;
            }
        }
        else if (child == right) {
            if (getOrientation() == HORIZONTAL_SPLIT) {
                type = DropType.RIGHT;
            }
            else {
                type = DropType.BOTTOM;
            }
        }
        path.addParent(DockPathEntry.createSplit(type));
        return parent.buildPath(path, this);
    }
    
}
