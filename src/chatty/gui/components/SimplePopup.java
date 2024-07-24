
package chatty.gui.components;

import chatty.util.Debugging;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Show a popup in relation to a component that will disappear after a few
 * seconds as well as when the mouse is moved over it.
 *
 * @author tduva
 */
public class SimplePopup {

    private static final Border POPUP_BORDER = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    private final Component owner;
    private final SimplePopupListener listener;
    private Popup popup;
    private Timer timer;
    
    public SimplePopup(Component owner, SimplePopupListener listener) {
        this.owner = owner;
        this.listener = listener;
    }
    
    public void showPopup(String text) {
        if (owner == null || !owner.isShowing()) {
            return;
        }
        hidePopup();
        
        JLabel label = new JLabel(text);
        label.setOpaque(false);
        label.setBorder(POPUP_BORDER);
        label.addMouseMotionListener(new MouseAdapter() {
            
            private int movedCount;
            
            @Override
            public void mouseMoved(MouseEvent e) {
                movedCount++;
                /**
                 * When the label appears with the mouse in it's location a
                 * moved event is already triggered, however the popup should
                 * only be removed when the mouse is actually actively moved.
                 */
                if (movedCount > 1) {
                    hidePopup();
                    if (listener != null) {
                        listener.popupHidden();
                    }
                }
            }
            
        });
        
        Dimension labelSize = label.getPreferredSize();
        
        Point location = owner.getLocationOnScreen();
        if (owner instanceof JTextComponent) {
            try {
                JTextComponent textComponent = (JTextComponent) owner;
                location = textComponent.modelToView(textComponent.getCaretPosition()).getLocation();
                SwingUtilities.convertPointToScreen(location, textComponent);
            } catch (BadLocationException ex) {
                
            }
        }
        
        popup = PopupFactory.getSharedInstance().getPopup(owner, label, location.x, location.y - labelSize.height - 5);
        popup.show();
        
        if (timer == null) {
            timer = new Timer(2000, e -> {
                        hidePopup();
                        if (listener != null) {
                            listener.popupHidden();
                        }
                    });
            timer.setRepeats(false);
        }
        timer.start();
    }
    
    private void hidePopup() {
        Debugging.edtLoud();
        if (popup != null) {
            popup.hide();
            timer.stop();
            popup = null;
        }
    }
    
    public static interface SimplePopupListener {
        
        public void popupHidden();
        
    }
    
}
