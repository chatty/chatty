
package chatty.gui.components.srl;

import chatty.gui.GuiUtil;
import chatty.util.srl.Race;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/**
 * Dialog that shows the result of searching for races with a certain entrant
 * and allows found races to be opened.
 * 
 * @author tduva
 */
public class SRLRaceFinder extends JDialog {
    
    private final RacesTable selection;
    private final JLabel infoLabel = new JLabel();
    private final JButton cancelButton = new JButton("Close");
    private final SRL srl;
    
    private String currentStream;
    
    public SRLRaceFinder(Window parent, final SRL srl) {
        super(parent);
        this.srl = srl;
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        
        add(infoLabel, GuiUtil.makeGbc(0, 0, 1, 1, GridBagConstraints.CENTER));

        selection = new RacesTable(new RacesTable.RacesTableListener() {

            @Override
            public void openRace(Race race) {
                srl.openRaceInfo(race);
            }
        });
        gbc = GuiUtil.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(selection), gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(cancelButton, gbc);
        
        cancelButton.setMnemonic(KeyEvent.VK_C);
        cancelButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        Dimension defaultSize = new Dimension(400, 200);
        setPreferredSize(defaultSize);
        setSize(defaultSize);
    }
    
    /**
     * Open and reset dialog for the given stream name, waiting for data to come
     * in.
     * 
     * @param stream The name of the stream that is searched for
     */
    public void open(String stream) {
        setTitle("Races with "+stream+" - SpeedRunsLive");
        currentStream = stream;
        selection.setData(null);
        setVisible(true);
    }
    
    public void setLoading(boolean loading) {
        if (loading) {
            infoLabel.setText("Loading..");
        }
    }
    
    protected void error() {
        infoLabel.setText("Error loading races.");
    }
    
    /**
     * Sets the found races and updates the info text.
     * 
     * @param races The races found for the current stream
     */
    public void setFoundRaces(Collection<Race> races) {
        String info = "Found "+races.size()+" race"
                +(races.size() == 1 ? "" : "s")+" with "+currentStream;
        if (races.size() > 0) {
            String srlName = SRL.findSrlName(currentStream, races.iterator().next());
            if (srlName != null && !srlName.equalsIgnoreCase(currentStream)) {
                info += " ("+srlName+")";
            }
        }
        if (races.size() == 1 && srl.isOpen(races.iterator().next())) {
            info += " (already open)";
        }
        infoLabel.setText(info);
        selection.setData(races);
    }
    
    public void close() {
        setVisible(false);
    }
}
