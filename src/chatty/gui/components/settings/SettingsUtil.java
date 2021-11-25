
package chatty.gui.components.settings;

import static chatty.gui.components.settings.SettingsDialog.makeGbc;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Function;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class SettingsUtil {
    
    public static void addSubsettings(JCheckBox control, Component... subs) {
        control.addItemListener(e -> {
            for (Component sub : subs) {
                if (sub != control) {
                    sub.setEnabled(control.isSelected() && control.isEnabled());
                }
            }
        });
        control.addPropertyChangeListener("enabled", e -> {
            for (Component sub : subs) {
                if (sub != control) {
                    sub.setEnabled(control.isSelected() && control.isEnabled());
                }
            }
        });
        for (Component sub : subs) {
            if (sub != control) {
                sub.setEnabled(false);
            }
        }
    }
    
    public static void addSubsettings(ComboStringSetting control, Function<String, Boolean> req, Component... subs) {
        control.addSettingChangeListener(c -> {
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
    
    public static JLabel createLabel(String settingName) {
        return createLabel(settingName, false);
    }
    
    public static JLabel createLabel(String settingName, boolean info) {
        String baseKey = "settings.label."+settingName;
        if (settingName.contains(".")) {
            baseKey = settingName;
        }
        String text = Language.getString(baseKey);
        String tip = Language.getString(baseKey+".tip", false);
        JLabel label;
        if (info) {
            label = new JLabel(SettingConstants.HTML_PREFIX+text);
        } else {
            label = new JLabel(text);
        }
        label.setToolTipText(SettingsUtil.addTooltipLinebreaks(tip));
        return label;
    }
    
    public static void addLabeledComponent(JPanel panel, String labelSettingName, int x, int y, int w, int labelAlign, JComponent component) {
        addLabeledComponent(panel, labelSettingName, x, y, w, labelAlign, component, false);
    }
    
    public static void addLabeledComponent(JPanel panel, String labelSettingName, int x, int y, int w, int labelAlign, JComponent component, boolean stretchComponent) {
        JLabel label = createLabel(labelSettingName);
        label.setLabelFor(component);
        panel.add(label, SettingsDialog.makeGbc(x, y, 1, 1, labelAlign));
        GridBagConstraints gbc = SettingsDialog.makeGbc(x+1, y, w, 1, GridBagConstraints.WEST);
        if (stretchComponent) {
            gbc.fill = GridBagConstraints.BOTH;
        }
        panel.add(component, gbc);
    }
    
    public static JPanel createPanel(String settingName, JComponent... settingComponent) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = makeGbc(0, 0, 1, 1);
        // Make sure to only have space between the two components, since other
        // spacing will be added when this panel is added to the layout
        gbc.insets = new Insets(0, 0, 0, gbc.insets.right);
        panel.add(createLabel(settingName), gbc);
        gbc = makeGbc(1, 0, 1, 1);
        gbc.insets = new Insets(0, gbc.insets.left, 0, 0);
        for (JComponent comp : settingComponent) {
            panel.add(comp, gbc);
            gbc.gridx++;
        }
        return panel;
    }
    
}
