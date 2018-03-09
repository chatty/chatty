
package chatty.gui.components.settings;

import chatty.gui.LaF;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author tduva
 */
public class LookSettings extends SettingsPanel {

    protected LookSettings(SettingsDialog d) {

        JPanel lafSettingsPanel = addTitledPanel(Language.getString("settings.section.lookandfeel"), 0);
        
        GridBagConstraints gbc;
        
        //=============
        // Look & Feel
        //=============
        
        Map<String, String> lafDef = new LinkedHashMap<>();
        lafDef.put("default", "Default");
        lafDef.put("system", "System");
        lafDef.put("hifi2", "HiFi Soft (Dark)");
        lafDef.put("hifi", "HiFi (Dark)");
        lafDef.put("noire", "Noire (Dark)");
        lafDef.put("mint", "Mint");
        lafDef.put("graphite", "Graphite");
        lafDef.put("aero", "Aero");
        lafDef.put("fast", "Fast");
        lafDef.put("luna", "Luna");
        ComboStringSetting laf = new ComboStringSetting(lafDef);
        d.addStringSetting("laf", laf);
        
        Map<String, String> themeDef = new LinkedHashMap<>();
        themeDef.put("Default", Language.getString("settings.laf.option.defaultFont"));
        themeDef.put("Small-Font", Language.getString("settings.laf.option.smallFont"));
        themeDef.put("Large-Font", Language.getString("settings.laf.option.largeFont"));
        themeDef.put("Giant-Font", Language.getString("settings.laf.option.giantFont"));
        ComboStringSetting theme = new ComboStringSetting(themeDef);
        d.addStringSetting("lafTheme", theme);
        
        laf.addActionListener(e -> {
            String selected = laf.getSettingValue();
            theme.setEnabled(!selected.equals("default") && !selected.equals("system"));
        });
        
        JButton lafPreviewButton = new JButton("Preview");
        lafPreviewButton.addActionListener(e -> {
            LaF.setLookAndFeel(laf.getSettingValue(), theme.getSettingValue());
            LaF.updateLookAndFeel();
            d.pack();
        });

        gbc = d.makeGbc(0, 0, 1, 1);
        lafSettingsPanel.add(new JLabel(Language.getString("settings.laf.lookandfeel")), gbc);
        
        gbc = d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST);
        lafSettingsPanel.add(laf, gbc);
        
        gbc = d.makeGbc(2, 0, 1, 1);
        lafSettingsPanel.add(new JLabel(Language.getString("settings.laf.font")), gbc);
        
        gbc = d.makeGbc(3, 0, 1, 1);
        lafSettingsPanel.add(theme, gbc);
        
        gbc = d.makeGbc(4, 0, 1, 1);
        lafSettingsPanel.add(lafPreviewButton, gbc);
        
        gbc = d.makeGbc(0, 1, 5, 1);
        gbc.insets = new Insets(0, 5, 7, 5);
        lafSettingsPanel.add(new JLabel(Language.getString("settings.laf.restartRequired")), gbc);
        
        JLabel info = new JLabel(SettingConstants.HTML_PREFIX
                + Language.getString("settings.laf.info")
                + "<br /><br />"
                + Language.getString("settings.laf.info2"));
        gbc = d.makeGbc(0, 2, 5, 1);
        lafSettingsPanel.add(info, gbc);
        
    }
    
}
