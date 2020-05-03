
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author tduva
 */
public class EditorStringSetting extends JPanel implements StringSetting {

    private final StringEditor editor;
    private final JTextField preview;
    private final JButton editButton;
    
    private String value;
    private String info;
    private ChangeListener listener;
    
    public EditorStringSetting(Window parent, final String title, int size,
            boolean allowEmpty, boolean allowLinebreaks, final String info) {
        this(parent, title, size, allowEmpty, allowLinebreaks, info, null);
    }
    
    public EditorStringSetting(Window parent, final String title, int size,
            boolean allowEmpty, boolean allowLinebreaks, String defaultInfo,
            Editor.Tester tester) {
        this(parent, title, size, createEditor(parent, allowEmpty, allowLinebreaks, tester));
        this.info = defaultInfo;
    }
    
    private static Editor createEditor(Window parent, boolean allowEmpty,
                                       boolean allowLinebreaks, Editor.Tester tester) {
        Editor editor = new Editor(parent);
        editor.setAllowEmpty(allowEmpty);
        editor.setAllowLinebreaks(allowLinebreaks);
        editor.setTester(tester);
        return editor;
    }
    
    public EditorStringSetting(Window parent, final String title, int size,
                               StringEditor editor) {
        this.editor = editor;
        
        setLayout(new BorderLayout(2, 0));
        
        editButton = new JButton(Language.getString("dialog.button.edit"));
        editButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String result = editor.showDialog(title, value, info);
                if (result != null) {
                    setSettingValue(result);
                }
            }
        });
        
        if (size > -1) {
            preview = new JTextField(size);
            preview.setEditable(false);
            add(preview, BorderLayout.CENTER);
        }
        else {
            preview = null;
        }
        add(editButton, BorderLayout.EAST);
    }
    
    public void setChangeListener(ChangeListener listener) {
        this.listener = listener;
    }
    
    /**
     * Listener for links in the info/help text.
     * 
     * @param listener 
     */
    public void setLinkLabelListener(LinkLabelListener listener) {
        editor.setLinkLabelListener(listener);
    }
    
    @Override
    public String getSettingValue() {
        return value;
    }

    @Override
    public void setSettingValue(String value) {
        if (!Objects.equals(this.value, value)) {
            this.value = value;
            if (preview != null) {
                preview.setText(value);
            }
            if (listener != null) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }
    
    public void setInfo(String info) {
        this.info = info;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        editButton.setEnabled(enabled);
    }

}
