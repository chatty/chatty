
package chatty.gui.components;

import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.lang.Language;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class ConnectionDialog extends JDialog {

    private final static Insets SMALL_BUTTON_INSETS = new Insets(-1, 10, -1, 10);
    
    private final JLabel passwordLabel = new JLabel("Access token:");
    private final JTextField name = new JTextField("");
    private final JTextField password = new JPasswordField(14);
    private final JButton connect = new JButton(Language.getString("connect.button.connect"));
    private final JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
    private final JButton favorites = new JButton(Language.getString("connect.button.favoritesHistory"));
    private final JTextField channel = new JTextField(16);
    private final JButton getToken = new JButton(Language.getString("connect.button.configureLogin"));
    private final JCheckBox rejoinOpenChannels = new JCheckBox(Language.getString("connect.button.rejoin"));
    
    private final GridBagConstraints passwordGc = makeGbc(1,1,2,1,GridBagConstraints.WEST);
    private final GridBagConstraints passwordLabelGc = makeGbc(0,1,1,1,GridBagConstraints.WEST);
    
    private String currentUsername = "";
    
    public ConnectionDialog(MainGui owner) {
        super(owner, Language.getString("connect.title"), true);
        this.setResizable(false);
        setLayout(new GridBagLayout());

        password.setEditable(false);

        connect.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "connect");
        connect.getActionMap().put("connect", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                connect.doClick();
            }
        });
        
        
        GridBagConstraints gbc;
        
        // Account
        final JLabel nameLabel = new JLabel(Language.getString("connect.account"));
        nameLabel.setLabelFor(name);
        add(nameLabel, makeGbc(0,0,1,1,GridBagConstraints.EAST));
        name.setEditable(false);
        gbc = makeGbc(1,0,2,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(name, gbc);
        
        // Passwort or Token Button
        //add(passwordLabel, makeGbc(0,1,1,1,GridBagConstraints.EAST));
        
        
        // Configure Login Button
        getToken.setMnemonic(KeyEvent.VK_L);
        gbc = makeGbc(1,2,2,1,GridBagConstraints.WEST);
        gbc.insets = new Insets(0,5,2,20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        getToken.setMargin(SMALL_BUTTON_INSETS);
        add(getToken, gbc);
 
        // Rejoin Open Channels Checkbox
        rejoinOpenChannels.setMargin(SMALL_BUTTON_INSETS);
        gbc = makeGbc(1, 3, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, -7, 0, 5);
        add(rejoinOpenChannels, gbc);
        
        rejoinOpenChannels.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateChannels();
            }
        });
        
        // Channels and Favorites
        final JLabel channelLabel = new JLabel(Language.getString("connect.channel"));
        channelLabel.setLabelFor(channel);
        add(channelLabel, makeGbc(0,4,1,1,GridBagConstraints.EAST));

        GuiUtil.installLengthLimitDocumentFilter(channel, 8000, false);
        gbc = makeGbc(1,4,2,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5,5,5,8);
        channel.setToolTipText(Language.getString("connect.channel.tip"));
        add(channel, gbc);
        
        
        // Favorites Button
        favorites.setMnemonic(KeyEvent.VK_F);
        gbc = makeGbc(1,5,2,1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0,5,10,8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        favorites.setMargin(SMALL_BUTTON_INSETS);
        add(favorites, gbc);

        
        // Main Buttons
        connect.setMnemonic(KeyEvent.VK_E);
        gbc = makeGbc(1,6,1,1,GridBagConstraints.EAST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.6;
        add(connect, gbc);
        gbc = makeGbc(2,6,1,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        cancel.setMnemonic(KeyEvent.VK_C);
        add(cancel, gbc);
        
        
        // Listeners
        ActionListener actionListener = owner.getActionListener();
        connect.addActionListener(actionListener);
        cancel.addActionListener(actionListener);
        getToken.addActionListener(actionListener);
        channel.addActionListener(actionListener);
        favorites.addActionListener(actionListener);
        
        pack();
    }
    
    public JTextField getChannelInput() {
        return channel;
    }
    
    public JButton getConnectButton() {
        return connect;
    }

    public JButton getCancelButton() {
        return cancel;
    }
    
    public JButton getGetTokenButton() {
        return getToken;
    }
    
    public JButton getFavoritesButton() {
        return favorites;
    }
    
    public String getUsername() {
        return name.getText();
    }
    
    public String getChannel() {
        return channel.getText();
    }
    
    public String getPassword() {
        return password.getText();
    }

    public void setUsername(String username) {
        name.setText(username);
        currentUsername = username;
    }
    
    public void setChannel(String channel) {
        this.channel.setText(channel);
    }
    
    /**
     * Whether rejoining open channels is active and selected.
     *
     * @return {@code true} if open channels should be rejoined, {@code false}
     * otherwise
     */
    public boolean rejoinOpenChannels() {
        return rejoinOpenChannels.isEnabled() && rejoinOpenChannels.isSelected();
    }
    
    /**
     * Tells the dialog whether channels are currently open, which determines
     * whether the "Rejoin open channels" option is active.
     * 
     * @param areChannelsOpen Whether any channels are currently open
     */
    public void setAreChannelsOpen(boolean areChannelsOpen) {
        rejoinOpenChannels.setEnabled(areChannelsOpen);
        updateChannels();
    }
    
    public void update(String currentPassword, String currentToken, boolean usePasswordInstead) {
        if (usePasswordInstead) {
            // Using password
            password.setEditable(true);
            passwordLabel.setText("Password:");
            password.setText(currentPassword);
            add(password,passwordGc);
            add(passwordLabel,passwordLabelGc);
        }
        else {
            // Using access token
            remove(password);
            remove(passwordLabel);
            if (currentUsername.isEmpty() || currentToken.isEmpty()) {
                name.setText("");
                name.setToolTipText(Language.getString("connect.accountEmpty.tip"));
            }
            else {
                name.setText(currentUsername);
                name.setToolTipText(Language.getString("connect.account.tip"));
            }
        }
        pack();
    }
    
    /**
     * Enables or disables the channel inputbox depending on whether the rejoin
     * open channels option is active and selected.
     */
    private void updateChannels() {
        channel.setEnabled(!rejoinOpenChannels.isEnabled() || !rejoinOpenChannels.isSelected());
    }
    
    private GridBagConstraints makeGbc(int x, int y,int w, int h, int anchor) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.insets = new Insets(5,5,5,5);
        constraints.anchor = anchor;
        return constraints;
    }
    
}
