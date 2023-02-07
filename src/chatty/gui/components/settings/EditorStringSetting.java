
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.SyntaxHighlighter;
import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.function.Supplier;
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

    private StringEditor editor;
    private final JTextField preview;
    private final JButton editButton;
    private LinkLabelListener linkLabelListener;
    private boolean showInfoByDefault;
    private SyntaxHighlighter syntaxHighlighter;
    
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
        this(parent, title, size, () -> createEditor(parent, allowEmpty, allowLinebreaks, tester));
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
                               Supplier<StringEditor> editorCreator) {
        setLayout(new BorderLayout(2, 0));
        
        editButton = new JButton(Language.getString("dialog.button.edit"));
        GuiUtil.smallButtonInsets(editButton);
        editButton.setToolTipText(title);
        editButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (editor == null) {
                    editor = editorCreator.get();
                    setLinkLabelListener(linkLabelListener);
                    setShowInfoByDefault(showInfoByDefault);
                    if (editor instanceof Editor) {
                        ((Editor) editor).setSyntaxHighlighter(syntaxHighlighter);
                    }
                }
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
        if (editor != null) {
            editor.setLinkLabelListener(listener);
        }
        else {
            linkLabelListener = listener;
        }
    }
    
    public void setSyntaxHighlighter(SyntaxHighlighter hl) {
        this.syntaxHighlighter = hl;
        if (editor instanceof Editor) {
            ((Editor) editor).setSyntaxHighlighter(syntaxHighlighter);
        }
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
    
    public void setShowInfoByDefault(boolean show) {
        if (editor != null) {
            if (editor instanceof Editor) {
                ((Editor) editor).setShowInfoByDefault(show);
            }
        }
        else {
            showInfoByDefault = show;
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        editButton.setEnabled(enabled);
    }

}
