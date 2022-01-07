
package chatty.gui.components.settings;

import chatty.util.colors.HtmlColors;
import chatty.lang.Language;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A color chooser with a preview that has a primary and secondary color.
 * 
 * <ul>
 * <li>The primary color is the one to be chosen and can either be of the type
 * {@literal FOREGROUND} or of the type {@literal BACKGROUND}.</li>
 * <li>The secondary color is only used for the preview, as
 * background/foreground for the primary color (depending on whether the primary
 * color is set to foreground/background).</li>
 * </ul>
 * 
 * @author tduva
 */
public class ColorChooser extends JDialog {
    
    /**
     * The type of the primary color if it is to be used as foreground color
     */
    public static final int FOREGROUND = 0;
    /**
     * The type of the primary color if it is to be used as background color
     */
    public static final int BACKGROUND = 1;
    
    /**
     * The actual type of the primary color
     */
    private int type = FOREGROUND;
    
    private final JColorChooser chooser = new JColorChooser();
    private final MyPreview preview = new MyPreview();
    private final JButton doneButton = new JButton(Language.getString("settings.colorChooser.button.useSelected"));
    private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
    
    /**
     * The secondary color that is used for the preview.
     */
    private Color secondaryColor;
    
    /**
     * Whether to actually return the chosen Color, or return the preset, which
     * is determined which button is used for closing the dialog.
     */
    private boolean returnNewColor;
    
    public ColorChooser(JDialog parent) {
        super(parent);
        setModal(true);
        setTitle("Choose color");
        setLayout(new GridBagLayout());
        setResizable(false);
        
        configureChooser();
        
        // Add listener to update preview when a new color has been selected
        chooser.getSelectionModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                updatePreview();
            }
        });
        
        // Layout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(chooser, gbc);
        
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        
        add(doneButton, gbc);
        
        gbc.gridx = 1;
        
        gbc.weightx = 0.2;
        add(cancelButton, gbc);
        
        // Button listener to close the dialog
        ActionListener buttonListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == doneButton) {
                    returnNewColor = true;
                }
                setVisible(false);
            }
        };
        
        // Add button listener to buttons
        doneButton.addActionListener(buttonListener);
        cancelButton.addActionListener(buttonListener);
        
        pack();
    }
    
    /**
     * When updating LaF, it doesn't add the panel back, so do it if necessary.
     */
    private void configureChooser() {
        for (AbstractColorChooserPanel panel : chooser.getChooserPanels()) {
            if (panel instanceof NamedColorsPanel) {
                // Already configured
                return;
            }
        }
        chooser.addChooserPanel(new NamedColorsPanel());
        chooser.setPreviewPanel(preview);
    }
    
    /**
     * Update the preview with the new primary color. Use as foreground or
     * background depending on the type and use secondary color as the other
     * one.
     */
    private void updatePreview() {
        Color newColor = chooser.getColor();
        if (type == FOREGROUND) {
            preview.update(newColor, secondaryColor);
        } else {
            preview.update(secondaryColor, newColor);
        }
    }
    
    /**
     * Let the user choose a Color and return a String representation of that
     * Color.
     * 
     * @param type The type of the color to choose ({@code FOREGROUND} or
     * {@code BACKGROUND})
     * @param presetColor The {@code Color} to preset the color to choose with
     * @param secondaryColor The secondary color to be used for the preview
     * @param name The name of the color to choose (a short description)
     * @param text The text to be used for the preview
     * @return A String representation of the chosen Color, or of the preset
     * Color if the dialog was canceled
     * @see chooseColor(int, Color, Color, String, String)
     */
    public String chooseColorString(int type, Color presetColor,
            Color secondaryColor, String name, String text) {
        return HtmlColors.getNamedColorString(
                chooseColor(type, presetColor, secondaryColor, name, text));
    }
    
    /**
     * Let the user choose a Color and return that Color.
     * 
     * @param type The type of the color to choose ({@code FOREGROUND} or
     * {@code BACKGROUND})
     * @param presetColor The {@code Color} to preset the color to choose with
     * @param secondaryColor The secondary color to be used for the preview
     * @param name The name of the color to choose (a short description)
     * @param text The text to be used for the preview
     * @return The chosen color, or the preset Color if the dialog was canceled
     */
    public Color chooseColor(int type, Color presetColor, Color secondaryColor,
            String name, String text) {
        configureChooser();
        
        // Update dialog
        returnNewColor = false;
        setTitle(Language.getString("settings.colorChooser.title", name));
        
        // Save type and secondary color for further use (primary color is not
        // used outside this method and the chooser)
        this.type = type;
        this.secondaryColor = secondaryColor;
        
        // Update preview and chooser
        preview.setText(text);
        chooser.setColor(presetColor);
        updatePreview();
        pack();
        setLocationRelativeTo(getParent());
        
        setVisible(true);
        
        // Wait until the user closes the dialog, which might also change
        // {@code returnNewColor} if the "Done" button is pressed
        if (returnNewColor) {
            return chooser.getColor();
        }
        return presetColor;
    }
    
    /**
     * Preview to be used for the color chooser.
     */
    static class MyPreview extends JLabel {
        
        MyPreview() {
            super("Preview Text", CENTER);
            setPreferredSize(new Dimension(400,50));
            
            // Set opaque so the background is rendered
            setOpaque(true);
            setFont(new Font("Arial", Font.BOLD, 14));
        }
        
        /**
         * Update the foreground and background colors.
         * 
         * @param foreground
         * @param background 
         */
        public void update(Color foreground, Color background) {
            if (foreground != null) {
                super.setForeground(new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue()));
            }
            else {
                super.setForeground(null);
            }
            if (background != null) {
                super.setBackground(new Color(background.getRed(), background.getGreen(), background.getBlue()));
            }
            else {
                super.setBackground(null);
            }
        }
        
        /**
         * Override so the JColorChooser doesn't change the color on it's own,
         * which might not have the wanted effect.
         * 
         * @param foregroundColor 
         */
        @Override
        public void setForeground(Color foregroundColor) {
        }

        /**
         * Override so the JColorChooser doesn't change the color on it's own,
         * which might not have the wanted effect.
         * 
         * @param foregroundColor 
         */
        @Override
        public void setBackground(Color backgroundColor) {
        }
        
    }
    
    
}
