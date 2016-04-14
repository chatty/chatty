
package chatty.gui.components.settings;

import javax.swing.JCheckBox;

/**
 * Default boolean setting that consist of a simple JCheckBox.
 * 
 * @author tduva
 */
public class SimpleBooleanSetting extends JCheckBox implements BooleanSetting {

    public SimpleBooleanSetting(String text, String tooltip) {
        super(text);
        setToolTipText(tooltip);
    }
    
    @Override
    public Boolean getSettingValue() {
        return isSelected();
    }

    @Override
    public void setSettingValue(Boolean value) {
        setSelected(value);
    }
    
}
