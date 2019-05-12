
package chatty.gui.components.settings;

import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.FlowLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class SettingsUtil {
    
    public static void addSubsettings(JCheckBox control, Component... subs) {
        control.addItemListener(e -> {
            for (Component sub : subs) {
                sub.setEnabled(control.isSelected() && control.isEnabled());
            }
        });
        control.addPropertyChangeListener("enabled", e -> {
            for (Component sub : subs) {
                sub.setEnabled(control.isSelected() && control.isEnabled());
            }
        });
        for (Component sub : subs) {
            sub.setEnabled(false);
        }
    }
    
    public static String addTooltipLinebreaks(String tooltipText) {
        if (tooltipText != null && !tooltipText.isEmpty()) {
            tooltipText = "<html><body>"+StringUtil.addLinebreaks(tooltipText, 70, true);
        }
        return tooltipText;
    }
    
    public static JPanel createStandardGapPanel() {
        JPanel result = new JPanel();
        result.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 4));
        return result;
    }
    
}
