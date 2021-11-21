
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.gui.GuiUtil;
import static chatty.gui.GuiUtil.SMALL_BUTTON_INSETS;
import chatty.gui.components.LinkLabel;
import chatty.gui.notifications.Notification;
import chatty.lang.Language;
import chatty.util.Debugging;
import chatty.util.Sound;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class NotificationSettings extends SettingsPanel {
    
    public final static long NOTIFICATION_TYPE_OFF = -1;
    public final static long NOTIFICATION_TYPE_CUSTOM = 0;
    public final static long NOTIFICATION_TYPE_TRAY = 1;
    public final static long NOTIFICATION_TYPE_COMMAND = 2;

    private final ComboLongSetting nType;
    private final ComboLongSetting nScreen;
    private final ComboLongSetting nPosition;
    private final DurationSetting nDisplayTime;
    private final DurationSetting nMaxDisplayTime;
    private final JCheckBox userActivity;
    private final EditorStringSetting nCommand;
    
    private final PathSetting soundsPath;
    
    private final JLabel filesResult = new JLabel();
    
    private final NotificationEditor editor;
    
    public NotificationSettings(SettingsDialog d, Settings settings) {
        // Expand
        super(true);
        
        GridBagConstraints gbc;
        
        //==========================
        // Events
        //==========================
        editor = new NotificationEditor(d, settings);
        editor.setLinkLabelListener(d.getSettingsHelpLinkLabelListener());
        
        //=======================
        // Notification Settings
        //=======================
        JPanel notificationSettings = new JPanel(new GridBagLayout());

        gbc = d.makeGbc(0, 0, 4, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(10,5,4,5);
        
        Map<Long, String> nTypeOptions = new LinkedHashMap<>();
        nTypeOptions.put(NOTIFICATION_TYPE_OFF, "Off");
        nTypeOptions.put(NOTIFICATION_TYPE_CUSTOM, "Chatty Notifications");
        nTypeOptions.put(NOTIFICATION_TYPE_TRAY, "Tray Notifications (OS dependant)");
        nTypeOptions.put(NOTIFICATION_TYPE_COMMAND, "Run OS Command");
        nType = new ComboLongSetting(nTypeOptions);

        nType.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                updateSettingsState();
            }
        });
        d.addLongSetting("nType", nType);
        notificationSettings.add(nType, gbc);
        
        notificationSettings.add(new JLabel("Position:"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        
        Map<Long, String> nPositionOptions = new LinkedHashMap<>();
        nPositionOptions.put(Long.valueOf(0), "Top-Left");
        nPositionOptions.put(Long.valueOf(1), "Top-Right");
        nPositionOptions.put(Long.valueOf(2), "Bottom-Left");
        nPositionOptions.put(Long.valueOf(3), "Bottom-Right");
        nPosition = new ComboLongSetting(nPositionOptions);
        d.addLongSetting("nPosition", nPosition);
        gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST);
        notificationSettings.add(nPosition, gbc);
        
        notificationSettings.add(new JLabel("Screen:"),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.EAST));
        
        Map<Long, String> nScreenOptions = new LinkedHashMap<>();
        nScreenOptions.put(Long.valueOf(-1), "Auto");
        nScreenOptions.put(Long.valueOf(0), "1");
        nScreenOptions.put(Long.valueOf(1), "2");
        nScreenOptions.put(Long.valueOf(2), "3");
        nScreenOptions.put(Long.valueOf(3), "4");
        nScreen = new ComboLongSetting(nScreenOptions);
        d.addLongSetting("nScreen", nScreen);
        notificationSettings.add(nScreen,
                d.makeGbc(1, 2, 1, 1, GridBagConstraints.WEST));

        
        notificationSettings.add(new JLabel("Display Time:"),
                d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST));
        
        nDisplayTime = new DurationSetting(3, true);
        d.addLongSetting("nDisplayTime", nDisplayTime);
        notificationSettings.add(nDisplayTime,
                d.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST));
        
        
        userActivity = d.addSimpleBooleanSetting("nActivity", "No User Activity:",
                "Display longer unless the mouse was recently moved");
        notificationSettings.add(userActivity,
                d.makeGbc(2, 2, 1, 1, GridBagConstraints.EAST));
        //main.add(new JLabel("Max Display Time:"), d.makeGbc(2, 2, 1, 1, GridBagConstraints.EAST));
        
        nMaxDisplayTime = new DurationSetting(3, true);
        d.addLongSetting("nMaxDisplayTime", nMaxDisplayTime);
        notificationSettings.add(nMaxDisplayTime,
                d.makeGbc(3, 2, 1, 1, GridBagConstraints.WEST));

        notificationSettings.add(new JLabel("Command:"), d.makeGbc(0, 4, 1, 1, GridBagConstraints.EAST));

        nCommand = d.addEditorStringSetting("nCommand", 20, true, "Edit system command (recommended for advanced users only, read help)", false, ""
                + "<html><body style='width: 400px;'>"
                + "<p>Enter a command/program with parameters, which will be "
                + "executed as a new process on your system (so please be "
                + "careful with this, especially considering the <code>$(message)</code> "
                + "comes directly from Twitch Chat, so the program you call "
                + "must be able to handle that safely).</p>"
                + "<p>You can use the following replacements: "
                + "<code>$(title), $(message), $(chan)</code></p>"
                + "<p><em>Tip:</em> Add quotes around replacements, as they "
                + "may contain spaces. Use <code>\\\"</code> to escape quotes, "
                + "to include them as their actual character. "
                + "Quotes in the replacements are escaped automatically.</p>"
                + "<p>For example to run 'notify-send' to show a native notification on Linux: "
                + "<code>notify-send&nbsp;\"$(title)\"&nbsp;\"$(message)\"</code></p>"
                + "<p>To view the output of executed commands (for example to "
                + "debug if it doesn't work as expected) you can open &lt;Extra"
                + " - Debug window&gt;.</p>"
                + "<p>Use the \"Test\" button to execute the current command "
                + "with some example data.</p>", new Editor.Tester() {

            @Override
            public String test(Window parent, Component component, int x, int y, String value) {
                String result = GuiUtil.showCommandNotification(value, "Example Title",
                        "Example \"message\" for this test notification",
                        "#example_channel");
                JOptionPane.showMessageDialog(component, result);
                return null;
            }
        });
        notificationSettings.add(nCommand, d.makeGbc(1, 4, 3, 1, GridBagConstraints.WEST));

        //================
        // Sound Settings
        //================
        JPanel soundSettings = new JPanel(new GridBagLayout());
        
        gbc = d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST);
        JCheckBox soundsEnabled = d.addSimpleBooleanSetting("sounds", "Enable sounds",
                "Use this to enable/disable all sounds.");
        soundSettings.add(soundsEnabled, gbc);
        
        gbc = d.makeGbc(0, 1, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(8,10,2,6);
        soundSettings.add(new JLabel("Chatty looks for sound files (.wav) in this "
                + "folder:"), gbc);
        
        gbc = d.makeGbc(0, 2, 3, 1);
        gbc.insets = new Insets(3,10,3,8);
        
        PathSetting path = new PathSetting(d, Chatty.getSoundDirectory());
        d.addStringSetting("soundsPath", path);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        soundSettings.add(path, gbc);
        soundsPath = path;
        path.setPathChangeListener(p -> {
            scanFiles(false);
        });
        
        gbc = d.makeGbc(1, 3, 1, 1, GridBagConstraints.EAST);
        JButton rescanButton = new JButton("Rescan folder");
        rescanButton.setMargin(SMALL_BUTTON_INSETS);
        rescanButton.addActionListener(e -> {
            scanFiles(true);
        });
        rescanButton.setActionCommand("scanFolder");
        soundSettings.add(rescanButton, gbc);
        
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 1;
        soundSettings.add(filesResult, gbc);
        
        //---------------
        // Output Device
        //---------------
        JPanel devicePanel = new JPanel();
        devicePanel.add(new JLabel("Output Device: "));
        
        Map<String, String> devicePresets = new HashMap<>();
        devicePresets.put("", "<default>");
        for (String dev : Sound.getDeviceNames()) {
            devicePresets.put(dev, dev);
        }
        final ComboStringSetting device = new ComboStringSetting(devicePresets);
        device.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Sound.setDeviceName(device.getSettingValue());
            }
        });
        d.addStringSetting("soundDevice", device);
        devicePanel.add(device);
        gbc = d.makeGbc(0, 4, 2, 1);
        soundSettings.add(devicePanel, gbc);
        
        //--------------------------
        // Info
        //--------------------------
        soundSettings.add(new JLabel("<html><body width='300px'>Wav files that probably work are uncompressed PCM, 8-48kHz, 8/16bit "
                + "(e.g. exported in Audacity as WAV Signed 16-bit PCM). If the file plays silent (but the default ones work), try making "
                + "the sound longer (for example add some silence)."),
                d.makeGbc(0, 6, 2, 1));
        
        //======
        // Tabs
        //======
        JPanel notificationsPanel = addTitledPanel(Language.getString("settings.page.notifications"), 0, true);
        
        JTabbedPane tabs = new JTabbedPane();
        gbc = GuiUtil.makeGbc(0, 0, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        notificationsPanel.add(tabs, gbc);

        editor.setPreferredSize(new Dimension(10,260));
        tabs.add(l("tab.events"), editor);
        tabs.setToolTipTextAt(0, l("tab.events.tip"));
        // Needs to start at "Off", since no event will be triggered at first
        tabs.add(l("tab.notificationSettingsOff"), GuiUtil.northWrap(notificationSettings));
        tabs.add(l("tab.soundSettingsOff"), GuiUtil.northWrap(soundSettings));

        nType.addActionListener(e -> {
            if (nType.getSettingValue() != NOTIFICATION_TYPE_OFF) {
                tabs.setTitleAt(1, l("tab.notificationSettings"));
            } else {
                tabs.setTitleAt(1, l("tab.notificationSettingsOff"));
            }
        });
        
        soundsEnabled.addItemListener(e -> {
            if (soundsEnabled.isSelected()) {
                tabs.setTitleAt(2, l("tab.soundSettings"));
            } else {
                tabs.setTitleAt(2, l("tab.soundSettingsOff"));
            }
        });
        
        notificationsPanel.add(d.addSimpleBooleanSetting("nHideOnStart"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        String tip = StringUtil.randomString(new String[]{
            "Tip: Double-click on Sound column to directly open on the 'Sound' tab.",
            "Tip: Right-click on Chatty notifications to join associated channel.",
            "Tip: Left-click on Chatty notifications to close them."
        });
        GuiUtil.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST);
        notificationsPanel.add(new JLabel(tip),
                d.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST));
        
        //=======
        // Other
        //=======
        add(new LinkLabel("Followed Streams setting moved to [settings:LIVE_STREAMS Live Streams Settings]",
                d.getLinkLabelListener()), getGbc(10));
        
        updateSettingsState();
    }
    
    protected static String l(String id) {
        return Language.getString("settings.notifications."+id);
    }
    
    private void updateSettingsState() {
        boolean enabled = nType.getSettingValue().equals(Long.valueOf(0));
        boolean cmdEnabled = nType.getSettingValue().equals(Long.valueOf(2));

        nPosition.setEnabled(enabled);
        nScreen.setEnabled(enabled);
        nDisplayTime.setEnabled(enabled);
        nMaxDisplayTime.setEnabled(enabled);
        userActivity.setEnabled(enabled);
        nCommand.setEnabled(cmdEnabled);
    }
    
    protected void setData(List<Notification> data) {
        editor.setData(data);
    }
    
    protected List<Notification> getData() {
        return editor.getData();
    }
    
    
    
    protected void scanFiles(boolean showMessage) {
        
        Path path = soundsPath.getCurrentPath();
        Debugging.println("scan Files "+path);
        File file = path.toFile();
        File[] files = file.listFiles(new WavFilenameFilter());
        String resultText = "";
        String warningText = "";
        if (files == null) {
            resultText = "Error scanning folder.";
            editor.setSoundFiles(path, new String[0]);
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
            editor.setSoundFiles(path, fileNames);
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
//                MiscUtil.openFolder(new File(FILE_PATH), d);
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
