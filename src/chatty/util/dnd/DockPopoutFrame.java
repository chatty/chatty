
package chatty.util.dnd;

import java.awt.BorderLayout;
import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 *
 * @author tduva
 */
public class DockPopoutFrame extends JFrame implements DockPopout {

    private static int counter = 0;
    
    private final DockBase base;
    private String id;

    public DockPopoutFrame(DockManager m) {
        id = "frame"+(counter++);
        base = new DockBase(m);
        add(base, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    @Override
    public DockBase getBase() {
        return base;
    }

    @Override
    public Window getWindow() {
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId() {
        this.id = id;
    }

}
