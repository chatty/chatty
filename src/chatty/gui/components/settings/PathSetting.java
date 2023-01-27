
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.MiscUtil;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A String setting that represents a path. A JPanel with textfield and buttons
 * to change, reset or open the path.
 * 
 * @author tduva
 */
public class PathSetting extends JPanel implements StringSetting {

    // Set size so that it doesn't use content size (can still grow larger)
    private final JTextField display = new JTextField(20);
    private final JButton changeButton = new JButton(Language.getString("settings.chooseFolder.button.change"));
    private final JButton resetButton = new JButton(Language.getString("settings.chooseFolder.button.default"));
    private final JButton openButton = new JButton(Language.getString("settings.chooseFolder.button.open"));
    
    private String value;
    private final String defaultPath;
    private final Component parentComponent;
    
    private PathChangeListener listener;
    
    /**
     * Create a new PathSetting instancen.
     * 
     * @param parentComponent The component to open the file chooser on
     * @param defaultPath The path to display as default path, if path is empty
     */
    public PathSetting(final Component parentComponent, String defaultPath) {
        this.defaultPath = defaultPath;
        this.parentComponent = parentComponent;
        
        display.setEditable(false);
        GuiUtil.smallButtonInsets(changeButton);
        GuiUtil.smallButtonInsets(resetButton);
        GuiUtil.smallButtonInsets(openButton);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridwidth = 3;
        
        add(display, gbc);
        
        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        add(changeButton, gbc);
        gbc.gridx = 1;
        gbc.weightx = 0;
        add(resetButton, gbc);
        gbc.gridx = 2;
        gbc.insets.left += 6;
        add(openButton, gbc);
        
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == changeButton) {
                    chooseDirectory();
                } else if (e.getSource() == resetButton) {
                    setSettingValue("");
                } else if (e.getSource() == openButton) {
                    Path path = getCurrentPath();
                    if (path == null) {
                        path = Chatty.getPath(Chatty.PathType.SETTINGS);
                    }
                    MiscUtil.openFile(path.toFile(), parentComponent);
                }
            }
        };
        changeButton.addActionListener(buttonAction);
        resetButton.addActionListener(buttonAction);
        openButton.addActionListener(buttonAction);
    }
    
    /**
     * The current setting value, either the path or an empty String to
     * represent the default path.
     * 
     * @return The current setting value
     */
    @Override
    public String getSettingValue() {
        return value;
    }

    /**
     * Set the setting and update the display.
     * 
     * @param value The new value to set (should not be null)
     * @throws NullPointerException if value is null
     */
    @Override
    public void setSettingValue(String value) {
        if (this.value == null || !this.value.equals(value)) {
            this.value = value;
            if (listener != null) {
                listener.pathChanged(getCurrentPath());
            }
        }
        String isInvalid = getCurrentPath() == null ? "[invalid]" : "";
        String isDefault = value.isEmpty() ? "[" + Language.getString("settings.chooseFolder.default") + "] " : "";
        display.setText(isDefault + isInvalid + getCurrentPathValue());
    }
    
    /**
     * Get the current path as a String. If the setting value is empty, then use
     * the default path.
     * 
     * @return The setting value or the default path if the setting value is
     * empty
     */
    public String getCurrentPathValue() {
        if (value.isEmpty()) {
            return defaultPath;
        } else {
            return value;
        }
    }
    
    /**
     * Gets the current Path.
     * 
     * @return The Path based on the current setting value, or the default Path
     * if the setting value is empty
     * @see getCurrentPathValue()
     */
    public Path getCurrentPath() {
        try {
            return Paths.get(getCurrentPathValue());
        } catch (InvalidPathException ex) {
            return null;
        }
    }
    
    public void setPathChangeListener(PathChangeListener listener) {
        this.listener = listener;
    }

    /**
     * Open a JFileChooser to select a directory to use.
     */
    private void chooseDirectory() {
        Path path = getCurrentPath();
        if (path == null) {
            path = Chatty.getPath(Chatty.PathType.SETTINGS);
        }
        JFileChooser chooser = new JFileChooser(path.toFile());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showDialog(parentComponent, Language.getString("settings.chooseFolder")) == JFileChooser.APPROVE_OPTION) {
            setSettingValue(chooser.getSelectedFile().getPath());
        }
    }
    
    public interface PathChangeListener {
        public void pathChanged(Path newPath);
    }
    
}
