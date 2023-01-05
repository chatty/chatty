
package chatty.util.commands;

import chatty.Helper;
import chatty.gui.Channels;
import chatty.gui.GuiUtil;
import chatty.gui.components.Channel;
import chatty.gui.components.ChannelEditBox;
import chatty.util.StringUtil;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Shows a dialog where the user can input text.
 * 
 * @author tduva
 */
public class Input implements Item {
    
    private final boolean isRequired;
    private final Item type;
    private final Item message;
    private final Item initial;
    
    public Input(Item type, Item message, Item initial, boolean isRequired) {
        this.type = type;
        this.message = message;
        this.initial = initial;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String messageValue = null;
        if (message != null) {
            messageValue = message.replace(parameters);
            if (messageValue == null) {
                return null;
            }
        }
        String initialValue = null;
        if (initial != null) {
            initialValue = initial.replace(parameters);
            if (initialValue == null) {
                return null;
            }
        }
        String typeValue = null;
        if (type != null) {
            typeValue = type.replace(parameters);
            if (typeValue == null) {
                return null;
            }
        }
        String result = new InputResult().getResult(typeValue, messageValue, initialValue, parameters);
        if (!Item.checkReq(isRequired, result)) {
            return null;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Input "+type+" '"+message+"' "+initial+"";
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, type, message, initial);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, type, message, initial);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Input other = (Input) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        if (!Objects.equals(this.message, other.message)) {
            return false;
        }
        if (!Objects.equals(this.initial, other.initial)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (this.isRequired ? 1 : 0);
        hash = 83 * hash + Objects.hashCode(this.type);
        hash = 83 * hash + Objects.hashCode(this.message);
        hash = 83 * hash + Objects.hashCode(this.initial);
        return hash;
    }
    
    private static class InputResult {
        
        private String result;
        
        public String getResult(String type, String msg, String preset, Parameters parameters) {
            GuiUtil.edtAndWait(() -> {
                Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
                if ("simple".equals(type)) {
                    result = JOptionPane.showInputDialog(activeWindow, msg, preset);
                }
                else {
                    result = InputDialog.showInputDialog(
                            activeWindow,
                            parameters.get("chan"),
                            msg,
                            preset);
                }
            }, "Custom Command Input");
            return result;
        }
        
    }
    
    /**
     * Dialog with an input box that is identical to the channel input box
     * (e.g. Emote Tab Completion). If the given channel can not be found, then
     * the default simple JOptionPane input dialog is used.
     */
    private static class InputDialog extends JDialog {
        
        public static String showInputDialog(Window owner, String chan, String msg, String preset) {
            chan = Helper.toChannel(chan);
            Channels channels = Channels.getInstance();
            Channel channel = channels.getExistingChannel(chan);
            if (channel == null) {
                channel = channels.getActiveChannel();
            }
            if (channel == null) {
                return JOptionPane.showInputDialog(owner, msg, preset);
            }
            InputDialog dialog = new InputDialog(owner, channel, msg, preset);
            // For the modal behaviour without preventing other interactions
            SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
            dialog.addWindowListener(new WindowAdapter() {
                
                @Override
                public void windowClosed(WindowEvent e) {
                    loop.exit();
                }
                
                @Override
                public void windowClosing(WindowEvent e) {
                    loop.exit();
                }

            });
            dialog.setVisible(true);
            loop.enter();
            return dialog.getResult();
        }
        
        private final ChannelEditBox input;
        private String result;
        
        public InputDialog(Window owner, Channel channel, String msg, String preset) {
            setTitle("Custom Command Input ("+channel.getChannel()+")");
            input = channel.createInputBox();
            input.setColumns(40);
            // To set the completion colors
            input.setForeground(input.getForeground());
            input.setBackground(input.getBackground());
            input.addActionListener(e -> {
                ok();
            });
            input.setText(preset);
            setLayout(new GridBagLayout());
            
            JButton okButton = new JButton("OK");
            JButton cancelButton = new JButton("Cancel");
            
            GridBagConstraints gbc = new GridBagConstraints();
            if (!StringUtil.isNullOrEmpty(msg)) {
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.insets = new Insets(5, 5, 0, 5);
                add(new JLabel(msg), gbc);
            }
            
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(input, gbc);
            
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(okButton, gbc);
            
            gbc = new GridBagConstraints();
            gbc.gridx = 1;
            gbc.gridy = 2;
            gbc.insets = new Insets(5, 5, 5, 5);
            add(cancelButton, gbc);
            
            okButton.addActionListener(e -> {
                ok();
            });
            cancelButton.addActionListener(e -> {
                dispose();
            });
            
            GuiUtil.addChangeListener(input.getDocument(), e -> {
                SwingUtilities.invokeLater(() -> {
                    pack();
                });
            });
            pack();
            setLocationRelativeTo(owner);
            setResizable(false);
            setAlwaysOnTop(true);
            GuiUtil.installEscapeCloseOperation(this);
        }
        
        private void ok() {
            result = input.getText();
            dispose();
        }
        
        public String getResult() {
            return result;
        }
        
    }
    
}
