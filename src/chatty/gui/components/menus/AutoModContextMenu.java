
package chatty.gui.components.menus;

import chatty.gui.DockedDialogHelper;
import chatty.gui.components.AutoModDialog;
import java.awt.event.ActionEvent;

/**
 *
 * @author tduva
 */
public class AutoModContextMenu extends ContextMenu {

    private final AutoModDialog.Item item;
    private final AutoModContextMenuListener listener;
    
    public AutoModContextMenu(AutoModDialog.Item item, DockedDialogHelper dockedHelper, AutoModContextMenuListener listener) {
        if (item != null) {
            addItem("approve", "Approve [A]").setMnemonic('A');
            addItem("reject", "Deny [D]").setMnemonic('D');
            addSeparator();
            addItem("copy", "Copy Message");
            addItem("user", "User Info");
            addSeparator();
        }
        addItem("help", "Help");
        addSeparator();
        addItem("close", "Close [Q]").setMnemonic('Q');
        addSeparator();
        dockedHelper.addToContextMenu(this);
        
        this.item = item;
        this.listener = listener;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        this.listener.itemClicked(item, e);
    }

    public static interface AutoModContextMenuListener {

        public void itemClicked(AutoModDialog.Item item, ActionEvent e);
    }
}
