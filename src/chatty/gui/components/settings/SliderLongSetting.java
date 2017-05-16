
package chatty.gui.components.settings;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A slider that shows the currently selected value and implements the
 * LongSetting interface.
 * 
 * @author tduva
 */
public class SliderLongSetting extends JPanel implements LongSetting {
    
    private final JSlider slider;
    private long value = 0;
    private final JLabel valueLabel;
    private final static String LABEL_PREFIX = "<html><body style='"
            + "width: 30px;"
            + "text-align: right;"
            + "border: 1px solid #AAAAAA;"
            + "margin-left: 4px;"
            + "padding-right: 2px;"
            + "'>";
    
    public SliderLongSetting(int orientation, int min, int max, int presetValue) {
        slider = new JSlider(orientation, min, max, presetValue);
        this.value = value;
        valueLabel = new JLabel(LABEL_PREFIX);
        valueLabel.setMinimumSize(valueLabel.getPreferredSize());
        
        slider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                value = slider.getValue();
                updateLabel();
            }
        });
        
        add(slider);
        add(valueLabel);
    }

    @Override
    public Long getSettingValue() {
        return value;
    }
    
    @Override
    public Long getSettingValue(Long def) {
        return value;
    }

    @Override
    public void setSettingValue(Long value) {
        slider.setValue(value.intValue());
        this.value = value;
        updateLabel();
    }
    
    private void updateLabel() {
        valueLabel.setText(LABEL_PREFIX+new Long(value).toString());
    }
    
    public void setMajorTickSpacing(int value) {
        slider.setMajorTickSpacing(value);
    }
    
    public void setMinorTickSpacing(int value) {
        slider.setMinorTickSpacing(value);
    }
    
    public void setPaintLabels(boolean value) {
        slider.setPaintLabels(value);
    }
    
    public void setPaintTicks(boolean value) {
        slider.setPaintTicks(value);
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        slider.setEnabled(enabled);
    }

}
