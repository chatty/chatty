
package chatty.gui.components.settings;

import chatty.gui.NamedColor;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.colors.HtmlColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Show a preview of the different Nickcolor Correction settings.
 * 
 * @author tduva
 */
public class UsercolorBackgroundPreview extends JDialog {
    
    private int column = 0;
    
    public UsercolorBackgroundPreview(Window parent) {
        super(parent);
        
        setTitle("Usercolor Correction Preview");
        
        JPanel main = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 2, 1, 1);
        
        add(main, gbc, 0);
        add(main, gbc, 38);
        add(main, gbc, 89);

        add(new JScrollPane(main), BorderLayout.CENTER);
        
        setPreferredSize(new Dimension(getPreferredSize().width + 100, 600));
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
    
    private void add(JPanel main, GridBagConstraints gbc, int threshold) {
        gbc.gridx = column;
        gbc.gridy = 0;
        column++;
        List<NamedColor> colors = HtmlColors.getNamedColors();
        for (int i=0;i<colors.size();i += 4) {
            Color bgColor = colors.get(i);
            for (int j=0;j<colors.size();j += 4) {
                Color fgColor = colors.get(j);
                int difference = Math.abs(ColorCorrectionNew.getLightnessDifference(fgColor, bgColor));
                if (difference < 90) {
                    Color actualBgColor = bgColor;
                    Color actualFgColor = fgColor;
                    if (difference < threshold) {
                        if (ColorCorrectionNew.getLightness(actualFgColor) > 128) {
                            actualBgColor = Color.BLACK;
                        } else {
                            actualBgColor = Color.WHITE;
                        }
                        if (ColorCorrectionNew.getLightnessDifferenceAbs(fgColor, actualBgColor) < difference) {
                            actualBgColor = bgColor;
                        }
                        //actualBgColor = Color.WHITE;
//                        actualFgColor = Color.WHITE;
                    }
                    JLabel label = createLabel("Test", actualFgColor, actualBgColor);
                    label.setToolTipText(String.valueOf(difference));
                    main.add(label, gbc);
                    gbc.gridy++;
                }
            }
        }
    }
    
    private static JLabel createLabel(String text, Color foreground, Color background) {
        JLabel label = new JLabel(text);
        label.setForeground(foreground);
        label.setBackground(background);
        label.setOpaque(true);
        return label;
    }
    
    //=========
    // Testing
    //=========
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UsercolorBackgroundPreview(null).addComponentListener(new ComponentAdapter() {

                @Override
                public void componentHidden(ComponentEvent e) {
                    System.exit(0);
                }
            });
        });
    }
    
}
