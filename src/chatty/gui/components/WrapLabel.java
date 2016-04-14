
package chatty.gui.components;

import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JTextArea;

/**
 *
 * @author tduva
 */
public class WrapLabel extends JTextArea {
    
    private static final JLabel LABEL = new JLabel();
    
    public WrapLabel() {
        setWrapStyleWord(true);
        setLineWrap(true);
        setBackground(LABEL.getBackground());
        setFont(LABEL.getFont());
        setBorder(LABEL.getBorder());
        setFocusable(false);
        setForeground(LABEL.getForeground());
        setOpaque(false);
        System.out.println(getColumns());
        setRows(1);
        setColumns(20);
    }
    
    public WrapLabel(String text) {
        this();
        setText(text);
    }
    
//    @Override
//    public boolean getScrollableTracksViewportWidth() {
//        return false;
//    }
    
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        System.out.println(d);
        return d;
    }
    
}
