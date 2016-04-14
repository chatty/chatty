
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author tduva
 */
public class EditorStringSetting extends JPanel implements StringSetting {

    private final Editor editor;
    private final JTextField preview;
    private final JButton editButton;
    
    private String value;
    
    public EditorStringSetting(Window parent, final String title, int size, boolean allowEmpty, boolean allowLinebreaks, final String info) {
        ((FlowLayout)getLayout()).setHgap(2);
        ((FlowLayout)getLayout()).setVgap(0);
        
        editor = new Editor(parent);
        editor.setAllowEmpty(allowEmpty);
        editor.setAllowLinebreaks(allowLinebreaks);
        
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
        
        add(preview);
        add(editButton);
    }
    
    public void setFormatter(DataFormatter<String> formatter) {
        editor.setFormatter(formatter);
    }
    
    @Override
    public String getSettingValue() {
        return value;
    }

    @Override
    public void setSettingValue(String value) {
        this.value = value;
        preview.setText(value);
    }
    
}
