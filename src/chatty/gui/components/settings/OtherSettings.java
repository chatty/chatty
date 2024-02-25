
package chatty.gui.components.settings;

import chatty.Chatty;
import chatty.Chatty.PathType;
import chatty.gui.components.LinkLabel;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import static java.awt.GridBagConstraints.WEST;
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
        JPanel updates = addTitledPanel("Updates", 1);
        JPanel other = addTitledPanel("Other", 2);
        JPanel paths = addTitledPanel("Paths", 3);

        //------------------
        // Graphics settings
        //------------------
        graphics.add(new JLabel(INFO), d.makeGbc(0, 0, 2, 1));

        graphics.add(d.addSimpleBooleanSetting(
                "nod3d",
                "Disable Direct3D",
                ""),
                d.makeGbc(0, 1, 1, 1));

        graphics.add(d.addSimpleBooleanSetting(
                "noddraw",
                "Disable DirectDraw",
                ""),
                d.makeGbc(1, 1, 1, 1));
        
        SettingsUtil.addLabeledComponent(graphics, "uiScale", 0, 2, 1, GridBagConstraints.EAST,
                d.addComboLongSetting("uiScale", 0, 100, 125, 150, 175, 200));
        
        //--------
        // Updates
        //--------
        JLabel info = new JLabel(SettingConstants.HTML_PREFIX+"<em>Note:</em> You can manually check for updates via the "
                + "'Help' menu even if you have automatic checks disabled.");
        updates.add(info,
                d.makeGbc(0, 0, 1, 1));
        
        JCheckBox versionCheck = d.addSimpleBooleanSetting(
                "checkNewVersion",
                "Automatically check for new version",
                "Automatically check for a new version every few days and output a message if a new one is available.");
        updates.add(versionCheck,
                d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST));
        
        JCheckBox versionCheckBeta = d.addSimpleBooleanSetting(
                "checkNewBeta",
                "Include beta versions",
                "In addition to full releases, also inform about new betas.");
        updates.add(versionCheckBeta,
                d.makeGbcSub(0, 2, 1, 1, GridBagConstraints.WEST));
        
        versionCheckBeta.setEnabled(false);
        versionCheck.addItemListener(e -> {
            versionCheckBeta.setEnabled(versionCheck.isEnabled() && versionCheck.isSelected());
        });
        if (!Chatty.VERSION_CHECK_ENABLED) {
            versionCheck.setEnabled(false);
            versionCheck.setToolTipText("Feature disabled in this distributed version.");
        }
        
        //---------------
        // Other settings
        //---------------
        other.add(d.addSimpleBooleanSetting(
                "newsAutoRequest",
                "Check for important announcements",
                "Automatically checks for announcements about Chatty"),
                d.makeGbc(0, 5, 3, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting(
                "enableStatusWriter",
                "Write Stream Status:", ""),
                d.makeGbc(0, 6, 2, 1));
        
        other.add(d.addEditorStringSetting(
                "statusWriter", 20, true, "Write Stream Status:", true, INFO_WRITER),
                d.makeGbc(2, 6, 1, 1));
        
        SettingsUtil.addLabeledComponent(other, "titleAddition", 0, 8, 2, WEST,
                d.addSimpleStringSetting("titleAddition", 10, true));
        
        other.add(d.addSimpleBooleanSetting(
                "abSaveOnChange",
                "Save Addressbook immediately after changing entries",
                "Save immediately after updating addressbook (including changes via commands)"),
                d.makeGbc(0, 9, 3, 1, GridBagConstraints.WEST));
        
        JPanel pronouns = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JCheckBox pronouns1 = d.addSimpleBooleanSetting(
                "pronouns",
                "Display pronouns in User Dialog",
                "Click on a user in chat to open User Dialog, the pronoun (if available) will be shown in the title next to the username");
        pronouns.add(pronouns1);
        pronouns.add(new LinkLabel("(based on [url:https://pr.alejo.io pr.alejo.io])", d.getLinkLabelListener()));
        other.add(pronouns,
                SettingsDialog.makeGbc(0, 10, 3, 1, GridBagConstraints.WEST));
        
        JCheckBox pronouns2 = d.addSimpleBooleanSetting(
                "pronounsChat",
                "Display pronouns in chat (may not immediately show for all users)",
                "Will work best in chats with a small amount of users. May not show up on the first message of a user.");
        other.add(pronouns2,
                SettingsDialog.makeGbcSub(0, 11, 3, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(pronouns1, pronouns2);
        
        //--------------------------
        // Paths
        //--------------------------
        PathSetting cachePath = new PathSetting(d, Chatty.getDefaultPath(PathType.CACHE).toString());
        d.addStringSetting("cachePath", cachePath);
        SettingsUtil.addLabeledComponent(paths, "cachePath", 0, 0, 1, GridBagConstraints.NORTHEAST, cachePath, true);
        
        PathSetting imgPath = new PathSetting(d, Chatty.getDefaultPath(PathType.IMAGE).toString());
        d.addStringSetting("imgPath", imgPath);
        SettingsUtil.addLabeledComponent(paths, "imgPath", 0, 1, 1, GridBagConstraints.NORTHEAST, imgPath, true);
        
        PathSetting exportPath = new PathSetting(d, Chatty.getDefaultPath(PathType.EXPORT).toString());
        d.addStringSetting("exportPath", exportPath);
        SettingsUtil.addLabeledComponent(paths, "exportPath", 0, 2, 1, GridBagConstraints.NORTHEAST, exportPath, true);
    }
    
}
