
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.lang.Language;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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
    private String baseColorSetting;
    /**
     * The text field that stores the color code.
     */
    private final JTextField textField =  new JTextField(6);
    /**
     * Preview
     */
    private final JLabel preview = new JLabel();
    /**
     * The base color as a string
     */
    private String baseColor;
    
    private boolean useBaseColor = true;
    
    /**
     * Primary and secondary colors as Color objects.
     */
    private Color currentColor;
    private Color secondaryColor;
    private String previewText;
    
    private final Set<ColorSettingListener> listeners = new HashSet<>();
    
    private final ColorChooser colorChooser;
    private final JButton chooseColor = new JButton();
    
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
        setLayout(new GridBagLayout());
        
        this.type = type;
        this.baseColorSetting = baseColorSetting;
        this.colorChooser = chooser;
        this.previewText = text;
        
        // Set text and size of preview
        if (!text.isEmpty()) {
            preview.setText(" "+text);
            preview.setOpaque(true);
        }
        preview.setToolTipText(name);
        preview.setPreferredSize(new Dimension(120,20));
        
        // Choose color button action
        chooseColor.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String result = colorChooser.chooseColorString(type, currentColor, secondaryColor, name, previewText);
                setSettingValue(result);
            }
        });
        chooseColor.setMargin(new Insets(0, 0, 0, 0));
        chooseColor.setIcon(new ImageIcon(ColorSetting.class.getResource("colorpicker.png")));
        
        // Textfield settings
        textField.setEditable(false);
        textField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 3));
        textField.setHorizontalAlignment(JTextField.RIGHT);
        
        initiate();
    }
    
    public boolean hasBase(String setting) {
        return setting.equals(baseColorSetting);
    }
    
    /**
     * Adds the components to the panel.
     */
    private void initiate() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        add(preview, gbc);
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(textField, gbc);
        gbc.weightx = 0;
        add(chooseColor, gbc);
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
        if (useBaseColor) {
            secondaryColor = HtmlColors.decode(baseColor);
        }
        else {
            secondaryColor = null;
        }
        
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
        textField.setForeground(foregroundColor);
        textField.setBackground(backgroundColor);
    }
    
    /**
     * Sets a new base color and updates the preview.
     * 
     * @param baseColor 
     */
    public void setBaseColor(String baseColor) {
        this.baseColor = baseColor;
        updated();
    }
    
    public void setBaseColor(Color color) {
        setBaseColor(HtmlColors.getColorString(color));
    }
    
    public void setBaseColorSetting(String setting) {
        this.baseColorSetting = setting;
    }
    
    public void setUseBaseColor(boolean use) {
        this.useBaseColor = use;
        updated();
    }
    
    public void setPreviewText(String previewText) {
        if (previewText == null) {
            previewText = "";
        }
        preview.setText(" "+previewText);
        this.previewText = previewText;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        // Call so isEnabled() returns the correct value
        super.setEnabled(enabled);
        // Change state of children
        preview.setEnabled(enabled);
        textField.setEnabled(enabled);
        chooseColor.setEnabled(enabled);
    }

    @Override
    public String getSettingValue() {
        return textField.getText();
    }

    @Override
    public void setSettingValue(String value) {
        if (Objects.equals(textField.getText(), value)) {
            return;
        }
        textField.setText(value);
        updated();
        // Inform after updating current color (for getSettingValueAsColor())
        for (ColorSettingListener listener : listeners) {
            listener.colorUpdated();
        }
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
    
    public void addMouseListener(MouseListener listener) {
        textField.addMouseListener(listener);
        preview.addMouseListener(listener);
    }
    
}
