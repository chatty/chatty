
package chatty.gui.components.userinfo;

import chatty.Room;
import chatty.User;
import chatty.gui.GuiUtil;
import chatty.gui.components.menus.TextSelectionMenu;
import chatty.lang.Language;
import chatty.util.Pronouns;
import chatty.util.StringUtil;
import chatty.util.api.TwitchApi;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Provides methods to get user notes and a dialog to edit them.
 * 
 * @author tduva
 */
public class UserNotes {
    
    private static final String SETTING_NOTES = "userNotes";
    private static final String SETTING_CHAT_NOTES = "userNotesChat";
    
    private static UserNotes instance;
    
    /**
     * Must only be called once.
     * 
     * @param api
     * @param settings 
     */
    public static void init(TwitchApi api, Settings settings) {
        instance = new UserNotes(api, settings);
    }
    
    public static UserNotes instance() {
        return instance;
    }
    
    private final TwitchApi api;
    private final Settings settings;
    
    private UserNotes(TwitchApi api, Settings settings) {
        this.api = api;
        this.settings = settings;
    }
    
    public void showDialog(User user, Window parent, Consumer<User> listener) {
        if (user.getId() == null) {
            JOptionPane.showMessageDialog(parent, "User ID not found.");
        }
        else {
            UserNotesDialog d = new UserNotesDialog(user, parent, get(SETTING_CHAT_NOTES, user), get(SETTING_NOTES, user));
            d.showDialog(e -> {
                set(SETTING_NOTES, user, d.getNotes());
                set(SETTING_CHAT_NOTES, user, d.getChatNotes());
                if (listener != null) {
                    listener.accept(user);
                }
            });
        }
    }
    
    public String getNotes(User user) {
        return get(SETTING_NOTES, user);
    }
    
    /**
     * Get the notes to be displayed in chat. May include the pronoun if the
     * setting is enabled.
     * 
     * @param user
     * @return 
     */
    public String getNotesForChat(User user) {
        String notes = get(SETTING_CHAT_NOTES, user);
        String pronouns = null;
        if (settings.getBoolean("pronounsChat")) {
            pronouns = Pronouns.instance().getUser2(user.getName());
        }
        return StringUtil.append(notes, ", ", pronouns);
    }
    
    /**
     * Get the notes that are displayed in chat.
     * 
     * @param user
     * @return 
     */
    public String getChatNotes(User user) {
        return get(SETTING_CHAT_NOTES, user);
    }
    
    public boolean hasNotes(User user) {
        return get(SETTING_NOTES, user) != null;
    }
    
    public String get(String setting, User user) {
        if (user.getId() == null) {
            return null;
        }
        String result = (String) settings.mapGet(setting, user.getId());
        if (StringUtil.isNullOrEmpty(result)) {
            return null;
        }
        return result;
    }
    
    public void set(String setting, User user, String newNotes) {
        if (user.getId() != null) {
            if (StringUtil.isNullOrEmpty(newNotes)) {
                settings.mapRemove(setting, user.getId());
            }
            else {
                settings.mapPut(setting, user.getId(), newNotes);
            }
        }
    }
    
    private static class UserNotesDialog extends JDialog {
        
        private final JTextField chatNotesTextField = new JTextField(20);
        private final JTextArea notesTextArea = new JTextArea();
        
        private final JButton saveButton = new JButton(Language.getString("dialog.button.save"));
        private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
        
        public UserNotesDialog(User user, Window parent, String chatNotes, String notes) {
            super(parent);
            setTitle("User Notes: "+user.toString());
            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            
            setLayout(new GridBagLayout());
            
            notesTextArea.setRows(6);
            notesTextArea.setColumns(40);
            TextSelectionMenu.install(chatNotesTextField);
            TextSelectionMenu.install(notesTextArea);
            
            add(new JLabel("Shown in chat messages after the username:"),
                    GuiUtil.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
            
            add(chatNotesTextField,
                    GuiUtil.makeGbc(0, 2, 2, 1, GridBagConstraints.WEST));
            
            add(new JLabel("Notes (just shown here):"),
                    GuiUtil.makeGbc(0, 3, 2, 1, GridBagConstraints.WEST));
            
            add(new JScrollPane(notesTextArea),
                    GuiUtil.makeGbc(0, 4, 2, 1, GridBagConstraints.WEST));
            
            add(new JLabel("All notes are stored locally only."),
                    GuiUtil.makeGbc(0, 5, 2, 1, GridBagConstraints.WEST));
            
            GridBagConstraints gbc = GuiUtil.makeGbc(0, 6, 1, 1, GridBagConstraints.WEST);
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(saveButton,
                    gbc);
            add(cancelButton,
                    GuiUtil.makeGbc(1, 6, 1, 1, GridBagConstraints.WEST));
            
            cancelButton.addActionListener(e -> {
                setVisible(false);
                dispose();
            });
            
            notesTextArea.setText(notes);
            chatNotesTextField.setText(chatNotes);
            
            pack();
            setLocationRelativeTo(parent);
        }
        
        public void showDialog(ActionListener listener) {
            saveButton.addActionListener(e -> {
                setVisible(false);
                dispose();
                listener.actionPerformed(e);
            });
            setVisible(true);
        }
        
        public String getChatNotes() {
            return chatNotesTextField.getText();
        }
        
        public String getNotes() {
            return notesTextArea.getText();
        }
        
    }
    
    public static void main(String[] args) {
        User user = new User("abc", Room.EMPTY);
        
        UserNotesDialog d = new UserNotesDialog(user, null, "chat notes", "regular notes");
        d.showDialog(e -> {
            System.out.println("Save");
        });
    }
    
}
