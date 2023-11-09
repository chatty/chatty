
package chatty.gui.components;

import chatty.Helper;
import chatty.TwitchClient;
import chatty.User;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.MainGui;
import chatty.gui.components.menus.AutoModContextMenu;
import chatty.util.DateTime;
import chatty.util.MiscUtil;
import chatty.util.api.TwitchApi;
import chatty.util.api.TwitchApi.AutoModAction;
import chatty.util.api.TwitchApi.AutoModActionResult;
import chatty.util.api.pubsub.ModeratorActionData;
import chatty.util.dnd.DockContent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class AutoModDialog extends JDialog {
    
    private static final int MESSAGE_LIMIT = 20;

    private final MainGui gui;
    private final TwitchApi api;
    private final TwitchClient client;
    
    private final DockedDialogHelper helper;
    
    private final JList<Item> list;
    private final DefaultListModel<Item> data;
    private final Map<String, List<Item>> cache = new HashMap<>();

    private String currentRoom = "";
    private String currentRoomLoaded = "";
    
    public AutoModDialog(MainGui main, TwitchApi api, TwitchClient client,
                         DockedDialogManager dockedDialogs) {
        super(main);
        setTitle("AutoMod");
        
        this.gui = main;
        this.api = api;
        this.client = client;

        list = new JList<Item>() {
            
            /**
             * To prevent horizontal scrolling and allow for tracking of the
             * viewport width.
             *
             * @return
             */
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
            
        };
        data = new DefaultListModel<>();
        list.setModel(data);
        list.setCellRenderer(new MyCellRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll);
        
        DockContent content = dockedDialogs.createStyledContent(scroll, "AutoMod", "-automod-");
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            @Override
            public void setVisible(boolean visible) {
                AutoModDialog.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return AutoModDialog.super.isVisible();
            }

            @Override
            public void addComponent(Component comp) {
                add(comp);
            }

            @Override
            public void removeComponent(Component comp) {
                remove(comp);
            }

            @Override
            public Window getWindow() {
                return AutoModDialog.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
        });
        helper.setChannelChangeListener(channel -> {
            if (isVisible()) {
                setStream(Helper.toStream(channel));
            }
        });
        
        list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openUserInfoDialog();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                openContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }
        });
        
        Timer timer = new Timer(30000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                list.repaint();
            }
        });
        timer.setRepeats(true);
        timer.start();
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("A"), "automod.approve");
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("alt A"), "automod.approve");
        list.getActionMap().put("automod.approve", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                approve(null);
            }
        });
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("D"), "automod.deny");
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("alt D"), "automod.deny");
        list.getActionMap().put("automod.deny", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deny(null);
            }
        });
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("S"), "automod.next");
        list.getActionMap().put("automod.next", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectNext();
            }
        });
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("W"), "automod.previous");
        list.getActionMap().put("automod.previous", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectPrevious();
            }
        });
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("alt S"), "automod.nextUnhandled");
        list.getActionMap().put("automod.nextUnhandled", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectNext(true);
            }
        });
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("alt W"), "automod.previousUnhandled");
        list.getActionMap().put("automod.previousUnhandled", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectPrevious(true);
            }
        });
        
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("Q"), "automod.close");
        list.getActionMap().put("automod.close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        ComponentListener l = new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
// Trick from kleopatra:
// https://stackoverflow.com/questions/7306295/swing-jlist-with-multiline-text-and-dynamic-height
                // next line possible if list is of type JXList
                // list.invalidateCellSizeCache();
                // for core: force cache invalidation by temporarily setting fixed height
                list.setFixedCellHeight(10);
                list.setFixedCellHeight(-1);
            }

        };
        list.addComponentListener(l);

        setSize(new Dimension(400, 200));
    }
    
    @Override
    public void setVisible(boolean visible) {
        helper.setVisible(visible, true);
    }

    @Override
    public boolean isVisible() {
        if (helper != null) {
            return helper.isVisible();
        }
        return super.isVisible();
    }
    
    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (helper != null) {
            helper.getContent().setLongTitle(title);
        }
    }
    
    public void showDialog() {
        switchDataToCurrent();
        setVisible(true);
        list.setSelectedIndex(data.size() - 1);
        scrollDown();
    }
    
    public void setStream(String stream) {
        if (stream != null && !stream.equals(currentRoom)) {
            currentRoom = stream;
            helper.setCurrentChannel(stream);
            if (isVisible()) {
                switchDataToCurrent();
            }
        }
    }
    
    private void switchDataToCurrent() {
        if (!currentRoom.equals(currentRoomLoaded)) {
            currentRoomLoaded = currentRoom;
            
            setTitle("AutoMod (" + currentRoom + ") [Use Context Menu to Approve/Deny]");

            List<Item> cached = cache.get(currentRoom);
            data.removeAllElements();
            if (cached != null) {
                for (Item item : cached) {
                    data.addElement(item);
                }
            }
            scrollDown();
        }
    }
    
    public void addData(ModeratorActionData modData) {
        if (modData.stream == null) {
            return;
        }
        if (modData.type == ModeratorActionData.Type.AUTOMOD_REJECTED) {
            addItem(modData);
        }
        if (modData.type == ModeratorActionData.Type.AUTOMOD_APPROVED) {
            handledExternally(modData, Item.STATUS_APPROVED);
        }
        if (modData.type == ModeratorActionData.Type.AUTOMOD_DENIED) {
            handledExternally(modData, Item.STATUS_DENIED);
        }
    }

    /**
     * Result of an API request to approve/deny a message.
     * 
     * @param action
     * @param msgId
     * @param result
     */
    public void requestResult(TwitchApi.AutoModAction action, String msgId, TwitchApi.AutoModActionResult result) {
        Item changedItem = findItemByMsgId(msgId);
        if (changedItem != null) {
            changedItem.setRequestPending(false);
            if (result == AutoModActionResult.SUCCESS && action == AutoModAction.ALLOW) {
                changedItem.setStatus(Item.STATUS_APPROVED);
            }
            else if (result == AutoModActionResult.SUCCESS && action == AutoModAction.DENY) {
                changedItem.setStatus(Item.STATUS_DENIED);
            }
            else if (changedItem.status <= Item.STATUS_NONE) {
                if (result == AutoModActionResult.ALREADY_PROCESSED) {
                    changedItem.setStatus(Item.STATUS_HANDLED);
                }
                else if (result == AutoModActionResult.NOT_FOUND) {
                    changedItem.setStatus(Item.STATUS_NA);
                }
                else {
                    changedItem.setStatus(Item.STATUS_ERROR);
                }
            }
        }
        repaintFor(changedItem);
    }
    
    private Item findItemByMsgId(String msgId) {
        for (List<Item> items : cache.values()) {
            for (Item item : items) {
                if (item.data.msgId.equals(msgId)) {
                    return item;
                }
            }
        }
        return null;
    }
    
    private void addItem(ModeratorActionData modData) {
        if (modData.args.size() < 2 || modData.msgId.isEmpty()) {
            return;
        }
        
        String room = modData.stream;
        String channel = Helper.toValidChannel(modData.stream);
        String username = modData.args.get(0);
        String message = modData.args.get(1);
        String reason = modData.getArg(2, null);
        if (channel == null) {
            return;
        }
        if (!Helper.isValidStream(username)) {
            return;
        }
        
        User user = client.getUser(channel, username);
        user.addAutoModMessage(message, modData.msgId, reason);
        gui.updateUserinfo(user);
        Item item = new Item(modData, user);

        if (!cache.containsKey(room)) {
            cache.put(room, new ArrayList<Item>());
        }
        cache.get(room).add(item);
        if (cache.get(room).size() > MESSAGE_LIMIT) {
            cache.get(room).remove(0);
            if (room.equals(currentRoom) && !data.isEmpty()) {
                data.remove(0);
            }
        }

        if (room.equals(currentRoom)) {
            data.addElement(item);
            scrollDownIfApplicable();
            helper.setNewMessage();
        }
    }

    /**
     * Message has been handled by another user, so determine if corresponding
     * message can be found and set the status accordingly.
     * 
     * @param modData
     * @param status 
     */
    private void handledExternally(ModeratorActionData modData, int status) {
        if (modData.args.size() != 1 || modData.created_by.isEmpty()) {
            return;
        }
        String handledBy = modData.created_by;
        String targetUsername = modData.args.get(0);
        String room = modData.stream;
        Item item;
        if (!modData.msgId.isEmpty()) {
            item = findItemByMsgId(modData.msgId);
        } else {
            item = findItemByUsername(room, targetUsername);
        }
        if (item != null && !item.hasRequestPending && !item.isHandled()) {
            item.setStatus(status, handledBy);
            repaintFor(item);
        }
    }
    
    /**
     * Find a single Item by username for a given room. The issue with this is
     * that Twitch only provides the username for approved messages (and a new
     * message id), so only return an Item if only one for that username was
     * found in the last 5 minutes that hasn't been handled yet. Messages seem
     * to get removed after a few minutes, so messages older than 5 minutes
     * probably can't be approved anymore. Still, this isn't that pretty.
     * 
     * @param room
     * @param username
     * @return 
     */
    private Item findItemByUsername(String room, String username) {
        Item foundItem = null;
        List<Item> items = cache.get(room);
        if (items != null) {
            boolean oldEnoughHistory = false;
            for (int i=items.size() - 1; i>=0; i--) {
                Item item = items.get(i);
                if (item.getAge() > 5*60) {
                    oldEnoughHistory = true;
                    break;
                }
                if (!item.hasRequestPending && !item.isHandled()
                        && item.targetUser.getName().equals(username)) {
                    if (foundItem != null) {
                        // Can't be more than one Item, since we don't know
                        // which is the correct one by just the username
                        foundItem = null;
                        break;
                    }
                    foundItem = item;
                }
            }
            if (!oldEnoughHistory) {
                foundItem = null;
            }
        }
        return foundItem;
    }
    
    private void scrollDownIfApplicable() {
        if (list.getLastVisibleIndex() >= data.size() - 2) {
            scrollDown();
        }
    }
    
    private void scrollDown() {
        list.ensureIndexIsVisible(data.size() - 1);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                list.ensureIndexIsVisible(data.size() - 1);
            }
        });
    }

    /**
     * Select the list item at the location of this MouseEvent, if any.
     * 
     * @param e The MouseEvent
     */
    private void selectClicked(MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index != -1) {
            list.setSelectedIndex(index);
        }
    }
    
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            selectClicked(e);
            Item selectedItem = list.getSelectedValue();
            AutoModContextMenu m = new AutoModContextMenu(selectedItem, helper, new AutoModContextMenu.AutoModContextMenuListener() {

                @Override
                public void itemClicked(Item item, ActionEvent e) {
                    if (e.getActionCommand().equals("approve")) {
                        approve(item);
                    }
                    else if (e.getActionCommand().equals("reject")) {
                        deny(item);
                    }
                    else if (e.getActionCommand().equals("copy")) {
                        MiscUtil.copyToClipboard(item.toString());
                    }
                    else if (e.getActionCommand().equals("help")) {
                        gui.openHelp(null, "automod");
                    }
                    else if (e.getActionCommand().equals("user")) {
                        openUserInfoDialog();
                    }
                    else if (e.getActionCommand().equals("close")) {
                        setVisible(false);
                    }
                    helper.menuAction(e);
                }
            });
            m.show(list, e.getX(), e.getY());
        }
    }
    
    private void openUserInfoDialog() {
        Item item = list.getSelectedValue();
        if (item != null) {
            gui.openUserInfoDialog(item.targetUser, null, item.data.msgId);
        }
    }
    
    private void approve(Item item) {
        if (item == null) {
            item = list.getSelectedValue();
        }
        if (item == null) {
            return;
        }
        setPending(item);
        api.autoModApprove(item.data.msgId);
    }
    
    private void deny(Item item) {
        if (item == null) {
            item = list.getSelectedValue();
        }
        if (item == null) {
            return;
        }
        setPending(item);
        api.autoModDeny(item.data.msgId);
    }
    
    private void setPending(Item item) {
        item.setRequestPending(true);
        repaintFor(item);
    }
    
    private void repaintFor(Item item) {
        if (data.contains(item)) {
            list.repaint();
        }
    }
    
    /**
     * Selects the next entry in the list, relative to the current selection.
     */
    private void selectNext() {
        selectNext(false);
    }
    
    /**
     * Selects the previous entry in the list, relative to the current
     * selection.
     */
    private void selectPrevious() {
        selectPrevious(false);
    }
    
    /**
     * Selects the next entry in the list, relative to what is currently
     * selected.
     *
     * @param onlyUnhandled If true will only select an Item has a status of not
     * having been handled in any way yet
     */
    private void selectNext(boolean onlyUnhandled) {
        if (data.isEmpty()) {
            return;
        }
        int selected = list.getSelectedIndex();
        if (selected  == -1) {
            selected = data.size() - 2;
        }
        for (int i=selected+1;i < data.size();i++) {
            if (select(i, onlyUnhandled)) {
                return;
            }
        }
        /**
         * Tried, but could not select anything new, but also not currently at
         * the last item. This can happen with Alt+S when last item is already
         * handled to indicate there are no more items, but still scroll down
         * for items that may be added later.
         */
        if (list.getSelectedIndex() != data.size() - 1) {
            scrollDown();
            list.removeSelectionInterval(selected, selected);
        }
    }
    
    /**
     * Selects the previous entry in the list, relative to what is currently
     * selected.
     *
     * @param onlyUnhandled If true will only select an Item has a status of not
     * having been handled in any way yet
     */
    private void selectPrevious(boolean onlyUnhandled) {
        if (data.isEmpty()) {
            return;
        }
        int selected = list.getSelectedIndex();
        if (selected == -1) {
            selected = data.size();
        }
        for (int i=selected-1;i >= 0;i--) {
            if (select(i, onlyUnhandled)) {
                return;
            }
        }
    }
    
    /**
     * Select the given index of the list.
     * 
     * @param i The index to select
     * @param onlyUnhandled If true will only select this index if this Item is
     * not handled yet in any way
     * @return true if this index was selected, false otherwise
     */
    private boolean select(int i, boolean onlyUnhandled) {
        Item item = data.get(i);
        if (!onlyUnhandled || item.status == Item.STATUS_NONE) {
            list.setSelectedIndex(i);
            list.ensureIndexIsVisible(i);
            return true;
        }
        return false;
    }
    
    public static class Item {
        
        public static final int STATUS_NONE = 0;
        public static final int STATUS_HANDLED = 2;
        public static final int STATUS_ERROR = 3;
        public static final int STATUS_NA = 4;
        public static final int STATUS_APPROVED = 5;
        public static final int STATUS_DENIED = 6;
        
        public final ModeratorActionData data;
        public final User targetUser;
        private int status;
        private String handledBy;
        private boolean hasRequestPending;
        
        private Item(ModeratorActionData data, User targetUser) {
            this.data = data;
            this.targetUser = targetUser;
        }
        
        public void setStatus(int status, String handledBy) {
            this.status = status;
            this.handledBy = handledBy;
        }
        
        public void setStatus(int status) {
            setStatus(status, null);
        }
        
        public void setRequestPending(boolean isPending) {
            this.hasRequestPending = isPending;
        }
        
        public boolean hasRequestPending() {
            return hasRequestPending;
        }
        
        public String getHandledBy() {
            return handledBy;
        }
        
        public boolean isHandled() {
            return status == STATUS_APPROVED || status == STATUS_DENIED || status == STATUS_HANDLED || status == STATUS_NA;
        }
        
        /**
         * Returns the age of this item in seconds.
         * 
         * @return 
         */
        public long getAge() {
            return (System.currentTimeMillis() - data.created_at) / 1000;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] <%s> %s",
                    DateTime.format(data.created_at),
                    data.args.get(0),
                    data.args.get(1));
        }
        
        public String getStatusText() {
            if (hasRequestPending) {
                return "Pending";
            }
            switch (status) {
                case STATUS_NONE: return "";
                case STATUS_HANDLED: return "Handled";
                case STATUS_APPROVED: return "Approved";
                case STATUS_DENIED: return "Denied";
                case STATUS_ERROR: return "Error";
                case STATUS_NA: return "N/A";
            }
            return "";
        }
        
    }
    
   /**
     * Custom renderer to use a text area and borders etc.
     */
    private static class MyCellRenderer extends DefaultListCellRenderer {
        
        private final JTextArea area;
        
        public MyCellRenderer() {
            area = new JTextArea();
            area.setBorder(BorderFactory.createEmptyBorder(4, 5, 5, 5));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                area.setText(null);
                return area;
            }
            //System.out.println("Getting rubberstamp for "+value);
            Item item = (Item)value;
            
            // Make Text
            String agoText;
            if (System.currentTimeMillis() - item.data.created_at < 60*1000) {
                agoText = "now";
            } else {
                agoText = DateTime.agoSingleCompact(item.data.created_at);
            }
            String status;
            if (item.hasRequestPending()) {
                status = "-Pending- ";
            } else if (item.getHandledBy() == null) {
                status = item.status > Item.STATUS_NONE ? "-"+item.getStatusText()+"- " : "";
            } else {
                status = item.status > Item.STATUS_NONE ? "-"+item.getStatusText()+" by "+item.getHandledBy()+"- " : "";
            }
            String text = String.format("%s[%s] <%s> %s",
                    status,
                    agoText,
                    item.data.args.get(0),
                    item.data.args.get(1));
            area.setText(text);
            
            // Adjust size
            int width = list.getWidth();
            if (width > 0) {
                area.setSize(width, Short.MAX_VALUE);
            }

            // Selected Color
            if (isSelected) {
                area.setBackground(list.getSelectionBackground());
            } else {
                area.setBackground(list.getBackground());
            }
            if (item.isHandled() || item.getAge() > 60 * 5) {
                area.setForeground(Color.GRAY);
            } else {
                area.setForeground(list.getForeground());
            }
            return area;
        }
    }
    
}
