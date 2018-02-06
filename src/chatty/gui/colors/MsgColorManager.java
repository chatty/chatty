
package chatty.gui.colors;

import chatty.User;
import chatty.gui.HtmlColors;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Store message color items.
 * 
 * Loaded from settings on creation, saved to settings when new data is set.
 * 
 * @author tduva
 */
public class MsgColorManager {
    
    private static final String DATA_SETTING = "msgColors";
    private static final String ENABLED_SETTING = "msgColorsEnabled";
    
    private final Settings settings;
    private volatile List<MsgColorItem> data;
    
    public MsgColorManager(Settings settings) {
        this.settings = settings;
        loadFromSettings();
    }
    
    /**
     * Loads the data from the settings, creating {@code Usercolor} objects from
     * the loaded entries.
     * 
     * <p>The items are assumed to be in the format:<br />
     * <code>[id],[color]</code> (the last comma is used as seperating comma,
     * since {@code id} may contain a comma, but {@code color} should not)</p>
     */
    private void loadFromSettings() {
        List<String> loadedList = new LinkedList<>();
        settings.getList(DATA_SETTING, loadedList);
        List<MsgColorItem> loadedData = new ArrayList<>();
        for (String entry : loadedList) {
            int splitAt = entry.lastIndexOf(",");
            if (splitAt > 0 && entry.length() > splitAt+1) {
                String id = entry.substring(0, splitAt);
                Color color = HtmlColors.decode(entry.substring(splitAt + 1));
                loadedData.add(new MsgColorItem(id, color));
            }
        }
        data = loadedData;
    }
    
    /**
     * Copy the current data to the settings.
     */
    private void saveToSettings() {
        List<String> dataToSave = new LinkedList<>();
        for (MsgColorItem item : data) {
            dataToSave.add(item.getId()+","+HtmlColors.getColorString(item.getColor()));
        }
        settings.putList(DATA_SETTING, dataToSave);
    }
    
    /**
     * Gets the current data.
     * 
     * @return An ordered list of {@link UsercolorItem}s.
     */
    public synchronized List<MsgColorItem> getData() {
        return new ArrayList<>(data);
    }
    
    /**
     * Sets new data and copies it to the settings as well.
     * 
     * @param newData A list of ordered {@link UsercolorItem}s.
     */
    public synchronized void setData(List<MsgColorItem> newData) {
        data = new ArrayList<>(newData);
        saveToSettings();
    }
    
    /**
     * Returns the color for this user and text (message), or null if no items
     * matched.
     * 
     * @param user
     * @param text
     * @return 
     */
    public synchronized Color getColor(User user, String text) {
        if (data == null || !settings.getBoolean(ENABLED_SETTING)) {
            return null;
        }
        for (MsgColorItem item : data) {
            if (item.matches(user, text)) {
                return item.getColor();
            }
        }
        return null;
    }
    
}
