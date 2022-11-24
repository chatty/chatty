
package chatty.util.dnd;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.Collection;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 *
 * @author tduva
 */
public class DockPopoutFrame extends JFrame implements DockPopout {

    private final DockBase base;
    private String id;
    
    private boolean fixedAlwaysOnTop;

    public DockPopoutFrame(DockManager m) {
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
    public void setId(String id) {
        this.id = id;
    }
    
    @Override
    public void setId(Collection<DockPopout> inUse) {
        id = DockPopout.makeId("f", inUse);
    }
    
    protected void setFixedAlwaysOnTop(boolean fixed) {
        this.fixedAlwaysOnTop = fixed;
    }
    
    protected boolean isFixedAlwaysOnTop() {
        return fixedAlwaysOnTop;
    }

}
