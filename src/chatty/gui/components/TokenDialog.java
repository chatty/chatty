
package chatty.gui.components;

import chatty.gui.MainGui;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class TokenDialog extends JDialog {
    
    private final static String OK_IMAGE = "<img style='vertical-align:bottom' src='"+TokenDialog.class.getResource("ok.png").toString()+"'>";
    private final static String NO_IMAGE = "<img src='"+TokenDialog.class.getResource("no.png").toString()+"'>";
    
    JLabel nameLabel = new JLabel(Language.getString("login.accountName"));
    JLabel name = new JLabel("<no account>");
    LinkLabel accessLabel;
    JLabel access = new JLabel("<none>");
    
    JLabel info = new JLabel("<html><body style='width:200px'>");
    JButton deleteToken = new JButton(Language.getString("login.button.removeLogin"));
    JButton requestToken = new JButton(Language.getString("login.button.requestLogin"));
    JButton verifyToken = new JButton(Language.getString("login.button.verifyLogin"));
    private final LinkLabel tokenInfo;
    private final LinkLabel foreignTokenInfo;
    JButton done = new JButton(Language.getString("dialog.button.close"));
    
    String currentUsername = "";
    String currentToken = "";
    
    public TokenDialog(MainGui owner) {
        super(owner, Language.getString("login.title"), true);
        this.setResizable(false);
       
        this.setLayout(new GridBagLayout());
        
        accessLabel = new LinkLabel("Access: [help:login (help)]", owner.getLinkLabelListener());
        //tokenInfo = new JLabel();
        tokenInfo = new LinkLabel("", owner.getLinkLabelListener());
        foreignTokenInfo = new LinkLabel("<html><body style='width:170px'>"
                    + "Login data set externally with -token parameter.", owner.getLinkLabelListener());
        foreignTokenInfo.setVisible(false);
        
        GridBagConstraints gbc;
        
        add(nameLabel, makeGridBagConstraints(0,0,1,1,GridBagConstraints.WEST));
        add(name, makeGridBagConstraints(0,1,2,1,GridBagConstraints.CENTER,new Insets(0,5,5,5)));
        
        add(accessLabel, makeGridBagConstraints(0,2,1,1,GridBagConstraints.WEST));
        
        gbc = makeGridBagConstraints(0,3,2,1,GridBagConstraints.CENTER,new Insets(0,5,5,5));
        add(access, gbc);
        
        gbc = makeGridBagConstraints(0,4,2,1,GridBagConstraints.WEST);
        add(tokenInfo, gbc);
        
        gbc = makeGridBagConstraints(0,5,2,1,GridBagConstraints.WEST);
        add(foreignTokenInfo, gbc);
        
        gbc = makeGridBagConstraints(0,6,1,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(deleteToken, gbc);
        
        gbc = makeGridBagConstraints(0,6,2,1,GridBagConstraints.CENTER);
        add(requestToken, gbc);
        
        gbc = makeGridBagConstraints(1,6,1,1,GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(verifyToken, gbc);
        
        gbc = makeGridBagConstraints(0,7,2,1,GridBagConstraints.EAST);
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
     * @param chat
     * @param editor
     * @param commercial
     * @param user
     * @param subs 
     */
    public void updateAccess(boolean chat, boolean editor, boolean commercial,
            boolean user, boolean subs, boolean follow) {
        boolean empty = currentUsername.isEmpty() || currentToken.isEmpty();
        access.setVisible(!empty);
        accessLabel.setVisible(!empty);

        StringBuilder b = new StringBuilder("<html><body style='line-height:28px;'>");
        b.append(accessStatusImage(chat)).append("&nbsp;").append(Language.getString("login.access.chat")).append("<br />");
        b.append(accessStatusImage(user)).append("&nbsp;").append(Language.getString("login.access.user")).append("<br />");
        b.append(accessStatusImage(editor)).append("&nbsp;").append(Language.getString("login.access.editor")).append("<br />");
        b.append(accessStatusImage(commercial)).append("&nbsp;").append(Language.getString("login.access.commercials")).append("<br />");
        b.append(accessStatusImage(subs)).append("&nbsp;").append(Language.getString("login.access.subscribers")).append("<br />");
        b.append(accessStatusImage(follow)).append("&nbsp;").append(Language.getString("login.access.follow"));

        access.setText(b.toString());
        update();
    }
    
    private static String accessStatusImage(boolean status) {
        if (status) {
            return OK_IMAGE;
        }
        return NO_IMAGE;
    }
    
    /**
     * Change status to verifying token.
     */
    public void verifyingToken() {
        setTokenInfo(Language.getString("login.verifyingLogin"));
        verifyToken.setEnabled(false);
    }
    
    /**
     * Set the result of the token verification (except the scopes).
     * 
     * @param valid
     * @param result 
     */
    public void tokenVerified(boolean valid, String result) {
        setTokenInfo(result);
        verifyToken.setEnabled(true);
        update();
    }
    
    private void setTokenInfo(String info) {
        tokenInfo.setText("<html><body style='width:170px'>"+info);
        pack();
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                pack();
            }
        });
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
