
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.HtmlColors;
import chatty.lang.Language;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A single Color Setting represented by a preview, a textfield that contains
 * the color code and a button to change color.
 * 
 * @author tduva
 */
public class ColorSetting extends JPanel implements StringSetting {
    
    /**
     * Used to define whether this color represents a foreground or background
     * color.
     */
    public final static int FOREGROUND = 0;
    public final static int BACKGROUND = 1;
    
    private final int type;
    
    /**
     * The name of the color setting that should be the base (background) for
     * this one.
     */
    private final String baseColorSetting;
    /**
     * The text field that stores the color code.
     */
    private JTextField textField =  new JTextField(7);
    /**
     * Preview
     */
    private final JLabel preview = new JLabel();
    /**
     * The base color as a string
     */
    private String baseColor;
    /**
     * Primary and secondary colors as Color objects.
     */
    private Color currentColor;
    private Color secondaryColor;
    
    private final Set<ColorSettingListener> listeners = new HashSet<>();
    
    private final ColorChooser colorChooser;
    private final JButton chooseColor = new JButton(Language.getString("settings.colors.button.choose"));
    
    /**
     * 
     * 
     * @param type Foreground or background color.
     * @param baseColorSetting The name of the color setting for the background.
     * @param name The name of the color.
     * @param text A description or example of the text for this color.
     * @param chooser ColorChooser to use to select a new color.
     */
    public ColorSetting(final int type, String baseColorSetting,
            final String name, final String text, ColorChooser chooser) {
        
        this.type = type;
        this.baseColorSetting = baseColorSetting;
        this.colorChooser = chooser;
        
        // Set text and size of preview
        if (!text.isEmpty()) {
            preview.setText(" "+text);
            preview.setOpaque(true);
        }
        preview.setPreferredSize(new Dimension(160,20));
        
        // Choose color button action
        chooseColor.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String result = colorChooser.chooseColorString(type, currentColor, secondaryColor, name, text);
                setSettingValue(result);
            }
        });
        chooseColor.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        
        // Textfield settings
        textField.setEditable(false);
        
        
        initiate();
    }
    
    public boolean hasBase(String setting) {
        return setting.equals(baseColorSetting);
    }
    
    /**
     * Adds the components to the panel.
     */
    private void initiate() {
        add(preview);
        add(textField);
        add(chooseColor);
    }
    
    public String getText() {
        return textField.getText();
    }
    
    /**
     * Updates the preview and current Color objects based on the saved colors.
     */
    public void updated() {
        // Update Color objects based on current values
        currentColor = HtmlColors.decode(getSettingValue());
        secondaryColor = HtmlColors.decode(baseColor);
        
        // Choose the approriate background/foreground colors depending on type
        Color foregroundColor;
        Color backgroundColor;
        if (type == FOREGROUND) {
            foregroundColor = currentColor;
            backgroundColor = secondaryColor;
        } else {
            foregroundColor = secondaryColor;
            backgroundColor = currentColor;
        }
        //System.out.println(mainColor+" "+secondColor);
        //System.out.println(baseColor);
        preview.setForeground(foregroundColor);
        preview.setBackground(backgroundColor);
    }
    
    /**
     * Sets a new base color and updates the preview.
     * 
     * @param baseColor 
     */
    public void update(String baseColor) {
        this.baseColor = baseColor;
        updated();
    }

    @Override
    public String getSettingValue() {
        return textField.getText();
    }

    @Override
    public void setSettingValue(String value) {
        textField.setText(value);
        for (ColorSettingListener listener : listeners) {
            listener.colorUpdated();
        }
        updated();
    }
    
    public Color getSettingValueAsColor() {
        return currentColor;
    }
    
    public void addListener(ColorSettingListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    public void removeListener(ColorSettingListener listener) {
        listeners.remove(listener);
    }
    
}
