
package chatty.gui.components;

import chatty.gui.MainGui;
import chatty.lang.Language;
import chatty.util.api.TokenInfo;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class TokenDialog extends JDialog {
    
    private final static ImageIcon OK_IMAGE = new ImageIcon(TokenDialog.class.getResource("ok.png"));
    private final static ImageIcon NO_IMAGE = new ImageIcon(TokenDialog.class.getResource("no.png"));
    
    private final JLabel nameLabel = new JLabel(Language.getString("login.accountName"));
    private final JLabel name = new JLabel("<no account>");
    private final LinkLabel accessLabel;
    private final JPanel access;
    
    private final Map<String, JLabel> accessScopes = new HashMap<>();
    
    private final JButton deleteToken = new JButton(Language.getString("login.button.removeLogin"));
    private final JButton requestToken = new JButton(Language.getString("login.button.requestLogin"));
    private final JButton verifyToken = new JButton(Language.getString("login.button.verifyLogin"));
    private final LinkLabel foreignTokenInfo;
    private final LinkLabel otherInfo;
    private final JButton done = new JButton(Language.getString("dialog.button.close"));
    
    private String currentUsername = "";
    private String currentToken = "";
    
    public TokenDialog(MainGui owner) {
        super(owner, Language.getString("login.title"), true);
        this.setResizable(false);
       
        this.setLayout(new GridBagLayout());
        
        accessLabel = new LinkLabel("Access: [help:login (help)]", owner.getLinkLabelListener());
        //tokenInfo = new JLabel();
        foreignTokenInfo = new LinkLabel("<html><body>"
                    + "Login data set externally with -token parameter.", owner.getLinkLabelListener());
        foreignTokenInfo.setVisible(false);
        otherInfo = new LinkLabel("<html><body>To add or "
                + "reduce access remove login and request again.", owner.getLinkLabelListener());
        
        GridBagConstraints gbc;
        
        add(nameLabel, makeGridBagConstraints(0,0,1,1,GridBagConstraints.WEST));
        add(name, makeGridBagConstraints(0,1,2,1,GridBagConstraints.CENTER,new Insets(0,5,5,5)));
        
        add(accessLabel, makeGridBagConstraints(0,2,1,1,GridBagConstraints.WEST));
        
        access = new JPanel();
        access.setLayout(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.anchor = GridBagConstraints.WEST;
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        Insets categoryInsets = new Insets(0, 5, 5, 5);
        Insets scopeInsets = new Insets(0, 5, 0, 5);
        for (TokenInfo.ScopeCategory scopeCat : TokenInfo.ScopeCategory.values()) {
            JLabel label = new JLabel(scopeCat.label);
            gbc2.insets = categoryInsets;
            access.add(label, gbc2);
            gbc2.gridy++;
            for (TokenInfo.Scope scope : scopeCat.scopes) {
                label = new JLabel(scope.label);
                label.setToolTipText(scope.description);
                accessScopes.put(scope.scope, label);
                gbc2.gridy++;
                gbc2.insets = scopeInsets;
                access.add(label, gbc2);
            }
            gbc2.gridy = 0;
            gbc2.gridx++;
        }
        gbc = makeGridBagConstraints(0,3,2,1,GridBagConstraints.CENTER,new Insets(0,5,5,5));
        add(access, gbc);
        
        gbc = makeGridBagConstraints(0, 4, 2, 1, GridBagConstraints.WEST);
        add(otherInfo, gbc);
        
        gbc = makeGridBagConstraints(0,6,2,1,GridBagConstraints.WEST);
        add(foreignTokenInfo, gbc);
        
        deleteToken.setToolTipText(Language.getString("login.button.removeLogin.tip"));
        gbc = makeGridBagConstraints(0,7,1,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(deleteToken, gbc);
        
        gbc = makeGridBagConstraints(0,7,2,1,GridBagConstraints.CENTER);
        add(requestToken, gbc);
        
        gbc = makeGridBagConstraints(1,7,1,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(verifyToken, gbc);
        
        gbc = makeGridBagConstraints(0,8,2,1,GridBagConstraints.EAST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(done, gbc);
        
        ActionListener actionListener = owner.getActionListener();
        requestToken.addActionListener(actionListener);
        deleteToken.addActionListener(actionListener);
        verifyToken.addActionListener(actionListener);
        done.addActionListener(actionListener);

        pack();
    }
    
    public JButton getRequestTokenButton() {
        return requestToken;
    }
    
    public JButton getDeleteTokenButton() {
        return deleteToken;
    }
    
    public JButton getVerifyTokenButton() {
        return verifyToken;
    }
    
    public JButton getDoneButton() {
        return done;
    }
    
    public void update() {
        boolean empty = currentUsername.isEmpty() || currentToken.isEmpty();
        deleteToken.setVisible(!empty);
        requestToken.setVisible(empty);
        verifyToken.setVisible(!empty);
        otherInfo.setVisible(!empty);
        pack();
    }
    
    public void update(String username, String currentToken) {
        this.currentUsername = username;
        this.currentToken = currentToken;
        if (currentUsername.isEmpty() || currentToken.isEmpty()) {
            name.setText(Language.getString("login.createLogin"));
        }
        else {
            name.setText(currentUsername);
        }
        //setTokenInfo("");
        update();
    }
    
    /**
     * Update the text showing what scopes are available.
     * 
     * @param scopes
     */
    public void updateAccess(Collection<String> scopes) {
        boolean empty = currentUsername.isEmpty() || currentToken.isEmpty();
        access.setVisible(!empty);
        accessLabel.setVisible(!empty);

        for (TokenInfo.Scope s : TokenInfo.Scope.values()) {
            JLabel label = accessScopes.get(s.scope);
            boolean enabled = scopes.contains(s.scope);
            if (enabled) {
                label.setIcon(OK_IMAGE);
            } else {
                label.setIcon(NO_IMAGE);
            }
        }
        update();
    }
    
    /**
     * Change status to verifying token.
     */
    public void verifyingToken() {
//        setTokenInfo(Language.getString("login.verifyingLogin"));
        verifyToken.setEnabled(false);
    }
    
    /**
     * Set the result of the token verification (except the scopes).
     * 
     * @param valid
     * @param result 
     */
    public void tokenVerified(boolean valid, String result) {
        if (isVisible()) {
            // Only show when verifying while the token dialog is open
            JOptionPane.showMessageDialog(this, result);
        }
        verifyToken.setEnabled(true);
        update();
    }
    
    public void setForeignToken(boolean foreign) {
        foreignTokenInfo.setVisible(foreign);
        pack();
    }
    
    private GridBagConstraints makeGridBagConstraints(int x, int y,int w, int h, int anchor, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = w;
        constraints.gridheight = h;
        constraints.insets = insets;
        constraints.anchor = anchor;
        return constraints;
    }
    
    private GridBagConstraints makeGridBagConstraints(int x, int y,int w, int h, int anchor) {
        return makeGridBagConstraints(x,y,w,h,anchor,new Insets(5,5,5,5));
        
    }
    
}
