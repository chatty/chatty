
package chatty.gui.components.srl;

import chatty.gui.components.menus.ContextMenuListener;
import chatty.util.DateTime;
import chatty.util.srl.Race;
import chatty.util.srl.Race.Entrant;
import chatty.util.srl.SpeedrunsLive;
import chatty.util.srl.SpeedrunsLiveListener;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Manages the GUI part of the SRL races stuff. Requests updates of the race
 * data, can open the appropriate dialogs etc.
 * 
 * @author tduva
 */
public class SRL {
    
    /**
     * The actual update timer, which checks IF a data update should be done and
     * updates parts of the GUI.
     */
    private final static int UPDATE_DELAY = 4000;
    
    /**
     * Update races data automatically at most at this interval.
     */
    private final static int AUTO_UPDATE_DELAY = 2 * 60 * 1000;

    /**
     * After how much time the current race data should be considered old, which
     * means it is updated when an active action is performed (like opening the
     * dialog, searching for a player). This does not mean it updates fully
     * automatic after this time.
     */
    private final static int DATA_STALE_DELAY = 30*1000;
    
    private final Window parent;
    private final SRLRaces racesDialog;
    private final SRLRace raceInfo;
    private final SRLRaceFinder raceFinder;
    private final SpeedrunsLive srl;
    
    private Collection<Race> currentRaces;
    private long lastReceived;
    private boolean loading;
    private long lastError;
    private String errorMessage;
    
    private String entrantToSearch;
    
    public SRL(Window parent, SpeedrunsLive srl, ContextMenuListener contextMenuListener) {
        this.srl = srl;
        this.parent = parent;
        this.racesDialog = new SRLRaces(parent, this);
        this.raceInfo = new SRLRace(parent, contextMenuListener);
        this.raceFinder = new SRLRaceFinder(parent, this);
        
        raceFinder.addComponentListener(new ComponentAdapter() {
            
            @Override
            public void componentHidden(ComponentEvent e) {
                cancelSearch();
            }
        });
        
        Timer timer = new Timer(UPDATE_DELAY, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });
        timer.setRepeats(true);
        timer.start();
        
        srl.addListener(new MySpeedrunsLiveListener());
    }

    /**
     * Shows the list of current races, requesting a data update if the data is
     * old enough.
     */
    public void openRaceList() {
        if (isDataStale()) {
            reload();
        }
        racesDialog.setLocationRelativeTo(parent);
        racesDialog.showDialog();
    }
    
    /**
     * Search for a race with the given entrant. Opens the race finder as
     * appropriate feedback and either request data if it is old or immediately
     * start searching in the cached data.
     * 
     * @param stream 
     */
    public void searchRaceWithEntrant(String stream) {
        entrantToSearch = stream;
        raceFinder.setLocationRelativeTo(parent);
        raceFinder.open(stream);
        if (isDataStale()) {
            reload();
        } else {
            searchRaceWithEntrant(currentRaces);
        }
    }
    
    /**
     * Actually search in the current race data for races that contain the
     * saved entrant. Loads the found races into the race finder or opens the
     * race info directly if only one was found and no race info is open.
     * 
     * @param races 
     */
    private void searchRaceWithEntrant(Collection<Race> races) {
        if (entrantToSearch != null) {
            Collection<Race> foundRaces = findRaces(entrantToSearch, races);
            if (foundRaces.size() == 1 && !raceInfo.isVisible()) {
                raceFinder.close();
                openRaceInfo(foundRaces.iterator().next());
            } else {
                raceFinder.setFoundRaces(foundRaces);
            }
            entrantToSearch = null;
        }
    }
    
    /**
     * Don't continue the search if requested data is received. This happens
     * when the race finder is closed.
     */
    private void cancelSearch() {
        entrantToSearch = null;
    }
    
    /**
     * Opens the info dialog for the given race.
     * 
     * @param race 
     */
    protected void openRaceInfo(Race race) {
        if (!raceInfo.isVisible()) {
            if (racesDialog.isVisible()) {
                raceInfo.setLocationRelativeTo(racesDialog);
            } else {
                raceInfo.setLocationRelativeTo(parent);
            }
        }
        raceInfo.open(race);
    }
    
    protected boolean isAutoUpdating() {
        return raceInfo.isVisible();
    }
    
    protected boolean isOpen(Race race) {
        return raceInfo.isVisible() && race.equals(raceInfo.getRace());
    }
    
    private void update() {
        if (loading) {
            racesDialog.setStatusText("Loading..");
        } else {
            String infoText = "";

            long ago = System.currentTimeMillis() - lastReceived;
            long errorAgo = System.currentTimeMillis() - lastError;
            if (lastReceived > 0) {
                infoText += "Last updated: "
                        + DateTime.duration(ago, 1, 0) + " ago";
            }
            if (isAutoUpdating()) {
                infoText += " (auto updating)";
                if (ago > AUTO_UPDATE_DELAY
                        && (lastError == -1 || errorAgo > AUTO_UPDATE_DELAY)) {
                    reload();
                    return;
                }
            }
            if (lastError != -1) {
                infoText += String.format(" [%s ago: %s]",
                        DateTime.duration(errorAgo, 2, 0),
                        errorMessage);
            }
            racesDialog.setStatusText(infoText);
        }
    }
    
    private boolean isDataStale() {
        return System.currentTimeMillis() - lastReceived > DATA_STALE_DELAY;
    }
    
    protected void reload() {
        setLoading(true);
        srl.requestRaces();
    }
    
    private void setLoading(boolean loading) {
        racesDialog.setLoading(loading);
        raceFinder.setLoading(loading);
        this.loading = loading;
        update();
    }
    
    public void setRaces(List<Race> newRaces) {
        currentRaces = newRaces;
        lastError = -1;
        racesDialog.setRaces(newRaces);
        raceInfo.update(newRaces);
        lastReceived = System.currentTimeMillis();
        setLoading(false);
        searchRaceWithEntrant(newRaces);
    }

    protected void requestError(String description) {
        lastError = System.currentTimeMillis();
        errorMessage = description;
        raceInfo.error();
        raceFinder.error();
        setLoading(false);
    }
    
    /**
     * Receiving data from SRL.
     */
    private class MySpeedrunsLiveListener implements SpeedrunsLiveListener {

        @Override
        public void racesReceived(final List<Race> newRaces) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    setRaces(newRaces);
                }
            });
        }

        @Override
        public void error(final String description) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    requestError(description);
                }
            });
        }
    }
    
    /**
     * Help function to find races that contain the entrant with the given
     * stream.
     * 
     * @param stream
     * @param races
     * @return 
     */
    public static final Collection<Race> findRaces(String stream, Collection<Race> races) {
        Collection<Race> result = new ArrayList<>();
        for (Race r : races) {
            for (Entrant e : r.getEntrants()) {
                if (stream.equalsIgnoreCase(e.twitch)) {
                    result.add(r);
                }
            }
        }
        return result;
    }
    
    /**
     * Return the SRL name for entrant with the given stream, based on the
     * given race.
     * 
     * @param stream
     * @param race
     * @return The SRL name for the given stream or {@code null} if not in this
     * race
     */
    public static final String findSrlName(String stream, Race race) {
        for (Entrant entrant : race.getEntrants()) {
            if (stream.equalsIgnoreCase(entrant.twitch)) {
                return entrant.name;
            }
        }
        return null;
    }
    
}
