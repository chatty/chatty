
package chatty.gui.components;

import chatty.Helper;
import chatty.User;
import chatty.User.BanMessage;
import chatty.User.Message;
import chatty.User.ModAction;
import chatty.User.SubMessage;
import chatty.User.TextMessage;
import chatty.gui.GuiUtil;
import chatty.gui.HtmlColors;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.UserContextMenu;
import chatty.gui.components.settings.Editor;
import chatty.gui.components.settings.GenericComboSetting;
import chatty.gui.components.settings.SettingsDialog;
import chatty.util.DateTime;
import chatty.util.DateTime.Formatting;
import chatty.util.StringUtil;
import chatty.util.api.ChannelInfo;
import chatty.util.settings.Settings;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 *
 * @author tduva
 */
public class UserInfo extends JDialog {
    
    private static final SimpleDateFormat TIMESTAMP = new SimpleDateFormat("[HH:mm:ss]");
    
    private static final String SINGLE_MESSAGE_CHECK = "Remove only selected message";
    
    public enum Action {
        NONE, TIMEOUT, MOD, UNMOD, COMMAND
    }
    
    private final JLabel firstSeen = new JLabel("");
    private final JLabel numberOfLines = new JLabel("");
    private final JTextArea lines = new JTextArea();
    private final JLabel colorInfo = new JLabel("Color: #123456");

    private final JButton modButton = new JButton("Mod");
    private final JButton unmodButton = new JButton("Unmod");
    private final HashMap<JButton, Integer> timeoutButtons = new HashMap<>();
    private final HashMap<JButton, String> commandButtons = new HashMap<>();
    private final JButton closeButton = new JButton("Close");
    private final JCheckBox singleMessage = new JCheckBox(SINGLE_MESSAGE_CHECK);
    private final BanReasons banReasons;
    
    private final JPanel buttonPane;
    private final JPanel buttonPane2;
    private final JPanel infoPane;
    private final JPanel infoPane2;

    private final ActionListener actionListener;
    
    private User currentUser;
    private String currentLocalUsername;
    private String currentMessageId;
    
    private float fontSize;
    
    private final JLabel createdAt = new JLabel("Loading..");
    private final JLabel followers = new JLabel();
    
    private final MainGui owner;
   
    public UserInfo(final MainGui owner, Settings settings,
            final ContextMenuListener contextMenuListener) {
        super(owner);
        this.owner = owner;
        banReasons = new BanReasons(this, settings);
        actionListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                if (getAction(e.getSource()) != Action.NONE) {
                    owner.getActionListener().actionPerformed(e);
                }
            }
        };
        modButton.addActionListener(actionListener);
        unmodButton.addActionListener(actionListener);
        closeButton.addActionListener(actionListener);

        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;

        buttonPane = new JPanel();
        gbc = makeGbc(0,0,3,1);
        gbc.insets = new Insets(2, 2, 0, 2);
        add(buttonPane,gbc);
        
        gbc = makeGbc(0,3,3,1);
        gbc.insets = new Insets(0, 6, 2, 2);
        gbc.anchor = GridBagConstraints.CENTER;
        singleMessage.setToolTipText("When doing a ban/timeout only remove a single message of that user [S to toggle]");
        //add(singleMessage, gbc);
        
        JComboBox<String> reasons = new JComboBox<>();
        reasons.addItem("-- Select Ban/Timeout Reason --");
        reasons.addItem("No CatBag posted");
        reasons.addItem("Custom Ban/Timeout Reason:");
        gbc = makeGbc(0,1,3,1);
        gbc.insets = new Insets(2, 10, 5, 10);
        //gbc.fill = GridBagConstraints.HORIZONTAL;
        add(banReasons, gbc);
        
        lines.setEditable(false);
        lines.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(lines);
        scrollPane.setPreferredSize(new Dimension(300,200));
        gbc = makeGbc(0,4,3,1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.9;
        add(scrollPane,gbc);
        
        buttonPane2 = new JPanel();
        gbc = makeGbc(0,5,3,1);
        gbc.insets = new Insets(0,0,0,0);
        add(buttonPane2, gbc);
        
        infoPane = new JPanel();
        gbc = makeGbc(0,6,3,1);
        add(infoPane,gbc);
        
        infoPane.add(numberOfLines);
        infoPane.add(firstSeen);
        infoPane.add(colorInfo);
        infoPane2 = new JPanel();
        LinkLabel l = new LinkLabel("[open:details More..]", new LinkLabelListener() {

            @Override
            public void linkClicked(String type, String ref) {
                toggleInfo();
            }
        });
        infoPane.add(l);
        
        
        infoPane2.add(createdAt);
        infoPane2.add(followers);
//        add(infoPane2, gbc);

        gbc = makeGbc(0,8,3,1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10,5,3,5);
        add(closeButton,gbc);

        addUserDefinedButtons("30,120,600,1800");
        
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
                JPopupMenu menu = new UserContextMenu(currentUser, contextMenuListener);
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
    
    public void setFontSize(float size) {
        if (size != fontSize) {
            GuiUtil.setFontSize(size, this);
            pack();
            finishDialog();
        }
        this.fontSize = size;
    }
    
    private void addUserDefinedButtons(String def) {
        updateButtonPane2Margin();
        boolean noKeyLabel = def.contains("nokeylabels");
        Pattern p = Pattern.compile("(?:([0-9]+)([smhd]?)|/?/?([^\\[,\\s]+))(?:\\[([^,\\s]+)\\])?");
        Matcher m = p.matcher(def);
        while (m.find()) {
            String match = m.group();
            if (match.startsWith("/")) {
                addCommandButton(m.group(3), match.startsWith("//"), m.group(4), noKeyLabel);
            } else {
                addTimeoutButton(m.group(1), m.group(2), m.group(4), noKeyLabel);
            }
        }
        updateButtonPane2Margin();
    }
    
    /**
     * Change the margin of the pane containing the lower row of buttons
     * depending on whether there are any buttons added to it.
     */
    private void updateButtonPane2Margin() {
        FlowLayout layout = (FlowLayout) buttonPane2.getLayout();
        if (buttonPane2.getComponentCount() == 0) {
            layout.setVgap(0);
        } else {
            layout.setVgap(4);
        }
    }
    
    private void addTimeoutButton(String timeString, String factor, String key,
            boolean noKeyLabel) {
        try {
            int time = Integer.parseInt(timeString);
            String label;
            if (!factor.isEmpty()) {
                time *= getFactor(factor);
                label = timeString+factor;
            } else {
                label = timeFormat(time);
            }
            JButton button = new JButton(label);
            buttonPane.add(button);
            timeoutButtons.put(button, time);
            button.addActionListener(actionListener);
            button.setToolTipText("Timeout for " + time + " seconds");
            addShortcut(key, button, false, noKeyLabel);
        } catch (NumberFormatException ex) {
            // Shouldn't happen, but if it does just don't add the button
        }
    }
    
    private void addCommandButton(String command, boolean secondMenu, String key,
            boolean noKeyLabel) {
        JButton button = new JButton(Helper.replaceUnderscoreWithSpace(command));
        if (secondMenu) {
            button.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            buttonPane2.add(button);
        } else {
            buttonPane.add(button);
        }
        commandButtons.put(button, command);
        button.addActionListener(actionListener);
        button.setToolTipText("Custom command");
        addShortcut(key, button, secondMenu, noKeyLabel);
    }
    
    
    
    private KeyStroke getKeyStroke(String key) {
        return KeyStroke.getKeyStroke(key != null ? key.replace("+", " ") : key);
    }
    
    private void addShortcut(String key, final JButton button, boolean smallButton,
            boolean noKeyLabel) {
        String label = null;
        if (key != null) {
            int index = key.indexOf("|");
            if (index > 0) {
                label = key.substring(index+1);
                key = key.substring(0, index);
            }
        }
        KeyStroke keyStroke = getKeyStroke(key);
        if (keyStroke == null) {
            return;
        }
        
        if (!noKeyLabel && label == null) {
            label = key;
        }
        if (label != null && !label.isEmpty()) {
            button.setText("<html>" + button.getText() + 
                "<span style='font-size:0.85em;font-weight:normal;color:gray;'>" + " ["+label+"]");
            if (smallButton) {
                button.setMargin(GuiUtil.SPECIAL_SMALL_BUTTON_INSETS);
            } else {
                button.setMargin(GuiUtil.SPECIAL_BUTTON_INSETS);
            }
        }
        button.setToolTipText(button.getToolTipText()+" [Shortcut: "+key+"]");
        
        
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, button);
        getRootPane().getActionMap().put(button, new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getFocusOwner().getClass() == JTextField.class) {
                    return;
                }
                button.doClick();
            }
        });
    }
    
    /**
     * Removes the action for this button. Since a new button will be created
     * everytime they are updated, the action will not be readded for this
     * button, so the shortcut will have no action to perform. It's easier to
     * remove from the action map based on the button than from the input map.
     * 
     * @param button 
     */
    private void clearShortcut(JButton button) {
        getRootPane().getActionMap().remove(button);
    }
    
    private int getFactor(String factorString) {
        switch (factorString) {
            case "s": return 1;
            case "m": return 60;
            case "h": return 60*60;
            case "d": return 60*60*24;
            default: return 1;
        }
    }
    
    /**
     * Remove current custom created buttons from the button panes.
     */
    private void removeUserDefinedButtons() {
        for (JButton button : commandButtons.keySet()) {
            buttonPane.remove(button);
            buttonPane2.remove(button);
            clearShortcut(button);
        }
        commandButtons.clear();
        for (JButton button : timeoutButtons.keySet()) {
            buttonPane.remove(button);
            clearShortcut(button);
        }
        timeoutButtons.clear();
    }
    
    /**
     * Sets the new custom buttons definition, which is just a single String
     * that is parsed accordingly. Removes current buttons, then adds the new
     * ones, resizing the dialog if necessary.
     * 
     * @param def 
     */
    public void setUserDefinedButtonsDef(String def) {
        removeUserDefinedButtons();
        addUserDefinedButtons(def);
        updateModButtons();
        GuiUtil.setFontSize(fontSize, this);
        // Pack because otherwise the dialog won't be sized correctly when
        // displaying it for the first time (not sure why)
        banReasons.addCustomInput();
        pack();
        finishDialog();
        banReasons.removeCustomInput();
    }
    
    private void finishDialog() {
        setMinimumSize(getPreferredSize());
    }
    
    private String timeFormat(int seconds) {
        if (seconds < 60) {
            return seconds+"s";
        }
        if (seconds < 60*60) {
            int minutes = seconds / 60;
            return String.format("%dm", (int) minutes);
        }
        if (seconds < 60*60*24*2+1) {
            return String.format("%dh", seconds / (60*60));
        }
        return String.format("%dd", seconds / (60*60*24));
    }
    
    /**
     * Check if the given Object is a timeout button.
     * 
     * Uses Object as parameter type because you get that from the ActionEvent
     * and it is more convenient to be able to just look it up like this.
     * 
     * @param button
     * @return 
     */
    @SuppressWarnings("element-type-mismatch")
    public boolean isTimeoutButton(Object button) {
        return timeoutButtons.containsKey(button);
    }
    
    /**
     * Get the timeout length saved for the given button. Uses Object as
     * parameter type because that is what you get from the event.
     * 
     * @param button The button to get the time for
     * @return The time, or null if none exists
     */
    @SuppressWarnings("element-type-mismatch")
    public Integer getTimeoutButtonTime(Object button) {
        return timeoutButtons.get(button);
    }
    
    /**
     * Check if the given Object is a command button.
     * 
     * Uses Object as parameter type because you get that from the ActionEvent
     * and it is more convenient to be able to just look it up like this.
     * 
     * @param button The object to check
     * @return true if the given object is a command button, false otherwise
     */
    @SuppressWarnings("element-type-mismatch")
    public boolean isCommandButton(Object button) {
        return commandButtons.containsKey(button);
    }
    
    /**
     * Get the command name for the given command button. Each command button
     * should have a command name associated with it, so this should not return
     * null unless the given object is not a command button.
     * 
     * Uses Object as parameter type because you get that from the ActionEvent
     * and it is more convenient to be able to just look it up like this.
     * 
     * @param button The button to get the command name for
     * @return The name of the command or null if none exists for the given
     * object
     */
    @SuppressWarnings("element-type-mismatch")
    public String getCommandButtonCommand(Object button) {
        return commandButtons.get(button);
    }
    
    /**
     * Get an action type for the given Object, which should be the source of an
     * ActionEvent that originated from this dialog.
     * 
     * @param source The source of an ActionEvent to check the type for
     * @return The Action associated with the given source, Action.NONE if none
     * could be found
     */
    public Action getAction(Object source) {
        if (source == modButton) {
            return Action.MOD;
        } else if (source == unmodButton) {
            return Action.UNMOD;
        } else if (isTimeoutButton(source)) {
            return Action.TIMEOUT;
        } else if (isCommandButton(source)) {
            return Action.COMMAND;
        }
        return Action.NONE;
    }
    
    private void setUser(User user, String messageId, String localUsername) {
        if (currentUser != user) {
            currentUser = user;
            if (infoAdded) {
                showInfo();
            }
            currentMessageId = messageId;
        } else if (messageId != null) {
            currentMessageId = messageId;
        }
        currentLocalUsername = localUsername;
        
        //infoPane.setComponentPopupMenu(new UserContextMenu(user, contextMenuListener));
        
        String categoriesString = "";
        Set<String> categories = user.getCategories();
        if (categories != null && !categories.isEmpty()) {
            categoriesString = categories.toString();
        }
        String displayNickInfo = user.hasDisplayNickSet() ? "" : "*";
        this.setTitle("User: "+user.toString()
                +(user.hasCustomNickSet() ? " ("+user.getDisplayNick()+")" : "")
                +(!user.hasRegularDisplayNick() ? " ("+user.getNick()+")" : "")
                +displayNickInfo
                +" / "+user.getChannel()
                +" "+categoriesString);
        lines.setText(null);
        lines.setText(makeLines());
        firstSeen.setText(" First seen: "+DateTime.format(user.getCreatedAt()));
        firstSeen.setToolTipText("First seen (this session only): "+DateTime.formatFullDatetime(user.getCreatedAt()));
        numberOfLines.setText(" Messages: "+user.getNumberOfMessages());
        singleMessage.setEnabled(currentMessageId != null);
        updateColor();
        updateModButtons();
        finishDialog();
    }
    
    private void updateModButtons() {
        if (currentUser == null) {
            return;
        }
        buttonPane.remove(modButton);
        buttonPane.remove(unmodButton);
        // Check that local user is the streamer here
        if (currentUser.getStream().equalsIgnoreCase(currentLocalUsername)) {
            if (currentUser.isModerator()) {
                buttonPane.add(unmodButton);
            } else {
                buttonPane.add(modButton);
            }
        }
    }
    
    private void updateColor() {
        Color color = currentUser.getColor();
        Color correctedColor = currentUser.getCorrectedColor();
        
        String colorNamed = HtmlColors.getNamedColorString(color);
        String correctedColorNamed = HtmlColors.getNamedColorString(correctedColor);
        
        String colorCode = HtmlColors.getColorString(color);
        String correctedColorCode = HtmlColors.getColorString(correctedColor);
        
        String colorText;
        String colorTooltipText;

        if (currentUser.hasCustomColor()) {
            Color plainColor = currentUser.getPlainColor();
            colorText = "Color: "+colorNamed+"**";
            colorTooltipText = "Custom Color: "+colorCode
                    +" (Original: "+HtmlColors.getNamedColorString(plainColor)+"/"
                    + HtmlColors.getColorString(plainColor)+")";
        } else if (currentUser.hasDefaultColor()) {
            colorText = "Color: "+colorNamed+"*";
            colorTooltipText = "Color: "+colorCode+" (default)";
        } else if (currentUser.hasCorrectedColor() && !colorCode.equals(correctedColorCode)) {
            colorText = "Color: "+correctedColorNamed+" ("+colorNamed+")";
            colorTooltipText = "Corrected Color: "+correctedColorCode
                    +" (Original: "+colorNamed+"/"+colorCode+")";
        } else {
            colorText = "Color: "+colorNamed;
            colorTooltipText = "Color: "+colorCode;
        }
        colorInfo.setText(colorText);
        colorInfo.setToolTipText(colorTooltipText);
    }
    
    private String makeLines() {
        StringBuilder b = new StringBuilder();
        if (currentUser.maxNumberOfLinesReached()) {
            b.append("<only last ");
            b.append(currentUser.getMaxNumberOfLines());
            b.append(" lines are saved>\n");
        }
        List<Message> messages = currentUser.getMessages();
        for (Message m : messages) {
            if (m.getType() == Message.MESSAGE) {
                TextMessage tm = (TextMessage)m;
                if (!StringUtil.isNullOrEmpty(currentMessageId)
                        && currentMessageId.equals(tm.id)) {
                    b.append(">");
                    singleMessage.setText(SINGLE_MESSAGE_CHECK+" ("+StringUtil.shortenTo(tm.text, 14)+")");
                }
                b.append(DateTime.format(m.getTime(), TIMESTAMP));
                if (tm.action) {
                    b.append("* ");
                } else {
                    b.append(" ");
                }
                b.append(tm.text);
                b.append("\n");
            }
            else if (m.getType() == Message.BAN) {
                BanMessage bm = (User.BanMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                if (bm.duration > 0) {
                    b.append("Timed out (").append(bm.duration).append("s)");
                }
                else {
                    b.append("Banned permanently");
                }
                if (bm.id != null) {
                    b.append(" (single message)");
                }
                if (bm.reason != null && !bm.reason.isEmpty()) {
                    b.append(" [").append(bm.reason).append("]");
                }
                
                b.append("\n");
            }
            else if (m.getType() == Message.SUB) {
                SubMessage sm = (SubMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append("$ ");
                if (sm.months == 1) {
                    b.append("[new sub] ");
                }
                else if (sm.months > 1) {
                    b.append("[").append(sm.months).append(" months sub] ");
                }
                b.append(sm.message);
                b.append("\n");
            }
            else if (m.getType() == Message.MOD_ACTION) {
                ModAction ma = (ModAction)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                b.append("ModAction: /");
                b.append(ma.commandAndParameters);
                b.append("\n");
            }
            else if (m.getType() == Message.AUTO_MOD_MESSAGE) {
                User.AutoModMessage ma = (User.AutoModMessage)m;
                b.append(DateTime.format(m.getTime(), TIMESTAMP)).append(">");
                b.append("Filtered by AutoMod: ").append(ma.message);
                b.append("\n");
            }
        }
        return b.toString();
    }
    
    public void show(Component owner, User user, String messageId, String localUsername) {
        banReasons.updateReasonsFromSettings();
        banReasons.reset();
        singleMessage.setSelected(false);
        setUser(user, messageId, localUsername);
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
            setUser(user, null, localUsername);
        }
    }
    
    public User getUser() {
        return currentUser;
    }
    
    public String getChannel() {
        return currentUser.getChannel();
    }
    
    public String getTargetMsgId() {
        if (singleMessage.isSelected()) {
            return currentMessageId;
        }
        return null;
    }
    
    public String getBanReason() {
        return banReasons.getSelectedReason();
    }
    
    private GridBagConstraints makeGbc(int x, int y, int w, int h) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.insets = new Insets(2,2,2,2);
        return gbc;
    }
    
    private boolean infoAdded;
    
    private void addInfo() {
        GridBagConstraints gbc = makeGbc(0, 7, 3, 1);
        gbc.insets = new Insets(-8, 5, 0, 5);
        add(infoPane2, gbc);
        revalidate();
        finishDialog();
        infoAdded = true;
    }
    
    private void removeInfo() {
        remove(infoPane2);
        revalidate();
        finishDialog();
        infoAdded = false;
    }
    
    private void toggleInfo() {
        if (infoAdded) {
            removeInfo();
        } else {
            showInfo();
        }
    }
    
    private void showInfo() {
        ChannelInfo requestedInfo = owner.getCachedChannelInfo(currentUser.nick);
        if (requestedInfo == null) {
            addInfo();
            createdAt.setText("Loading..");
            createdAt.setToolTipText(null);
            followers.setText(null);
        } else {
            setChannelInfo(requestedInfo);
        }
    }
    
    public void setChannelInfo(ChannelInfo info) {
        if (info == null || currentUser == null || !currentUser.nick.equals(info.name)) {
            return;
        }
        addInfo();
        createdAt.setText("Registered: "+DateTime.formatAccountAge(info.createdAt, Formatting.VERBOSE)+" ago");
        createdAt.setToolTipText("Account created: "+DateTime.formatFullDatetime(info.createdAt));
        
        followers.setText(" Followers: "+Helper.formatViewerCount(info.followers));
    }
    
    private static class BanReasons extends JPanel {
        
        private final GenericComboSetting<String> combo = new GenericComboSetting<>();
        private final Settings settings;
        
        private final Editor settingEditor;
        private final JTextField customReasonInput = new JTextField();
        
        private final GridBagConstraints customInputGbc;
        
        private String currentReasons;

        private final int[] codes = new int[]{KeyEvent.VK_1,
            KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4, KeyEvent.VK_5,
            KeyEvent.VK_6, KeyEvent.VK_7, KeyEvent.VK_8, KeyEvent.VK_9};
        
        public BanReasons(final Window parent, Settings settings) {
            this.settings = settings;
            
            setLayout(new GridBagLayout());

            JButton editButton = new JButton();
            editButton.setIcon(new ImageIcon(SettingsDialog.class.getResource("edit.png")));
            editButton.setMargin(new Insets(0,2,0,2));
            editButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    editReasons();
                }
            });
            
            settingEditor = new Editor(parent);
            settingEditor.setAllowEmpty(true);
            settingEditor.setAllowLinebreaks(true);
            
            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(combo, gbc);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.VERTICAL;
            add(editButton, gbc);
            customInputGbc = new GridBagConstraints();
            customInputGbc.gridx = 0;
            customInputGbc.gridy = 1;
            customInputGbc.gridwidth = 2;
            customInputGbc.fill = GridBagConstraints.HORIZONTAL;
            add(customReasonInput, customInputGbc);
            
            /**
             * Custom renderer to display the value of the items for the
             * selected item, instead of the label (hide the shortcut).
             */
            combo.setRenderer(new BasicComboBoxRenderer() {
            
                @Override
                public Component getListCellRendererComponent(JList list,
                        Object value, int index, boolean isSelected,
                        boolean hasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
                    
                    if (index == -1 && value != null) {
                        String text = ((GenericComboSetting.Entry<String>)value).value;
                        if (text != null && !text.isEmpty()) {
                            setText(text);
                        }
                    }
                    return this;
                }
            
            });
            
            /**
             * Add/remove custom reason input box if last item is selected.
             */
            combo.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (combo.getItemCount() > 1 && combo.getSelectedIndex() == combo.getItemCount() - 1) {
                        addCustomInput();
                    } else {
                        removeCustomInput();
                    }
                    
                }
            });
            
            /**
             * When the popup is open, allow shortcuts to select items.
             */
            combo.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    if (!combo.isPopupVisible()) {
                        return;
                    }
                    for (int i=0;i<codes.length;i++) {
                        if (codes[i] == e.getKeyCode()) {
                            int indexToSelect = i+1;
                            if (combo.getItemCount() > indexToSelect+1) {
                                combo.setPopupVisible(false);
                                combo.setSelectedIndex(indexToSelect);
                            }
                            e.consume();
                        }
                    }
                    if (e.getKeyCode() == KeyEvent.VK_C) {
                        combo.setSelectedIndex(combo.getItemCount() - 1);
                        e.consume();
                    }
                }
            });
            
            // TODO: Disable for now until it can be configured
//            combo.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("R"), combo);
            combo.getActionMap().put(combo, new AbstractAction() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (parent.getFocusOwner().getClass() == JTextField.class) {
                        return;
                    }
                    if (!combo.isPopupVisible() && combo.getSelectedIndex() != 0) {
                        combo.setSelectedIndex(0);
                    }
                    combo.requestFocusInWindow();
                    combo.setPopupVisible(!combo.isPopupVisible());
                }
            });
            customReasonInput.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent();
                }
            });
            customReasonInput.addKeyListener(new KeyAdapter() {
                
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        combo.requestFocusInWindow();
                        combo.setPopupVisible(true);
                        combo.setSelectedIndex(combo.getItemCount() - 2);
                    }
                }
            });
        }
        
        private void editReasons() {
            String currentReasons = settings.getString("banReasons");
            String editedReasons = settingEditor.showDialog(
                    "Edit Preset Ban Reasons (one per line):",
                    currentReasons,
                    null);
            if (editedReasons != null) {
                settings.setString("banReasons", editedReasons);
                updateReasonsFromSettings();
            }
        }
        
        public void updateReasonsFromSettings() {
            String reasons = settings.getString("banReasons");
            if (reasons.equals(currentReasons)) {
                return;
            }
            String[] split = reasons.split("\n");
            combo.removeAllItems();
//            combo.add("", "Select a Ban Reason (optional) [R]");
            combo.add("", "Select a Ban Reason (optional)");
            for (int i=0;i<split.length;i++) {
                if (!split[i].trim().isEmpty()) {
                    String shortcut = "-";
                    if (codes.length > i) {
                        shortcut = KeyEvent.getKeyText(codes[i]);
                    }
                    combo.add(split[i], "["+shortcut+"] "+split[i]);
                }
            }
            combo.add("[C] Non-preset reason:");
            currentReasons = reasons;
        }
        
        public String getSelectedReason() {
            int index = combo.getSelectedIndex();
            if (index == 0 || index == -1) {
                return "";
            }
            if (index == combo.getItemCount() - 1) {
                return customReasonInput.getText();
            }
            return combo.getSettingValue();
        }
        
        public void reset() {
            if (combo.getItemCount() > 0) {
                combo.setSelectedIndex(0);
            }
        }
        
        public void addCustomInput() {
            add(customReasonInput, customInputGbc);
            revalidate();
            customReasonInput.requestFocusInWindow();
            customReasonInput.setSelectionStart(0);
            customReasonInput.setSelectionEnd(customReasonInput.getText().length());
        }
        
        public void removeCustomInput() {
            remove(customReasonInput);
            revalidate();
        }
        
    }
    
}
