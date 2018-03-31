
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * Providing a simpler way to layout a panel with settings.
 * 
 * @author tduva
 */
public class SettingsPanel extends JPanel {
    
    /**
     * The inner border to use to create some more space.
     */
    private static final Border PADDING = new EmptyBorder(2, 2, 5, 2);
    
    /**
     * The JPanel that holds the other panels added through this.
     */
    private final JPanel base;
    
    public SettingsPanel() {
        this(false);
    }
    
    public SettingsPanel(boolean expand) {
        setLayout(new GridBagLayout());
        base = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        if (expand) {
            gbc.fill = GridBagConstraints.BOTH;
        } else {
            gbc.fill = GridBagConstraints.HORIZONTAL;
        }
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(base, gbc);
    }
    
    /**
     * Creates a panel with a titled border.
     * 
     * @param title The title of the panel.
     * @return The created JPanel.
     */
    protected JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        Border titleBorder = BorderFactory.createTitledBorder(title);
        panel.setBorder(BorderFactory.createCompoundBorder(titleBorder, PADDING));
        return panel;
    }
    
    protected JPanel addTitledPanel(String title, int row) {
        return addTitledPanel(title, row, false);
    }
    
    /**
     * Creates a panel with a titled border and adds it to this SettingsPanel.
     * 
     * @param title The title of the panel.
     * @param row The row to insert the panel in.
     * @return The created and added JPanel.
     */
    protected JPanel addTitledPanel(String title, int row, boolean expand) {
        JPanel panel = createTitledPanel(title);
        GridBagConstraints gbc = getGbc(row);
        gbc.fill = GridBagConstraints.BOTH;
        if (expand) {
            gbc.weighty = 1;
        } else {
            gbc.weighty = 0;
        }
        base.add(panel, gbc);
        return panel;
    }
    
    /**
     * Create constraints with the given row.
     * 
     * @param row The row index.
     * @return The created GridBagConstraints.
     */
    protected GridBagConstraints getGbc(int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy = row;
        gbc.insets = new Insets(8,7,4,7);
        return gbc;
    }
    
    protected JComponent addPanel(JComponent panel, GridBagConstraints gbc) {
        base.add(panel, gbc);
        return panel;
    }
    
}
