
package chatty.gui.components;

import chatty.Helper;
import chatty.TwitchClient;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import chatty.lang.Language;
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
    
    private final JTextField urlField = new JTextField(20);
    private final LinkLabel status;
    private final JButton copyUrl = new JButton(Language.getString("openUrl.button.copy"));
    private final JButton openUrl = new JButton(Language.getString("openUrl.button.open", 1));
    private final JButton close = new JButton(Language.getString("dialog.button.close"));

    private final Map<Scope, JCheckBox> checkboxes = new HashMap<>();
    
    private String currentUrl = TwitchClient.REQUEST_TOKEN_URL;
    
    public TokenGetDialog(MainGui owner) {
        super(owner,"Get login data",true);
        this.setResizable(false);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(owner.getWindowListener());
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        gbc = makeGridBagConstraints(0,0,2,1,GridBagConstraints.CENTER);
        gbc.insets = new Insets(5,5,10,5);
        add(new LinkLabel("<html><body style='width:300px;'>"+Language.getString("login.getTokenInfo"), owner.getLinkLabelListener()), gbc);
        
        JPanel permissions = new JPanel(new GridBagLayout());
        permissions.setBorder(BorderFactory.createTitledBorder(Language.getString("login.tokenPermissions")));
        
        int y = 0;
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
            permissions.add(checkbox, gbc);
            y++;
        }
        
        gbc = makeGridBagConstraints(0, 1, 2, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(permissions, gbc);

        // URL Display and Buttons
        gbc = makeGridBagConstraints(0,y+1,2,1,GridBagConstraints.CENTER);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = new Insets(10,5,10,5);
        urlField.setEditable(false);
        urlField.setToolTipText(Language.getString("login.tokenUrlInfo"));
        add(urlField, gbc);
        gbc = makeGridBagConstraints(0,y+2,1,1,GridBagConstraints.EAST);
        gbc.insets = new Insets(0,5,10,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(copyUrl,gbc);
        gbc = makeGridBagConstraints(1,y+2,1,1,GridBagConstraints.EAST);
        gbc.insets = new Insets(0,0,10,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        add(openUrl,gbc);
        
        // Status and Close Button
        status = new LinkLabel("", owner.getLinkLabelListener());
        add(status,makeGridBagConstraints(0,y+3,2,1,GridBagConstraints.CENTER));
        gbc = makeGridBagConstraints(0,y+4,2,1,GridBagConstraints.EAST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(close, gbc);
        
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
        openUrl.setEnabled(true);
        copyUrl.setEnabled(true);
        urlField.setEnabled(true);
        setStatus("Error: "+errorMessage+"<br />"
                + "Read the [help-guide2: help] on how to proceed.");
    }
    
    public void tokenReceived() {
        setStatus("Token received.. completing..");
    }
    
    private void setStatus(String text) {
        status.setText("<html><body style='width:250px;text-align:center'>"+text);
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
