
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.lang.Language;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * The combo box is in an extra dialog, which allows the combo items to be
 * loaded lazily when it's opened rather than when the setting itself is
 * created.
 *
 * @author tduva
 */
public class DialogComboSetting extends JPanel implements StringSetting {

    private final JTextField display;
    private String value;
    private final Supplier<Map<String, String>> optionsCreator;
    private final Function<String, String> valueFormatter;
    private Set<Consumer<DialogComboSetting>> changeListeners;

    public DialogComboSetting(Window parent,
                              Supplier<Map<String, String>> optionsCreator,
                              Function<String, String> valueFormatter) {
        display = new JTextField(20);
        display.setEditable(false);
        this.optionsCreator = optionsCreator;
        this.valueFormatter = valueFormatter;

        JButton changeButton = new JButton(Language.getString("dialog.button.change"));
        GuiUtil.smallButtonInsets(changeButton);
        changeButton.addActionListener(e -> {
            change(parent);
        });

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = GuiUtil.makeGbc(0, 0, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        add(display, gbc);
        gbc = GuiUtil.makeGbc(1, 0, 1, 1);
        add(changeButton, gbc);
    }

    private void change(Window parent) {
        JDialog dialog = new JDialog(parent);
        dialog.setTitle("Change value");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setModal(true);
        dialog.setLayout(new GridBagLayout());
        dialog.setResizable(false);

        ComboStringSetting list = new ComboStringSetting(optionsCreator.get());
        list.setSettingValue(value);

        JButton save = new JButton(Language.getString("dialog.button.save"));
        save.addActionListener(e -> {
            setSettingValue(list.getSettingValue());
            dialog.setVisible(false);
        });

        JButton cancel = new JButton(Language.getString("dialog.button.cancel"));
        cancel.addActionListener(e -> {
            dialog.setVisible(false);
        });

        dialog.add(list, GuiUtil.makeGbc(0, 0, 2, 1));
        GridBagConstraints gbc = GuiUtil.makeGbc(0, 1, 1, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        dialog.add(save, gbc);
        dialog.add(cancel, GuiUtil.makeGbc(1, 1, 1, 1));

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    @Override
    public String getSettingValue() {
        return value;
    }

    @Override
    public void setSettingValue(String value) {
        this.value = value;
        display.setText(valueFormatter.apply(value));
        informChangeListeners();
    }

    public void addSettingChangeListener(Consumer<DialogComboSetting> listener) {
        if (listener != null) {
            if (changeListeners == null) {
                changeListeners = new HashSet<>();
            }
            changeListeners.add(listener);
        }
    }

    private void informChangeListeners() {
        if (changeListeners != null) {
            changeListeners.forEach(listener -> listener.accept(this));
        }
    }

}
