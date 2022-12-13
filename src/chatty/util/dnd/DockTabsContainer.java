
package chatty.util.dnd;

import chatty.util.dnd.DockDropInfo.DropType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
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
    private boolean noSingle;
    private JPanel emptyLayout;
    private boolean keepEmpty = false;
    
    public DockTabsContainer() {
        setLayout(new BorderLayout());
        this.tabs = new DockTabs();
        this.tabs.setDockParent(this);
        addEmptyLayout();
    }
    
    private void addEmptyLayout() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel info = new JLabel("Nothing here at the moment.");
        info.setToolTipText("Drag&drop content here. In some cases content may also still be added automatically.");
        info.setAlignmentX(0.5f);
        JButton removeButton = new JButton("Remove");
        removeButton.setToolTipText("Remove this empty content area.");
        removeButton.setAlignmentX(0.5f);
        removeButton.addActionListener(e -> {
            if (isEmpty()) {
                cleanUp();
                parent.replace(DockTabsContainer.this, null);
            }
        });
        panel.add(Box.createVerticalGlue());
        panel.add(info);
        panel.add(Box.createVerticalStrut(5));
        panel.add(removeButton);
        panel.add(Box.createVerticalGlue());
        add(panel, BorderLayout.CENTER);
        emptyLayout = panel;
    }
    
    /**
     * Prevent the tab pane from asserting too much vertical space when a lot of
     * tabs are added (at least it seems as if each tab adds to the minimum
     * height, tested in horizontal/top tab layout).
     *
     * Of course this would also do the same for other components in the tab
     * pane that could prevent resizing of split panes.
     *
     * @return
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, 0);
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
    
    public boolean isSingleAllowed() {
        return singleAllowed;
    }
    
    public boolean isInSplit() {
        return !(parent instanceof DockBase);
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
        if (replacement == null && keepEmpty) {
            remove(tabs);
            addEmptyLayout();
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
        Component focusedComp = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        remove(singleContent.getComponent());
        add(tabs, BorderLayout.CENTER);
        tabs.addContent(singleContent);
        if (focusedComp != null) {
            /**
             * Even if getFocusOwner() still returns the correct component, the
             * focus doesn't seems to necessarily be there. Request focus again
             * explicitly to reset focus when switching to tabs, e.g. when a
             * whisper tab opens in the background while typing in a channel the
             * focus would get stolen away. Since it's an action without user
             * interaction this should at least be worked around a bit.
             */
            focusedComp.requestFocusInWindow();
        }
        singleContent = null;
    }

    @Override
    public void addContent(DockContent content) {
        if (emptyLayout != null) {
            remove(emptyLayout);
        }
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
            if (!keepEmpty) {
                parent.replace(this, null);
            }
            else {
                addEmptyLayout();
            }
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
        if (singleContent != null || isEmpty()) {
            DockDropInfo.DropType location = DockDropInfo.determineLocation(this, info.getLocation(this), 30, 1200, 20);
            if (location == location.CENTER) {
                return new DockDropInfo(this, DropType.TAB, DockDropInfo.makeRect(this, location, 40, 1200), isEmpty() ? 0 : 1);
            }
            return null;
        }
        else {
            return tabs.findDrop(info);
        }
    }

    @Override
    public void drop(DockTransferInfo info) {
        if (isEmpty()) {
            info.importInfo.content.setTargetPath(null);
            
            DockContent content = info.importInfo.content;
            info.importInfo.source.removeContent(content);
            addContent(content);
        }
        else {
            tabs.drop(info);
        }
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
            setSingleAllowed(parent instanceof DockBase && !noSingle);
            tabs.updateSingleAllowed();
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
        if (setting == DockSetting.Type.KEEP_EMPTY) {
            keepEmpty = DockSetting.getBoolean(value);
        }
        if (setting == DockSetting.Type.NO_SINGLE) {
            noSingle = DockSetting.getBoolean(value);
            updateSingleAllowed();
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
        String contentId = path.getContentId();
        if (contentId != null) {
            if (singleContent != null && singleContent.getId().equals(contentId)) {
                path.addParent(DockPathEntry.createTab(0));
            }
            else {
                path.addParent(DockPathEntry.createTab(tabs.getIndexByContentId(contentId)));
            }
        }
        return parent.buildPath(path, this);
    }

    @Override
    public DockLayoutElement getLayoutElement() {
        DockContent active = getCurrentContent();
        return new DockLayoutTabs(DockUtil.getContentIds(getContents()), active != null ? active.getId() : null);
    }

    @Override
    public void cleanUp() {
        tabs.cleanUp();
    }

    @Override
    public void sortContent(DockContent content) {
        if (!tabs.isEmpty()) {
            tabs.sortContent(content);
        }
    }

}
