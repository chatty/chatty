
package chatty.util.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author tduva
 */
public abstract class AdvancedSetting<T> {
    
    private final Object LOCK = new Object();
    
    private final List<T> cache = new ArrayList<>();
    private final Settings settings;
    private final String settingName;
    private final boolean notifyChanges;
    
    public AdvancedSetting(Settings settings, String settingName, boolean notifyChanges) {
        this.settings = settings;
        this.settingName = settingName;
        this.notifyChanges = notifyChanges;
    }
    
    public abstract T toObject(Object input);
    
    public abstract Object fromObject(T input);
    
    public void init() {
        loadSettingsIfNecessary();
    }
    
    private void loadSettingsIfNecessary() {
        Collection<Object> settingsList = settings.getList(settingName);
        synchronized (LOCK) {
            if (cache.isEmpty()) {
                for (Object item : settingsList) {
                    cache.add(toObject(item));
                }
            }
        }
    }
    
    private void saveSettings() {
        List<Object> settingsData = new ArrayList<>();
        synchronized (LOCK) {
            for (T object : cache) {
                Object raw = fromObject(object);
                settingsData.add(raw);
            }
        }
        settings.putList(settingName, settingsData);
        if (notifyChanges) {
            notifyChanges();
        }
    }
    
    public void setData(Collection<T> data) {
        synchronized (LOCK) {
            cache.clear();
            cache.addAll(data);
        }
        saveSettings();
    }
    
    public Collection<T> getData() {
        synchronized (LOCK) {
            return new ArrayList<>(cache);
        }
    }
    
    public boolean contains(T item) {
        synchronized(LOCK) {
            return cache.contains(item);
        }
    }
    
    public void add(T item) {
        addInternal(item);
    }
    
    private void addInternal(T item) {
        synchronized(LOCK) {
            cache.add(item);
        }
        settings.listAdd(settingName, fromObject(item));
        if (notifyChanges) {
            notifyChanges();
        }
    }
    
    public void setAdd(T item) {
        if (!contains(item)) {
            addInternal(item);
        }
    }
    
    public void remove(T item) {
        boolean removed;
        synchronized(LOCK) {
            removed = cache.remove(item);
        }
        if (removed) {
            /**
             * Overwrite fully so that a changed export format doesn't cause the
             * removed setting item to not be found/deleted. Adding isn't a
             * problem because no item has to be found, just appended.
             */
            saveSettings();
        }
    }
    
    public void notifyChanges() {
        settings.setSettingChanged(settingName);
    }
    
}
