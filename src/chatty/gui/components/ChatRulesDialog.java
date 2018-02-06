
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.MainGui;
import chatty.lang.Language;
import chatty.util.api.ChatInfo;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author tduva
 */
public class ChatRulesDialog extends JDialog implements Channel.OnceOffEditListener {
    
    private final JTextArea rulesText = new JTextArea();
    private final JCheckBox autoShowCheckbox;
    private final MainGui g;
    private String currentRoom;
    private boolean autoShow;
    
    public ChatRulesDialog(MainGui parent) {
        super(parent);
        this.g = parent;

        rulesText.setEditable(false);
        rulesText.setLineWrap(true);
        rulesText.setWrapStyleWord(true);
        rulesText.setBorder(BorderFactory.createEmptyBorder(6, 5, 10, 4));
        
        JButton closeButton = new JButton(Language.getString("dialog.button.close"));
        closeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        autoShowCheckbox = new JCheckBox(Language.getString("chatRules.setting.autoShow"));
        autoShowCheckbox.setToolTipText(Language.getString("chatRules.setting.autoShow.tip"));
        autoShowCheckbox.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                g.getSettings().setBoolean("rulesAutoShow", autoShowCheckbox.isSelected());
            }
        });
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc;
        
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new JScrollPane(rulesText), gbc);
        
        gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.insets = new Insets(2,3,4,2);
        add(autoShowCheckbox, gbc);
        
        gbc = new GridBagConstraints();
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(closeButton, gbc);
        
        setMinimumSize(new Dimension(400,80));
        setMaximumSize(new Dimension(600,800));
    }
    
    public void showRules(String channel) {
        if (!Helper.isRegularChannel(channel)) {
            g.printSystem("No valid channel.");
            return;
        }
        String room = Helper.toStream(channel);
        setRoom(room);
        g.getChatInfo(room);
        rulesText.setText("Loading..");
        showDialog();
    }
    
    private void showDialog() {
        autoShowCheckbox.setSelected(g.getSettings().getBoolean("rulesAutoShow"));
        
        setAutoRequestFocus(false);
        setFocusableWindowState(false);
        // Make focusable after showing the dialog, so that it can be focused
        // by the user, but doesn't steal focus from the user when it opens.
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                setFocusableWindowState(true);
            }
        });
        setVisible(true);
        packAndPosition();
        
        // Do again since it otherwise doesn't properly work if dialog isn't
        // already open
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                packAndPosition();
            }
        });
    }
    
    private void packAndPosition() {
        pack();
        setLocationRelativeTo(g);
    }
    
    private void setRoom(String room) {
        currentRoom = room;
        setTitle(Language.getString("chatRules.title", room));
    }
    
    public void setChatInfo(ChatInfo info) {
        if (info == null || !info.room.equals(currentRoom)) {
            return;
        }
        List<String> rules = info.rules;
        if (!autoShow && !isVisible()) {
            return;
        }
        if (rules == null) {
            if (!isVisible()) {
                // If dialog not yet visible, don't show it if no rules
                return;
            }
            rulesText.setText(Language.getString("chatRules.noRules"));
        }
        else {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < rules.size(); i++) {
                String rule = rules.get(i);
                if (rules.size() > 1 && !rule.matches("^\\s*(-|\\d\\.|\\*).*") && !rule.trim().isEmpty()) {
                    b.append("- ");
                }
                b.append(rule);
                if (i != rules.size() - 1) {
                    b.append("\n");
                }
            }
            rulesText.setText(b.toString());
            setAsShown(info.room);
        }
        showDialog();
    }

    private void autoShow(String channel) {
        if (!Helper.isRegularChannel(channel)) {
            return;
        }
        String room = Helper.toStream(channel);
        if (autoShowEnabled() && !alreadyShown(room)) {
            setRoom(room);
            autoShow = true;
            g.getChatInfo(room);
        }
    }
    
    private boolean alreadyShown(String room) {
        return g.getSettings().listContains("rulesShown", room);
    }
    
    private void setAsShown(String room) {
        g.getSettings().setAdd("rulesShown", room);
    }
    
    private boolean autoShowEnabled() {
        return g.getSettings().getBoolean("rulesAutoShow");
    }

    @Override
    public void edited(String channel) {
        autoShow(channel);
    }
    
}
