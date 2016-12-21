
package chatty.gui.components;

import chatty.Helper;
import chatty.TwitchClient;
import chatty.User;
import chatty.gui.MainGui;
import chatty.gui.components.menus.AutoModContextMenu;
import chatty.util.DateTime;
import chatty.util.MiscUtil;
import chatty.util.api.TwitchApi;
import chatty.util.api.pubsub.ModeratorActionData;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
    
    private static final int MESSAGE_LIMIT = 50;

    private final MainGui gui;
    private final TwitchApi api;
    private final TwitchClient client;
    
    private final JList<Item> list;
    private final DefaultListModel<Item> data;
    private final Map<String, List<Item>> cache = new HashMap<>();

    private String currentRoom = "";
    private String currentRoomLoaded = "";
    
    public AutoModDialog(MainGui main, TwitchApi api, TwitchClient client) {
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
        
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("A"), "automod.approve");
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt A"), "automod.approve");
        list.getActionMap().put("automod.approve", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                approve(null);
            }
        });
        
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("D"), "automod.deny");
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt D"), "automod.deny");
        list.getActionMap().put("automod.deny", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                deny(null);
            }
        });
        
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("S"), "automod.next");
        list.getActionMap().put("automod.next", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectNext();
            }
        });
        
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("W"), "automod.previous");
        list.getActionMap().put("automod.previous", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectPrevious();
            }
        });
        
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt S"), "automod.nextUnhandled");
        list.getActionMap().put("automod.nextUnhandled", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectNext(true);
            }
        });
        
        list.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt W"), "automod.previousUnhandled");
        list.getActionMap().put("automod.previousUnhandled", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                selectPrevious(true);
            }
        });
        
        ComponentListener l = new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
// Trick from kleopatra:
// http://stackoverflow.com/questions/7306295/swing-jlist-with-multiline-text-and-dynamic-height
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
    
    public void showDialog() {
        switchDataToCurrent();
        setVisible(true);
        list.setSelectedIndex(data.size() - 1);
        scrollDown();
    }
    
    public void setChannel(String channel) {
        if (channel != null && !channel.equals(currentRoom)) {
            currentRoom = channel;
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
        if (!"twitchbot".equals(modData.created_by)) {
            return;
        }
        if ("twitchbot_rejected".equals(modData.moderation_action)) {
            addItem(modData);
        }
    }
    
    public void requestResult(String result, String msgId) {
        Item changedItem = findItemByMsgId(msgId);
        if (changedItem != null) {
            if (result.equals("approved")) {
                changedItem.setStatus(Item.STATUS_APPROVED);
            } else if (result.equals("denied")) {
                changedItem.status = Item.STATUS_DENIED;
            } else if (changedItem.status <= Item.STATUS_PENDING) {
                if (result.equals("400")) {
                    changedItem.status = Item.STATUS_HANDLED;
                } else if (result.equals("404")) {
                    changedItem.status = Item.STATUS_NA;
                } else {
                    changedItem.status = Item.STATUS_ERROR;
                }
            }
        }
        if (data.contains(changedItem)) {
            list.repaint();
        }
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
        if (modData.args.size() != 2 || modData.msgId.isEmpty()) {
            return;
        }
        
        String room = modData.stream;
        String channel = Helper.toValidChannel(modData.stream);
        String username = modData.args.get(0);
        String message = modData.args.get(1);
        if (channel == null) {
            return;
        }
        if (!Helper.validateStream(username)) {
            return;
        }
        
        User user = client.getUser(channel, username);
        user.addAutoModMessage(message);
        gui.updateUserinfo(user);
        Item item = new Item(modData, user);

        if (!cache.containsKey(room)) {
            cache.put(room, new ArrayList<Item>());
        }
        cache.get(room).add(item);
        if (cache.get(room).size() > MESSAGE_LIMIT) {
            cache.get(room).remove(0);
            data.remove(0);
        }

        if (room.equals(currentRoom)) {
            data.addElement(item);
            scrollDownIfApplicable();
        }
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
            if (selectedItem == null) {
                return;
            }
            AutoModContextMenu m = new AutoModContextMenu(selectedItem, new AutoModContextMenu.AutoModContextMenuListener() {

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
                }
            });
            m.show(list, e.getX(), e.getY());
        }
    }
    
    private void openUserInfoDialog() {
        Item item = list.getSelectedValue();
        if (item != null) {
            gui.openUserInfoDialog(item.targetUser, null);
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
        api.autoMod("approve", item.data.msgId);
    }
    
    private void deny(Item item) {
        if (item == null) {
            item = list.getSelectedValue();
        }
        if (item == null) {
            return;
        }
        setPending(item);
        api.autoMod("deny", item.data.msgId);
    }
    
    private void setPending(Item item) {
        item.status = Item.STATUS_PENDING;
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
        public static final int STATUS_PENDING = 1;
        public static final int STATUS_HANDLED = 2;
        public static final int STATUS_ERROR = 3;
        public static final int STATUS_NA = 4;
        public static final int STATUS_APPROVED = 5;
        public static final int STATUS_DENIED = 6;
        
        public final ModeratorActionData data;
        public final User targetUser;
        private int status;
        
        private Item(ModeratorActionData data, User targetUser) {
            this.data = data;
            this.targetUser = targetUser;
        }
        
        public void setStatus(int status) {
            this.status = status;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] <%s> %s",
                    DateTime.format(data.created_at),
                    data.args.get(0),
                    data.args.get(1));
        }
        
        public String getStatusText() {
            switch (status) {
                case STATUS_NONE: return "";
                case STATUS_PENDING: return "Pending";
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
            String status = item.status > 0 ? "-"+item.getStatusText()+"- " : "";
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
                area.setForeground(list.getSelectionForeground());
            } else {
                area.setBackground(list.getBackground());
                if (item.status > Item.STATUS_PENDING) {
                    area.setForeground(Color.GRAY);
                } else {
                    area.setForeground(list.getForeground());
                }
            }
            return area;
        }
    }
    
}
