
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

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
        main.add(new JLabel("Chat buffer size (default):"),
                gbc);
        
        gbc = d.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST);
        main.add(d.addSimpleLongSetting("bufferSize", 3, true),
                gbc);
        
        BufferSizes bufferSizes = new BufferSizes(d);
        JButton bufferSizesButton = new JButton("Per tab buffer sizes");
        bufferSizesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        bufferSizesButton.addActionListener(e -> {
            bufferSizes.setLocationRelativeTo(ChatSettings.this);
            bufferSizes.setVisible(true);
        });
        
        gbc = d.makeGbc(2, 4, 1, 1, GridBagConstraints.WEST);
        main.add(bufferSizesButton,
                gbc);
        
        gbc = d.makeGbc(0, 5, 3, 1, GridBagConstraints.WEST);
        main.add(d.addSimpleBooleanSetting("inputHistoryMultirowRequireCtrl",
                "On a multirow inputbox require Ctrl to navigate input history",
                null), gbc);
        
        gbc = d.makeGbc(0, 6, 3, 1, GridBagConstraints.WEST);
        main.add(d.addSimpleBooleanSetting("showImageTooltips",
                "Show Emoticon/Badge tooltips",
                null), gbc);
        
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
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        Map<String, String> commandChoices = new HashMap<>();
        commandChoices.put("", "Off");
        commandChoices.put("/timeout", "Timeout");
        commandChoices.put("/ban", "Ban");
        commandChoices.put("/delete $$(msg-id)", "Delete message");
        ComboStringSetting commandOnCtrlClick = d.addComboStringSetting("commandOnCtrlClick", 100, true, commandChoices);
        gbc = d.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        commandPanel.add(commandOnCtrlClick, gbc);
        
        gbc = d.makeGbc(0, 2, 1, 1);
        commandPanel.add(new LinkLabel(SettingConstants.HTML_PREFIX
                + "When manually editing the command: If there is only a single "
                + "word, it will execute the command of that name (username "
                + "as parameter), otherwise you can use "
                + "[help-commands:replacements Custom Command Replacements] "
                + "such as <code>$1</code> (username) or <code>$(msg-id)</code>.",
                d.getLinkLabelListener()), gbc);
        
        gbc = d.makeGbc(0, 3, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(0, 0, 0, 0);
        pauseChat.add(commandPanel, gbc);
    }
    
    private static class BufferSizes extends JDialog {
        
        private static final String INFO = "<html><body style='width:240px;padding:5px;'>"
                + "Specify the buffer size per tab, specifying the lowercase name of the "
                + "tab (for a channel that includes the leading #)."
                + "<br /><br />This can "
                + "be useful if you want to have a low default buffer size, to "
                + "reduce memory usage, but want a huge scrollback on a few "
                + "select channels.";
        
        private BufferSizes(SettingsDialog d) {
            super(d);
            setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            setLayout(new GridBagLayout());
            setTitle("Per-tab buffer size (scrollback)");
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            add(new JLabel(INFO), gbc);
            
            gbc = d.makeGbc(0, 1, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            SimpleTableEditor<Long> editor = d.addLongMapSetting("bufferSizes", 300, 200);
            add(editor, gbc);
            
            gbc = d.makeGbc(0, 2, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            JButton closeButton = new JButton("Close");
            add(closeButton, gbc);
            closeButton.addActionListener(e -> {
                setVisible(false);
            });
            
            pack();
            setMinimumSize(getPreferredSize());
        }
        
    }
    
}
