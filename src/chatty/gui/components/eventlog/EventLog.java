
package chatty.gui.components.eventlog;

import chatty.gui.MainGui;
import chatty.lang.Language;
import java.awt.BorderLayout;
import java.awt.Color;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class EventLog extends JDialog {
    
    private final MainGui g;
    private final EventList notificationList;
    private final EventList systemList;
    private final JTabbedPane tabs;
    private final JButton systemMarkRead;
    private final Set<String> sessionReadEvents = new HashSet<>();
    
    public EventLog(MainGui g) {
        super(g);
        setTitle(Language.getString("eventLog.title"));
        this.g = g;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        notificationList = new EventList(this);
        systemList = new EventList(this);
        
        setLayout(new BorderLayout());
        
        JScrollPane scroll1 = new JScrollPane(notificationList);
        scroll1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        JScrollPane scroll2 = new JScrollPane(systemList);
        scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        tabs = new JTabbedPane();
        // Disabled for now
//        tabs.addTab("Notifications", scroll1);
        JPanel systemPanel = new JPanel();
        systemPanel.setLayout(new BorderLayout());
        systemMarkRead = new JButton(Language.getString("eventLog.button.markAllAsRead"));
        systemMarkRead.setToolTipText(Language.getString("eventLog.button.markAllAsRead.tip"));
        systemPanel.add(systemMarkRead, BorderLayout.SOUTH);
        systemPanel.add(scroll2, BorderLayout.CENTER);
        tabs.addTab("Chatty Info", systemPanel);
        
        systemMarkRead.addActionListener(e -> {
            if (g.getSettings().getList("readEvents").isEmpty()
                    && sessionReadEvents.isEmpty()) {
                JOptionPane.showMessageDialog(this, Language.getString("eventLog.firstMarkReadNote"));
            }
            for (String id : systemList.getEventIds()) {
                // Some events should only be marked as read per session
                if (id.startsWith("session.")) {
                    sessionReadEvents.add(id);
                }
                else {
                    g.getSettings().setAdd("readEvents", id);
                }
            }
            updateEventState();
        });
        
        add(tabs, BorderLayout.CENTER);
        
        // TEST
//        for (int i=0;i<100;i++) {
//            systemList.addEvent(new Event(Event.Type.SYSTEM, null, "Test "+i, "Note: You can also open the Event Log through the 'View'-menu. "+i, null, null));
//        }
        
        setSize(460, 400);
    }
    
    @Override
    public void setVisible(boolean visible) {
        setLocationRelativeTo(g);
        super.setVisible(visible);
        if (visible) {
            updateEventState();
            SwingUtilities.invokeLater(() -> {
                systemList.fixLayout();
                notificationList.fixLayout();
            });
        }
    }
    
    public void setTab(int tab) {
        // Disabled for now
//        tabs.setSelectedIndex(tab);
    }
    
    protected boolean isReadEvent(String id) {
        return id != null && (g.getSettings().listContains("readEvents", id)
                || sessionReadEvents.contains(id));
    }
    
    private void updateEventState() {
        systemList.repaint();
        int count = getNewSystemEvents();
        g.setSystemEventCount(count);
        systemMarkRead.setEnabled(count > 0);
    }
    
    public int getNewSystemEvents() {
        return systemList.getNewEvents();
    }
    
    public void add(Event event) {
        if (event.type == Event.Type.NOTIFICATION) {
            // Disabled for now
//            notificationList.addEvent(event);
        }
        else {
            systemList.addEvent(event);
            updateEventState();
        }
    }
    
    public void remove(Event event) {
        if (event.type == Event.Type.NOTIFICATION) {
            // Disabled for now
        }
        else {
            systemList.removeEvent(event);
            updateEventState();
        }
    }
    
    private static EventLog main;
    
    /**
     * Add an event with the given id and arguments. If the id already exists in
     * the list the event will not be added (it can be removed first to update
     * it).
     * 
     * @param id
     * @param arguments 
     */
    public static void addSystemEvent(String id, Object... arguments) {
        String stringId = id;
        if (id.startsWith("session.")) {
            stringId = id.substring("session.".length());
        }
        String title = Language.getString("eventLog.entry."+stringId+".title");
        String text = Language.getString("eventLog.entry."+stringId+".text");
        String origText = Language.getBundleForLanguage("en").getString("eventLog.entry."+stringId+".text");
        if (arguments.length > 0) {
            text = MessageFormat.format(text, arguments);
            origText = MessageFormat.format(origText, arguments);
        }
        if (!origText.equals(text)) {
            text += "\n[English Original: "+origText+"]";
        }
        addSystemEvent(id, title, text);
    }
    
    public static void addSystemEvent(String id, String title, String text) {
        if (main != null) {
            main.add(new Event(Event.Type.SYSTEM, id, title, text, null, null));
        }
    }
    
    public static void removeSystemEvent(String id) {
        if (main != null) {
            main.remove(new Event(Event.Type.SYSTEM, id, null, null, null, null));
        }
    }
    
    public static void setMain(EventLog main) {
        EventLog.main = main;
    }
    
}
