
package chatty.gui.components.menus;

import chatty.lang.Language;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Action;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

/**
 * For adding a copy or copy/cut/paste context menu, including Custom Commands.
 *
 * @author tduva
 */
public class TextSelectionMenu extends ContextMenu {
    
    public static ContextMenuListener listener;
    private final JTextComponent component;
    
    /**
     * Copy given text to clipboard.
     * 
     * @param editingEnabled Whether cut/paste should be added as well.
     */
    public TextSelectionMenu(JTextComponent component, boolean editingEnabled) {
        this.component = component;
        
        add(new DefaultEditorKit.CopyAction(), Language.getString("textCm.copy"), KeyEvent.VK_C);
        if (editingEnabled) {
            add(new DefaultEditorKit.CutAction(), Language.getString("textCm.cut"), KeyEvent.VK_X);
            add(new DefaultEditorKit.PasteAction(), Language.getString("textCm.paste"), KeyEvent.VK_P);
        }
        CommandMenuItems.addCommands(CommandMenuItems.MenuType.TEXT, this, null);
    }
    
    private void add(Action action, String name, int key) {
        action.putValue(Action.NAME, name);
        action.putValue(Action.MNEMONIC_KEY, key);
        add(action);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (listener != null) {
            listener.textMenuItemClick(e, component.getSelectedText());
        }
    }
    
    private static final List<WeakReference<JTextComponent>> components = new ArrayList<>();
    
    /**
     * Sets copy/cut/paste context menu (including Custom Commands) to the text
     * component.
     *
     * Menu may be set on the component again if the contents of the menu
     * (Custom Commands) change. The JTextComponent is stored as a weak
     * reference for that purpose.
     *
     * @param c The text component
     */
    public static void install(JTextComponent c) {
        c.setComponentPopupMenu(new TextSelectionMenu(c, true));
        components.add(new WeakReference<>(c));
    }
    
    /**
     * When the Custom Commands in the menu changed, need to update the menu.
     */
    public static void update() {
        Iterator<WeakReference<JTextComponent>> it = components.iterator();
        while (it.hasNext()) {
            JTextComponent c = it.next().get();
            // Check if GC or menu on component was changed to something else
            if (c == null || !(c.getComponentPopupMenu() instanceof TextSelectionMenu)) {
                it.remove();
            }
            else {
                c.setComponentPopupMenu(new TextSelectionMenu(c, true));
            }
        }
    }
    
}
