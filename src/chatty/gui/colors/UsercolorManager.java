
package chatty.gui.colors;

import chatty.Helper;
import chatty.User;
import chatty.util.colors.HtmlColors;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages {@link UsercolorItem}s, loading them from the settings on start,
 * making them available to other parts of the program ({@link getData()}) and
 * saving them to the settings when they are modified ({@link setData(List)}).
 * 
 * <p>
 * It also provides a method to get a {@code Color} matching a {@code User}.</p>
 * 
 * <p>
 * The {@code UsercolorItem}s order has to be kept throughout, so a List is used
 * for storing them both here and in the settings.</p>
 *
 * @author tduva
 */
public class UsercolorManager {
    
    private final Settings settings;
    private volatile List<UsercolorItem> data;
    
    public UsercolorManager(Settings settings) {
        this.settings = settings;
        loadFromSettings();
        // TEST
//        data = new ArrayList<>();
//        for (int i=0;i<10000;i++) {
//            data.add(new UsercolorItem("user"+i, Color.BLACK));
//        }
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
        List<String> l = new LinkedList<>();
        settings.getList("usercolors", l);
        List<UsercolorItem> loadedData = new ArrayList<>();
        for (String entry : l) {
            int splitAt = entry.lastIndexOf(",");
            if (splitAt > 0 && entry.length() > splitAt+1) {
                String id = entry.substring(0, splitAt);
                Color color = HtmlColors.decode(entry.substring(splitAt + 1));
                loadedData.add(new UsercolorItem(id, color));
            }
        }
        data = loadedData;
    }
    
    /**
     * Copy the current data to the settings.
     */
    private void saveToSettings() {
        List<String> dataToSave = new LinkedList<>();
        for (UsercolorItem item : data) {
            dataToSave.add(item.getId()+","+HtmlColors.getColorString(item.getForeground()));
        }
        settings.putList("usercolors", dataToSave);
    }
    
    /**
     * Gets the current data.
     * 
     * @return An ordered list of {@link UsercolorItem}s.
     */
    public synchronized List<UsercolorItem> getData() {
        return new ArrayList<>(data);
    }
    
    /**
     * Sets new data and copies it to the settings as well.
     * 
     * @param newData A list of ordered {@link UsercolorItem}s.
     */
    public synchronized void setData(List<UsercolorItem> newData) {
        data = new ArrayList<>(newData);
        saveToSettings();
    }
    
    /**
     * Returns the color for this user, or null if no items matched this user.
     * Should only be called through the User object, not directly, for easier
     * synchronization.
     * 
     * @param user
     * @return 
     */
    public synchronized Color getColor(User user) {
        if (data == null || !settings.getBoolean("customUsercolors")) {
            return null;
        }
        for (UsercolorItem item : data) {
            if (item.type == UsercolorItem.TYPE_COLOR) {
                if (item.idColor.equals(user.getPlainColor())) {
                    return item.color;
                }
            }
            else if (item.type == UsercolorItem.TYPE_NAME) {
                if (item.id.equalsIgnoreCase(user.getName())) {
                    return item.color;
                }
            }
            else if (item.type == UsercolorItem.TYPE_STATUS) {
                if (Helper.matchUserStatus(item.id, user)) {
                    return item.color;
                }
            }
            else if (item.type == UsercolorItem.TYPE_CATEGORY) {
                if (user.hasCategory(item.category)) {
                    return item.color;
                }
            }
            else if (item.type == UsercolorItem.TYPE_DEFAULT_COLOR) {
                if (user.hasDefaultColor()) {
                    return item.color;
                }
            }
            else if (item.type == UsercolorItem.TYPE_ALL) {
                return item.color;
            }
        }
        return null;
    }
    
}
