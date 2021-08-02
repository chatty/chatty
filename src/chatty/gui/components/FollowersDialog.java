
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.User;
import chatty.gui.DockedDialogHelper;
import chatty.gui.DockedDialogManager;
import chatty.gui.GuiUtil;
import chatty.gui.LaF;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.StreamsContextMenu;
import chatty.gui.components.menus.UserContextMenu;
import chatty.gui.components.settings.ListTableModel;
import chatty.util.DateTime;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import chatty.util.api.Follower;
import chatty.util.api.FollowerInfo;
import chatty.util.api.TwitchApi;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.dnd.DockContent;
import chatty.util.dnd.DockContentContainer;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
    private final MainGui main;
    private final Type type;
    private final ContextMenuListener contextMenuListener;
    private final DockedDialogHelper helper;
    
    private final MyRenderer timeRenderer = new MyRenderer(MyRenderer.Type.TIME);
    private final MyRenderer timeRenderer2 = new MyRenderer(MyRenderer.Type.USER_TIME);
    
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
    
    private boolean compactMode;
    
    private boolean showRegistered;

    public FollowersDialog(Type type, MainGui owner, final TwitchApi api,
            ContextMenuListener contextMenuListener, DockedDialogManager dockedDialogs) {
        super(owner);
        
        this.contextMenuListener = contextMenuListener;
        this.type = type;
        this.main = owner;
        this.api = api;

        JPanel mainPanel = new JPanel(new GridBagLayout());
        
        GridBagConstraints gbc;
        gbc = GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        total.setToolTipText("Total number of "+type);
        mainPanel.add(total, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 6, 3, 5);
        gbc.weightx = 1;
        stats.setToolTipText(type+" in the last 7 days (Week), 24 hours (Day) and Hour (based on the current list)");
        mainPanel.add(stats, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 4, 5, 4);
        table = new JTable(followers);
        table.setShowGrid(false);
        table.setTableHeader(null);
        // Note: Column widths are adjusted when data is loaded
        table.getColumnModel().getColumn(0).setCellRenderer(new MyRenderer(MyRenderer.Type.NAME));
        table.getColumnModel().getColumn(1).setCellRenderer(timeRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(timeRenderer2);
        int nameMinWidth = table.getFontMetrics(table.getFont()).stringWidth("reasonblylong");
        table.getColumnModel().getColumn(0).setMinWidth(nameMinWidth);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(table.getFont().deriveFont(Font.BOLD));
        table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight()+2);
        mainPanel.add(new JScrollPane(table), gbc);
        
        gbc = GuiUtil.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(2, 5, 5, 5);
        mainPanel.add(loadInfo, gbc);
        
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
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int selectedRow = table.getSelectedRow();
                    if (selectedRow != -1) {
                        Follower follower = followers.get(selectedRow);
                        User user = main.getUser(Helper.toChannel(stream), follower.name);
                        contextMenuListener.userMenuItemClicked(
                                new ActionEvent(this, ActionEvent.ACTION_FIRST, "userinfo"),
                                user, null, null);
                    }
                }
            }
        });
        // Add to content pane, seems to work better than adding to "this"
        mainPanel.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                openMainContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                openMainContextMenu(e);
            }
        });
        
        add(mainPanel);
        
        DockContent content = dockedDialogs.createContent(mainPanel, type.name,
                type == Type.FOLLOWERS ? "-followers-" : "-subscribers-");
        helper = dockedDialogs.createHelper(new DockedDialogHelper.DockedDialog() {
            @Override
            public void setVisible(boolean visible) {
                FollowersDialog.super.setVisible(visible);
            }

            @Override
            public boolean isVisible() {
                return FollowersDialog.super.isVisible();
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
                return FollowersDialog.this;
            }

            @Override
            public DockContent getContent() {
                return content;
            }
        });
        
        pack();
        setSize(300,400);
        
        GuiUtil.installEscapeCloseOperation(this);
    }
    
    @Override
    public void setVisible(boolean visible) {
        helper.setVisible(visible, true);
    }
    
    @Override
    public boolean isVisible() {
        return helper.isVisible();
    }
    
    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        if (helper != null) {
            helper.getContent().setLongTitle(title);
        }
    }
    
    public void setCompactMode(boolean compactMode) {
        this.compactMode = compactMode;
        timeRenderer.setCompactMode(compactMode);
        timeRenderer2.setCompactMode(compactMode);
        updateColumnsAfterSettingChange();
    }
    
    private TableColumn regColumn;
    
    public void setShowRegistered(boolean show) {
        this.showRegistered = show;
        if (!show) {
            if (regColumn == null) {
                TableColumn column = table.getColumnModel().getColumn(2);
                table.removeColumn(column);
                regColumn = column;
            }
        }
        else {
            if (regColumn != null) {
                table.addColumn(regColumn);
                regColumn = null;
            }
        }
        updateColumnsAfterSettingChange();
    }
    
    private void updateColumnsAfterSettingChange() {
        adjustColumnSize();
        if (currentInfo != null) {
            // Reset data for proper resizing
            FollowerInfo current = currentInfo;
            setFollowerInfo(new FollowerInfo(Follower.Type.FOLLOWER, stream, ""));
            setFollowerInfo(current);
        }
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
            ContextMenu m = null;
            if (streams.size() == 1) {
                User user = main.getUser(Helper.toChannel(stream), streams.iterator().next());
                m = new UserContextMenu(user, null, null, contextMenuListener);
            }
            else if (!streams.isEmpty()) {
                m = new StreamsContextMenu(streams, contextMenuListener);
            }
            if (m != null) {
                m.addSeparator();
                m.addCheckboxItem("toggleBoolean_followersCompact", "Compact Mode", compactMode);
                m.addCheckboxItem("toggleBoolean_followersReg", "Show Registered", showRegistered);
                m.show(table, e.getX(), e.getY());
            }
        }
    }
    
    private void openMainContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            new MyContextMenu(helper.isDocked()).show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    private void adjustColumnSize() {
        adjustColumnSize(1);
        if (showRegistered) {
            adjustColumnSize(2);
        }
    }

    /**
     * Adjust the width of the time column to fit the current times.
     */
    private void adjustColumnSize(int column) {
        int width = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            TableCellRenderer renderer = table.getCellRenderer(row, column);
            Component comp = table.prepareRenderer(renderer, row, column);
            width = Math.max(comp.getPreferredSize().width, width);
        }
        setColumnWidth(column, width, width, width);
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
        if (Debugging.isEnabled("followerTest")) {
            info = createTestFollowerInfo();
        }
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
                            writer.write("\t" + DateTime.formatFullDatetime(f.follow_time));
                            writer.write(" (" + DateTime.agoSingleVerbose(f.follow_time) + ")");
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
        
        private static final Color BG_COLOR_NEW = LaF.isDarkTheme() ? new Color(114, 0, 0) : new Color(255,245,210);
        private static final Color BG_COLOR_RECENT;
        
        private static final Color COLOR_OLDER_THAN_WEEK;
        private static final Color COLOR_OLDER_THAN_DAY;
        
        static {
            JTable table = new JTable();
            BG_COLOR_RECENT = ColorCorrectionNew.offset(table.getBackground(), 0.96f);
            
            COLOR_OLDER_THAN_WEEK = ColorCorrectionNew.offset(table.getForeground(), 0.6f);
            COLOR_OLDER_THAN_DAY = ColorCorrectionNew.offset(table.getForeground(), 0.78f);
        }
        
        private final Type type;
        
        public enum Type {
            NAME, TIME, USER_TIME
        }
        
        private boolean compactMode;
        
        public MyRenderer(Type type) {
            this.type = type;
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }
        
        public void setCompactMode(boolean compactMode) {
            this.compactMode = compactMode;
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
                    setToolTipText(null);
                }
                else {
                    setText(f.display_name + " (" + f.name + ")");
                    setToolTipText(f.name);
                }
            }
            else if (type == Type.TIME) {
                if (compactMode) {
                    setText(DateTime.agoSingleCompact(f.follow_time));
                }
                else {
                    setText(DateTime.agoSingleVerbose(f.follow_time));
                }
                setToolTipText(DateTime.formatFullDatetime(f.follow_time));
            }
            else if (type == Type.USER_TIME) {
                if (f.user_created_time != -1) {
                    if (compactMode) {
                        setText("("+DateTime.agoSingleCompact(f.user_created_time)+")");
                    }
                    else {
                        setText("("+DateTime.agoSingleVerbose(f.user_created_time)+")");
                    }
                    setToolTipText("Registered "+DateTime.formatFullDatetime(f.user_created_time));
                } else {
                    setText("(n/a)");
                    setToolTipText("Time when user registered not available");
                }
            }

            // Colors
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setForeground(table.getForeground());

                // Set background for both name and time
                long ago = (System.currentTimeMillis() - f.follow_time) / 1000;
                if (f.newFollower) {
                    setBackground(BG_COLOR_NEW);
                } else if (ago < 60 * 60) {
                    setBackground(BG_COLOR_RECENT);
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
                // Set foreground for registered time
                if (type == Type.USER_TIME && f.user_created_time != -1) {
                    long registeredAgo = (System.currentTimeMillis() - f.user_created_time) / 1000;
                    if (registeredAgo >= 60 * 60 * 24 * 7) {
                        setForeground(COLOR_OLDER_THAN_WEEK);
                    } else if (registeredAgo >= 60 * 60 * 24) {
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
                if ((System.currentTimeMillis() - f.follow_time) / 1000 > seconds) {
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
                    if ((System.currentTimeMillis() - f.follow_time)/1000 > time) {
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
            super(new String[]{"Name","Followed","User Created"});
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

        public MyContextMenu(boolean isDocked) {
            final String saveMenu = "Export list to file";
            addItem("saveSimple", "Names only", saveMenu);
            addItem("saveVerbose", "Names and dates", saveMenu);
            addSeparator();
            addCheckboxItem("dockToggleDocked", "Dock as tab", isDocked);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("saveSimple")) {
                saveToFile(true);
            } else if (e.getActionCommand().equals("saveVerbose")) {
                saveToFile(false);
            }
            helper.menuAction(e);
        }
        
    }
    
    private FollowerInfo createTestFollowerInfo() {
        List<Follower> test = new ArrayList<>();
        test.add(createTestFollower(stream, 30, 24*60, true));
        test.add(createTestFollower(stream, 80, 24*1087, true));
        test.add(createTestFollower(stream, 90, 1, true));
        test.add(createTestFollower(stream, 120, 10, false));
        test.add(createTestFollower(stream, 180, 24*12, false));
        test.add(createTestFollower(stream, 680, 36, false));
        test.add(createTestFollower(stream, 980, 24*5, false));
        test.add(createTestFollower(stream, 1800, 24*800, false));
        test.add(createTestFollower(stream, 1900, 24*500, false));
        test.add(createTestFollower(stream, 2900, 24*500, false));
        test.add(createTestFollower(stream, 60*60, 24*321, false));
        test.add(createTestFollower(stream, 60*60, 24*1234, false));
        test.add(createTestFollower(stream, 60*60, 391, false));
        test.add(createTestFollower(stream, 60*60*2, 60, false));
        test.add(createTestFollower(stream, 60*60*3, 24*123, false));
        test.add(createTestFollower(stream, 60*60*3, 24*5, false));
        test.add(createTestFollower(stream, 60*60*24*2, 24*1, false));
        test.add(createTestFollower(stream, 60*60*24*3, 24*234, false));
        test.add(createTestFollower(stream, 60*60*24*8, 24*12, false));
        test.add(createTestFollower(stream, 60*60*24*90, 24*800, false));
        test.add(createTestFollower(stream, 60*60*24*90, 24*12544, false));
        test.add(createTestFollower(stream, 60*60*24*180, 24*900, false));
        test.add(createTestFollower(stream, 60*60*24*180, 24*900, false));
        test.add(createTestFollower(stream, 60*60*24*180, 24*900, false));
        test.add(createTestFollower(stream, 60*60*24*180, 24*900, false));
        return new FollowerInfo(Follower.Type.FOLLOWER, stream, test, 1338);
    }
    
    private static Follower createTestFollower(String name, long timeOffset, long userOffset, boolean newFollower) {
        boolean refollow = ThreadLocalRandom.current().nextInt(20) == 0;
        return new Follower(Follower.Type.FOLLOWER, name, name, System.currentTimeMillis() - timeOffset*1000, System.currentTimeMillis() - userOffset*60*60*1000, refollow, newFollower);
    }
    
}
