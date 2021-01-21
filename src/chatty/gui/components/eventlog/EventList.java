
package chatty.gui.components.eventlog;

import chatty.lang.Language;
import chatty.util.DateTime;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class EventList extends JList<Event> {
    
    private final MyListModel data;
    private final EventLog main;
    
    public EventList(EventLog main) {
        setCellRenderer(new MyCellRenderer(main));
        this.data = new MyListModel();
        this.main = main;
        setModel(data);
        
        ComponentListener cl = new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                fixLayout();
            }

        };
        addComponentListener(cl);
        
        // Update times
        Timer timer = new Timer(30*1000, e -> {
            repaint();
        });
        timer.setRepeats(true);
        timer.start();
    }

    /**
     * Fix list entry height. Seems to be required to do more often than when
     * just using a single JTextArea (like LiveStreamsList).
     */
    public void fixLayout() {
        setFixedCellHeight(10);
        setFixedCellHeight(-1);
    }
    
    public int getNewEvents() {
        int count = 0;
        for (Event event : data.items) {
            if (event.id != null && !main.isReadEvent(event.id)) {
                count++;
            }
        }
        return count;
    }
    
    public Set<String> getEventIds() {
        Set<String> result = new HashSet<>();
        for (Event event : data.items) {
            if (event.id != null) {
                result.add(event.id);
            }
        }
        return result;
    }
    
    public void addEvent(Event event) {
        data.add(event);
        if (data.getSize() == 1) {
            SwingUtilities.invokeLater(() -> fixLayout());
        }
    }
    
    public void removeEvent(Event event) {
        data.remove(event);
    }
    
    private static class MyListModel extends AbstractListModel<Event> {

        private static final int MAX_ENTRIES = 50;

        private final List<Event> items = new ArrayList<>();

        @Override
        public int getSize() {
            return items.size();
        }

        @Override
        public Event getElementAt(int index) {
            return items.get(index);
        }

        public void add(Event event) {
            if (getIndexOf(event) != -1) {
                return;
            }
            items.add(0, event);
            super.fireIntervalAdded(this, 0, 0);
            if (items.size() > MAX_ENTRIES) {
                int removeIndex = items.size() - 1;
                items.remove(removeIndex);
                super.fireIntervalRemoved(this, removeIndex, removeIndex);
            }
        }
        
        public void remove(Event event) {
            int index = getIndexOf(event);
            if (index != -1) {
                items.remove(index);
                super.fireIntervalRemoved(this, index, index);
            }
        }
        
        private int getIndexOf(Event event) {
            for (int i = 0; i < items.size(); i++) {
                Event e = items.get(i);
                if (e.id != null && e.id.equals(event.id)) {
                    return i;
                }
            }
            return -1;
        }

    }
    
    /**
     * To prevent horizontal scrolling and allow for tracking of the viewport
     * width.
     * 
     * @return 
     */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }
    
    /**
     * Custom renderer to use a text area and borders etc.
     */
    private static class MyCellRenderer extends DefaultListCellRenderer {
        
        private final JTextArea area;
        private final JPanel panel;
        private final JLabel title = new JLabel();
        private final JLabel time = new JLabel();
        private final EventLog main;
        
        public MyCellRenderer(EventLog main) {
            this.main = main;
            panel = new JPanel();
            panel.setLayout(new GridBagLayout());
            area = new JTextArea();
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setOpaque(false);
            time.setFont(time.getFont().deriveFont(Font.PLAIN));
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(4, 5, 1, 5);
            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            panel.add(title, gbc);
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            panel.add(time, gbc);
            
            gbc.insets = new Insets(1, 5, 6, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.gridwidth = 2;
            panel.add(area, gbc);
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            if (value == null) {
                title.setText(null);
                area.setText(null);
                time.setText(null);
                return panel;
            }
            Event event = (Event)value;
            
            if (event.id != null && !main.isReadEvent(event.id)) {
                title.setText(String.format("(%s) %s",
                        Language.getString("eventLog.new"),
                        event.title));
                title.setText("(New) "+event.title);
            }
            else {
                title.setText(event.title);
            }
            area.setText(event.text);
            time.setText(DateTime.agoSingleCompactAboveMinute(event.createdAt));
            
            // Adjust size
            int width = list.getWidth();
            if (width > 0) {
                panel.setSize(width, Short.MAX_VALUE);
            }
            
            Color foreground;
            Color background;
            if (isSelected) {
                foreground = list.getSelectionForeground();
                background = list.getSelectionBackground();
            }
            else if (event.foreground != null && event.background != null) {
                foreground = event.foreground;
                background = event.background;
            }
            else {
                foreground = list.getForeground();
                background = list.getBackground();
            }
            
            time.setForeground(foreground);
            title.setForeground(foreground);
            area.setForeground(foreground);
            panel.setBackground(background);
            return panel;
        }
        
    }
    
}
