
package chatty.gui.components.settings;

import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.function.Function;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
    
    public static void addSubsettings(ComboStringSetting control, Function<String, Boolean> req, Component... subs) {
        control.addItemListener(e -> {
            String selected = control.getSettingValue();
            for (Component sub : subs) {
                sub.setEnabled(req.apply(selected));
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
    
    /**
     * Remove sections in the input (usually HTML), based on the given type.
     *
     * <p>
     * Sections to remove must be enclosed in one of the two ways:
     * <ul>
     * <li>{@code <!--#START:<type>#-->} to {@code <!--#END:<type>#-->} (all
     * sections that don't match the given type are removed)
     * <li>{@code <!--#START:!<type>#-->} to {@code <!--#END:!<type>#-->} (all
     * sections that match the given type are removed)
     * </ul>
     * <p>
     * Not necessarily designed for overlapping or inside of eachother sections,
     * but those could still work to some extend.
     *
     * @param input The text to modify
     * @param type The type
     * @return The modified text
     */
    public static String removeHtmlConditions(String input, String type) {
        input = input.replaceAll(String.format(
                "(?s)<!--#START:(?!%1$s(?!\\w+))\\w+#-->(.*?)<!--#END:(?!%1$s(?!\\w+))\\w+#-->",
                type), "");
        input = input.replaceAll(String.format(
                "(?s)<!--#START:!%1$s#-->(.*?)<!--#END:!%1$s#-->",
                type), "");
        return input;
    }
    
    public static String getInfo(String file, String type) {
        String html = StringUtil.stringFromInputStream(SettingsUtil.class.getResourceAsStream(file));
        if (type != null) {
            return removeHtmlConditions(html, type);
        }
        return html;
    }
    
}
