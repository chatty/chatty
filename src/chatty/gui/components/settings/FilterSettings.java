
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;
import javax.swing.text.Highlighter;

/**
 *
 * @author tduva
 */
public class FilterSettings extends SettingsPanel {
    
    private static final String INFO = HighlightSettings.INFO
            + "Examples:"
            + "<dl>"
            + "<dt><code>user:Nightbot replacement:H: reg:Hours Watched:\\s</code></dt>"
            + "<dd>Replace 'Hours Watched: ' with 'H:' (in messages by Nightbot).</dd>"
            + "<dt><code>reg:^Stream Schedule:.*</code></dt>"
            + "<dd>Filter messages starting with 'Stream Schedule', default replacement ('..').</dd>"
            + "<dt><code>replacement:Schedule reg:^Stream Schedule:.*</code></dt>"
            + "<dd>Same as previous, but replace with 'Schedule'.</dd>"
            + "</dl>"
            + "<em>Note:</em> This only filters the parts of the message that "
            + "are matched. To ignore a message entirely use the Ignore feature.";
    
    public FilterSettings(SettingsDialog d) {
        super(true);
        
        JPanel base = addTitledPanel(Language.getString("settings.section.filterMessages"), 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.insets.bottom -= 3;
        gbc.anchor = GridBagConstraints.WEST;
        base.add(d.addSimpleBooleanSetting("filterEnabled"), gbc);

        gbc = d.makeGbc(0,1,1,1);
        gbc.insets = new Insets(5,10,5,5);
        ListSelector items = d.addListSetting("filter", "Filter", 220, 250, true, true);
        items.setInfo(INFO);
        HighlighterTester tester = new HighlighterTester(d, false);
        tester.setLinkLabelListener(d.getLinkLabelListener());
        items.setEditor(tester);
        items.setDataFormatter(input -> input.trim());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
    }
    
}
