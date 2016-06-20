
package chatty.gui.components.settings;

import chatty.Chatty;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * More settings..
 * 
 * @author tduva
 */
public class OtherSettings extends SettingsPanel {
    
    private static final String INFO = "<html><body style='width:300px'>"
            + "The following settings disable Hardware Acceleration for Chatty, which "
            + "may resolve display issues. You have to restart Chatty for "
            + "changes to take effect.";
    
    private static final String INFO_WRITER = "<html><body style='width:300px'>"
            + "<p>Add one file per line to write Stream Status of a stream into: "
            + "</p><p><code>&nbsp;&lt;streamname&gt; &lt;filename&gt; [online|offline] &lt;content&gt;</code> "
            + "<p>You can define whether to write the info if the stream is online or offline (online if omitted). "
            + "The <code>&lt;content&gt;</code> can contain the following codes, which "
            + "will be replaced:"
            + "</p>"
            + "<ul>"
            + "<li><code>%title</code> - The stream title</li>"
            + "<li><code>%game</code> - The game being played</li>"
            + "<li><code>%viewers</code> - The viewercount</li>"
            + "<li><code>%followers</code> / <code>%subscribers</code> - The follower/subscriber count (only when Follower/Subscriber List is open)</li>"
            + "<li><code>%viewersf</code> / <code>%followersf</code> / <code>%subscribersf</code> - Formatted numbers (e.g. 1,342)</li>"
            + "</ul>"
            + "<p>Example:</p>"
            + "<p><code> joshimuz status.txt %title %game (%viewers)<br />"
            + " joshimuz status.txt offline Stream offline</code></p>";
    
    public OtherSettings(SettingsDialog d) {
        
        JPanel graphics = addTitledPanel("Graphic Settings", 0);
        JPanel other = addTitledPanel("Other", 1);
        
        GridBagConstraints gbc;

        // Graphics settings
        gbc = d.makeGbc(0, 0, 2, 1);
        graphics.add(new JLabel(INFO), gbc);
        
        gbc = d.makeGbc(0, 1, 1, 1);
        graphics.add(d.addSimpleBooleanSetting("nod3d", "Disable Direct3D", ""), gbc);
        
        gbc = d.makeGbc(1, 1, 1, 1);
        graphics.add(d.addSimpleBooleanSetting("noddraw", "Disable DirectDraw", ""), gbc);
        
        // Other settings
        gbc = d.makeGbc(0, 4, 3, 1, GridBagConstraints.WEST);
        JCheckBox versionCheck = d.addSimpleBooleanSetting("checkNewVersion", "Inform me about new versions",
                "Automatically check for a new version every few days and output a message "
                + "if a new one is available.");
        other.add(versionCheck, gbc);
        if (!Chatty.VERSION_CHECK_ENABLED) {
            versionCheck.setEnabled(false);
            versionCheck.setToolTipText("Feature disabled in this distributed version.");
        }
        
        gbc = d.makeGbc(0, 5, 2, 1);
        other.add(d.addSimpleBooleanSetting("enableStatusWriter", "Write Stream Status:", ""), gbc);
        
        gbc = d.makeGbc(2, 5, 1, 1);
        other.add(d.addEditorStringSetting("statusWriter", 20, true, "Write Stream Status:", true, INFO_WRITER), gbc);
        
        
        gbc = d.makeGbc(0, 6, 3, 1, GridBagConstraints.WEST);
        other.add(d.addSimpleBooleanSetting("autoUnhost", "Auto-Unhost when your stream goes live",
                "Automatically sends the /unhost command in your channel if your stream went live in the last 15 minutes"), gbc);
        
        gbc = d.makeGbc(0, 7, 1, 1, GridBagConstraints.WEST);
        other.add(new JLabel("Prepend to window title:"), gbc);
        
        gbc = d.makeGbc(1, 7, 2, 1, GridBagConstraints.WEST);
        other.add(d.addSimpleStringSetting("titleAddition", 10, true), gbc);
        
    }
    
}
