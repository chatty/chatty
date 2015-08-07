
package chatty.gui.components.settings;

import chatty.WhisperConnection;
import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.util.UrlRequest;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Settings that should only be changed if you know what you're doing, includes
 * a warning about that.
 * 
 * @author tduva
 */
public class AdvancedSettings extends SettingsPanel {
    
    private static final Logger LOGGER = Logger.getLogger(AdvancedSettings.class.getName());
    
    private final StringSetting groupChatServer;
    private final StringSetting groupChatPort;
    
    public AdvancedSettings(final SettingsDialog d) {

        JPanel warning = new JPanel();
        
        warning.add(new JLabel("<html><body style='width:300px'>"
                + "These settings can break Chatty if you change them, "
                + "so you should only change these settings if you "
                + "know what you are doing."));
        
        addPanel(warning, getGbc(0));
        
        JPanel connection = addTitledPanel("Connection", 1);

        connection.add(new JLabel("Server:"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("serverDefault", 20, true),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("Port:"),
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        connection.add(d.addSimpleStringSetting("portDefault", 10, true),
                d.makeGbc(1, 1, 1, 1, GridBagConstraints.WEST));
        
        connection.add(new JLabel("(These might be overridden by commandline parameters.)"),
                d.makeGbc(0, 2, 2, 1));
        
        connection.add(d.addSimpleBooleanSetting("membershipEnabled",
                "Correct Userlist (receives joins/parts, userlist)",
                "Enables the membership capability while connecting, which allows receiving of joins/parts/userlist"),
                d.makeGbc(0, 4, 2, 1, GridBagConstraints.NORTHWEST));
        
        JPanel whisper = addTitledPanel("Whisper (experimental, read help!)", 3);
        
        whisper.add(
                d.addSimpleBooleanSetting("whisperEnabled", "Whisper Enabled",
                        "Connects to group chat to allow for whispering"),
                d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST)
        );
        
        whisper.add(
                d.addSimpleBooleanSetting("whisperWhitelist", "Whitelist",
                        "Only users in the Addressbook category 'whisper' may send messages to you."),
                d.makeGbc(4, 1, 1, 1, GridBagConstraints.EAST)
        );
        
        whisper.add(new JLabel("Display:"),
                d.makeGbc(3, 0, 1, 1));
        
        Map<Long, String> displayMode = new LinkedHashMap<>();
        displayMode.put(Long.valueOf(WhisperConnection.DISPLAY_IN_CHAT), "Active Chat");
        displayMode.put(Long.valueOf(WhisperConnection.DISPLAY_ONE_WINDOW), "One Window");
        displayMode.put(Long.valueOf(WhisperConnection.DISPLAY_PER_USER), "Per User");
        ComboLongSetting displayModeSetting = new ComboLongSetting(displayMode);
        d.addLongSetting("whisperDisplayMode", displayModeSetting);
        whisper.add(displayModeSetting,
                d.makeGbc(4, 0, 1, 1));
        
        groupChatServer = (StringSetting)d.addSimpleStringSetting("groupChatServer", 10, true);
        groupChatPort = (StringSetting)d.addSimpleStringSetting("groupChatPort", 4, true);
        whisper.add(new JLabel("Server:"), d.makeGbc(0, 1, 1, 1));
        whisper.add((JTextField)groupChatServer,
                d.makeGbc(1, 1, 1, 1));
        whisper.add(new JLabel("Port:"), d.makeGbc(2, 1, 1, 1, GridBagConstraints.EAST));
        whisper.add((JTextField)groupChatPort,
                d.makeGbc(3, 1, 1, 1, GridBagConstraints.WEST));
        
        JButton selectServer = new JButton("Select server..");
        selectServer.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        final GroupChatSelect groupChatSelect = new GroupChatSelect(d);
        whisper.add(selectServer,
                d.makeGbc(0, 3, 2, 1));
        selectServer.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                groupChatSelect.setLocationRelativeTo(d);
                groupChatSelect.showDialog();
            }
        });
        
        whisper.add(new LinkLabel("[help-whisper:top Whisper Help]", d.getLinkLabelListener()),
                d.makeGbc(2, 3, 2, 1));
        
        
    }
    
    private class GroupChatSelect extends JDialog {
        
        private final JPanel cards;
        private final CardLayout cardLayout = new CardLayout();
        private final JList<String> serversList = new JList<>();
        private final DefaultListModel<String> servers = new DefaultListModel<>();
        private final JButton selectButton = new JButton("Close");
        private final LinkLabel info;
        private boolean successfullyLoaded;
        
        public GroupChatSelect(SettingsDialog d) {
            super(d);
            setModal(true);
            setResizable(false);
            setTitle("Select server");
            
            info = new LinkLabel("", d.getLinkLabelListener());
            
            cards = new JPanel(cardLayout);
            cards.add(new JScrollPane(serversList), "servers");
            cards.add(info, "info");
            
            setLayout(new GridBagLayout());
            serversList.setPreferredSize(new Dimension(200, 50));
            GridBagConstraints gbc = d.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(cards,
                    gbc);
            
            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(selectButton, gbc);
            serversList.setModel(servers);
            selectButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String selected = serversList.getSelectedValue();
                    if (selected != null) {
                        int sep = selected.lastIndexOf(":");
                        try {
                            String server = selected.substring(0, sep);
                            String port = selected.substring(sep+1, selected.length());
                            groupChatServer.setSettingValue(server);
                            groupChatPort.setSettingValue(port);
                        } catch (Exception ex) {
                            LOGGER.warning("Invalid server info");
                        }
                    }
                    setVisible(false);
                }
            });
            
            pack();
        }
        
        public void showDialog() {
            if (!successfullyLoaded) {
                requestList();
            }
            pack();
            setVisible(true);
        }
        
        private void requestList() {
            showInfo("Loading..");
            UrlRequest request = new UrlRequest("https://tmi.twitch.tv/servers?cluster=group") {
                
                @Override
                public void requestResult(final String result, final int responseCode) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            parseResult(result, responseCode);
                        }
                    });
                }
            };
            request.setLabel("Group Chat Servers");
            new Thread(request).start();
        }
        
        private void parseResult(String result, int responseCode) {
            servers.clear();
            if (result == null || responseCode != 200) {
                error("Request error ("+responseCode+")");
                return;
            }
            JSONParser parser = new JSONParser();
            try {
                JSONObject root = (JSONObject) parser.parse(result);
                JSONArray serversData = (JSONArray) root.get("servers");
                for (Object server : serversData) {
                    servers.addElement((String)server);
                }
                serversList.setSelectedIndex(0);
                showServers();
                successfullyLoaded = true;
                selectButton.setText("Use selected server");
            } catch (Exception ex) {
                error("Error parsing servers");
                LOGGER.warning("Error parsing servers: " + ex);
            }
        }
        
        private void error(String error) {
            showInfo("<html><body style='width:200px;'>"+error+"<br /><br />"
                    + "This may be a temporary problem or the used API may have"
                    + " changed. Please try again later or consult the [help-whisper:top help] on"
                    + " how to add the server manually.<br /><br />"
                    + "If this keeps happening, check the usual channels"
                    + " ([url:http://chatty.github.io Chatty website], [url:https://twitter.com/chattyclient @ChattyClient]) for updates.");
            selectButton.setText("Close");
        }
        
        private void showInfo(String infoText) {
            info.setText(infoText);
            cardLayout.show(cards, "info");
            pack();
        }
        
        private void showServers() {
            cardLayout.show(cards, "servers");
            pack();
        }
        
    }
    
}
