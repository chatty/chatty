
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabelListener;
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

    private final Editor editor;
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
        setLayout(new BorderLayout(2, 0));
        this.info = defaultInfo;
        
        editor = new Editor(parent);
        editor.setAllowEmpty(allowEmpty);
        editor.setAllowLinebreaks(allowLinebreaks);
        editor.setTester(tester);
        
        preview = new JTextField(size);
        preview.setEditable(false);
        
        editButton = new JButton("Edit");
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
        
        add(preview, BorderLayout.CENTER);
        add(editButton, BorderLayout.EAST);
    }
    
    public void setFormatter(DataFormatter<String> formatter) {
        editor.setFormatter(formatter);
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
            preview.setText(value);
            if (listener != null) {
                listener.stateChanged(new ChangeEvent(this));
            }
        }
    }
    
    public void setInfo(String info) {
        this.info = info;
    }

}
