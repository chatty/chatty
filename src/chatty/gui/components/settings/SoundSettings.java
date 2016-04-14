
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.Helper;
import chatty.util.MiscUtil;
import chatty.util.Sound;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author tduva
 */
public class SoundSettings extends SettingsPanel {
    
    private static final Insets SMALL_BUTTON_INSETS = new Insets(-1,15,-1,15);
    
    private final static String FILE_PATH = Chatty.getSoundDirectory();
    
    private final static int VOLUME_MIN = 0;
    private static final int VOLUME_MAX = 100;
    
    private static final FileFilter FILE_FILTER = new FileNameExtensionFilter("Wav sounds", "wav");
    
    private final SettingsDialog d;
    
    private final Map<ComboStringSetting, String> fileSettings = new LinkedHashMap<>();
    private final JLabel filesResult = new JLabel();
    
    public SoundSettings(final SettingsDialog d) {
        this.d = d;

        JPanel general = addTitledPanel("General Sound Settings", 0);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0, 0, 3, 1);
        gbc.anchor = GridBagConstraints.WEST;
        general.add(d.addSimpleBooleanSetting("sounds", "Enable sounds",
                "Use this to enable/disable all sounds."), gbc);
        
        gbc = d.makeGbc(0, 1, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5,20,2,5);
        general.add(new JLabel("Chatty looks for sound files (.wav) in this "
                + "folder:"), gbc);
        
        gbc = d.makeGbc(0, 2, 3, 1);
        gbc.insets = new Insets(3,20,3,5);
        JTextField path = new JTextField(FILE_PATH);
        path.setPreferredSize(new Dimension(0, path.getPreferredSize().height));
        path.setEditable(false);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        general.add(path, gbc);

        ActionListener buttonListener = new MyButtonListener();
        
        gbc = d.makeGbc(1, 3, 1, 1, GridBagConstraints.EAST);
        JButton rescanButton = new JButton("Rescan folder");
        rescanButton.setMargin(SMALL_BUTTON_INSETS);
        rescanButton.addActionListener(buttonListener);
        rescanButton.setActionCommand("scanFolder");
        general.add(rescanButton, gbc);
        
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 1;
        general.add(filesResult, gbc);
        
        gbc = d.makeGbc(2, 3, 1, 1, GridBagConstraints.EAST);
        JButton openFolderButton = new JButton("Open folder");
        openFolderButton.setMargin(SMALL_BUTTON_INSETS);
        openFolderButton.addActionListener(buttonListener);
        openFolderButton.setActionCommand("openFolder");
        general.add(openFolderButton, gbc);
        
        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        gbc = d.makeGbc(0, 1, 3, 1);
        gbc.insets = new Insets(0,0,0,0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        addPanel(tabs, getGbc(1));

        addSoundOptions(tabs, "highlight", "Highlight");
        addSoundOptions(tabs, "status", "Stream Status");
        addSoundOptions(tabs, "message", "Message");
        addSoundOptions(tabs, "joinPart", "Join/Part");
        addSoundOptions(tabs, "follower", "Follower");
        
        // Delay
//        gbc = d.makeGbc(0, 2, 1, 1);
//        general.add(new JLabel("Sound delay (seconds): "), gbc);
//        
//        JTextField delay = d.addSimpleLongSetting("soundDelay", 3, true);
//        gbc = d.makeGbc(1, 2, 1, 1);
//        gbc.anchor = GridBagConstraints.WEST;
//        general.add(delay, gbc);
        

        
        addPanel(general, getGbc(0));
        
        scanFiles(false);
    }
    
    private void addSoundOptions(JTabbedPane tabs, final String id, final String label) {
        tabs.add(makeSoundOptions(id, label), label);
    }
    
    /**
     * Creates a set of sound options using the given setting names, that must
     * be valid.
     * 
     * @param statusSetting The setting that contains whether this sound is
     *  on/off/..
     * @param fileSetting The name of the sound file
     * @param volumeSetting The volume adjustment of this sound
     * @return A JPanel containing the components
     */
    private JPanel makeSoundOptions(final String id, final String label) {
        final String statusSetting = id+"Sound";
        final String fileSetting = id+"SoundFile";
        final String volumeSetting = id+"SoundVolume";
        final String delaySetting = id+"SoundDelay";
        
        GridBagConstraints gbc;
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10,5,5,5));
        
        // Status (on/off/..)
        gbc = d.makeGbc(0, 0, 2, 1);
        ComboStringSetting hlSetting = new ComboStringSetting(SettingConstants.requirementOptions);
        d.addStringSetting(statusSetting, hlSetting);
        panel.add(hlSetting, gbc);
        
        // File
        gbc = d.makeGbc(2, 0, 1, 1);
        final ComboStringSetting file = d.addComboStringSetting(fileSetting, 20, 
                false, new String[]{});
        //file.addCommand("choose", "Choose file..");
        file.setPreferredSize(new Dimension(120,file.getPreferredSize().height));
        fileSettings.put(file, label);
//        file.setActionListener(new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                JFileChooser fileChooser = new JFileChooser("sounds");
//                fileChooser.setFileFilter(FILE_FILTER);
//                int result = fileChooser.showOpenDialog(d);
//                if (result == JFileChooser.APPROVE_OPTION) {
//                    File selectedFile = fileChooser.getSelectedFile();
//                    String label = selectedFile.getName();
//                    String value = selectedFile.toString();
//                    file.add(value);
//                }
//            }
//        });
        panel.add(file, gbc);
        
        // Play button
        JButton play = new JButton("Play");
        play.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String file = d.getStringSetting(fileSetting);
                long volume = d.getLongSetting(volumeSetting);
                Sound.play(file, volume, "test", -1);
            }
        });
        gbc = d.makeGbc(3, 0, 1, 1);
        panel.add(play, gbc);

        JPanel secondPanel = new JPanel(new GridBagLayout());
        // Volume
        gbc = d.makeGbc(0, 0, 1, 1);
        gbc.anchor = GridBagConstraints.WEST;
        secondPanel.add(new JLabel("Volume:"), gbc);
        
        SliderLongSetting volumeSlider = new SliderLongSetting(JSlider.HORIZONTAL, VOLUME_MIN, VOLUME_MAX, 0);
        volumeSlider.setMajorTickSpacing(10);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setPaintTicks(true);
        
        d.addLongSetting(volumeSetting, volumeSlider);
        gbc = d.makeGbc(1, 0, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        secondPanel.add(volumeSlider, gbc);
        
        
        gbc = d.makeGbc(2, 0, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        secondPanel.add(new JLabel("Delay:"), gbc);

        JTextField delay = d.addSimpleLongSetting(delaySetting, 3, true);
        gbc = d.makeGbc(3, 0, 1, 1);
        gbc.insets = new Insets(5,5,5,2);
        gbc.anchor = GridBagConstraints.EAST;
        secondPanel.add(delay, gbc);
        
        gbc = d.makeGbc(0, 1, 4, 1);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(secondPanel, gbc);
        
        return panel;
    }
    
    
    private void scanFiles(boolean showMessage) {
        File file = new File(FILE_PATH);
        File[] files = file.listFiles(new WavFilenameFilter());
        String resultText = "";
        String warningText = "";
        if (files == null) {
            resultText = "Error scanning folder.";
        } else {
            if (files.length == 0) {
                resultText = "No sound files found.";
            } else {
                resultText = files.length+" sound files found.";
            }
            String[] fileNames = new String[files.length];
            for (int i=0;i<files.length;i++) {
                fileNames[i] = files[i].getName();
            }
            Arrays.sort(fileNames);
            for (ComboStringSetting s : fileSettings.keySet()) {
                Object selected = s.getSelectedItem();
                s.removeAllItems();
                boolean currentOneStillThere = false;
                for (String item : fileNames) {
                    if (item.equals(selected)) {
                        currentOneStillThere = true;
                    }
                    s.add(item);
                }
                if (!currentOneStillThere && selected != null) {
                    warningText += "\n'"+selected+"' (used as "+fileSettings.get(s)+" sound) wasn't found.";
                } else {
                    s.setSelectedItem(selected);
                }
            }
        }
        if (showMessage) {
            JOptionPane.showMessageDialog(this, resultText+warningText);
        }
        filesResult.setText(resultText);
    }
    
    private class MyButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("openFolder")) {
                MiscUtil.openFolder(new File(FILE_PATH), d);
            } else if (e.getActionCommand().equals("scanFolder")) {
                scanFiles(true);
            }
        }
        
    }
    
    private static class WavFilenameFilter implements FilenameFilter {

        @Override
        public boolean accept(File dir, String name) {
            if (name.endsWith(".wav")) {
                return true;
            }
            return false;
        }
        
    }
}
