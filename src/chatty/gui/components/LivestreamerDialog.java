
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.components.settings.EditorStringSetting;
import chatty.util.Debugging;
import chatty.util.Livestreamer;
import chatty.util.Livestreamer.LivestreamerListener;
import static chatty.util.Livestreamer.filterToken;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * Contains settings and information about Livestreamer and the tabs that are
 * created when you open a stream, that show output and have have controls to
 * select quality or stop/re-run the Livestreamer process.
 * 
 * @author tduva
 */
public class LivestreamerDialog extends JDialog {
    
    private final JButton closeButton = new JButton("Close");
    private final JTabbedPane tabs = new JTabbedPane();
    private final Window parent;
    
    private final JCheckBox enableContextMenu = new JCheckBox("Enable context menu entry");
    private final JCheckBox openDialog = new JCheckBox("Show dialog when opening stream");
    private final JCheckBox autoCloseDialog = new JCheckBox("Auto close dialog when starting player");
    private final EditorStringSetting qualities;
    
    private final EditorStringSetting commandDef;
    
    private final JTextField streamInput = new JTextField(30);
    private final JButton openStreamButton = new JButton("Open Stream");
    
    private static final String INFO = "Streamlink (a fork of Livestreamer) is an external program "
            + "you have to install separately that allows you to watch "
            + "streams of many websites in a player like VLC. "
            + "[help-livestreamer:top More information..]";
    
    private static final String BASE_COMMAND_INFO = "<html><body style='width:340px;font-weight:normal;'>"
            + "Example Usage (setting the window title for VLC):<br />"
            + "<code>streamlink -p \"'C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe' --meta-title '$stream/$quality'\"</code>"
            + "<br /><br />"
            + "This should point to the Streamlink executable and can contain "
            + "parameters that should always be included when you run "
            + "Streamlink via Chatty.<br /><br />"
            + "The url and quality are <em>automatically</em> appended when "
            + "you run Streamlink via Chatty, but you can use them in other parameters "
            + "via <code>$stream</code>, <code>$url</code> and <code>$quality</code>.</p>";
            
    
    private final Settings settings;
    
    public LivestreamerDialog(Window parent, LinkLabelListener linkLabelListener,
            final Settings settings) {
        super(parent);
        this.settings = settings;
        setTitle("Streamlink");
        
        this.parent = parent;
        
        setLocationRelativeTo(parent);
        
        setLayout(new BorderLayout());
        
        add(tabs, BorderLayout.CENTER);
        closeButton.setMnemonic(KeyEvent.VK_C);
        add(closeButton, BorderLayout.SOUTH);
        
        /**
         * Info Panel
         */
        JPanel infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc;
        
        LinkLabel info = new LinkLabel(INFO, linkLabelListener);
        info.setPreferredSize(new Dimension(300, 50));
        gbc = GuiUtil.makeGbc(0, 0, 2, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        infoPanel.add(info, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 0, 5);
        infoPanel.add(enableContextMenu, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 15, 0, 5);
        infoPanel.add(openDialog, gbc);
        
        gbc = GuiUtil.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 15, 5, 5);
        infoPanel.add(autoCloseDialog, gbc);
        
        gbc = GuiUtil.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 0, 5);
        infoPanel.add(new JLabel("Context menu qualities (\"Select\" to select quality):"), gbc);
        
        gbc = GuiUtil.makeGbc(0, 5, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 5, 5, 30);
        qualities = new EditorStringSetting(this,
                "Context Menu Qualities", 24, false, false, null);
        infoPanel.add(qualities, gbc);
        
        gbc = GuiUtil.makeGbc(0, 6, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 0, 5);
        infoPanel.add(new JLabel("Base command (Streamlink path and parameters):"), gbc);

        gbc = GuiUtil.makeGbc(0, 7, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(4, 5, 4, 30);
        commandDef = new EditorStringSetting(this,
                "Base command (Streamlink path and paramters)",
                24, false, false, BASE_COMMAND_INFO);
        infoPanel.add(commandDef, gbc);
        
        gbc = GuiUtil.makeGbc(0, 9, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 0, 5);
        JLabel streamLabel = new JLabel("Enter stream name or URL (or commandline options):");
        streamLabel.setLabelFor(streamInput);
        infoPanel.add(streamLabel, gbc);
        
        gbc = GuiUtil.makeGbc(0, 10, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        infoPanel.add(streamInput, gbc);
        
        gbc = GuiUtil.makeGbc(1, 10, 1, 1);
        openStreamButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(openStreamButton, gbc);
        
        tabs.add("Main", infoPanel);
        
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == closeButton) {
                    setVisible(false);
                    settings.setString("livestreamerQualities", qualities.getSettingValue());
                } else if (e.getSource() == openStreamButton
                        || e.getSource() == streamInput) {
                    String stream = streamInput.getText();
                    if (!stream.isEmpty()) {
                        open(stream, null);
                    }
                } else if (e.getSource() == enableContextMenu) {
                    // Only save setting, loading is done from the MainGui
                    settings.setBoolean("livestreamer", enableContextMenu.isSelected());
                } else if (e.getSource() == openDialog) {
                    settings.setBoolean("livestreamerShowDialog", openDialog.isSelected());
                } else if (e.getSource() == autoCloseDialog) {
                    settings.setBoolean("livestreamerAutoCloseDialog", autoCloseDialog.isSelected());
                }
            }
        };
        
        streamInput.addActionListener(buttonAction);
        openStreamButton.addActionListener(buttonAction);
        closeButton.addActionListener(buttonAction);
        enableContextMenu.addActionListener(buttonAction);
        openDialog.addActionListener(buttonAction);
        autoCloseDialog.addActionListener(buttonAction);
        
        commandDef.setChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                settings.setString("livestreamerCommand", commandDef.getSettingValue());
            }
        });
        
        pack();
        
        setMinimumSize(getSize());
    }
    
    /**
     * Opens the given stream and quality. Shows the dialog depending on the
     * settings and also loads the settings into the dialog.
     * 
     * @param stream The stream to open
     * @param quality The quality to open (null to select quality)
     */
    public void open(String stream, String quality) {
        if (stream != null) {
            String url = "twitch.tv/" + stream;
            if (!Helper.isValidChannel(stream)) {
                url = stream;
            }
            Item existingItem = getExisitingItem(url, quality);
            if (existingItem != null) {
                existingItem.start();
                tabs.setSelectedComponent(existingItem);
            } else {
                Item newItem = new Item(url, quality, stream);
                tabs.add(StringUtil.shortenTo(stream, -20), newItem);
                tabs.setSelectedComponent(newItem);
                tabs.setToolTipTextAt(tabs.getSelectedIndex(), stream);
                newItem.start();
                if (getWidth() < getPreferredSize().width) {
                    setSize(getPreferredSize().width, getHeight());
                }
            }
        }
        loadSettings();
        if (stream == null || quality == null || openDialog.isSelected()) {
            if (!isVisible()) {
                setLocationRelativeTo(parent);
            }
            setVisible(true);
        }
    }
    
    /**
     * Gets the item of an open tab with the given URL and quality, if the
     * process isn't running anymore and the quality isn't null (which would
     * mean that the user wants to select the quality).
     * 
     * @param url The URL
     * @param quality The quality
     * @return An Item with the given requirements, or null if none was found
     */
    private Item getExisitingItem(String url, String quality) {
        for (Object o : tabs.getComponents()) {
            if (o instanceof Item) {
                Item item = (Item)o;
                if (!item.running && item.quality != null
                        && item.quality.equals(quality) && item.url.equals(url)) {
                    return item;
                }
            }
        }
        return null;
    }
    
    private void loadSettings() {
        enableContextMenu.setSelected(settings.getBoolean("livestreamer"));
        this.qualities.setSettingValue(settings.getString("livestreamerQualities"));
        commandDef.setSettingValue(settings.getString("livestreamerCommand"));
        openDialog.setSelected(settings.getBoolean("livestreamerShowDialog"));
        autoCloseDialog.setSelected(settings.getBoolean("livestreamerAutoCloseDialog"));
    }
    
    /**
     * Manages one instance that contains a stream (or more general any
     * parameters) and a quality (which is just another parameter put behind the
     * first). Listens to the responses of the process and shows them in the GUI
     * and has buttons to control it.
     */
    private class Item extends JPanel implements LivestreamerListener,
            ActionListener {
        
        private final JButton closeButton = new JButton("Close");
        private final JButton retryButton = new JButton("Retry");
        private final JTextArea messages = new JTextArea();
        private final JLabel info = new JLabel();
        private final String url;
        private final String stream;
        
        /**
         * The quality of the stream, which is just another parameter that is
         * put behind the first one.
         */
        private String quality;
        
        /**
         * Whether the process is currently running.
         */
        private boolean running;
        
        /**
         * Reference to the current Livestreamer object.
         */
        private Livestreamer ls;
        private final ActionListener qualityButtonListener = new QualityButtonListener();
        
        private final JPanel buttonPanel = new JPanel();
        
        /**
         * Creates a new instance with the {@code url} to open (or any other
         * parameters) and the {@code quality} to use.
         * 
         * @param url The {@code url} (or any parameter) to use
         * @param quality The {@code quality}, can be {@code null}, which means
         * it is supposed to be selected in the dialog
         */
        private Item(String url, String quality, String stream) {
            this.url = url;
            this.quality = quality;
            this.stream = stream;
            if (quality != null) {
                info.setText("Selected quality: "+quality);
            }
            
            setLayout(new GridBagLayout());
            GridBagConstraints gbc;
            
            gbc = GuiUtil.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(info, gbc);
            
            gbc = GuiUtil.makeGbc(1, 0, 1, 1);
            retryButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            add(retryButton, gbc);
            
            gbc = GuiUtil.makeGbc(2, 0, 1, 1);
            closeButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            add(closeButton, gbc);
            
            gbc = GuiUtil.makeGbc(0, 1, 3, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            messages.setEditable(false);
            messages.setLineWrap(true);
            messages.setWrapStyleWord(true);
            JScrollPane scroll = new JScrollPane(messages);
            scroll.setPreferredSize(new Dimension(300, 150));
            add(scroll, gbc);
            
            gbc = GuiUtil.makeGbc(0, 2, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(buttonPanel, gbc);
            
            closeButton.addActionListener(this);
            retryButton.addActionListener(this);
            
        }
        
        /**
         * Adds a mesage to the text area.
         * 
         * @param message 
         */
        private void addMessage(String message) {
            if (quality == null && message.trim().startsWith("Available streams:")) {
                parseQualities(message);
            }
            if (message.trim().startsWith("Starting player") && autoCloseDialog.isSelected()) {
                LivestreamerDialog.this.setVisible(false);
            }
            
            Document doc = messages.getDocument();
            try {
                doc.insertString(doc.getLength(), filterToken(message)+"\n", null);
            } catch (BadLocationException ex) {
                Logger.getLogger(LivestreamerDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        /**
         * Parses the output of Livestreamer that lists the available qualities
         * and adds a button for each quality. This may not be very robust
         * parsing if the format changes.
         * 
         * @param message 
         */
        private void parseQualities(String message) {
            // Remove everything in ( ), which isn't an actual quality and can
            // get in the way otherwise
            message = message.replaceAll("\\([^)]*\\)", "");
            
            String[] split = message.split(":");
            if (split.length == 2) {
                String[] split2 = split[1].split(",");
                buttonPanel.removeAll();
                for (String part : split2) {
                    String q = part.trim();
                    JButton button = new JButton(q);
                    button.addActionListener(qualityButtonListener);
                    buttonPanel.add(button);
                }
                if (getWidth() < buttonPanel.getPreferredSize().width) {
                    pack();
                }
                info.setText("Click button to select quality.");
            }
        }
        
        /**
         * Start a new process (if none is currently running), based on the
         * current settings.
         */
        public void start() {
            if (running) {
                return;
            }
            setQualityButtonsEnabled(false);
            if (quality == null) {
                info.setText("No quality selected yet.");
            } else {
                info.setText("Selected quality: "+quality);
            }
            StringBuilder command = new StringBuilder();
            command.append(makeBaseCommand());
            command.append(" ");
            command.append(url);
            if (quality != null) {
                command.append(" ");
                command.append(quality);
            }
            Livestreamer ls = new Livestreamer(command.toString(), this);
            this.ls = ls;
            ls.start();
        }
        
        private String makeBaseCommand() {
            String command = settings.getString("livestreamerCommand");
            command = command.replace("$stream", stream);
            command = command.replace("$url", url);
            if (quality != null) {
                command = command.replace("$quality", quality);
            }
            return command;
        }
        
        /**
         * Sets the state of the process and changes the GUI accordingly.
         * 
         * @param running 
         */
        private void setRunning(boolean running) {
            if (running) {
                closeButton.setText("End process");
            } else {
                closeButton.setText("Close");
            }
            retryButton.setEnabled(!running);
            setQualityButtonsEnabled(!running);
            this.running = running;
            
            // Remove the tab only when the quality was already set, which means
            // it should have been the final run, and the dialog isn't open,
            // so the user probably doesn't need it anymore.
            if (!running && quality != null && !LivestreamerDialog.this.isVisible()) {
                tabs.remove(this);
            }
        }

        @Override
        public void processStarted(final String command) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    addMessage("COMMAND: "+command);
                    setRunning(true);
                }
            });
        }

        @Override
        public void message(final String message) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    addMessage(message.replace("[cli][info] ", ""));
                }
            });
        }

        @Override
        public void processFinished(final int exitValue) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    addMessage("PROCESS ENDED.");
                    setRunning(false);
                }
            });
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == closeButton) {
                if (running) {
                    ls.kill();
                } else {
                    tabs.remove(this);
                }
            } else if (e.getSource() == retryButton) {
                if (!running) {
                    start();
                }
            }
        }
        
        private class QualityButtonListener implements ActionListener {

            @Override
            public void actionPerformed(ActionEvent e) {
                quality = e.getActionCommand();
                start();
            }
            
        }
        
        private void setQualityButtonsEnabled(boolean enabled) {
            for (Component c : buttonPanel.getComponents()) {
                c.setEnabled(enabled);
            }
        }
        
    }
    
}
