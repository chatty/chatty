
package chatty.gui.components.srl;

import chatty.util.colors.HtmlColors;
import chatty.gui.TwitchUrl;
import chatty.gui.UrlOpener;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.RaceContextMenu;
import chatty.gui.components.settings.ListTableModel;
import chatty.util.DateTime;
import chatty.util.srl.Race;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

/**
 * Table to display a list of SRL Races. When a race is supposed to be opened
 * (from the context-menu or by double-clicking on it), it sends that request
 * back to the listener.
 * 
 * @author tduva
 */
public class RacesTable extends JTable {
    
    private static final int UPDATE_DELAY = 30*1000;
    
    private final RacesTableListener listener;
    private final MyTableModel races = new MyTableModel();
    private final ContextMenuListener contextMenuListener;
    
    public RacesTable(RacesTableListener listener) {
        this.listener = listener;
        
        contextMenuListener = new MyContextMenuListener();
        addMouseListener(new MyMouseListener());
        
        setModel(races);
        setRowSorter(new MySorter(races));
        setFillsViewportHeight(true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);

        getColumnModel().getColumn(2).setMaxWidth(100);
        getColumnModel().getColumn(3).setMaxWidth(50);
        getColumnModel().getColumn(4).setMaxWidth(40);
        
        getColumnModel().getColumn(2).setCellRenderer(new StateRenderer());
        
        Timer timer = new Timer(UPDATE_DELAY, new Updater());
        timer.setRepeats(true);
        timer.start();
    }
    
    public void setData(Collection<Race> data) {
        races.setData(data);
    }
    
    public Race getSelectedRace() {
        int selected = getSelectedRow();
        if (selected == -1) {
            return null;
        }
        return races.get(convertRowIndexToModel(selected));
    }
    
    private class Updater implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            races.updateTimes();
        }

    }
    
    private class MyMouseListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                Race selectedRace = getSelectedRace();
                if (selectedRace != null) {
                    listener.openRace(selectedRace);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            selectClicked(e);
            openContextMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            openContextMenu(e);
        }

        private void openContextMenu(MouseEvent e) {
            if (e.isPopupTrigger()) {
                Race selectedRace = getSelectedRace();
                if (selectedRace != null) {
                    ContextMenu m = new RaceContextMenu(selectedRace, contextMenuListener, false);
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        private void selectClicked(MouseEvent e) {
            int clickedRow = rowAtPoint(e.getPoint());
            if (clickedRow != -1) {
                setRowSelectionInterval(clickedRow, clickedRow);
            }
        }

    }
    
    private static class MyTableModel extends ListTableModel<Race> {
        
        public MyTableModel() {
            super(new String[]{"Game","Goal","State","Time","Entrants"});
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Race r = get(rowIndex);
            switch (columnIndex) {
                case 0: return r.game;
                case 2: return r.statetext;
                case 1: return r.goal;
                case 4: return r.getEntrants().size();
                case 3: return formatTime(r.time);
            }
            return null;
        }
        
        @Override
        public Class getColumnClass(int c) {
            if (c == 4) {
                return Integer.class;
            } else {
                return String.class;
            }
        }
        
        private String formatTime(long time) {
            if (time > 0) {
                return DateTime.agoClock(time, false);
            }
            return "-";
        }
        
        public void updateTimes() {
            for (int i=0;i<getRowCount();i++) {
                fireTableCellUpdated(i, 3);
            }
        }
        
    }
    
    private static class MySorter extends TableRowSorter<MyTableModel> {
        
        public MySorter(MyTableModel model) {
            super(model);
        }
        
        /**
         * @inherited
         * <p>
         */
        @Override
        public void toggleSortOrder(int column) {
            List<? extends RowSorter.SortKey> sortKeys = getSortKeys();
            if (sortKeys.size() > 0) {
                if (sortKeys.get(0).getSortOrder() == SortOrder.DESCENDING) {
                    setSortKeys(null);
                    return;
                }
            }
            super.toggleSortOrder(column);
        }
    }
    
    private static class StateRenderer extends DefaultTableCellRenderer {
        
        private static final Color OPEN = new Color(60,200,10);
        private static final Color ENDED = Color.RED;
        private static final Color COMPLETE = HtmlColors.getNamedColor("BlueViolet");
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            String state = (String)value;
            if (state.equals("Entry Open")) {
                setForeground(OPEN);
            } else if (state.equals("Race ended")) {
                setForeground(ENDED);
            } else if (state.equals("Complete") || state.equals("Race Over")) {
                setForeground(COMPLETE);
            } else {
                setForeground(Color.BLACK);
            }
            setText(state);
        }
    }
    
    
    private class MyContextMenuListener extends ContextMenuAdapter {

        @Override
        public void menuItemClicked(ActionEvent e) {
            Race selectedRace = getSelectedRace();
            if (selectedRace == null) {
                return;
            }
            if (e.getActionCommand().equals("raceInfo")) {
                listener.openRace(selectedRace);
            } else if (e.getActionCommand().equals("srlRacePage")) {
                String url = TwitchUrl.makeSrlRaceLink(selectedRace.id);
                UrlOpener.openUrlPrompt(RacesTable.this, url, true);
            } else if (e.getActionCommand().equals("speedruntv")) {
                String url = TwitchUrl.makeSrtRaceLink(selectedRace.id);
                UrlOpener.openUrlPrompt(RacesTable.this, url, true);
            } else if (e.getActionCommand().equals("joinSrlChannel")) {
                String url = TwitchUrl.makeSrlIrcLink(selectedRace.id);
                UrlOpener.openUrlPrompt(RacesTable.this, url, true);
            }
        }
    }
    
    public static interface RacesTableListener {
        void openRace(Race race);
    }
    
}
