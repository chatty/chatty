
package chatty.gui.components.menus;

import chatty.lang.Language;
import chatty.util.MiscUtil;
import java.awt.event.ActionEvent;

/**
 * For adding a copy or copy/cut/paste context menu.
 * 
 * For just setting a single copy/cut/paste context menu to a text component
 * without any custom handling, GuiUtil.addTextContextMenu() can be used.
 *
 * @author tduva
 */
public class TextSelectionMenu extends ContextMenu {

    private final String selectedText;
    
    /**
     * Copy given text to clipboard.
     * 
     * @param selectedText 
     */
    public TextSelectionMenu(String selectedText) {
        this.selectedText = selectedText;
        addItem("copy", Language.getString("textCm.copy"));
    }
    
    /**
     * Need to override actionPerformed for custom handling.
     */
    public TextSelectionMenu() {
        addItem("copy", Language.getString("textCm.copy"));
        addItem("cut", Language.getString("textCm.cut"));
        addItem("paste", Language.getString("textCm.paste"));
        this.selectedText = null;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("copy") && selectedText != null) {
            MiscUtil.copyToClipboard(selectedText);
        }
    }
    
}
