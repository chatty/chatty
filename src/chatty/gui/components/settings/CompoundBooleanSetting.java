
package chatty.gui.components.settings;

/**
 * Combines several BooleanSetting objects into one LongSetting. Each boolean
 * represents one bit in the long.
 * 
 * @author tduva
 */
public class CompoundBooleanSetting implements LongSetting {
    
    private final BooleanSetting[] settings;
    
    /**
     * Settings must always be given in the same order when referring to the
     * same Long setting. A setting could probably be added or removed at the
     * end without changing the value of the other settings (but of course
     * removing/exchanging could always be problematic).
     * 
     * @param settings 
     */
    public CompoundBooleanSetting(BooleanSetting... settings) {
        this.settings = settings;
    }

    @Override
    public Long getSettingValue() {
        long result = 0;
        for (int i = 0; i < settings.length; i++) {
            if (settings[i].getSettingValue()) {
                result += 1 << i;
            }
        }
        return result;
    }

    @Override
    public Long getSettingValue(Long def) {
        return getSettingValue();
    }

    @Override
    public void setSettingValue(Long settingValue) {
        int value = settingValue.intValue();
        for (int i = 0; i < settings.length; i++) {
            settings[i].setSettingValue((value & (1 << i)) != 0);
        }
    }
    
}
