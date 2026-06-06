
package chatty.gui.components;

import chatty.ChannelState;
import chatty.gui.GuiUtil;
import chatty.gui.UrlOpener;
import chatty.gui.components.settings.PresetsComboSetting;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.api.PinnedMessage;
import chatty.util.settings.Settings;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class ModerationPanel extends JPanel {

    /** Pinned Message **/
    private final ExtendedTextPane pinnedMessageText = new ExtendedTextPane();
    private final JButton unpinButton = new JButton("Unpin");
    private final EditPinnedMessageDialog editPinnedMessageDialog;
    private final JButton editPinnedMessageButton = new JButton("Edit");
    private final JButton refreshPinnedMessageButton = new JButton("Refresh");
    
    /** Modes **/
    private final JCheckBox subonlyCheckbox = new JCheckBox("Subscribers-Only Chat");
    private final JCheckBox emoteonlyCheckbox = new JCheckBox("Emotes-Only Chat");
    private final JCheckBox followeronlyCheckbox = new JCheckBox("Followers-Only Chat");
    private final JCheckBox slowmodeCheckbox = new JCheckBox("Slow Mode");
    private final JCheckBox uniquechatCheckbox = new JCheckBox("Unique Chat");
    private final PresetsComboSetting<Long> followeronlyDuration;
    private final PresetsComboSetting<Long> slowmodeDuration;
    
    private boolean updating;
    
    public ModerationPanel(Window parent, Settings settings) {
        
        pinnedMessageText.setLinkListener(url -> UrlOpener.openUrlPrompt(parent, url));
        pinnedMessageText.setEditable(false);
        
        editPinnedMessageDialog = new EditPinnedMessageDialog(false, parent, settings);
        
        
        followeronlyDuration = new PresetsComboSetting<>(parent, settings, "followeronlyDurations", s -> {
            return DateTime.parseDurationSeconds(s);
        },
        v -> {
            return DateTime.duration(v * 1000, DateTime.Formatting.NO_ZERO_VALUES, DateTime.Formatting.VERBOSE);
        }, null, null, false);
        followeronlyDuration.init();
        
        slowmodeDuration = new PresetsComboSetting<>(parent, settings, "slowmodeDurations", s -> {
            return DateTime.parseDurationSeconds(s);
        },
        v -> {
            return DateTime.duration(v * 1000, DateTime.Formatting.NO_ZERO_VALUES, DateTime.Formatting.VERBOSE);
        }, null, null, false);
        slowmodeDuration.init();
        
        
        //========
        // Layout
        //========
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        JPanel pinnedMessagePanel = new JPanel(new GridBagLayout());
        pinnedMessagePanel.setBorder(BorderFactory.createTitledBorder("Pinned Message"));
        
        gbc = GuiUtil.makeGbc(0, 10, 3, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        JScrollPane scroll = new JScrollPane(pinnedMessageText);
        scroll.setPreferredSize(new Dimension(300,70));
        scroll.setMinimumSize(new Dimension(1,45));
        pinnedMessagePanel.add(scroll, gbc);
        
        unpinButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        gbc = GuiUtil.makeGbc(0, 11, 1, 1, GridBagConstraints.WEST);
        pinnedMessagePanel.add(unpinButton, gbc);
        
        refreshPinnedMessageButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        gbc = GuiUtil.makeGbc(1, 11, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 1;
        pinnedMessagePanel.add(refreshPinnedMessageButton, gbc);
        
        editPinnedMessageButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        gbc = GuiUtil.makeGbc(2, 11, 1, 1, GridBagConstraints.EAST);
        pinnedMessagePanel.add(editPinnedMessageButton, gbc);
        
        JPanel modesPanel = new JPanel(new GridBagLayout());
        modesPanel.setBorder(BorderFactory.createTitledBorder("Channel Modes"));
        
        gbc = GuiUtil.makeGbc(0, 10, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(subonlyCheckbox, gbc);
        
        gbc = GuiUtil.makeGbc(0, 11, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(emoteonlyCheckbox, gbc);
        
        gbc = GuiUtil.makeGbc(0, 12, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(uniquechatCheckbox, gbc);
        
        gbc = GuiUtil.makeGbc(0, 13, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(followeronlyCheckbox, gbc);
        
        gbc = GuiUtil.makeGbc(1, 13, 1, 1, GridBagConstraints.EAST);
        modesPanel.add(followeronlyDuration, gbc);
        
        gbc = GuiUtil.makeGbc(0, 14, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(slowmodeCheckbox, gbc);
        
        gbc = GuiUtil.makeGbc(1, 14, 1, 1, GridBagConstraints.EAST);
        modesPanel.add(slowmodeDuration, gbc);
        
        
        
        gbc = GuiUtil.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(pinnedMessagePanel, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(modesPanel, gbc);
        
        
        //===================
        // Command Listeners
        //===================
        
        unpinButton.addActionListener(e -> {
            sendCommand("/unpin "+currentPinnedMessage.msgId);
        });
        
        refreshPinnedMessageButton.addActionListener(e -> {
            sendCommand("/refresh pinnedMessage");
            refreshPinnedMessageButton.setEnabled(false);
            Timer timer = new Timer(2000, e2 -> refreshPinnedMessageButton.setEnabled(true));
            timer.setRepeats(false);
            timer.start();
        });
        
        editPinnedMessageButton.addActionListener(e -> {
            editPinnedMessageDialog.setLocationRelativeTo(this);
            boolean applyChanges = editPinnedMessageDialog.showDialog(currentPinnedMessage);
            if (applyChanges) {
                String msgText = editPinnedMessageDialog.msg.getText();
                long duration = editPinnedMessageDialog.getDuration();
                if (currentPinnedMessage == null || !currentPinnedMessage.messageText.equals(msgText)) {
                    // Must send new message
                    sendCommand(String.format("/pin -t %s %s",
                                              duration, msgText));
                }
                else {
                    // Update duration
                    sendCommand(String.format("/pin -ud %s %s",
                                              currentPinnedMessage.msgId, duration));
                }
            }
        });
        
        addListener(c -> {
            return subonlyCheckbox.isSelected() ? "/subscribers" : "/subscribersOff";
        }, subonlyCheckbox);
        
        addListener(c -> {
            return emoteonlyCheckbox.isSelected() ? "/emoteonly" : "/emoteonlyOff";
        }, emoteonlyCheckbox);
        
        addListener(c -> {
            return uniquechatCheckbox.isSelected() ? "/uniquechat" : "/uniquechatOff";
        }, uniquechatCheckbox);
        
        addListener(c -> {
            if (followeronlyCheckbox.isSelected()) {
                long duration = followeronlyDuration.getSelectedValue() / 60;
                if (duration > 0) {
                    return "/followers "+duration+"m";
                }
                return "/followers";
            }
            if (c == followeronlyCheckbox) {
                return "/followersOff";
            }
            return null;
        }, followeronlyCheckbox, followeronlyDuration);
        
        addListener(c -> {
            if (slowmodeCheckbox.isSelected()) {
                long duration = slowmodeDuration.getSelectedValue();
                return "/slow " + duration;
            }
            if (c == slowmodeCheckbox) {
                return "/slowOff";
            }
            return null;
        }, slowmodeCheckbox, slowmodeDuration);
        
    }
    
    private final Set<Consumer<String>> commandListeners = new HashSet<>();
    
    public void addCommandListener(Consumer<String> listener) {
        if (listener != null) {
            commandListeners.add(listener);
        }
    }
    
    private void addListener(Function<JComponent, String> getCommand, JComponent... components) {
        for (JComponent c : components) {
            if (c instanceof JCheckBox) {
                ((JCheckBox) c).addItemListener(e -> {
                    if (!updating) {
                        sendCommand(getCommand.apply(c));
                    }
                });
            }
            else if (c instanceof PresetsComboSetting) {
                ((PresetsComboSetting) c).addChangeListener(e -> {
                    if (!updating) {
                        sendCommand(getCommand.apply(c));
                    }
                });
            }
        }
    }
    
    private void sendCommand(String command) {
        if (command == null) {
            return;
        }
        for (Consumer<String> listener : commandListeners) {
            listener.accept(command);
        }
    }
    
    private PinnedMessage currentPinnedMessage;
    
    public void updateState(ChannelState state) {
        updating = true;
        
        currentPinnedMessage = state.pinnedMessage();
        pinnedMessageText.setText(makePinnedMessageText(currentPinnedMessage));
        unpinButton.setEnabled(currentPinnedMessage != null);
        
        subonlyCheckbox.setSelected(state.subMode());
        emoteonlyCheckbox.setSelected(state.emoteOnly());
        uniquechatCheckbox.setSelected(state.r9kMode());
        followeronlyCheckbox.setSelected(state.followersOnly() > -1);
        if (state.followersOnly() > -1) {
            followeronlyDuration.setSelectedValue((long) state.followersOnly() * 60);
        }
        slowmodeCheckbox.setSelected(state.slowMode() > 0);
        if (state.slowMode() > 0) {
            slowmodeDuration.setSelectedValue((long) state.slowMode());
        }
        updating = false;
    }
    
    private String makePinnedMessageText(PinnedMessage pinnedMessage) {
        if (pinnedMessage == null) {
            return "No pinned message";
        }
        return String.format("[Pinned by %s until %s]\n<%s> %s",
                             pinnedMessage.pinnedByUsername,
                             formatPinnedDate(pinnedMessage.endsAt),
                             pinnedMessage.senderUsername,
                             pinnedMessage.messageText);
    }
    
    private String formatPinnedDate(long timestamp) {
        if (timestamp == -1) {
            return "end of stream.";
        }
        return DateTime.format(timestamp, new SimpleDateFormat("HH:mm"));
    }
    
    public static String formatPinnedDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            return "end of stream";
        }
        return DateTime.duration(durationSeconds * 1000, DateTime.Formatting.NO_ZERO_VALUES);
    }
    
    public static class EditPinnedMessageDialog extends JDialog {
        
        private final JTextArea msg;
        private final PresetsComboSetting<Long> duration;
        private final JButton okButton = new JButton(Language.getString("dialog.button.ok"));
        private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
        
        private boolean applyChanges;
        
        private EditPinnedMessageDialog(boolean pinSelected, Window parent, Settings settings) {
            super(parent);
            setModal(true);
            setResizable(false);
            setTitle(pinSelected ? "Pin message?" : "Edit pinned message");
            
            msg = new JTextArea(3, 30);
            msg.setEditable(!pinSelected);
            duration = new PresetsComboSetting<>(parent, settings, "pinnedMsgDurations", s -> {
                                             return DateTime.parseDurationSeconds(s);
                                         },
                                                 v -> {
                                                     if (v <= 0) {
                                                         return "End of stream";
                                                     }
                                                     return DateTime.duration(v * 1000, DateTime.Formatting.NO_ZERO_VALUES, DateTime.Formatting.VERBOSE);
                                                 }, "End of stream", null, false);
            duration.init();
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            JLabel label = new JLabel("Changing the text will send a new message:");
            label.setLabelFor(msg);
            gbc = GuiUtil.makeGbc(0, 0, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            if (!pinSelected) {
                add(label, gbc);
            }
            
            gbc = GuiUtil.makeGbc(0, 1, 3, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(new JScrollPane(msg), gbc);
            
            JLabel labelDuration = new JLabel("Duration:");
            labelDuration.setLabelFor(duration);
            gbc = GuiUtil.makeGbc(0, 2, 1, 1);
            add(labelDuration, gbc);
            
            gbc = GuiUtil.makeGbc(1, 2, 1, 1);
            add(duration, gbc);
            
            gbc = GuiUtil.makeGbc(0, 10, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(okButton, gbc);
            
            gbc = GuiUtil.makeGbc(2, 10, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(cancelButton, gbc);
            
            okButton.addActionListener(e -> {
                applyChanges = true;
                setVisible(false);
            });
            cancelButton.addActionListener(e -> {
                setVisible(false);
            });
            
            pack();
        }
        
        private boolean showDialog(PinnedMessage pinnedMsg) {
            // Not all fields may be set for pinnedMsg
            if (pinnedMsg == null) {
                msg.setText(null);
                duration.setSelectedValue((long)-1);
            }
            else {
                msg.setText(pinnedMsg.messageText);
                if (pinnedMsg.endsAt <= 0) {
                    duration.setSelectedValue((long) -1);
                }
                else {
                    duration.setSelectedValue((((pinnedMsg.endsAt - System.currentTimeMillis()) / 1000) / 60) * 60);
                }
            }
            applyChanges = false;
            setVisible(true);
            // Possible OK button pressed
            return applyChanges;
        }
        
        public long getDuration() {
            // A negative duration such as -1 may be parsed as a positive number by the command, so always set to 0
            if (duration.getSelectedValue() < 0) {
                return 0;
            }
            return duration.getSelectedValue();
        }
        
    }
    
    public static long PIN_DIALOG_CANCEL = -12345;
    
    public static long showPinMessageDialog(Window parent, Settings settings, String text) {
        EditPinnedMessageDialog dialog = new EditPinnedMessageDialog(true, parent, settings);
        dialog.setLocationRelativeTo(parent);
        boolean apply = dialog.showDialog(new PinnedMessage(null, null, null, null, null, text, -1));
        if (apply) {
            return dialog.getDuration();
        }
        return PIN_DIALOG_CANCEL;
    }
    
    /**
     * For testing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Settings settings = new Settings("", null);
            settings.addString("slowmodeDurations", "1\n2\n3");
            settings.addString("followeronlyDurations", "1\n2\n3");
            settings.addString("pinnedMsgDurations", "5m\n10m\n15m\n20m\n30m");
            
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ModerationPanel panel = new ModerationPanel(frame, settings);
            panel.addCommandListener(s -> {
                System.out.println(s);
            });
            frame.add(panel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            Timer timer = new Timer(1000, e -> {
                ChannelState state = new ChannelState("test");
                state.setSubMode(true);
                state.setSlowMode(10);
                panel.updateState(state);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }
    
}
