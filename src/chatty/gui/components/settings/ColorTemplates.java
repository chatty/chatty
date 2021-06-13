
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import chatty.util.settings.Settings;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
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
 * Note: Changed to support the more generic StringSetting objects. More
 * refactoring may make sense, but it will do for now.
 *
 * @author tduva
 */
public class ColorTemplates extends JPanel {
    
    private final List<StringSetting> colorSettings = new ArrayList<>();
    private final List<BooleanSetting> booleanSettings = new ArrayList<>();
    private final List<Preset> hardcodedPresets = new ArrayList<>();
    private final List<Preset> userPresets = new ArrayList<>();
    
    private final JButton saveButton = new JButton(Language.getString("settings.colorPresets.button.save"));
    private final JButton saveAsButton = new JButton(Language.getString("settings.colorPresets.button.saveAs"));
    private final JButton removeButton = new JButton(Language.getString("settings.colorPresets.button.delete"));
    
    private final GenericComboSetting<Preset> selection = new GenericComboSetting<>();
    
    private final Settings settings;
    private final String settingName;
    
    public ColorTemplates(Settings settings, String settingName, ColorSetting[] values) {
        this(settings, settingName, values, new BooleanSetting[0]);
    }
    
    public ColorTemplates(Settings settings, String settingName, StringSetting[] values, BooleanSetting[] booleanValues) {
        colorSettings.addAll(Arrays.asList(values));
        booleanSettings.addAll(Arrays.asList(booleanValues));
        
        for (StringSetting c : colorSettings) {
            if (c instanceof ColorSetting) {
                ((ColorSetting)c).addListener(() -> {
                    update();
                });
            }
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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        add(saveButton, gbc);
        
        gbc = GuiUtil.makeGbc(1, 1, 1, 1);
        gbc.insets = insets;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.3;
        add(saveAsButton, gbc);
        
        gbc = GuiUtil.makeGbc(2, 1, 1, 1);
        gbc.insets = insets;
        add(removeButton, gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 3, 1);
        JLabel saveNote = new JLabel(Language.getString("settings.colorPresets.info"));
        //add(saveNote, gbc);
        
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
        selection.add((Preset)null, "-- "+Language.getString("settings.colorPresets.colorPresets")+" --");
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
            StringSetting s = colorSettings.get(i);
            if (p.colors.size() <= i) {
                return false;
            }
            if (!p.colors.get(i).equalsIgnoreCase(s.getSettingValue())) {
                return false;
            }
        }
        for (int i = 0; i < booleanSettings.size(); i++) {
            BooleanSetting s = booleanSettings.get(i);
            if (p.booleans.size() <= i) {
                return false;
            }
            if (p.booleans.get(i) != s.getSettingValue().booleanValue()) {
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
            for (int i = 0; i < booleanSettings.size(); i++) {
                if (p.booleans.size() > i) {
                    booleanSettings.get(i).setSettingValue(p.booleans.get(i));
                }
            }
        }
    }
    
    private void addCurrentAsPreset() {
        String name = JOptionPane.showInputDialog(this,
                "Enter name to save current colors:");
        if (name != null) {
            Preset p = new Preset(name, getCurrentColors(), getCurrentBooleans());
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
        Preset p = new Preset(current.name, getCurrentColors(), getCurrentBooleans());
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
        for (StringSetting s : colorSettings) {
            colors.add(s.getSettingValue());
        }
        return colors;
    }
    
    private List<Boolean> getCurrentBooleans() {
        List<Boolean> booleans = new ArrayList<>();
        for (BooleanSetting s : booleanSettings) {
            booleans.add(s.getSettingValue());
        }
        return booleans;
    }
    
    private void removeSelectedPreset() {
        Preset p = selection.getSettingValue();
        userPresets.remove(p);
        selection.remove(p);
        saveToSettings();
    }
    
    public void addPreset(String name, String[] values) {
        addPreset(name, values, new Boolean[0]);
    }
    
    /**
     * Adds a preset with the given name. The order and length of specified
     * values must match the color settings this template object was created
     * with.
     * 
     * @param name
     * @param values 
     * @param booleanValues 
     */
    public void addPreset(String name, String[] values, Boolean[] booleanValues) {
        if (values.length < colorSettings.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (booleanValues.length < booleanSettings.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        Preset p = new Preset(name, Arrays.asList(values), Arrays.asList(booleanValues));
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
        private final List<Boolean> booleans;
        
        public Preset(String name, List<String> colors, List<Boolean> booleans) {
            this.name = name;
            this.colors = colors;
            this.booleans = booleans;
        }
        
        public List toList() {
            List result = new ArrayList<>();
            result.add(name);
            result.addAll(colors);
            result.addAll(booleans);
            return result;
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        public static Preset fromList(List list) {
            if (list.size() > 1 && list.get(0) instanceof String) {
                String name = (String)list.get(0);
                List<String> colors = new ArrayList<>();
                List<Boolean> booleans = new ArrayList<>();
                for (Object o : list.subList(1, list.size())) {
                    if (o instanceof String) {
                        colors.add((String)o);
                    } else if (o instanceof Boolean) {
                        booleans.add((Boolean)o);
                    }
                }
                return new Preset(name, colors, booleans);
            }
            return null;
        }
        
    }
    
}
