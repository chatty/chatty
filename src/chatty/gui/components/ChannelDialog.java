
package chatty.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.JDialog;

/**
 * Dialog for a popout channel
 * 
 * @author tduva
 */
public class ChannelDialog extends JDialog {
    
    public ChannelDialog(Window owner, Channel channel) {
        super(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(channel, BorderLayout.CENTER);

        setSize(new Dimension(600,400));
    }
    
}
