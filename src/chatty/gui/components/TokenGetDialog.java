
package chatty.gui.components;

import chatty.Helper;
import chatty.TwitchClient;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import chatty.util.MiscUtil;
import chatty.util.api.TokenInfo;
import chatty.util.api.TokenInfo.Scope;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

/**
 * Dialog thats builds the URL based on the selected access options and waits
 * for the access token.
 * 
 * Even through this dialog does not control it, the local webserver is started
 * once this dialog is opened and stopped when it is closed. It also shows the
 * responses from the webserver like whether it is ready or failed to listen
 * to the port.
 * 
 * @author tduva
 */
public class TokenGetDialog extends JDialog implements ItemListener, ActionListener {
    
    private static final String INFO = "<html><body>Request new login data ([help:login ?]):<br />"
            + "1. Open the link below<br />"
            + "2. Grant chat access for Chatty<br />"
            + "3. Get redirected";
    private final LinkLabel info;
    private final JTextField urlField = new JTextField(20);
    private final JLabel status = new JLabel();
    private final JButton copyUrl = new JButton("Copy URL");
    private final JButton openUrl = new JButton("Open (default browser)");
    private final JButton close = new JButton("Close");

    private final Map<Scope, JCheckBox> checkboxes = new HashMap<>();
    
    private String currentUrl = TwitchClient.REQUEST_TOKEN_URL;
    
    public TokenGetDialog(MainGui owner) {
        super(owner,"Get login data",true);
        this.setResizable(false);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(owner.getWindowListener());
        
        info = new LinkLabel(INFO, owner.getLinkLabelListener());
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        gbc = makeGridBagConstraints(0,0,2,1,GridBagConstraints.CENTER);
        gbc.insets = new Insets(5,5,10,5);
        add(info, gbc);
        
        int y = 1;
        for (Scope scope : TokenInfo.Scope.values()) {
            JCheckBox checkbox = new JCheckBox(scope.label);
            checkbox.setToolTipText(scope.description);
            checkbox.setSelected(true);
            checkbox.addItemListener(e -> updateUrl());
            if (scope == Scope.CHAT) {
                checkbox.setEnabled(false);
            }
            gbc = makeGridBagConstraints(0, y, 2, 1, GridBagConstraints.WEST);
            gbc.insets = new Insets(0,5,0,5);
            checkboxes.put(scope, checkbox);
            add(checkbox, gbc);
            y++;
        }

        // URL Display and Buttons
        gbc = makeGridBagConstraints(0,y+1,2,1,GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(10,5,10,5);
        urlField.setEditable(false);
        add(urlField, gbc);
        gbc = makeGridBagConstraints(0,y+2,1,1,GridBagConstraints.EAST);
        gbc.insets = new Insets(0,5,10,5);
        add(copyUrl,gbc);
        gbc = makeGridBagConstraints(1,y+2,1,1,GridBagConstraints.EAST);
        gbc.insets = new Insets(0,0,10,5);
        add(openUrl,gbc);
        
        // Status and Close Button
        add(status,makeGridBagConstraints(0,y+3,2,1,GridBagConstraints.CENTER));
        add(close,makeGridBagConstraints(1,y+4,1,1,GridBagConstraints.EAST));
        
        openUrl.addActionListener(this);
        copyUrl.addActionListener(this);
        close.addActionListener(owner.getActionListener());
        
        reset();
        updateUrl();
        
        pack();
    }
    
    public JButton getCloseButton() {
        return close;
    }
    
    public final void reset() {
        openUrl.setEnabled(false);
        copyUrl.setEnabled(false);
        urlField.setEnabled(false);
        setStatus("Please wait..");
    }
    
    public void ready() {
        openUrl.setEnabled(true);
        copyUrl.setEnabled(true);
        urlField.setEnabled(true);
        setStatus("Ready.");
    }
    
    public void error(String errorMessage) {
        setStatus("Error: "+errorMessage);
    }
    
    public void tokenReceived() {
        setStatus("Token received.. completing..");
    }
    
    private void setStatus(String text) {
        status.setText("<html><body style='width:150px;text-align:center'>"+text);
        pack();
    }
    
    private GridBagConstraints makeGridBagConstraints(int x, int y,int w, int h, int anchor) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.insets = new Insets(5,5,5,5);
        constraints.anchor = anchor;
        return constraints;
    }
    
    private void updateUrl() {
        String scopes = "";
        for (Map.Entry<Scope, JCheckBox> entry : checkboxes.entrySet()) {
            JCheckBox checkbox = entry.getValue();
            if (checkbox.isSelected()) {
                scopes += "+"+entry.getKey().scope;
            }
        }
        if (!scopes.isEmpty()) {
            scopes = scopes.substring(1);
        }
        String url = TwitchClient.REQUEST_TOKEN_URL+scopes;
        currentUrl = url;
        urlField.setText(url);
        urlField.setToolTipText(url);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        updateUrl();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == openUrl) {
            UrlOpener.openUrlPrompt(this, currentUrl);
        }
        else if (e.getSource() == copyUrl) {
            MiscUtil.copyToClipboard(currentUrl);
        }
    } 
   
}
