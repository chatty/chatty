
package chatty.gui.components.settings;

import chatty.util.StringUtil;
import java.awt.Component;
import javax.swing.JCheckBox;

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
    
}
