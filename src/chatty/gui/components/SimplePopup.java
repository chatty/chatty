
package chatty.gui.components;

import chatty.util.Debugging;
import chatty.util.ElapsedTime;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Show a popup in relation to a component that will disappear after a few
 * seconds as well as when the mouse is moved over it.
 *
 * @author tduva
 */
public class SimplePopup {

    private static final Border POPUP_BORDER_ERROR = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    private static final Border POPUP_BORDER_REGULAR = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
    
    public enum BorderStyle { REGULAR, ERROR }
    
    private final Component owner;
    private final SimplePopupListener listener;
    private Popup popup;
    private Timer timer;
    private final ElapsedTime shownTime;
    
    private String text;
    private int position;
    private BorderStyle borderStyle;
    
    private int showDuration;
    private AncestorListener visibilityListener;
    
    public SimplePopup(Component owner, SimplePopupListener listener) {
        this.owner = owner;
        this.listener = listener;
        shownTime = new ElapsedTime();
    }
    
    public void showPopup(String text) {
        showPopup(text, -1, 2000, BorderStyle.ERROR);
    }
    
    private void addVisibilityListener() {
        if (visibilityListener == null
                && owner instanceof JComponent) {
            visibilityListener = new AncestorListener() {
                @Override
                public void ancestorAdded(AncestorEvent event) {
                    reshow();
                }

                @Override
                public void ancestorRemoved(AncestorEvent event) {
                    tempHide();
                }

                @Override
                public void ancestorMoved(AncestorEvent event) {
                }
            };
            ((JComponent) owner).addAncestorListener(visibilityListener);
        }
    }
    
    private void removeVisibilityListener() {
        if (visibilityListener != null
                && owner instanceof JComponent) {
            ((JComponent) owner).removeAncestorListener(visibilityListener);
            visibilityListener = null;
        }
    }
    
    public void showPopup(String text, int position, int duration, BorderStyle borderStyle) {
        if (owner == null || !owner.isShowing()) {
            return;
        }
        showDuration = duration;
        hideNow();
        this.text = text;
        this.position = position;
        this.borderStyle = borderStyle;
        
        JLabel label = new JLabel(text);
        label.setOpaque(false);
        label.setBorder(borderStyle == BorderStyle.REGULAR ? POPUP_BORDER_REGULAR : POPUP_BORDER_ERROR);
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
                if (position == -1) {
                    position = textComponent.getCaretPosition();
                }
                location = textComponent.modelToView(position).getLocation();
                SwingUtilities.convertPointToScreen(location, textComponent);
            } catch (BadLocationException ex) {
                
            }
        }
        
        popup = PopupFactory.getSharedInstance().getPopup(owner, label, location.x, location.y - labelSize.height - 5);
        popup.show();
        shownTime.setSync();
        
        addVisibilityListener();
        
        createTimer(showDuration);
        timer.start();
    }
    
    private void createTimer(int duration) {
        timer = new Timer(duration, e -> {
                      hidePopup();
                      if (listener != null) {
                          listener.popupHidden();
                      }
                  });
        timer.setRepeats(false);
    }
    
    public void update() {
        if (popup != null) {
            showDuration -= shownTime.millisElapsedSync();
            if (showDuration > 0) {
                showPopup(text, position, showDuration, borderStyle);
            }
        }
    }
    
    private void hidePopup() {
        Debugging.edtLoud();
        hideNow();
        showDuration = 0;
        removeVisibilityListener();
    }
    
    private void tempHide() {
        hideNow();
        showDuration -= shownTime.millisElapsedSync();
    }
    
    private void reshow() {
        if (popup == null && showDuration > 500) {
            showPopup(text, position, showDuration, borderStyle);
        }
        else {
            removeVisibilityListener();
        }
    }
    
    private void hideNow() {
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
