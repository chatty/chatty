
package chatty.util.dnd;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JDialog;
import javax.swing.WindowConstants;

/**
 *
 * @author tduva
 */
public class DockPoputDialog extends JDialog implements DockPopout {

    private static int counter = 0;
    
    private final DockBase base;
    private String id;
    
    public DockPoputDialog(DockManager m, Frame parent) {
        super(parent);
        id = "frame"+(counter++);
        setLayout(new BorderLayout());
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
