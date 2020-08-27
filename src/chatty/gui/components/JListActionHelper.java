
package chatty.gui.components;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import javax.swing.JList;

/**
 *
 * @author tduva
 */
public class JListActionHelper<T> implements MouseListener, KeyListener {
    
    public enum Action { CONTEXT_MENU, ENTER, CTRL_ENTER, DOUBLE_CLICK, SPACE }
    
    private final JList<T> list;
    private final JListContextMenuListener<T> listener;
    private final boolean allowClearSelection;
    
    public JListActionHelper(JList<T> list, JListContextMenuListener<T> listener, boolean allowClearSelection) {
        this.list = list;
        this.listener = listener;
        this.allowClearSelection = allowClearSelection;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        selectClicked(e, true);
        checkOpenContextMenu(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        checkOpenContextMenu(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            otherAction(Action.DOUBLE_CLICK);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
    
    /**
     * Adds selection of the clicked element, or removes selection if no
     * element was clicked.
     * 
     * @param e
     * @param onlyOutside 
     */
    private void selectClicked(MouseEvent e, boolean onlyOutside) {
        int index = list.locationToIndex(e.getPoint());
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds != null && bounds.contains(e.getPoint())) {
            if (!onlyOutside) {
                if (list.isSelectedIndex(index)) {
                    list.addSelectionInterval(index, index);
                } else {
                    list.setSelectedIndex(index);
                }
            }
        } else if (allowClearSelection) {
            list.clearSelection();
        }
    }
    
    private void checkOpenContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            selectClicked(e, false);
            openContextMenu(e.getPoint());
        }
    }
    
    private void openContextMenu(Point location) {
        List<T> selected = list.getSelectedValuesList();
        listener.handleAction(Action.CONTEXT_MENU, location, selected);
    }
    
    private void otherAction(Action action) {
        listener.handleAction(action, getSelectedLocation(), list.getSelectedValuesList());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
            openContextMenu(getSelectedLocation());
        }
        else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (e.isControlDown()) {
                otherAction(Action.CTRL_ENTER);
            }
            else {
                otherAction(Action.ENTER);
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            otherAction(Action.SPACE);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
    
    private Point getSelectedLocation() {
        int selected = list.getSelectedIndex();
        Rectangle b = list.getCellBounds(selected, selected);
        if (b != null) {
            return new Point(b.x, b.y);
        }
        return new Point(0, 0);
    }
    
    public interface JListContextMenuListener<T> {
        public void handleAction(Action action, Point location, List<T> selected);
    }
    
    public static <T> void install(JList<T> list, JListContextMenuListener<T> listener) {
        JListActionHelper<T> helper = new JListActionHelper<>(list, listener, true);
        list.addMouseListener(helper);
        list.addKeyListener(helper);
    }
    
    public static <T> void install(JList<T> list, JListContextMenuListener<T> listener, boolean allowClearSelection) {
        JListActionHelper<T> helper = new JListActionHelper<>(list, listener, allowClearSelection);
        list.addMouseListener(helper);
        list.addKeyListener(helper);
    }
    
}
