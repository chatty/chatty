
package chatty.util.dnd;

import java.awt.Color;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * @author tduva
 */
public class DockContentContainer<T extends JComponent> implements DockContent {

    private final T component;
    private String title;
    private final DockManager m;
    private final Set<DockContentPropertyListener> listeners;
    private Color foregroundColor;
    private DockChild parent;
    private DockPath targetPath;
    
    public DockContentContainer(String title, T component, DockManager m) {
        this.component = component;
        this.title = title;
        this.m = m;
        listeners = new HashSet<>();
    }
    
    @Override
    public JComponent getComponent() {
        return component;
    }
    
    public T getContent() {
        return component;
    }
    
    public boolean isContentVisible() {
        return m.isContentVisible(this);
    }

    @Override
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String newTitle) {
        if (newTitle == null) {
            newTitle = "";
        }
        if (!newTitle.equals(title)) {
            title = newTitle;
            listeners.forEach(l -> l.titleChanged(this));
        }
    }
    
    @Override
    public String toString() {
        return getTitle()+" ["+getComponent()+"]";
    }

    @Override
    public JPopupMenu getContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem("Popout "+title);
        item.addActionListener(e -> {
            m.popout(this, DockSetting.PopoutType.DIALOG, null, null);
        });
        menu.add(item);
        JMenuItem item2 = new JMenuItem("Popout as window");
        item2.addActionListener(e -> {
            m.popout(this, DockSetting.PopoutType.FRAME, null, null);
        });
        menu.add(item2);
        JMenuItem closeItem = new JMenuItem("Close");
        closeItem.addActionListener(e -> {
            remove();
        });
        menu.add(closeItem);
        return menu;
    }

    @Override
    public void addListener(DockContentPropertyListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(DockContentPropertyListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public Color getForegroundColor() {
        return foregroundColor;
    }
    
    public void setForegroundColor(Color color) {
        if (!Objects.equals(color, foregroundColor)) {
            this.foregroundColor = color;
            listeners.forEach(l -> l.foregroundColorChanged(this));
        }
    }

    @Override
    public DockTabComponent getTabComponent() {
        return null;
    }

    @Override
    public void remove() {
        m.removeContent(this);
    }

    @Override
    public DockPath getPath() {
        if (parent != null) {
            return parent.buildPath(new DockPath(this), null);
        }
        return null;
    }

    @Override
    public void setTargetPath(DockPath path) {
        this.targetPath = path;
    }
    
    @Override
    public DockPath getTargetPath() {
        return targetPath;
    }

    @Override
    public void setDockParent(DockChild parent) {
        this.parent = parent;
    }

    @Override
    public boolean canPopout() {
        return true;
    }
    
}
