
package chatty.gui.components.userinfo;

import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.UserContextMenu;
import static chatty.gui.components.userinfo.Util.makeGbc;
import chatty.lang.Language;
import chatty.util.api.ChannelInfo;
import chatty.util.api.Follower;
import chatty.util.api.TwitchApi;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import chatty.util.settings.Settings;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import java.util.Set;
import javax.swing.*;

/**
 *
 * @author tduva
 */
public class UserInfo extends JDialog {

    private static final String SINGLE_MESSAGE_CHECK = "Remove only selected message";
    
    public enum Action {
        NONE, TIMEOUT, MOD, UNMOD, COMMAND
    }
    
    private final InfoPanel infoPanel = new InfoPanel(this);
    private final PastMessages pastMessages = new PastMessages();

    private final JButton closeButton = new JButton(Language.getString("dialog.button.close"));
    private final JCheckBox pinnedDialog = new JCheckBox(Language.getString("userDialog.setting.pin"));
    private final JCheckBox singleMessage = new JCheckBox(SINGLE_MESSAGE_CHECK);
    private final BanReasons banReasons;
    private final Buttons buttons;

    private final ActionListener actionListener;
    
    private User currentUser;
    private String currentLocalUsername;
    
    /**
     * The ID of the currently selected chat message (e.g. for use in timeouts),
     * or null if none is selected.
     */
    private String currentMsgId;
    
    /**
     * The ID of the currently selected AutoMod message (to approve/deny over
     * the API), or null if none is selected.
     */
    private String currentAutoModMsgId;
    
    private float fontSize;
    
    private final MainGui owner;
   
    public UserInfo(final MainGui owner, Settings settings,
            final ContextMenuListener contextMenuListener) {
        super(owner);
        GuiUtil.installEscapeCloseOperation(this);
        this.owner = owner;
        banReasons = new BanReasons(this, settings);
        
        buttons = new Buttons(this, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (settings.getBoolean("closeUserDialogOnAction")) {
                    setVisible(false);
                }
                CustomCommand command = getCommand(e.getSource());
                if (command == null) {
                    return;
                }
                
                owner.anonCustomCommand(getUser().getRoom(), command, makeParameters());
                owner.getActionListener().actionPerformed(e);
            }
        });
        
        actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        closeButton.addActionListener(actionListener);

        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        JPanel topPanel = new JPanel(new GridBagLayout());

        gbc = makeGbc(0,0,3,1);
        gbc.insets = new Insets(2, 2, 0, 2);
        topPanel.add(buttons.getPrimary(), gbc);
        
        gbc = makeGbc(0,3,3,1);
        gbc.insets = new Insets(0, 6, 2, 2);
        gbc.anchor = GridBagConstraints.CENTER;
        singleMessage.setToolTipText("When doing a ban/timeout only remove a single message of that user [S to toggle]");
        //add(singleMessage, gbc);

        gbc = makeGbc(2,1,1,1);
        gbc.insets = new Insets(2, 8, 2, 8);
        gbc.anchor = GridBagConstraints.EAST;
        pinnedDialog.setToolTipText("Pinned dialogs stay open on the same user until closed");
        topPanel.add(pinnedDialog, gbc);
        
        JComboBox<String> reasons = new JComboBox<>();
        reasons.addItem("-- Select Ban/Timeout Reason --");
        reasons.addItem("No CatBag posted");
        reasons.addItem("Custom Ban/Timeout Reason:");
        gbc = makeGbc(0, 1, 1, 1);
        // left = 2 (buttons gbc insets) + 5 (buttons layout hgap) + 1 indent
        gbc.insets = new Insets(2, 8, 5, 7);
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(banReasons, gbc);
        
        gbc = makeGbc(0, 0, 3, 1);
        gbc.insets = new Insets(0, 0, 0, 0);
        add(topPanel, gbc);
        
        JScrollPane scrollPane = new JScrollPane(pastMessages);
        scrollPane.setPreferredSize(new Dimension(300,200));
        gbc = makeGbc(0,4,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.9;
        add(scrollPane,gbc);
        
        gbc = makeGbc(0,5,3,1);
        gbc.insets = new Insets(0,0,0,0);
        add(buttons.getSecondary(), gbc);

        gbc = makeGbc(0,6,3,1);
        gbc.insets = new Insets(0, 0, 0, 0);
        add(infoPanel,gbc);
        
        gbc = makeGbc(0,8,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,5,3,5);
        add(closeButton,gbc);

        buttons.set("30,120,600,1800");
        
        finishDialog();
        
        
        // Open context menu
        this.getContentPane().addMouseListener(new MouseAdapter() {
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            private void showPopupMenu(MouseEvent e) {
                JPopupMenu menu = new UserContextMenu(currentUser, currentMsgId, currentAutoModMsgId, contextMenuListener);
                menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
      
//        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("S"), singleMessage);
//        getRootPane().getActionMap().put(singleMessage, new AbstractAction() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (getFocusOwner().getClass() == JTextField.class) {
//                    return;
//                }
//                if (singleMessage.isEnabled()) {
//                    singleMessage.setSelected(!singleMessage.isSelected());
//                }
//            }
//        });
    }
    
    public CustomCommand getCommand(Object object) {
        if (object instanceof JButton) {
            return buttons.getCommand((JButton)object);
        }
        return null;
    }
    
    private Parameters makeParameters() {
        User user = getUser();
        String nick = user.getName();
        String reason = getBanReason();
        if (!reason.isEmpty()) {
            reason = " " + reason;
        }
        Parameters parameters = Parameters.create(nick + reason);
        parameters.put("msg-id", getMsgId());
        parameters.put("target-msg-id", getTargetMsgId());
        parameters.put("automod-msg-id", getAutoModMsgId());
        parameters.put("followage", infoPanel.getFollowAge());
        parameters.put("user-id", user.getId());
        return parameters;
    }
    
    protected void updateButtons() {
        buttons.updateButtonForParameters(makeParameters());
    }
    
    public void setFontSize(float size) {
        if (size != fontSize) {
            GuiUtil.setFontSize(size, this);
            pack();
            finishDialog();
        }
        this.fontSize = size;
    }

    /**
     * Sets the new custom buttons definition, which is just a single String
     * that is parsed accordingly. Removes current buttons, then adds the new
     * ones, resizing the dialog if necessary.
     * 
     * @param def 
     */
    public void setUserDefinedButtonsDef(String def) {
        buttons.set(def);
        updateModButtons();
        GuiUtil.setFontSize(fontSize, this);
        // Pack because otherwise the dialog won't be sized correctly when
        // displaying it for the first time (not sure why)
        banReasons.addCustomInput();
        pack();
        finishDialog();
        banReasons.removeCustomInput();
    }
    
    protected void finishDialog() {
        setMinimumSize(getPreferredSize());
    }
    
    private void setUser(User user, String msgId, String autoModMsgId, String localUsername) {
        if (currentUser != user) {
            currentUser = user;
            currentMsgId = msgId;
            currentAutoModMsgId = autoModMsgId;
        }
        if (msgId != null || autoModMsgId != null) {
            currentMsgId = msgId;
            currentAutoModMsgId = autoModMsgId;
        }
        currentLocalUsername = localUsername;

        String categoriesString = "";
        Set<String> categories = user.getCategories();
        if (categories != null && !categories.isEmpty()) {
            categoriesString = categories.toString();
        }
        String displayNickInfo = user.hasDisplayNickSet() ? "" : "*";
        this.setTitle(Language.getString("userDialog.title")+" "+user.toString()
                +(user.hasCustomNickSet() ? " ("+user.getDisplayNick()+")" : "")
                +(!user.hasRegularDisplayNick() ? " ("+user.getName()+")" : "")
                +displayNickInfo
                +" / "+user.getRoom().getDisplayName()
                +" "+categoriesString);
        pastMessages.update(user, currentMsgId != null ? currentMsgId : currentAutoModMsgId);
        infoPanel.update(user);
        singleMessage.setEnabled(currentMsgId != null);
        updateModButtons();
        updateButtons();
        buttons.updateAutoModButtons(autoModMsgId);
        finishDialog();
    }
    
    public void updateModButtons() {
        if (currentUser == null) {
            return;
        }
        boolean localIsStreamer = currentUser.getStream() != null
                && currentUser.getStream().equalsIgnoreCase(currentLocalUsername);
        buttons.updateModButtons(localIsStreamer, currentUser.isModerator());
    }

    public void show(Component owner, User user, String msgId, String autoModMsgId, String localUsername) {
        if (user == currentUser && isVisible()) {
            GuiUtil.shake(this);
        }
        banReasons.updateReasonsFromSettings();
        banReasons.reset();
        singleMessage.setSelected(false);
        setUser(user, msgId, autoModMsgId, localUsername);
        closeButton.requestFocusInWindow();
        setVisible(true);
    }
    
    /**
     * Update sets the dialog to the given User, but only if the dialog is open
     * and it's the same User as the currently set User. This allows for chat
     * events that would need to update this to call this with any User.
     * 
     * @param user
     * @param localUsername 
     */
    public void update(User user, String localUsername) {
        if (currentUser == user && isVisible()) {
            setUser(user, null, null, localUsername);
        }
    }
    
    public User getUser() {
        return currentUser;
    }
    
    public String getChannel() {
        return currentUser.getChannel();
    }
    
    public String getMsgId() {
        return currentMsgId;
    }
    
    public String getTargetMsgId() {
        if (singleMessage.isSelected()) {
            return currentMsgId;
        }
        return null;
    }
    
    public String getAutoModMsgId() {
        return currentAutoModMsgId;
    }
    
    public String getBanReason() {
        return banReasons.getSelectedReason();
    }
    
    public boolean isPinned() {
        return pinnedDialog.isSelected();
    }
    
    public void setPinned(boolean isPinned) {
        pinnedDialog.setSelected(isPinned);
    }

    public void setChannelInfo(ChannelInfo info) {
        if (info == null || currentUser == null || !currentUser.getName().equals(info.name)) {
            return;
        }
        infoPanel.setChannelInfo(info);
    }
    
    protected ChannelInfo getChannelInfo() {
        return owner.getCachedChannelInfo(currentUser.getName(), currentUser.getId());
    }

    public void setFollowInfo(String stream, String user, Follower follow, TwitchApi.RequestResultCode result) {
        if (currentUser == null || !currentUser.getName().equals(user)
                || !Objects.equals(currentUser.getStream(), stream)) {
            return;
        }
        infoPanel.setFollowInfo(follow, result);
    }

    protected Follower getFollowInfo() {
        return owner.getSingleFollower(currentUser.getStream(), currentUser.getRoom().getStreamId(), currentUser.getName(), currentUser.getId());
    }
}
