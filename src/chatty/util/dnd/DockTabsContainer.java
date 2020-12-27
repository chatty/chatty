
package chatty.util.dnd;

import chatty.util.dnd.DockDropInfo.DropType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Contains a tab pane, which may not be shown when only a single content is
 * contained.
 * 
 * @author tduva
 */
public class DockTabsContainer extends JPanel implements DockChild {
    
    private DockBase base;
    
    /**
     * The current content in single mode. Must be null if in tabs mode.
     */
    private DockContent singleContent;
    private DockChild parent;
    private final DockTabs tabs;
    private boolean singleAllowed;
    private boolean singeAllowedLocked;
    
    public DockTabsContainer() {
        setLayout(new BorderLayout());
        this.tabs = new DockTabs();
        this.tabs.setDockParent(this);
    }
    
    /**
     * Single mode is only allowed when this component is directly added to a
     * base and only one content is contained.
     * 
     * @param allowed 
     */
    public void setSingleAllowed(boolean allowed) {
//        System.out.println("ALLOWED:"+allowed+" "+tabs.toString());
        if (allowed && tabs.getTabCount() == 1) {
            switchToSingle();
        }
        else if (singleContent != null) {
            switchToTabs();
        }
        this.singleAllowed = allowed;
    }
    
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void split(DockDropInfo info, DockContent content) {
        parent.split(info, content);
    }

    @Override
    public void replace(DockChild old, DockChild replacement) {
        if (replacement == null && !isEmpty()) {
            // If tabs are empty because it switched to single, don't do anything
            return;
        }
        if (old == tabs) {
            old = this;
        }
        parent.replace(old, replacement);
    }
    
    private void switchToSingle() {
        if (singleContent != null) {
            // Already in single mode
            return;
        }
        singleContent = tabs.getCurrentContent();
        if (singleContent != null) {
            tabs.removeContent(singleContent);
            add(singleContent.getComponent(), BorderLayout.CENTER);
            remove(tabs);
        }
    }
    
    public void switchToTabs() {
        if (getComponentCount() == 0) {
            // Just add tabs if currently nothing added
            add(tabs, BorderLayout.CENTER);
            return;
        }
        if (singleContent == null) {
            // Already in tabs mode
            return;
        }
        remove(singleContent.getComponent());
        add(tabs, BorderLayout.CENTER);
        tabs.addContent(singleContent);
        singleContent = null;
    }

    @Override
    public void addContent(DockContent content) {
        if (tabs.getTabCount() == 0) {
            // No content yet or one content in single mode
            if (singleContent == null && singleAllowed) {
                add(content.getComponent(), BorderLayout.CENTER);
                this.singleContent = content;
                this.validate();
                this.repaint();
            }
            else {
                switchToTabs();
                tabs.addContent(content);
            }
        }
        else {
            tabs.addContent(content);
        }
        content.setDockParent(this);
        // TODO: This would need to change when target is something that needs
        // to be created first (currently just here to be safe, it should also
        // work otherwise)
        content.setTargetPath(null);
    }

    @Override
    public void removeContent(DockContent content) {
        if (singleContent != null && singleContent == content) {
            // Currently single mode
            remove(singleContent.getComponent());
            singleContent = null;
            this.validate();
            this.repaint();
            parent.replace(this, null);
        }
        else if (tabs.containsContent(content)) {
            tabs.removeContent(content);
            if (tabs.getTabCount() == 1 && singleAllowed) {
                switchToSingle();
            }
        }
        content.setDockParent(null);
    }

    @Override
    public void setBase(DockBase base) {
        this.base = base;
        tabs.setBase(base);
    }

    @Override
    public DockDropInfo findDrop(DockImportInfo info) {
        if (singleContent != null) {
            DockDropInfo.DropType location = DockDropInfo.determineLocation(this, info.getLocation(this), 30, 1200, 20);
            if (location == location.CENTER) {
                return new DockDropInfo(this, DropType.TAB, DockDropInfo.makeRect(this, location, 40, 1200), 1);
            }
            return null;
        }
        else {
            return tabs.findDrop(info);
        }
    }

    @Override
    public void drop(DockTransferInfo info) {
        tabs.drop(info);
    }

    @Override
    public boolean isEmpty() {
        return tabs.isEmpty() && singleContent == null;
    }

    @Override
    public void setDockParent(DockChild parent) {
        this.parent = parent;
        updateSingleAllowed();
    }
    
    @Override
    public DockChild getDockParent() {
        return parent;
    }
    
    public DockContent getCurrentContent() {
        if (singleContent != null) {
            return singleContent;
        }
        else {
            return tabs.getCurrentContent();
        }
    }
    
    /**
     * Performs an update to single/tab mode based on whether this component
     * is directly added to a base.
     */
    public void updateSingleAllowed() {
        if (!singeAllowedLocked) {
            setSingleAllowed(parent instanceof DockBase);
        }
    }
    
    /**
     * Prevents automatic updates to single/tab mode until
     * {@link resetSingleAllowed()} is called.
     */
    public void setSingleAllowedLocked() {
        singeAllowedLocked = true;
    }
    
    /**
     * Allows automatic updates to single/tab mode and performs an update.
     */
    public void resetSingleAllowed() {
        singeAllowedLocked = false;
        updateSingleAllowed();
    }

    @Override
    public List<DockContent> getContents() {
        if (singleContent != null) {
            return Arrays.asList(new DockContent[]{singleContent});
        }
        return tabs.getContents();
    }

    @Override
    public void setActiveContent(DockContent content) {
        tabs.setActiveContent(content);
    }

    @Override
    public boolean isContentVisible(DockContent content) {
        return singleContent == content || tabs.isContentVisible(content);
    }

    @Override
    public List<DockContent> getContentsRelativeTo(DockContent content, int direction) {
        // If singleContent is active, the result would be empty anyway
        return tabs.getContentsRelativeTo(content, direction);
    }

    @Override
    public void setSetting(DockSetting.Type setting, Object value) {
        tabs.setSetting(setting, value);
        
        if (setting == DockSetting.Type.DEBUG) {
            setBorder(value == Boolean.TRUE ? BorderFactory.createLineBorder(Color.RED, 4) : null);
        }
    }
    
    @Override
    public String toString() {
        if (singleContent != null) {
            return "-["+singleContent.getTitle()+"]-";
        }
        return "-["+tabs.toString()+"]-";
    }

    @Override
    public DockPath getPath() {
        return parent.buildPath(new DockPath(), this);
    }

    @Override
    public DockPath buildPath(DockPath path, DockChild child) {
        DockContent content = path.getContent();
        if (content != null) {
            if (singleContent == content) {
                path.addParent(DockPathEntry.createTab(0));
            }
            else {
                path.addParent(DockPathEntry.createTab(tabs.indexOfComponent(content.getComponent())));
            }
        }
        return parent.buildPath(path, this);
    }

}
