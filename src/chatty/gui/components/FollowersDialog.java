
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.RoomsContextMenu;
import chatty.gui.components.menus.StreamsContextMenu;
import chatty.gui.components.settings.ListTableModel;
import chatty.util.DateTime;
import chatty.util.StringUtil;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.TwitchApi;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Dialog showing a list of followers or subscribers. The type given to the
 * constructor simply changes a few strings, the other functionality is
 * identical for both followers and subscribers.
 * 
 * @author tduva
 */
public class FollowersDialog extends JDialog {

    public enum Type {
        FOLLOWERS("Followers"), SUBSCRIBERS("Subscribers");
        
        private final String name;
        
        Type(String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * In what interval to redraw the GUI/try to request new data.
     */
    private static final int REFRESH_TIMER = 10*1000;
    
    private final JLabel total = new JLabel("Total: 123.456 (+123.456)");
    private final JLabel stats = new JLabel("| Week: 99+ | Day: 99+ | Hour: 23");
    
    private final JTable table;
    private final ListTableModel<Follower> followers = new MyListTableModel();
    private final JLabel loadInfo = new JLabel();
    
    private final TwitchApi api;
    private final Type type;
    private final ContextMenuListener contextMenuListener;
    
    private final MyContextMenu mainContextMenu = new MyContextMenu();
    
    /**
     * What stream the dialog was opened for.
     */
    private String stream;
    
    /**
     * The most recently received data.
     */
    private FollowerInfo currentInfo;
    
    /**
     * The most recent received data that wasn't an error.
     */
    private FollowerInfo lastValidInfo;
    
    /**
     * Whether data was requested and we're currently waiting for the response.
     */
    private boolean loading;
    
    /**
     * When the data was last updated.
     */
    private long lastUpdated = -1;

    public FollowersDialog(Type type, Window owner, final TwitchApi api,
            ContextMenuListener contextMenuListener) {
        super(owner);
        
        this.contextMenuListener = contextMenuListener;
        this.type = type;
        this.api = api;
        
        // Layout
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        gbc = GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        total.setToolTipText("Total number of "+type);
        add(total, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 6, 3, 5);
        gbc.weightx = 1;
        stats.setToolTipText(type+" in the last 7 days (Week), 24 hours (Day) and Hour (based on the current list)");
        add(stats, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 4, 5, 4);
        table = new JTable(followers);
        table.setShowGrid(false);
        table.setTableHeader(null);
        table.getColumnModel().getColumn(0).setCellRenderer(new MyRenderer(MyRenderer.Type.NAME));
        table.getColumnModel().getColumn(1).setCellRenderer(new MyRenderer(MyRenderer.Type.TIME));
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(table.getFont().deriveFont(Font.BOLD));
        table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight()+2);
        add(new JScrollPane(table), gbc);
        
        gbc = GuiUtil.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(2, 5, 5, 5);
        add(loadInfo, gbc);
        
        // Timer
        Timer timer = new Timer(REFRESH_TIMER, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                request();
                update();
                table.repaint();
            }
        });
        timer.setRepeats(true);
        timer.start();
        
        // Listener
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                selectClicked(e);
                openContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectClicked(e);
                openContextMenu(e);
            }
        });
        // Add to content pane, seems to work better than adding to "this"
        getContentPane().addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                openMainContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                openMainContextMenu(e);
            }
        });
        
        pack();
        setSize(300,400);
    }
    
    /**
     * Select the row the mouse cursor is over, except if it is already over an
     * selected row, in which case it just keeps the current selection.
     * 
     * @param e The MouseEvent
     */
    private void selectClicked(MouseEvent e) {
        int row = table.rowAtPoint(e.getPoint());
        if (row != -1 && !table.isRowSelected(row)) {
            table.getSelectionModel().setSelectionInterval(row, row);
        }
    }
    
    /**
     * Open the context menu for the given MouseEvent if it is the popup trigger
     * and rows are selected.
     *
     * @param e The MouseEvent
     */
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Collection<String> streams = new HashSet<>();
            int[] selectedRows = table.getSelectedRows();
            for (int selectedRow : selectedRows) {
                Follower selected = followers.get(selectedRow);
                streams.add(StringUtil.toLowerCase(selected.name));
            }
            if (!streams.isEmpty()) {
                ContextMenu m = new StreamsContextMenu(streams, contextMenuListener);
                m.show(table, e.getX(), e.getY());
            }
        }
    }
    
    private void openMainContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            mainContextMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    /**
     * Adjust the width of the time column to fit the current times.
     */
    private void adjustColumnSize() {
        int width = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, 1);
            Component comp = table.prepareRenderer(renderer, row, 1);
            width = Math.max(comp.getPreferredSize().width, width);
        }
        setColumnWidth(1, width, width, width);
    }

    /**
     * Helper method to set a fixed column width.
     * 
     * @param column
     * @param width
     * @param minwidth
     * @param maxwidth 
     */
    private void setColumnWidth(int column, int width, int minwidth, int maxwidth) {
        TableColumn tc = table.getColumnModel().getColumn(column);
        tc.setPreferredWidth(width);
        if (maxwidth > 0) {
            tc.setMaxWidth(maxwidth);
        }
        if (minwidth > 0) {
            tc.setMinWidth(minwidth);
        }
    }
    
    /**
     * Try to request new data if the dialog is open and a stream is set.
     */
    private void request() {
        if (isVisible() && stream != null && !stream.isEmpty()) {
            loading = true;
            if (type == Type.FOLLOWERS) {
                api.getFollowers(stream);
            } else if (type == Type.SUBSCRIBERS) {
                api.getSubscribers(stream);
            }
        }
    }
    
    /**
     * Update the last loaded label and the stats.
     */
    private void update() {
        if (currentInfo != null) {
            if (currentInfo.requestError) {
                if (lastUpdated != -1) {
                    loadInfo.setText(String.format("%s (%s ago, updated %s ago)",
                            currentInfo.requestErrorDescription,
                            DateTime.agoSingleCompact(currentInfo.time),
                            DateTime.agoSingleCompact(lastUpdated))
                    );
                } else {
                    loadInfo.setText(String.format("%s (%s ago)",
                            currentInfo.requestErrorDescription,
                            DateTime.agoSingleCompact(currentInfo.time))
                    );
                }
            } else {
                loadInfo.setText(String.format("Last updated %s ago",
                        DateTime.agoSingleVerbose(lastUpdated))
                );
            }
        }
        if (loading) {
            loadInfo.setText("Loading..");
        }
        updateStats();
    }
    
    /**
     * Open the dialog with the given stream.
     * 
     * @param stream 
     */
    public void showDialog(String stream) {
        this.stream = stream;
        setTitle(type+" of "+stream+" (100 most recent)");
        if (currentInfo == null || !currentInfo.stream.equals(stream)) {
            // Set to default if no info is set yet or if it is opened on a
            // different channel than before.
            followers.clear();
            total.setText("Total: -");
            stats.setText(null);
            currentInfo = null;
            lastValidInfo = null;
            lastUpdated = -1;
            updateStats();
        }
        setVisible(true);
        request();
        update();
    }
    
    /**
     * Set the FollowerInfo as it is received (from API or cache), if it is for
     * the same stream as the stream this dialog is currently opened for.
     * 
     * @param info The FollowerInfo to set
     */
    public void setFollowerInfo(FollowerInfo info) {
        if (info.stream.equals(stream)) {
            loading = false;
            if (!info.requestError &&
                    (currentInfo == null || currentInfo.time != info.time)) {
                // Only actually refresh list if this is new FollowerInfo and
                // not cached (and no error)
                setNewFollowerInfo(info);
            }
            currentInfo = info;
            update();
        }
    }
    
    /**
     * Set FollowerInfo that is actually new (and not cached).
     * 
     * @param info The FollwerInfo
     */
    private void setNewFollowerInfo(FollowerInfo info) {
        lastValidInfo = info;
        followers.setData(info.followers);
        updateTotalLabel(info, currentInfo);
        lastUpdated = info.time;
        adjustColumnSize();
    }

    
    /**
     * Update the total label, based on new and old data (to show the difference
     * if applicable).
     * 
     * @param newInfo
     * @param oldInfo 
     */
    private void updateTotalLabel(FollowerInfo newInfo, FollowerInfo oldInfo) {
        if (oldInfo != null && newInfo != oldInfo && oldInfo.stream.equals(stream)
                && !oldInfo.requestError) {
            int change = newInfo.total - oldInfo.total;
            String changeString = "";
            if (change < 0) {
                changeString = " (" + String.valueOf(change) + ")";
            } else if (change > 0) {
                changeString = " (+" + change + ")";
            }
            total.setText("Total: " + Helper.formatViewerCount(newInfo.total) + changeString);
        } else {
            total.setText("Total: " + Helper.formatViewerCount(newInfo.total));
        }
    }
    
    /**
     * Update the stats label.
     */
    private void updateStats() {
        if (lastValidInfo != null) {
            stats.setText("| "+Stats.makeFullStats(lastValidInfo));
            //System.out.println("Update stats");
        } else {
            stats.setText("| Week: - | Day: - | Hour: -");
        }
    }
    
    /**
     * Save last valid FollowerInfo entries to file.
     * 
     * @param onlyName Whether to only save the name, or including times
     */
    private void saveToFile(boolean onlyName) {
        FollowerInfo info = lastValidInfo;
        if (info != null) {
            Path path = Paths.get(Chatty.getUserDataDirectory(),"exported");
            Path file = path.resolve(StringUtil.toLowerCase(type.toString())+".txt");
            try {
                Files.createDirectories(path);
                try (BufferedWriter writer = Files.newBufferedWriter(file, Charset.forName("UTF-8"))) {

                    for (Follower f : lastValidInfo.followers) {
                        writer.write(f.name);
                        if (!onlyName) {
                            writer.write("\t" + DateTime.formatFullDatetime(f.time));
                            writer.write(" (" + DateTime.agoSingleVerbose(f.time) + ")");
                        }
                        writer.newLine();
                    }
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, ex, "Failed to write file.", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, type+" exported to: "+file, "File written.", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No data to write.", "Failed to write file.", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Renderer for both the name and time row, behaving slightly different
     * depending on the type set (but same background colors).
     */
    private static class MyRenderer extends DefaultTableCellRenderer {

        private static final Color BG_COLOR_NEW = new Color(255,245,210);
        private static final Color BG_COLOR_RECENT = new Color(255,250,240);
        private static final Color BG_COLOR_HOUR = new Color(245,245,245);
        
        private static final Color COLOR_OLDER_THAN_WEEK = new Color(180, 180, 180);
        private static final Color COLOR_OLDER_THAN_DAY = new Color(120, 120, 120);
        
        private final Type type;
        
        public enum Type {
            NAME, TIME
        }
        
        public MyRenderer(Type type) {
            this.type = type;
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            /**
             * In rare cases apparently value can be null (even though no
             * Follower object can be null).
             */
            if (value == null) {
                setText("");
                setToolTipText("error");
                return this;
            }
            
            Follower f = (Follower)value;
            
            // Text
            if (type == Type.NAME) {
                if (f.name.equalsIgnoreCase(f.display_name)) {
                    setText(f.display_name);
                    setToolTipText(f.display_name);
                } else {
                    setText(f.display_name+" ("+f.name+")");
                    setToolTipText(f.display_name+" ("+f.name+")");
                }
            } else {
                setText(DateTime.agoSingleVerbose(f.time));
                setToolTipText(DateTime.formatFullDatetime(f.time));
            }

            // Colors
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setForeground(table.getForeground());

                // Set background for both name and time
                long ago = (System.currentTimeMillis() - f.time) / 1000;
                if (f.newFollower) {
                    setBackground(BG_COLOR_NEW);
                } else if (ago < 15 * 60) {
                    setBackground(BG_COLOR_RECENT);
                } else if (ago < 60 * 60) {
                    setBackground(BG_COLOR_HOUR);
                } else {
                    setBackground(table.getBackground());
                }
                // Set foreground for time
                if (type == Type.TIME) {
                    if (ago > 60 * 60 * 24 * 7) {
                        setForeground(COLOR_OLDER_THAN_WEEK);
                    } else if (ago > 60 * 60 * 24) {
                        setForeground(COLOR_OLDER_THAN_DAY);
                    }
                }
                // Set foreground for name
                if (type == Type.NAME) {
                    if (f.refollow) {
                        setForeground(Color.GRAY);
                    }
                }
            }
            
            // Set alignment
            if (type == Type.TIME) {
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }
            return this;
        }

    }
    
    /**
     * Helper class making stats out of a FollowerInfo.
     */
    private static class Stats {

        private static final Map<Integer, String> TIMES = new LinkedHashMap<>();
        private static final Map<Integer, String> TIMES2 = new LinkedHashMap<>();
        
        static {
            add(60*60*24*30, "This Month");
            add(60*60*24*7, "This Week");
            add(60*60*24, "Today");
            add(60*60, "Last Hour");
            add(60*30, "Last 30 Minutes");
            add(60*15, "Last 15 Minutes");
            add(60*5, "Last 5 Minutes");

            add2(60*60*24*7, "Week");
            add2(60*60*24, "Day");
            add2(60*60, "Hour");
        }
        
        private static void add(int seconds, String label) {
            TIMES.put(seconds, label);
        }
        
        private static void add2(int seconds, String label) {
            TIMES2.put(seconds, label);
        }
        
        private static String makeFullStats(FollowerInfo info) {
            if (info.requestError) {
                return "";
            }
            StringBuilder b = new StringBuilder();
            List<Follower> followers = info.followers;
            boolean first = true;
            for (Integer time : TIMES2.keySet()) {
                if (!first) {
                    b.append(" | ");
                }
                b.append(statsForTime(followers, time, TIMES2.get(time)));
                first = false;
            }
            return b.toString();
        }
        
        private static String statsForTime(List<Follower> followers, int seconds, String label) {
            boolean ok = false;
            for (int i = followers.size() - 1; i >= 0; i--) {
                Follower f = followers.get(i);
                if ((System.currentTimeMillis() - f.time) / 1000 > seconds) {
                    ok = true;
                } else if (ok) {
                    return label + ": " + (i + 1);
                } else {
                    return label + ": " +i+"+";
                }
            }
            return label + ": 0";
        }
        
        private static String makeStats(FollowerInfo info) {
            if (info.requestError) {
                return "";
            }
            List<Follower> followers = info.followers;
            for (Integer time : TIMES.keySet()) {
                boolean ok = false;
                for (int i = followers.size()-1; i > 0; i--) {
                    Follower f = followers.get(i);
                    if ((System.currentTimeMillis() - f.time)/1000 > time) {
                        ok = true;
                    } else if (ok) {
                        return TIMES.get(time)+": "+(i+1);
                    }
                }
            }
            return "";
        }
        
    }
    
    /**
     * Table Model with 2 columns.
     */
    private class MyListTableModel extends ListTableModel<Follower> {

        public MyListTableModel() {
            super(new String[]{"Name","Followed"});
        }

        /**
         * Return the Follower for both columns, the renderer needs it to draw
         * the appropriate background color etc.
         * 
         * @param rowIndex
         * @param columnIndex
         * @return 
         */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return get(rowIndex);
        }
        
    }
    
    private class MyContextMenu extends ContextMenu {

        public MyContextMenu() {
            final String saveMenu = "Export list to file";
            addItem("saveSimple", "Names only", saveMenu);
            addItem("saveVerbose", "Names and dates", saveMenu);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("saveSimple")) {
                saveToFile(true);
            } else if (e.getActionCommand().equals("saveVerbose")) {
                saveToFile(false);
            }
        }
        
    }
    
}
