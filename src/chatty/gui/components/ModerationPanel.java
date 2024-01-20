
package chatty.gui.components;

import chatty.ChannelState;
import chatty.gui.GuiUtil;
import chatty.gui.components.settings.PresetsComboSetting;
import chatty.util.DateTime;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author tduva
 */
public class ModerationPanel extends JPanel {

    private final JCheckBox subonlyCheckbox = new JCheckBox("Subscribers-Only Chat");
    private final JCheckBox emoteonlyCheckbox = new JCheckBox("Emotes-Only Chat");
    private final JCheckBox followeronlyCheckbox = new JCheckBox("Followers-Only Chat");
    private final JCheckBox slowmodeCheckbox = new JCheckBox("Slow Mode");
    private final JCheckBox uniquechatCheckbox = new JCheckBox("Unique Chat");
    private final PresetsComboSetting<Long> followeronlyDuration;
    private final PresetsComboSetting<Long> slowmodeDuration;
    
    private boolean updating;
    
    public ModerationPanel(Window parent, Settings settings) {
        
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
        
        gbc = GuiUtil.makeGbc(1, 13, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(followeronlyDuration, gbc);
        
        gbc = GuiUtil.makeGbc(0, 14, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(slowmodeCheckbox, gbc);
        
        gbc = GuiUtil.makeGbc(1, 14, 1, 1, GridBagConstraints.WEST);
        modesPanel.add(slowmodeDuration, gbc);
        
        gbc = GuiUtil.makeGbc(0, 0, 1, 1);
        add(modesPanel, gbc);
        
        
        //===================
        // Command Listeners
        //===================
        
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
    
    public void updateState(ChannelState state) {
        updating = true;
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
    
    /**
     * For testing.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Settings settings = new Settings("", null);
            settings.addString("slowmodeDurations", "1\n2\n3");
            settings.addString("followeronlyDurations", "1\n2\n3");
            
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
