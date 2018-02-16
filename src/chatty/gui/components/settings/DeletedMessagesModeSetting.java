
package chatty.gui.components.settings;

import chatty.lang.Language;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Combination of mode selection for deleted messages, as well as for the
 * "Strike through, shorten" selection the number of max. characters.
 * 
 * @author tduva
 */
public class DeletedMessagesModeSetting extends JPanel {

    private static final String OPTION_DELETE = Language.getString("settings.option.deletedMessagesMode.delete");
    private static final String OPTION_STRIKE_THROUGH = Language.getString("settings.option.deletedMessagesMode.strikeThrough");
    private static final String OPTION_STRIKE_THROUGH_SHORTEN = Language.getString("settings.option.deletedMessagesMode.strikeThroughShorten");
    
    private final ComboStringSetting combo;
    private final SimpleLongSetting maxLength = new SimpleLongSetting(3, true);
    private final JLabel maxLengthLabel = new JLabel(" ("+Language.getString("settings.deletedMessages.max")+" ");
    private final JLabel maxLengthLabel2 = new JLabel(Language.getString("settings.deletedMessages.characters")+")");

    public DeletedMessagesModeSetting(SettingsDialog d) {
        Map<String,String> options = new LinkedHashMap<>();
        options.put("delete", OPTION_DELETE);
        options.put("keep", OPTION_STRIKE_THROUGH);
        options.put("keepShortened", OPTION_STRIKE_THROUGH_SHORTEN);
        combo = new ComboStringSetting(options);
        combo.setEditable(false);
        d.addStringSetting("deletedMessagesMode", combo);
        
        d.addLongSetting("deletedMessagesMaxLength", maxLength);
        combo.addItemListener(new MyItemListener());
        
        add(combo);
        add(maxLengthLabel);
        add(maxLength);
        add(maxLengthLabel2);
        
        update();
    }
    
    /**
     * Activate/Deactivate the max character input.
     */
    private void update() {
        String selected = combo.getSettingValue();
        boolean shorten = selected.equals("keepShortened");
        maxLength.setEnabled(shorten);
        maxLengthLabel.setEnabled(shorten);
        maxLengthLabel2.setEnabled(shorten);
    }
    
    private class MyItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            update();
        }
        
    }
    
}
