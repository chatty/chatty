
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.NamedColor;
import chatty.lang.Language;
import chatty.util.colors.ColorCorrection;
import chatty.util.colors.ColorCorrector;
import chatty.util.colors.HtmlColors;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
public class UsercolorCorrectionPreview extends JDialog {
    
    public UsercolorCorrectionPreview(Window parent, Color background) {
        super(parent);
        
        setTitle("Usercolor Correction Preview");
        
        boolean darkBg = ColorCorrection.isDarkColor(background);
        
        JPanel main = new JPanel(new GridBagLayout());
        main.setBackground(background);
        
        //===============
        // Setting Names
        //===============
        int column = 1;
        for (String type : ColorCorrector.ACTIVE_TYPES) {
            String label = Language.getString("settings.string.nickColorCorrection.option."+type);
            main.add(createLabel(label, darkBg ? Color.WHITE : Color.BLACK),
                    GuiUtil.makeGbc(column, 0, 1, 1));
            column++;
        }
        
        //=========
        // Preview
        //=========
        int row = 1;
        for (NamedColor color : HtmlColors.getNamedColors()) {
            int diff = ColorCorrection.getBrightnessDifference(color, background);
            if (Math.abs(diff) > 100) {
                continue;
            }
            
            //----------------
            // Original Color
            //----------------
            main.add(createLabel(color.getName(), color),
                    GuiUtil.makeGbc(0, row, 1, 1));
            
            //------------------
            // Corrected Colors
            //------------------
            column = 1;
            for (String type : ColorCorrector.ACTIVE_TYPES) {
                Color correctedColor = ColorCorrector.get(type).correctColor(color, background);
                String label = HtmlColors.getColorString(correctedColor);
                if (correctedColor.equals(color)) {
                    label = "Unchanged";
                }
                main.add(createLabel(label, correctedColor),
                        GuiUtil.makeGbc(column, row, 1, 1));
                column++;
            }
            
            row++;
        }

        add(new JScrollPane(main), BorderLayout.CENTER);
        
        setPreferredSize(new Dimension(getPreferredSize().width + 100, 600));
        pack();
        setLocationRelativeTo(parent);
        setVisible(true);
    }
    
    private static JLabel createLabel(String text, Color foreground) {
        JLabel label = new JLabel(text);
        label.setForeground(foreground);
        return label;
    }
    
    //=========
    // Testing
    //=========
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UsercolorCorrectionPreview(null, new Color(250, 250, 250)).addComponentListener(new ComponentAdapter() {

                @Override
                public void componentHidden(ComponentEvent e) {
                    System.exit(0);
                }
            });
        });
    }
    
}
