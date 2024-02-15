
package chatty.gui.colors;

import chatty.Addressbook;
import chatty.User;
import chatty.gui.Highlighter.HighlightItem;
import chatty.util.colors.HtmlColors;
import chatty.util.irc.MsgTags;
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
    
    public static final MsgColorItem EMPTY = new MsgColorItem(null, null, false, null, false);
    
    private static final String DATA_SETTING = "msgColors";
    private static final String ENABLED_SETTING = "msgColorsEnabled";
    
    private final Settings settings;
    private volatile List<MsgColorItem> data;
    
    public MsgColorManager(Settings settings) {
        this.settings = settings;
    }
    
    /**
     * Loads the data from the settings, creating {@code Usercolor} objects from
     * the loaded entries.
     * 
     * <p>The items are assumed to be in the format:<br />
     * <code>[id],[color]</code> (the last comma is used as seperating comma,
     * since {@code id} may contain a comma, but {@code color} should not)</p>
     * 
     * <p>New format:<br />
     * <code>[id],[foreground]/[enabled]/[background]/[enabled]</p>
     */
    public synchronized void loadFromSettings() {
        List<String> loadedList = new LinkedList<>();
        settings.getList(DATA_SETTING, loadedList);
        List<MsgColorItem> loadedData = new ArrayList<>();
        for (String entry : loadedList) {
            int splitAt = entry.lastIndexOf(",");
            if (splitAt > 0 && entry.length() > splitAt+1) {
                String id = entry.substring(0, splitAt);
                String colorsPart = entry.substring(splitAt + 1);
                String[] colors = colorsPart.split("/");
                Color foreground = HtmlColors.decode(colors[0]);
                Color background;
                boolean foregroundEnabled;
                boolean backgroundEnabled;
                if (colors.length == 4) {
                    // New format with background color
                    background = HtmlColors.decode(colors[2], null);
                    foregroundEnabled = colors[1].equals("1");
                    backgroundEnabled = colors[3].equals("1");
                } else {
                    // Old or invalid format, make defaults
                    background = null;
                    foregroundEnabled = true;
                    backgroundEnabled = false;
                }
                
                loadedData.add(new MsgColorItem(id,
                        foreground, foregroundEnabled,
                        background, backgroundEnabled));
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
            dataToSave.add(String.format("%s,%s/%s/%s/%s",
                    item.getId(),
                    HtmlColors.getColorString(item.getForeground()),
                    item.getForegroundEnabled() ? "1" : "0",
                    HtmlColors.getColorString(item.getBackground()),
                    item.getBackgroundEnabled() ? "1" : "0"));
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
    private synchronized MsgColorItem getColor(HighlightItem.Type type, User user, User localUser,
            String text, int msgStart, int msgEnd, String channel, MsgTags tags, Addressbook ab) {
        if (data == null || !settings.getBoolean(ENABLED_SETTING)) {
            return EMPTY;
        }
        for (MsgColorItem item : data) {
            if (item.matches(type, text, msgStart, msgEnd, channel, ab, user, localUser, tags)) {
                return item;
            }
        }
        return EMPTY;
    }
    
    public synchronized MsgColorItem getMsgColor(User user, User localUser, String text, int msgStart, int msgEnd, MsgTags tags) {
        return getColor(HighlightItem.Type.REGULAR, user, localUser, text, msgStart, msgEnd, user.getChannel(), tags, user.getAddressbook());
    }
    
    public synchronized MsgColorItem getInfoColor(String text, int msgStart, int msgEnd, String channel, Addressbook ab, User user, User localUser, MsgTags tags) {
        return getColor(HighlightItem.Type.INFO, user, localUser, text, msgStart, msgEnd, channel, tags, ab);
    }
    
}
