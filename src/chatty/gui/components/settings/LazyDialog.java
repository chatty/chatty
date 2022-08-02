
package chatty.gui.components.settings;

import java.awt.Component;
import javax.swing.JDialog;

/**
 *
 * @author tduva
 */
public abstract class LazyDialog {

    private JDialog dialog;
    
    public abstract JDialog createDialog();
    
    public void show(Component comp) {
        if (dialog == null) {
            dialog = createDialog();
        }
        dialog.setLocationRelativeTo(comp);
        dialog.setVisible(true);
    }
    
}
