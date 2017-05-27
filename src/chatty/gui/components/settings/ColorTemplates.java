
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Provides presets/templates for the given ColorSetting objects and saves it in
 * the specified setting. Colors are saved based on the order of the
 * ColorSetting objects, so changing the order would break things, although
 * adding new ColorSetting objects is fine.
 * 
 * Colors are stored as String objects, which also allows for more readable
 * color names like "Black" or "BlueViolet".
 *
 * @author tduva
 */
public class ColorTemplates extends JPanel {
    
    private final List<ColorSetting> colorSettings = new ArrayList<>();
    private final List<Preset> hardcodedPresets = new ArrayList<>();
    private final List<Preset> userPresets = new ArrayList<>();
    
    private final JButton saveButton = new JButton("Save");
    private final JButton saveAsButton = new JButton("Save as..");
    private final JButton removeButton = new JButton("Delete");
    
    private final GenericComboSetting<Preset> selection = new GenericComboSetting<>();
    
    private final Settings settings;
    private final String settingName;
    
    public ColorTemplates(Settings settings, String settingName, ColorSetting... values) {
        colorSettings.addAll(Arrays.asList(values));
        
        for (ColorSetting c : colorSettings) {
            c.addListener(() -> {
                update();
            });
        }
        
        this.settings = settings;
        this.settingName = settingName;
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        Insets insets = new Insets(2, 2, 2, 2);
        
        gbc = GuiUtil.makeGbc(0, 0, 3, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.insets = insets;
        add(selection, gbc);
        
        gbc = GuiUtil.makeGbc(0, 1, 1, 1);
        gbc.insets = insets;
        add(saveButton, gbc);
        
        gbc = GuiUtil.makeGbc(1, 1, 1, 1);
        gbc.insets = insets;
        add(saveAsButton, gbc);
        
        gbc = GuiUtil.makeGbc(2, 1, 1, 1);
        gbc.insets = insets;
        add(removeButton, gbc);
        
        selection.addItemListener(e -> {
            boolean hardcoded = hardcodedPresets.contains(selection.getSettingValue());
            if (selection.getSettingValue() == null) {
                hardcoded = true;
            }
            saveButton.setEnabled(!hardcoded);
            removeButton.setEnabled(!hardcoded);
        });
        
        selection.addActionListener(e -> {
            loadSelectedPreset();
        });
        
        saveButton.addActionListener(e -> {
            overwriteCurrent();
        });
        
        saveAsButton.addActionListener(e -> {
            addCurrentAsPreset();
        });
        
        removeButton.addActionListener(e -> {
            removeSelectedPreset();
        });
        
        loadFromSettings();
        init();
    }
    
    /**
     * Select default preset.
     */
    public void selectDefault() {
        selection.setSelectedIndex(0);
    }
    
    /**
     * Initialize presets lists after adding hardcoded presets.
     */
    public void init() {
        selection.clear();
        selection.add((Preset)null, "-- Color Presets --");
        for (Preset p : userPresets) {
            selection.add(p, p.name);
        }
        for (Preset p : hardcodedPresets) {
            selection.add(p, p.name+"*");
        }
    }
    
    /**
     * Update Save button state.
     */
    private void update() {
        //System.out.println("update");
        Preset p = selection.getSettingValue();
        if (p != null && !hardcodedPresets.contains(p)) {
            saveButton.setEnabled(!currentEqualTo(p));
        }
    }
    
    /**
     * Check if the currently selected colors are equal to the given Preset.
     * 
     * Only compares color names, so e.g. "Black" and "#000000" count as
     * different, although that should usually not be an issue, since colors are
     * chosen through the ColorSetting, which always uses the same names. Still,
     * it should only be used for stuff like disabling the Save button.
     * 
     * @param p
     * @return 
     */
    private boolean currentEqualTo(Preset p) {
        for (int i = 0; i < colorSettings.size(); i++) {
            ColorSetting s = colorSettings.get(i);
            if (p.colors.size() <= i) {
                return false;
            }
            if (!p.colors.get(i).equalsIgnoreCase(s.getSettingValue())) {
                return false;
            }
        }
        return true;
    }
    
    private void loadSelectedPreset() {
        //System.out.println("loadSelected");
        Preset p = selection.getSettingValue();
        if (p != null) {
            for (int i = 0; i < colorSettings.size(); i++) {
                if (p.colors.size() > i) {
                    colorSettings.get(i).setSettingValue(p.colors.get(i));
                }
            }
        }
    }
    
    private void addCurrentAsPreset() {
        String name = JOptionPane.showInputDialog(this,
                "Enter name to save current colors:");
        if (name != null) {
            Preset p = new Preset(name, getCurrentColors());
            userPresets.add(p);
            int insertPos = userPresets.size();
            selection.insert(p, p.name, insertPos);
            selection.setSettingValue(p);
            saveToSettings();
        }
    }
    
    /**
     * Save button action, overwriting the currently selected Preset. This
     * should only be done for user presets, for others the Save button should
     * be disabled.
     * 
     * Note that changing the selection also updates the colors, and thus also
     * runs update() to change the state of the Save button accordingly.
     */
    private void overwriteCurrent() {
        Preset current = selection.getSettingValue();
        Preset p = new Preset(current.name, getCurrentColors());
        int pos = userPresets.indexOf(current);
        userPresets.set(pos, p);
        selection.replace(current, p);
        selection.setSettingValue(p);
        saveToSettings();
    }
    
    /**
     * Gets list of colors currently in the ColorSetting objects.
     * 
     * @return 
     */
    private List<String> getCurrentColors() {
        List<String> colors = new ArrayList<>();
        for (ColorSetting s : colorSettings) {
            colors.add(s.getSettingValue());
        }
        return colors;
    }
    
    private void removeSelectedPreset() {
        Preset p = selection.getSettingValue();
        userPresets.remove(p);
        selection.remove(p);
        saveToSettings();
    }
    
    public void addPreset(String name, String... values) {
        if (values.length < colorSettings.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Preset p = new Preset(name, Arrays.asList(values));
        hardcodedPresets.add(p);
    }
    
    private void saveToSettings() {
        List<List<String>> result = new ArrayList<>();
        for (Preset p : userPresets) {
            result.add(p.toList());
        }
        settings.putList(settingName, result);
    }
    
    private void loadFromSettings() {
        userPresets.clear();
        List source = settings.getList(settingName);
        for (Object o : source) {
            if (o instanceof List) {
                List<String> list = (List)o;
                Preset p = Preset.fromList(list);
                if (p != null) {
                    userPresets.add(p);
                }
            }
        }
    }
    
    private static class Preset {
        
        private final String name;
        private final List<String> colors;
        
        public Preset(String name, List<String> colors) {
            this.name = name;
            this.colors = colors;
        }
        
        public List<String> toList() {
            List<String> result = new ArrayList<>();
            result.add(name);
            result.addAll(colors);
            return result;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        public static Preset fromList(List<String> list) {
            if (list.size() > 1) {
                String name = list.get(0);
                List<String> colors = new ArrayList<>(list.subList(1, list.size()));
                return new Preset(name, colors);
            }
            return null;
        }
        
    }
    
}
