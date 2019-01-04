
package chatty.gui.components.srl;

import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.gui.LinkListener;
import chatty.gui.TwitchUrl;
import chatty.gui.UrlOpener;
import chatty.gui.components.ExtendedTextPane;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.RaceContextMenu;
import chatty.gui.components.menus.RaceEntrantContextMenu;
import chatty.gui.components.settings.ListTableModel;
import chatty.util.DateTime;
import chatty.util.srl.Race;
import chatty.util.srl.Race.Entrant;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author tduva
 */
public class SRLRace extends JDialog {
    
    private static final int UPDATE_DELAY = 500;
    
    private final LinkLabel id;
    private final JLabel state = new JLabel();
    private final JTextField game = new JTextField();
    private final ExtendedTextPane goal = new ExtendedTextPane();
    private final JTable entrantsTable = new JTable();
    private final JLabel time = new JLabel();
    
    private final ContextMenuListener streamsContextMenuListener;
    private final ContextMenuListener raceContextMenuListener;
    private long timeStarted;
    private Race currentRace;
    
    private final MyTableModel entrants = new MyTableModel();
    
    public SRLRace(Window parent, ContextMenuListener streamsContextMenuListener) {
        super(parent);
        this.streamsContextMenuListener = streamsContextMenuListener;
        this.raceContextMenuListener = new RaceContextMenuListener();
        Timer timer = new Timer(UPDATE_DELAY, new UpdateTimer());
        timer.setRepeats(true);
        timer.start();
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        gbc = GuiUtil.makeGbc(1, 0, 1, 1, GridBagConstraints.EAST);
        id = new LinkLabel("", new MyLinkLabelListener());
        id.addMouseListener(new ContextMenuOpener());
        gbc.weightx = 0.5;
        gbc.insets = new Insets(5, 5, 0, 5);
        add(id, gbc);
        
        gbc = GuiUtil.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST);
        add(state, gbc);
        
        gbc = GuiUtil.makeGbc(1, 4, 1, 1, GridBagConstraints.EAST);
        add(time, gbc);
        
        gbc = GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 0, 5);
        add(new JLabel("Game:"), gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        game.setEditable(false);
        add(game, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 0, 5);
        add(new JLabel("Goal:"), gbc);
        
        gbc = GuiUtil.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        goal.setEditable(false);
        goal.setBackground(game.getBackground());
        goal.setBorder(game.getBorder());
        goal.setLinkListener(new MyLinkListener());
        goal.setMargin(game.getMargin());
        add(goal, gbc);
        
        entrantsTable.setModel(entrants);
        entrantsTable.setFillsViewportHeight(true);
        entrantsTable.addMouseListener(new MouseClicks());
        TableRowSorter<MyTableModel> sorter = new TableRowSorter<>(entrants);
        entrantsTable.setRowSorter(sorter);
        TableColumnModel columns = entrantsTable.getColumnModel();
        columns.getColumn(0).setMaxWidth(40);
        columns.getColumn(2).setMaxWidth(80);
        columns.getColumn(3).setMinWidth(70);
        columns.getColumn(3).setMaxWidth(120);
        columns.getColumn(4).setMinWidth(40);
        columns.getColumn(4).setMaxWidth(40);
        sorter.setComparator(0, new PlaceSorter());
        columns.getColumn(0).setCellRenderer(new PlaceRenderer());
        columns.getColumn(3).setCellRenderer(new StateRenderer());
        //sorter.toggleSortOrder(0);
        
        gbc = GuiUtil.makeGbc(0, 5, 2, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(entrantsTable), gbc);
        
        setPreferredSize(new Dimension(350, 410));
        setSize(new Dimension(340, 410));
        setMinimumSize(new Dimension(200,300));
    }
    
    public void open(Race race) {
        setRace(race);
        setVisible(true);
    }
    
    public void setRace(Race race) {
        currentRace = race;
        state.setText(race.statetext+" ("+race.getEntrants().size()+" entrants)");
        game.setText(race.game);
        goal.setText(race.goal);
        entrants.setData(race.getEntrants());
        id.setText("[srl:"+race.id+" #srl-"+race.id+"]");
        timeStarted = race.time;
        update();
        setTitle("Race: "+race.game+" ["+race.statetext+"]");
    }
    
    /**
     * Give this list of races, whereas the dialog picks out the one it
     * currently has open (if present).
     * 
     * @param races 
     */
    public void update(List<Race> races) {
        if (currentRace == null) {
            return;
        }
        if (races.contains(currentRace)) {
            setRace(races.get(races.indexOf(currentRace)));
        }
    }
    
    public void error() {
        if (currentRace != null) {
            setTitle("(error) "+currentRace.game);
        }
    }
    
    public Race getRace() {
        return currentRace;
    }
    
    private void update() {
        if (timeStarted > 0 && currentRace.state == Race.IN_PROGRESS) {
            time.setText(DateTime.agoClock(timeStarted, true));
        } else {
            time.setText("");
        }
    }

    private class ContextMenuOpener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            openContextMenu(e);
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            openContextMenu(e);
        }
        
        private void openContextMenu(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (currentRace != null) {
                    ContextMenu m = new RaceContextMenu(currentRace,
                            raceContextMenuListener, true);
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }
    
    private class RaceContextMenuListener extends ContextMenuAdapter {

        @Override
        public void menuItemClicked(ActionEvent e) {
            Race selectedRace = currentRace;
            if (selectedRace == null) {
                return;
            }
            if (e.getActionCommand().equals("srlRacePage")) {
                String url = TwitchUrl.makeSrlRaceLink(selectedRace.id);
                UrlOpener.openUrlPrompt(SRLRace.this, url, true);
            } else if (e.getActionCommand().equals("speedruntv")) {
                String url = TwitchUrl.makeSrtRaceLink(selectedRace.id);
                UrlOpener.openUrlPrompt(SRLRace.this, url, true);
            } else if (e.getActionCommand().equals("joinSrlChannel")) {
                String url = TwitchUrl.makeSrlIrcLink(selectedRace.id);
                UrlOpener.openUrlPrompt(SRLRace.this, url, true);
            }
        }
    }
    
    /**
     * Updates the dialog on a regular interval (mainly the time).
     */
    private class UpdateTimer implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
        }
        
    }
    
    /**
     * Sorts the place, which returns an Entrant object, so just sort by that,
     * which should sort by place (unless SRL changes something and other sort
     * fields have to be included).
     */
    private static class PlaceSorter implements Comparator<Entrant> {

        @Override
        public int compare(Entrant o1, Entrant o2) {
            return o1.compareTo(o2);
        }
        
    }
    
    private static class MyTableModel extends ListTableModel<Race.Entrant> {

        public MyTableModel() {
            super(new String[]{"Place", "Name", "Time", "State", "Points"});
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Entrant e = get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return e;
                case 1:
                    return e.name;
                case 2:
                    return formatTime(e.time);
                case 3:
                    return e;
                case 4:
                    return e.points;
            }
            return null;
        }

        @Override
        public Class getColumnClass(int c) {
            if (c == 4) {
                return Integer.class;
            } else if (c == 0 || c == 3) {
                return Entrant.class;
            } else {
                return String.class;
            }
        }
        
        private String getPlace(Entrant e) {
            if (e.time > 0) {
                return String.valueOf(e.place);
            }
            return "-";
        }
        
        private String formatTime(long time) {
            if (time > 0) {
                return DateTime.durationClock(time, true);
            }
            return "-";
        }

    }
    
    private List<Entrant> getSelectedEntrants() {
        int[] selected = entrantsTable.getSelectedRows();
        List<Entrant> selectedEntrants = new ArrayList<>();
        for (int i : selected) {
            selectedEntrants.add(entrants.get(
                    entrantsTable.convertRowIndexToModel(i)));
        }
        return selectedEntrants;
    }
    
    private class MyLinkListener implements LinkListener {

        @Override
        public void linkClicked(String url) {
            UrlOpener.openUrlPrompt(SRLRace.this, url);
        }

    }
    
    private class MyLinkLabelListener implements LinkLabelListener {

        @Override
        public void linkClicked(String type, String ref) {
            UrlOpener.openUrlPrompt(SRLRace.this, TwitchUrl.makeSrlRaceLink(ref));
        }
        
    }
    
    private class MouseClicks extends MouseAdapter {

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
                List<Entrant> selectedEntrants = getSelectedEntrants();
                if (!selectedEntrants.isEmpty()) {
                    ContextMenu m = new RaceEntrantContextMenu(selectedEntrants, streamsContextMenuListener);
                    m.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
        
        private void selectClicked(MouseEvent e) {
            int clickedRow = entrantsTable.rowAtPoint(e.getPoint());
            if (clickedRow != -1 && !entrantsTable.isRowSelected(clickedRow)) {
                entrantsTable.setRowSelectionInterval(clickedRow, clickedRow);
            }
        }
        
    }
    
    /**
     * Turns the place into an appropriate String. This uses an Entrant, which
     * can also be sorted properly.
     */
    private static class PlaceRenderer extends DefaultTableCellRenderer {
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Entrant e = (Entrant)value;
            if (e.time > 0) {
                setText(String.valueOf(e.place));
            } else {
                setText("-");
            }
            setToolTipText(e.message);
        }
    }
    
    /**
     * Draws the state text in different colors.
     */
    private static class StateRenderer extends DefaultTableCellRenderer {

        private static final Color DEFAULT = Color.BLACK;
        private static final Color FORFEIT = Color.RED;
        private static final Color ENTERED = new Color(120,120,120);
        private static final Color FINISHED = HtmlColors.getNamedColor("BlueViolet");
        
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Entrant e = (Entrant)value;
            String state = e.statetext;
            if (state.equals("Forfeit") || state.equals("Disqualified")) {
                setForeground(FORFEIT);
            } else if (state.equals("Entered")) {
                setForeground(ENTERED);
            } else if (state.equals("Finished")) {
                setForeground(FINISHED);
            } else {
                setForeground(DEFAULT);
            }
            setText(state+(e.message.isEmpty() ? "" : " [..]"));
            setToolTipText(e.message);
        }
    }
}
