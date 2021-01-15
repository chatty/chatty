
package chatty.gui.components.userinfo;

import chatty.Helper;
import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.UserContextMenu;
import static chatty.gui.components.userinfo.Util.makeGbc;
import chatty.lang.Language;
import chatty.util.MiscUtil;
import chatty.util.Pronouns;
import chatty.util.StringUtil;
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
import java.text.SimpleDateFormat;
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
    
    private final InfoPanel infoPanel;
    private final PastMessages pastMessages = new PastMessages();

    private final JButton closeButton = new JButton(Language.getString("dialog.button.close"));
    private final JCheckBox pinnedDialog = new JCheckBox(Language.getString("userDialog.setting.pin"));
    private final JButton notesButton = new JButton("Notes");
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
    
    private final UserInfoRequester requester;
    
    private final Settings settings;
    
    public UserInfo(final Window parent, UserInfoListener listener,
            UserInfoRequester requester,
            Settings settings,
            final ContextMenuListener contextMenuListener) {
        super(parent);
        this.requester = requester;
        this.settings = settings;
        GuiUtil.installEscapeCloseOperation(this);
        banReasons = new BanReasons(this, settings);
        
        buttons = new Buttons(this, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (settings.getBoolean("closeUserDialogOnAction")
                        && !isPinned()) {
                    dispose();
                }
                CustomCommand command = getCommand(e.getSource());
                if (command == null) {
                    return;
                }
                
                if (listener != null) {
                    listener.anonCustomCommand(getUser().getRoom(), command, makeParameters());
                }
            }
        });
        
        actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        closeButton.addActionListener(actionListener);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        //==========================
        // Top Panel
        //==========================
        JPanel topPanel = new JPanel(new GridBagLayout());

        gbc = makeGbc(0,0,4,1);
        gbc.insets = new Insets(2, 2, 0, 2);
        topPanel.add(buttons.getPrimary(), gbc);
        
        gbc = makeGbc(0,3,3,1);
        gbc.insets = new Insets(0, 6, 2, 2);
        gbc.anchor = GridBagConstraints.CENTER;
        singleMessage.setToolTipText("When doing a ban/timeout only remove a single message of that user [S to toggle]");
        //add(singleMessage, gbc);
        
        //--------------------------
        // Second row
        //--------------------------
        JComboBox<String> reasons = new JComboBox<>();
        reasons.addItem("-- Select Ban/Timeout Reason --");
        reasons.addItem("No CatBag posted");
        reasons.addItem("Custom Ban/Timeout Reason:");
        gbc = makeGbc(0, 1, 1, 1);
        // left = 2 (buttons gbc insets) + 5 (buttons layout hgap) + 1 indent
        gbc.insets = new Insets(2, 8, 5, 7);
        gbc.anchor = GridBagConstraints.WEST;
        topPanel.add(banReasons, gbc);

        gbc = makeGbc(2, 1, 1, 1);
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        notesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        notesButton.addActionListener(e -> {
            UserNotes.instance().showDialog(currentUser, this, user -> {
                if (user == currentUser) {
                    updateStuff(currentUser);
                }
            });
        });
        topPanel.add(notesButton, gbc);
        
        gbc = makeGbc(3, 1, 1, 1);
        gbc.insets = new Insets(2, 8, 2, 8);
        gbc.anchor = GridBagConstraints.EAST;
        pinnedDialog.setToolTipText(Language.getString("userDialog.setting.pin.tip"));
        topPanel.add(pinnedDialog, gbc);
        
        // Add to dialog
        gbc = makeGbc(0, 0, 3, 1);
        gbc.insets = new Insets(0, 0, 0, 0);
        add(topPanel, gbc);
        
        //==========================
        // Message log
        //==========================
        pastMessages.setRows(4);
        pastMessages.setPreferredSize(pastMessages.getPreferredSize());
        JScrollPane scrollPane = new JScrollPane(pastMessages);
        scrollPane.setPreferredSize(scrollPane.getPreferredSize());
        pastMessages.setRows(0);
        pastMessages.setPreferredSize(null);
        gbc = makeGbc(0,4,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.9;
        add(scrollPane, gbc);
        
        gbc = makeGbc(0,5,3,1);
        gbc.insets = new Insets(0,0,0,0);
        add(buttons.getSecondary(), gbc);

        infoPanel = new InfoPanel(this, new ContextMenuAdapter() {
            
            public void menuItemClicked(ActionEvent e) {
                switch (e.getActionCommand()) {
                    case "copyUserId":
                        MiscUtil.copyToClipboard(currentUser.getId());
                        break;
                    case "sendFollowAge":
                        listener.anonCustomCommand(getUser().getRoom(), InfoPanel.COMMAND_FOLLOW_AGE, makeParameters());
                        break;
                    case "copyFollowAge":
                        MiscUtil.copyToClipboard(InfoPanel.COMMAND_FOLLOW_AGE.replace(makeParameters()));
                        break;
                    case "sendAccountAge":
                        listener.anonCustomCommand(getUser().getRoom(), InfoPanel.COMMAND_ACCOUNT_AGE, makeParameters());
                        break;
                    case "copyAccountAge":
                        MiscUtil.copyToClipboard(InfoPanel.COMMAND_ACCOUNT_AGE.replace(makeParameters()));
                        break;
                    case "refresh":
                        infoPanel.setRefreshingFollowAge();
                        getFollowInfo(true);
                        break;
                    case "copyChannelInfo":
                        MiscUtil.copyToClipboard(infoPanel.getChannelInfoTooltipText());
                        break;
                }
            }
            
        });
        gbc = makeGbc(0,6,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 0, 0);
        add(infoPanel,gbc);
        
        gbc = makeGbc(0,8,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8,5,3,5);
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
                if (contextMenuListener != null) {
                    JPopupMenu menu = new UserContextMenu(currentUser, currentMsgId, currentAutoModMsgId, contextMenuListener);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
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
        Helper.addUserParameters(user, getMsgId(), getAutoModMsgId(), parameters);
        parameters.put("reason", reason);
        parameters.put("target-msg-id", getTargetMsgId());
        parameters.put("followage", infoPanel.getFollowAge());
        parameters.put("followdate", infoPanel.getFollowDate());
        parameters.put("accountage", infoPanel.getAccountAge());
        parameters.put("accountdate", infoPanel.getAccountDate());
        return parameters;
    }
    
    /**
     * This has to be called after any updates to the active User, button
     * settings or other dialog parameters that could affect buttons, in order
     * to hide/show/activate/deactive buttons.
     */
    protected void updateButtons() {
        if (currentUser == null) {
            return;
        }
        
        //------------
        // Parameters
        //------------
        buttons.updateButtonForParameters(makeParameters());
        
        //------------------
        // Mod/Unmod Button
        //------------------
        boolean localIsStreamer = currentUser.getStream() != null
                && currentUser.getStream().equalsIgnoreCase(currentLocalUsername);
        buttons.updateModButtons(localIsStreamer, currentUser.isModerator());
        
        //---------
        // AutoMod
        //---------
        buttons.updateAutoModButtons(currentAutoModMsgId);
        
        //--------
        // Finish
        //--------
        buttons.updateButtonRows();
    }
    
    public void setFontSize(float size) {
        if (size != fontSize) {
            GuiUtil.setFontSize(size, this);
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
        updateButtons();
        GuiUtil.setFontSize(fontSize, this);
        // Pack because otherwise the dialog won't be sized correctly when
        // displaying it for the first time (not sure why)
        banReasons.addCustomInput();
        /**
         * TODO: Want to remove pack() because it makes the window smaller when
         * manually sized large, however will have to test if removing it has
         * any negative effects (which maybe could be circumvented somehow, e.g.
         * revalidate() or something).
         */
//        pack();
        finishDialog();
        banReasons.removeCustomInput();
        banReasons.updateHotkey();
    }
    
    private void updateMessages() {
        pastMessages.update(currentUser, currentMsgId != null ? currentMsgId : currentAutoModMsgId);
    }
    
    public void setTimestampFormat(SimpleDateFormat timestampFormat) {
        pastMessages.setTimestampFormat(timestampFormat);
        updateMessages();
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
        
        updateStuff(user);
        
        updateMessages();
        infoPanel.update(user);
        singleMessage.setEnabled(currentMsgId != null);
        updateButtons();
        finishDialog();
    }
    
    /**
     * Update notes (that are user id based) and title (which can change due to
     * notes changing).
     * 
     * @param user 
     */
    private void updateStuff(User user) {
        updateTitle(user, null);
        if (settings.getBoolean("pronouns")) {
            Pronouns.instance().getUser((username, pronoun) -> {
                if (currentUser.getName().equals(username)) {
                    updateTitle(user, pronoun);
                }
            }, user.getName());
        }
        notesButton.setText(UserNotes.instance().hasNotes(user) ? "Notes*" : "Notes");
    }
    
    private void updateTitle(User user, String additionalInfo) {
        String categoriesString = "";
        Set<String> categories = user.getCategories();
        if (categories != null && !categories.isEmpty()) {
            categoriesString = categories.toString();
        }
        String displayNickInfo = user.hasDisplayNickSet() ? "" : "*";
        additionalInfo = StringUtil.append(UserNotes.instance().getChatNotes(user), ", ", additionalInfo);
        this.setTitle(Language.getString("userDialog.title")+" "+user.toString()
                +(user.hasCustomNickSet() ? " ("+user.getDisplayNick()+")" : "")
                +(!user.hasRegularDisplayNick() ? " ("+user.getName()+")" : "")
                +displayNickInfo
                +(additionalInfo != null ? " ("+additionalInfo+")" : "")
                +" / "+user.getRoom().getDisplayName()
                +" "+categoriesString);
    }

    public void show(Component owner, User user, String msgId, String autoModMsgId, String localUsername) {
        if (user == currentUser && isVisible()) {
            if (Objects.equals(currentMsgId, msgId)) {
                GuiUtil.shake(this, 2, 2);
            } else {
                GuiUtil.shake(this, 1, 1);
            }
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
    
    public String getMsg() {
        if (currentUser != null) {
            return currentUser.getMessageText(currentMsgId);
        }
        return null;
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

    public void setChannelInfo(String stream, ChannelInfo info) {
        if (currentUser == null || !currentUser.getName().equals(stream)) {
            return;
        }
        infoPanel.setChannelInfo(info);
        updateStuff(currentUser);
    }
    
    protected ChannelInfo getChannelInfo() {
        if (requester != null) {
            return requester.getCachedChannelInfo(currentUser.getName(), currentUser.getId());
        }
        return null;
    }

    public void setFollowInfo(String stream, String user, Follower follow, TwitchApi.RequestResultCode result) {
        if (currentUser == null || !currentUser.getName().equals(user)
                || !Objects.equals(currentUser.getStream(), stream)) {
            return;
        }
        infoPanel.setFollowInfo(follow, result);
    }

    protected Follower getFollowInfo(boolean refresh) {
        if (requester != null) {
            return requester.getSingleFollower(currentUser.getStream(),
                    currentUser.getRoom().getStreamId(),
                    currentUser.getName(),
                    currentUser.getId(),
                    refresh);
        }
        return null;
    }
}
