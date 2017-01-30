
package chatty.gui.components.userinfo;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 *
 * @author tduva
 */
public class Util {
    
    public static GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(2,2,2,2);
        return gbc;
    }
    
}
