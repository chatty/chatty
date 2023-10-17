
package chatty.gui.components.admin;

import chatty.lang.Language;
import chatty.util.api.StreamLabels;
import chatty.util.api.StreamLabels.StreamLabel;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

/**
 *
 * @author tduva
 */
public class SelectLabelsDialog extends JDialog {
    
    // General Buttons
    private final JButton ok = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    
    private final Map<StreamLabel, JCheckBox> checkboxes = new HashMap<>();
    private final List<StreamLabel> preset = new ArrayList<>();
    private boolean save;
    
    public SelectLabelsDialog(Frame owner) {
        super(owner, Language.getString("admin.labels.title"), true);
        
        ok.addActionListener(e -> {
            save = true;
            dispose();
        });
        
        cancel.addActionListener(e -> {
            dispose();
        });
        
        //========
        // Layout
        //========
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        int y = 0;
        
        for (StreamLabel label : StreamLabels.getAvailableLabels()) {
            JCheckBox checkbox = new JCheckBox(label.getDisplayName());
            checkbox.addItemListener(e -> {
                updateOkButton();
            });
            checkbox.setEnabled(label.isEditable());
            if (label.getDescription() != null) {
                checkbox.setToolTipText(label.getDescription());
            }
            checkboxes.put(label, checkbox);
            
            gbc = makeGbc(0, y, 2,1);
            gbc.anchor = GridBagConstraints.WEST;
            add(checkbox, gbc);
            
            y++;
        }
        
        //--------------------
        // Save/close buttons
        //--------------------
        
        ok.setMnemonic(KeyEvent.VK_S);
        gbc = makeGbc(0,y,1,1);
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(ok, gbc);
        
        cancel.setMnemonic(KeyEvent.VK_C);
        gbc = makeGbc(1,y,1,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(cancel, gbc);
        
        pack();
    }
    
    /**
     * Open the dialog with the given tags preset.
     * 
     * @param preset
     * @return The list of tags to use, or {@code null} if they should not be
     * changed
     */
    public List<StreamLabel> open(List<StreamLabel> preset) {
        this.preset.clear();
        this.preset.addAll(preset);
        setCurrent(preset);
        save = false;
        setVisible(true);

        // Blocking dialog, so stuff can change in the meantime
        if (save) {
            return getCurrent();
        }
        return null;
    }
    
    private void setCurrent(List<StreamLabel> preset) {
        for (Map.Entry<StreamLabel, JCheckBox> entry : checkboxes.entrySet()) {
            entry.getValue().setSelected(preset.contains(entry.getKey()));
        }
    }
    
    private List<StreamLabel> getCurrent() {
        List<StreamLabel> labels = new ArrayList<>();
        for (Map.Entry<StreamLabel, JCheckBox> entry : checkboxes.entrySet()) {
            StreamLabel label = entry.getKey();
            JCheckBox checkbox = entry.getValue();
            if (checkbox.isSelected()) {
                labels.add(label);
            }
        }
        return labels;
    }
    
    private void updateOkButton() {
        ok.setEnabled(!preset.equals(getCurrent()));
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(4,6,4,6);
        return gbc;
    }
    
}
