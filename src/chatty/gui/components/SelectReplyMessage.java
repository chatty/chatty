
package chatty.gui.components;

import chatty.Room;
import chatty.User;
import chatty.User.TextMessage;
import chatty.gui.GuiUtil;
import chatty.gui.components.JListActionHelper.Action;
import chatty.lang.Language;
import chatty.util.ReplyManager;
import chatty.util.StringUtil;
import chatty.util.settings.Settings;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 *
 * @author tduva
 */
public class SelectReplyMessage {
    
    private static final String SETTING = "mentionReplyRestricted";
    private static final SelectReplyMessageResult DONT_SEND_RESULT = new SelectReplyMessageResult(SelectReplyMessageResult.Action.DONT_SEND);
    private static final SelectReplyMessageResult SEND_NORMALLY_RESULT = new SelectReplyMessageResult(SelectReplyMessageResult.Action.SEND_NORMALLY);
    
    public static Settings settings;
    
    /**
     * Show the messages of the given user. Returns the selected message as a
     * result when it should be sent as a reply, a result with send=false if it
     * should not be sent or null if it should be sent normally.
     * 
     * @param user
     * @return 
     */
    public static SelectReplyMessageResult show(User user) {
        Dialog dialog = new Dialog(user);
        return dialog.select();
    }
    
    private static class Dialog extends JDialog {
        
        private final JList<User.TextMessage> list;
        private final JCheckBox continueThread = new JCheckBox("Continue thread");
        
        private SelectReplyMessageResult result = DONT_SEND_RESULT;
        
        private Dialog(User user) {
            super(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow());
            setTitle("Send as a reply to a message of "+user);
            setLayout(new GridBagLayout());
            setResizable(false);
            
            List<User.TextMessage> msgs = new ArrayList<>();
            for (User.Message msg : user.getMessages()) {
                if (msg instanceof User.TextMessage) {
                    User.TextMessage m = (User.TextMessage)msg;
                    if (!StringUtil.isNullOrEmpty(m.id)) {
                        msgs.add(m);
                    }
                }
            }
            
            list = new JList<>(msgs.toArray(new User.TextMessage[msgs.size()]));
            list.setCellRenderer(new DefaultListCellRenderer() {
                
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list,
                        Object value,
                        int index,
                        boolean isSelected,
                        boolean cellHasFocus) {
                    
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    
                    if (value != null) {
                        User.TextMessage msg = (User.TextMessage) value;
                        setText(msg.text);
                    }
                    return this;
                }
                
            });
            list.setFixedCellWidth(400);
            list.setVisibleRowCount(14);
            list.addListSelectionListener(e -> {
                TextMessage m = list.getSelectedValue();
                if (m != null) {
                    boolean hasParentId = ReplyManager.getParentMsgId(m.id) != null;
                    continueThread.setSelected(hasParentId);
                    continueThread.setEnabled(hasParentId);
                }
            });
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JListActionHelper.install(list, (JListActionHelper.Action action, Point location, List<User.TextMessage> selected) -> {
                if (action == Action.ENTER || action == Action.DOUBLE_CLICK) {
                    confirm();
                }
                else if (action == Action.CTRL_ENTER) {
                    result = SEND_NORMALLY_RESULT;
                    setVisible(false);
                }
            }, false);

            JButton ok = new JButton("Send reply");
            ok.addActionListener(e -> {
                confirm();
            });
            JButton decline = new JButton("Send normally");
            decline.addActionListener(e -> {
                result = SEND_NORMALLY_RESULT;
                setVisible(false);
            });
            
            if (settings != null) {
                JCheckBox restrictSetting = new JCheckBox("Only show when message starts with @@<username>");
                restrictSetting.setSelected(settings.getBoolean(SETTING));
                restrictSetting.addItemListener(e -> {
                    settings.setBoolean(SETTING, restrictSetting.isSelected());
                });
                add(restrictSetting, GuiUtil.makeGbc(0, 2, 3, 1, GridBagConstraints.WEST));
            }
            JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
            cancel.setToolTipText("Don't send message at all");
            cancel.addActionListener(e -> {
                result = DONT_SEND_RESULT;
                setVisible(false);
            });

            add(new JScrollPane(list), GuiUtil.makeGbc(0, 0, 3, 1));
            add(continueThread, GuiUtil.makeGbc(0, 1, 3, 1, GridBagConstraints.WEST));
            add(new JLabel("<html><body style='width:380'>Tip: Press <kbd>Enter</kbd> to send reply, <kbd>Ctrl+Enter</kbd> to send normally, <kbd>ESC</kbd> to cancel"), GuiUtil.makeGbc(0, 3, 3, 1, GridBagConstraints.WEST));
            GridBagConstraints gbc = GuiUtil.makeGbc(0, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.5;
            add(ok, gbc);
            gbc = GuiUtil.makeGbc(1, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(decline, gbc);
            gbc = GuiUtil.makeGbc(2, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(cancel, gbc);
            pack();
            setModal(true);
            
            GuiUtil.installEscapeCloseOperation(this);
        }
        
        private void confirm() {
            User.TextMessage selected = list.getSelectedValue();
            if (selected != null) {
                result = new SelectReplyMessageResult(selected.id, selected.text);
                if (continueThread.isSelected()) {
                    String parentMsgId = ReplyManager.getParentMsgId(selected.id);
                    if (parentMsgId != null) {
                        // Overwrite result with parent msg-id if available
                        // Msg should be null for this, since the selected.text
                        // isn't the parent text
                        result = new SelectReplyMessageResult(parentMsgId, null);
                    }
                }
            }
            setVisible(false);
        }
        
        public SelectReplyMessageResult select() {
            if (list.getModel().getSize() == 0) {
                // No messages available, so immediatelly return
                return SEND_NORMALLY_RESULT;
            }
            list.setSelectedIndex(list.getModel().getSize() - 1);
            list.ensureIndexIsVisible(list.getSelectedIndex());
            setLocationRelativeTo(getParent());
            setVisible(true);
            return result;
        }
        
    }
    
    public static class SelectReplyMessageResult {
        
        public enum Action {
            REPLY, SEND_NORMALLY, DONT_SEND;
        }
        
        public final String atMsgId;
        public final String atMsg;
        public final Action action;
        
        public SelectReplyMessageResult(String atMsgId, String atMsg) {
            this.atMsgId = atMsgId;
            this.atMsg = atMsg;
            this.action = Action.REPLY;
        }
        
        public SelectReplyMessageResult(Action action) {
            this.atMsgId = null;
            this.atMsg = null;
            this.action = action;
        }
        
    }
    
    public static void main(String[] args) {
        User user = new User("sbc", Room.EMPTY);
        user.addMessage("abc", true, "1");
        user.addMessage("abc2", true, "2");
        user.addMessage("abc2", true, null);
        for (int i=0;i<30;i++) {
            user.addMessage("blah"+i, false, i+"msg-id");
        }
        System.out.println(SelectReplyMessage.show(user));
        System.exit(0);
    }
    
}
