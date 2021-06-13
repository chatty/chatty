
package chatty.gui.components.admin;

import chatty.gui.MainGui;
import chatty.gui.components.WrapLayout;
import static chatty.gui.components.admin.AdminDialog.hideableLabel;
import static chatty.gui.components.admin.AdminDialog.makeGbc;
import chatty.gui.components.settings.DurationSetting;
import chatty.util.DateTime;
import chatty.util.api.TwitchApi;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 *
 * @author tduva
 */
public class CommercialPanel extends JPanel {
    
    /**
     * After a commercial was attempted to run/the result was returned wait for
     * this long before displaying new data of a scheduled commercial.
     */
    private static final int COMMERCIAL_RUN_ATTEMPT_DELAY = 5*1000;
    
    // Saves last commercial run time for several channels (although it's
    // probably unncessary, at least this way another currentChannel can be opened in
    // the dialog and it's still saved)
    private final Map<String, Long> lastCommercial = new HashMap<>();
    
    private final static int[] commercialButtonsDef = {30,60,90,120,180};
    private final Map<Integer, JToggleButton> commercialButtons = new LinkedHashMap<>();
    private final JLabel commercialResult;
    private final JLabel lastCommercialInfo = new JLabel("");
    private final JCheckBox useCommercialDelay = new JCheckBox("Use delay: ");
    private final JCheckBox repeatCommercial = new JCheckBox("Repeat");
    private final DurationSetting commercialDelay = new DurationSetting(3, true);
    
    private final MainGui main;
    
    private String currentChannel;
    
    private long lastCommercialRun;
    private long scheduledCommercialTime;
    private int scheduledCommercialLength;
    private long lastCommercialRunAttempt;
    
    public CommercialPanel(MainGui main) {
        
        GridBagConstraints gbc;
        
        this.main = main;
        
        setLayout(new GridBagLayout());
        
        gbc = makeGbc(1,1,4,1);
        gbc.insets = new Insets(5,5,0,5);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(createCommercialButtons(), gbc);
        
        gbc = makeGbc(1,2,1,1);
        gbc.insets = new Insets(0,5,5,5);
        add(useCommercialDelay, gbc);
        gbc = makeGbc(2,2,1,1);
        gbc.insets = new Insets(0,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        // Prevent 0 size when dialog size reduced
        commercialDelay.setMinimumSize(commercialDelay.getPreferredSize());
        add(commercialDelay, gbc);
        
        gbc = makeGbc(3,2,1,1);
        gbc.insets = new Insets(0,5,5,5);
        add(repeatCommercial, gbc);
        
        gbc = makeGbc(4,2,1,1);
        gbc.insets = new Insets(0,10,5,5);
        add(lastCommercialInfo, gbc);

        commercialResult = new JLabel("...");
        gbc = makeGbc(1,3,4,1);
        gbc.insets = new Insets(3,5,15,5);
        add(commercialResult, gbc);
        
        setCommercialResult("");
        
    }
    
    public boolean checkOnClose() {
        if (scheduledCommercialTime != 0) {
            int result = confirmContinueScheduledOnClose();
            if (result == JOptionPane.CLOSED_OPTION) {
                return false;
            }
            if (result == 1) {
                clearScheduledCommercial();
            }
        }
        return true;
    }
    
    public void update() {
        if (isVisible()) {
            if (lastCommercialRun > 0) {
                long ago = System.currentTimeMillis() - lastCommercialRun;
                lastCommercialInfo.setText("Last run: "+DateTime.duration(ago, 1, 0)+" ago");
                lastCommercialInfo.setToolTipText("Last run: "+DateTime.formatFullDatetime(lastCommercialRun));
            }
        }
    }
    
    public void checkScheduled() {
        if (scheduledCommercialTime > 0) {
            long timeLeft = scheduledCommercialTime - System.currentTimeMillis();
            if (timeLeft <= 0) {
                runCommercialNow(scheduledCommercialLength);
            } else if (System.currentTimeMillis() - lastCommercialRunAttempt
                    > COMMERCIAL_RUN_ATTEMPT_DELAY) {
                setCommercialResult(
                        String.format("Commercial (%ds) scheduled to run in %s",
                                scheduledCommercialLength,
                                DateTime.duration(timeLeft, 2, 0))
                );
            }
        }
    }
    
    /**
     * Save commercial settings.
     */
    protected void saveSettings() {
        boolean enabled = useCommercialDelay.isSelected();
        long length = commercialDelay.getSettingValue();
        main.saveCommercialDelaySettings(enabled, length);
    }

    /**
     * Allows the commercial settings to be set.
     * 
     * @param enabled
     * @param length 
     */
    public void updateCommercialDelaySettings(boolean enabled, long length) {
        useCommercialDelay.setSelected(enabled);
        commercialDelay.setSettingValue(length);
    }
    
    public void commercialHotkey(int length) {
        if (commercialButtons.containsKey(length)) {
            commercialButtons.get(length).doClick();
        } else {
            commercialButtons.get(30).doClick();
        }
    }
    
    public void changeChannel(String channel) {
        this.currentChannel = channel;
        
        // Load/reset last commercial run time
        Long lastCommercialTemp = lastCommercial.get(channel);
        if (lastCommercialTemp != null) {
            lastCommercialRun = lastCommercialTemp;
        } else {
            lastCommercialRun = 0;
            lastCommercialInfo.setText(null);
        }
        
        // Clear scheduled commercial
        clearScheduledCommercial();
        
        // Reset result
        setCommercialResult("");
    }
    
    /**
     * Creates buttons for the defined commercial lengths, adds them to a JPanel,
     * a ButtonGroup and a Map for further reference.
     * 
     * @return The JPanel to be added to the GUI.
     */
    private JPanel createCommercialButtons() {
        ButtonGroup g = new NoneSelectedButtonGroup();
        JPanel panel = new JPanel();
        panel.setLayout(new WrapLayout(WrapLayout.LEFT));
        ActionListener listener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                commercialButtonClicked(e.getActionCommand());
            }
        };
        for (int i : commercialButtonsDef) {
            JToggleButton b = new JToggleButton(i+"s");
            b.setActionCommand(String.valueOf(i));
            g.add(b);
            commercialButtons.put(i, b);
            panel.add(b);
            b.addActionListener(listener);
        }
        return panel;
    }
    
    /**
     * One of the commercial buttons was clicked. Find out which one by the
     * action command and check if it was selected/unselected.
     * 
     * @param command 
     */
    private void commercialButtonClicked(String command) {
        int length = Integer.parseInt(command);
        JToggleButton button = commercialButtons.get(length);
        if (button.isSelected()) {
            // If commercial is already scheduled, just change the length.
            if (scheduledCommercialTime == 0) {
                runCommercial(length);
            } else {
                scheduledCommercialLength = length;
                update();
            }
        } else {
            scheduledCommercialTime = 0;
            setCommercialResult("Canceled scheduled commercial.");
        }
    }
    
    /**
     * Unselect all run commercial buttons.
     */
    private void clearCommercialButtonsSelection() {
        for (JToggleButton b : commercialButtons.values()) {
            b.setSelected(false);
        }
    }
    
    /**
     * A run commercial button was pressed. Either play commercial immediately
     * or schedule it for the given time if that is enabled.
     * 
     * @param length 
     */
    private void runCommercial(int length) {
        saveSettings();
        if (useCommercialDelay.isSelected()) {
            scheduleCommercial(length);
        } else {
            runCommercialNow(length);
        }
    }
    
    /**
     * Schedules a commercial of the given length to be run after the delay
     * currently set in the delay input field.
     * 
     * @param length 
     */
    private void scheduleCommercial(int length) {
        Long delay = commercialDelay.getSettingValue();
        if (delay == null) {
            setCommercialResult("Invalid delay specified.");
            clearCommercialButtonsSelection();
        } else {
            scheduledCommercialTime = System.currentTimeMillis() + delay * 1000;
            scheduledCommercialLength = length;
            update();
        }
    }
    
    /**
     * Immediately run commercial of the given length.
     * 
     * @param length 
     */
    private void runCommercialNow(int length) {
        lastCommercialRunAttempt = System.currentTimeMillis();
        if (repeatCommercial.isSelected()) {
            scheduleCommercial(length);
        } else {
            clearScheduledCommercial();
        }
        main.runCommercial(currentChannel, length);
        setLoadingCommercial(true);
    }
    
    /**
     * Unschedules a scheduled commercial.
     */
    private void clearScheduledCommercial() {
        clearCommercialButtonsSelection();
        scheduledCommercialTime = 0;
        setCommercialResult("");
    }
    
    /**
     * Changes the status to waiting for commercial to run.
     * 
     * @param loading 
     */
    private void setLoadingCommercial(boolean loading) {
        for (JToggleButton b : commercialButtons.values()) {
            b.setEnabled(!loading);
        }
        if (loading) {
            setCommercialResult("Trying to run commercial..");
        }
    }
    
    /**
     * Sets the text of the commercialResult label.
     * 
     * @param result 
     */
    private void setCommercialResult(String result) {
        hideableLabel(commercialResult, result);
    }
    
    /**
     * Dialog to confirm whether the currentChannel should be switched when a
     * commercial is currently scheduled on currentChannel.
     * 
     * @param channel
     * @return 
     */
    public boolean confirmChannelChange(String channel) {
        if (scheduledCommercialTime == 0) {
            return true;
        }
        String message = "<html><body style='width:240'>"
                + "There is currently a commercial scheduled on '"+currentChannel
                +"'. Changing channel to '"+channel+"' will cancel that.";
        String[] options = new String[]{"Change channel", "Don't change"};
        int result = JOptionPane.showOptionDialog(main, message, 
                "Changing channel will cancel commercial", JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE, null,
                options, options[1]);
        return result == 0;
    }
    
    /**
     * The commercial result as returned from the API. Updates the result text
     * and when the commercial was last run, also sets it to "not loading" 
     * state.
     * 
     * @param stream
     * @param resultText
     * @param result 
     */
    public void commercialResult(String stream, String resultText, TwitchApi.RequestResultCode result) {
        setCommercialResult(DateTime.currentTime()+" "+resultText);
        lastCommercialRunAttempt = System.currentTimeMillis();
        setLoadingCommercial(false);
        if (result == TwitchApi.RequestResultCode.RUNNING_COMMERCIAL) {
            lastCommercial.put(stream, System.currentTimeMillis());
            if (stream != null && stream.equals(currentChannel)) {
                lastCommercialRun = System.currentTimeMillis();
            }
        }
    }
    
    /**
     * Confirm dialog when a commercial is still scheduled when the dialog
     * is closed.
     * 
     * @return 
     */
    private int confirmContinueScheduledOnClose() {
        String message = "<html><body style='width:240'>"
                + "There is currently a commercial scheduled to be run. It can also"
                + " be run if you close this. What do you want to do?";
        String[] options = new String[]{"Run on schedule", "Cancel commercial"};
        int result = JOptionPane.showOptionDialog(main, message, 
                "Closing Admin Dialog while commercial is scheduled", JOptionPane.YES_NO_OPTION, 
                JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);
        return result;
    }
    
    /**
     * Allows to deselect a button on a ButtonGroup, instead of always having
     * one selected.
     * 
     * Source: http://blog.frankel.ch/unselect-all-toggle-buttons-of-a-group
     */
    public static class NoneSelectedButtonGroup extends ButtonGroup {

        @Override
        public void setSelected(ButtonModel model, boolean selected) {
            if (selected) {
                super.setSelected(model, selected);
            } else {
                clearSelection();
            }
        }
    }
    
}
