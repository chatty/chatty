
package chatty.gui.components;

import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.StreamInfosContextMenu;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.StreamInfo;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * List where items are added to that were removed from the LiveStreamsDialog.
 * This includes offline streams as well as streams that didn't receive any
 * data anymore and are assumed offline (e.g. if you leave a channel).
 * 
 * @author tduva
 */
public class LiveStreamsRemovedList extends JPanel {
    
    private final JList<RemovedListItem> list;
    private final JButton button;
    private final LiveStreamListener streamListener;
    private final List<ContextMenuListener> contextMenuListeners;
    private final SortedListModel<RemovedListItem> data;
    
    public LiveStreamsRemovedList(LiveStreamListener l) {
        setLayout(new GridBagLayout());
        streamListener = l;
        contextMenuListeners = new ArrayList<>();
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        button = new JButton(Language.getString("streams.removed.button.back"));
        add(button, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        list = new JList<>();
        data = new SortedListModel<>();
        list.setModel(data);
        list.addMouseListener(new MyMouseListener());
        add(new JScrollPane(list), gbc);
    }
    
    public void addStreamInfo(StreamInfo info) {
        data.add(new RemovedListItem(info));
    }
    
    public void removeStreamInfo(StreamInfo info) {
        data.remove(getListItem(info));
    }
    
    public void addContextMenuListener(ContextMenuListener cml) {
        if (cml != null) {
            contextMenuListeners.add(cml);
        }
    }
    
    public void addBackButtonListener(ActionListener listener) {
        button.addActionListener(listener);
    }
    
    private RemovedListItem getListItem(StreamInfo info) {
        for (RemovedListItem item : data) {
            if (item.getStreamInfo() == info) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * Open context menu for this user, if the event points at one.
     *
     * @param e
     */
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            selectClicked(e, false);
            List<RemovedListItem> selectedItems = list.getSelectedValuesList();
            List<StreamInfo> selected = new ArrayList<>();
            for (RemovedListItem item : selectedItems) {
                selected.add(item.getStreamInfo());
            }
            StreamInfosContextMenu m = new StreamInfosContextMenu(selected, false, false, false);
            for (ContextMenuListener cml : contextMenuListeners) {
                m.addContextMenuListener(cml);
            }
            m.show(list, e.getX(), e.getY());
        }
    }
    
    /**
     * Adds selection of the clicked element, or removes selection if no
     * element was clicked.
     * 
     * @param e
     * @param onlyOutside 
     */
    private void selectClicked(MouseEvent e, boolean onlyOutside) {
        int index = list.locationToIndex(e.getPoint());
        Rectangle bounds = list.getCellBounds(index, index);
        if (bounds != null && bounds.contains(e.getPoint())) {
            if (!onlyOutside) {
                if (list.isSelectedIndex(index)) {
                    list.addSelectionInterval(index, index);
                } else {
                    list.setSelectedIndex(index);
                }
            }
        } else {
            list.clearSelection();
        }
    }
    
    private class MyMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            selectClicked(e, true);
            openContextMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            openContextMenu(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                RemovedListItem item = list.getSelectedValue();
                if (item != null && streamListener != null) {
                    streamListener.liveStreamClicked(item.getStreamInfo());
                }
            }
        }
    }
    
    private static class RemovedListItem implements Comparable<RemovedListItem> {
        
        private final StreamInfo info;
        private final long time;
        
        public RemovedListItem(StreamInfo info) {
            this.info = info;
            this.time = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return DateTime.format(time)+" "+info.getCapitalizedName()
                    +(info.getFollowed() ? " (followed)" : "");
        }
        
        public StreamInfo getStreamInfo() {
            return info;
        }
        
        public long getTime() {
            return time;
        }

        @Override
        public int compareTo(RemovedListItem o) {
            if (time > o.getTime()) {
                return -1;
            }
            if (time == o.getTime()) {
                return info.getStream().compareTo(o.getStreamInfo().getStream());
            }
            return 1;
        }
        
    }
    
}
