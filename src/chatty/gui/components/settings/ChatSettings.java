
package chatty.gui.components.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author tduva
 */
public class ChatSettings extends SettingsPanel {
    
    private static final String PAUSE_CHAT_INFO = "<html><body style='width:310px'>"
            + "Pausing the chat stops scrolling, only works when window already"
            + " filled with messages (and thus scrollbar is active). Read the "
            + "help for more information.";
    
    public ChatSettings(SettingsDialog d) {
        
        JPanel main = addTitledPanel("Chat Settings", 0, true);
        GridBagConstraints gbc;
        
        JPanel autoScrollPanel = new JPanel();
        autoScrollPanel.add(d.addSimpleBooleanSetting("autoScroll", "Scroll down after",
                "After the given number of seconds of not changing position of the scrollbar, automatically scroll down."));
        autoScrollPanel.add(d.addSimpleLongSetting("autoScrollTimeout", 3, true));
        autoScrollPanel.add(new JLabel("seconds of inactiviy"));

        gbc = d.makeGbc(0, 0, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(4,1,0,5);
        main.add(autoScrollPanel, gbc);
        
        
 
        gbc = d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST);
        main.add(new JLabel("Chat buffer size:"),
                gbc);
        
        gbc = d.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST);
        main.add(d.addSimpleLongSetting("bufferSize", 3, true),
                gbc);
        
        gbc = d.makeGbc(2, 4, 1, 1, GridBagConstraints.WEST);
        main.add(new JLabel("(too high values may lower performance)"),
                gbc);
        
        
        
        JPanel pauseChat = addTitledPanel("Pause Chat", 1);
        
        gbc = d.makeGbc(0, 0, 3, 1);
        gbc.insets.bottom += 1;
        pauseChat.add(new JLabel(PAUSE_CHAT_INFO),
                gbc);
        
        final JCheckBox pause = d.addSimpleBooleanSetting("pauseChatOnMouseMove",
                "Pause chat when moving the mouse over it",
                "Stop scrolling while moving the mouse over chat (only if the scrollbar is active)");
        /**
         * Select by default so loading the settings will trigger the
         * ItemListener to disable the other setting if set to false.
         */
        pause.setSelected(true);
        pauseChat.add(pause,
                d.makeGbc(0, 1, 3, 1, GridBagConstraints.WEST));
        
        
        final JCheckBox ctrl = d.addSimpleBooleanSetting("pauseChatOnMouseMoveCtrlRequired",
                "Require Ctrl being pressed to start pausing chat",
                "Requires you to have Ctrl pressed when moving the mouse over that to pause chat");
        gbc = d.makeGbc(0, 2, 3, 1, GridBagConstraints.WEST);
        gbc.insets.left += 10;
        gbc.insets.top -= 4;
        pauseChat.add(ctrl,
                gbc);
        
        /**
         * Enable/disable Ctrl Required setting based on whether pausing on
         * mouseover is enabled at all.
         */
        pause.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                ctrl.setEnabled(pause.isSelected());
            }
        });
        
        JPanel commandPanel = new JPanel(new GridBagLayout());
        
        commandPanel.add(new JLabel("Run command when clicking on user (holding Ctrl):"),
                d.makeGbc(0, 0, 1, 1));
        
        Map<String, String> commandChoices = new HashMap<>();
        commandChoices.put("", "Off");
        commandChoices.put("/timeout", "Timeout");
        commandChoices.put("/ban", "Ban");
        ComboStringSetting commandOnCtrlClick = d.addComboStringSetting("commandOnCtrlClick", 30, false, commandChoices);
        commandPanel.add(commandOnCtrlClick,
                d.makeGbc(1, 0, 1, 1));
        
        gbc = d.makeGbc(0, 3, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 0, 0, 0);
        pauseChat.add(commandPanel, gbc);
    }
    
}
