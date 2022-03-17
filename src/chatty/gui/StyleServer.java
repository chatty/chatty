
package chatty.gui;

import chatty.util.Timestamp;
import chatty.util.colors.ColorCorrector;
import java.awt.Color;
import java.awt.Font;
import javax.swing.text.MutableAttributeSet;

/**
 * Provide style information to other objects.
 * 
 * @author tduva
 */
public interface StyleServer {
    public Color getColor(String type);
    public MutableAttributeSet getStyle(String type);
    public Font getFont(String type);
    public Timestamp getTimestampFormat();
    public ColorCorrector getColorCorrector();
}
