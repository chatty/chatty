
package chatty.gui;

import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps all added windows on top of the main window.
 * 
 * @author tduva
 */
public class WindowManager {
    
    private final List<Window> ontop = new ArrayList<>();
    
    public WindowManager(Window main) {
        main.addWindowListener(new Listener());
    }
    
    public final void addWindowOnTop(Window window) {
        ontop.add(window);
    }
    
    /**
     * Brings all added windows to the front, without giving them focus.
     */
    private void updateOnTop() {
        for (final Window w : ontop) {
            w.setAutoRequestFocus(false);
            w.toFront();
            w.setAutoRequestFocus(true);
        }
    }
    
    /**
     * Reacts on the main window becoming activated.
     */
    private class Listener extends WindowAdapter {

        @Override
        public void windowActivated(WindowEvent e) {
            updateOnTop();
        }
    }
    
}
