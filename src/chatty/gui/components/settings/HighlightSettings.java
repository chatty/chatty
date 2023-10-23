
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.Replacer2;
import chatty.util.Replacer2.Part;
import chatty.util.StringUtil;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import static javax.swing.WindowConstants.HIDE_ON_CLOSE;

/**
 *
 * @author tduva
 */
public class HighlightSettings extends SettingsPanel {
    
    public static final String INFO_HEADER = "<html><body style='width:370px;font-weight:normal;'>";
    
    public static String getMatchingHelp(String type) {
        return INFO_HEADER+SettingsUtil.getInfo("info-matching.html", type);
    }
    
    private final ListSelector items;
    private final NoHighlightUsers noHighlightUsers;
    private final HighlightBlacklist highlightBlacklist;
    private final Substitutes substitutes;
    
    public HighlightSettings(SettingsDialog d) {
        super(true);
        
        noHighlightUsers = new NoHighlightUsers(d);
        highlightBlacklist = new HighlightBlacklist(d, "highlight", "highlightBlacklist");
        substitutes = new Substitutes(d);
        
        JPanel base = addTitledPanel(Language.getString("settings.section.highlightMessages"), 0, true);
        
        GridBagConstraints gbc;
        
        gbc = d.makeGbc(0,0,1,1);
        gbc.insets.bottom -= 3;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightEnabled = d.addSimpleBooleanSetting("highlightEnabled");
        base.add(highlightEnabled, gbc);
        
        Insets settingInsets = new Insets(1,14,1,4);
        
        gbc = d.makeGbc(0,1,1,1);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = settingInsets;
        JCheckBox highlightUsername = d.addSimpleBooleanSetting("highlightUsername");
        base.add(highlightUsername, gbc);

        gbc = d.makeGbc(1,1,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightNextMessages = d.addSimpleBooleanSetting("highlightNextMessages");
        base.add(highlightNextMessages, gbc);
        
        gbc = d.makeGbc(0,2,1,1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightOwnText = d.addSimpleBooleanSetting("highlightOwnText");
        base.add(highlightOwnText, gbc);
        
        gbc = d.makeGbc(1, 2, 1, 1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightIgnored = d.addSimpleBooleanSetting("highlightIgnored");
        base.add(highlightIgnored, gbc);
        
        gbc = d.makeGbc(1, 3, 1, 1);
        gbc.insets = settingInsets;
        gbc.anchor = GridBagConstraints.WEST;
        JCheckBox highlightOverrideIgnored = d.addSimpleBooleanSetting("highlightOverrideIgnored");
        base.add(highlightOverrideIgnored, gbc);
        
        gbc = d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST);
        gbc.insets = settingInsets;
        JCheckBox highlightMatches = d.addSimpleBooleanSetting("highlightMatches");
        base.add(highlightMatches, gbc);
        
        gbc = d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(1, 23, 1, 14);
        JCheckBox highlightMatchesAll = d.addSimpleBooleanSetting("highlightMatchesAll");
        base.add(highlightMatchesAll, gbc);
        
        gbc = d.makeGbc(1, 4, 1, 1, GridBagConstraints.WEST);
        gbc.insets = settingInsets;
        JCheckBox highlightMatchesAllEntries = d.addSimpleBooleanSetting("highlightMatchesAllEntries");
        base.add(highlightMatchesAllEntries, gbc);
        
        gbc = d.makeGbc(0, 5, 2, 1, GridBagConstraints.WEST);
        JCheckBox highlightByPoints = d.addSimpleBooleanSetting("highlightByPoints");
        base.add(highlightByPoints, gbc);
        
        gbc = d.makeGbc(0,6,2,1);
        gbc.insets = new Insets(5,10,5,5);
        items = d.addListSetting("highlight", "Highlight", 220, 250, true, true);
        items.setInfo(getMatchingHelp("highlight"));
        items.setInfoLinkLabelListener(d.getLinkLabelListener());
        items.setEditor(() -> {
            HighlighterTester tester = new HighlighterTester(d, true, "highlight");
            tester.setAddToBlacklistListener(e -> {
                highlightBlacklist.addItem(e.getActionCommand());
            });
            tester.setLinkLabelListener(d.getLinkLabelListener());
            return tester;
        });
        items.setDataFormatter(input -> input.trim());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        base.add(items, gbc);
        
        JButton noHighlightUsersButton = new JButton("Users to never highlight");
        GuiUtil.smallButtonInsets(noHighlightUsersButton);
        noHighlightUsersButton.addActionListener(e -> {
            noHighlightUsers.show(HighlightSettings.this);
        });
        gbc = d.makeGbc(0, 7, 1, 1);
        gbc.insets = new Insets(1,10,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        base.add(noHighlightUsersButton, gbc);
        
        JButton highlightBlacklistButton = new JButton("Highlight Blacklist");
        GuiUtil.smallButtonInsets(highlightBlacklistButton);
        highlightBlacklistButton.addActionListener(e -> {
            highlightBlacklist.show(HighlightSettings.this);
        });
        gbc = d.makeGbc(1, 7, 1, 1);
        gbc.insets = new Insets(1,5,5,30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        base.add(highlightBlacklistButton, gbc);
        
        JButton presetsButton = new JButton("Presets");
        presetsButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        presetsButton.addActionListener(e -> {
            d.showMatchingPresets();
        });
        gbc = d.makeGbc(0, 8, 1, 1);
        gbc.insets = new Insets(1,10,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        base.add(presetsButton, gbc);
        
        JButton substitutesButton = new JButton("Substitutes / Lookalikes");
        substitutesButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        substitutesButton.addActionListener(e -> {
            substitutes.show(HighlightSettings.this);
        });
        gbc = d.makeGbc(1, 8, 1, 1);
        gbc.insets = new Insets(1,5,5,30);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        base.add(substitutesButton, gbc);
        
        SettingsUtil.addSubsettings(highlightEnabled, highlightUsername,
                highlightNextMessages, highlightOwnText, highlightIgnored,
                highlightOverrideIgnored,
                highlightMatches, items, noHighlightUsersButton,
                highlightBlacklistButton);
        
        SettingsUtil.addSubsettings(highlightMatches, highlightMatchesAll, highlightMatchesAllEntries);
    }
    
    public void selectItem(String item) {
        items.setSelected(item);
    }
    
    public void selectItems(Collection<String> selectItems) {
        items.setSelected(selectItems);
    }
    
    private static class NoHighlightUsers extends LazyDialog {

        private static final DataFormatter<String> FORMATTER = new DataFormatter<String>() {

            @Override
            public String format(String input) {
                return StringUtil.toLowerCase(input.replaceAll("\\s", ""));
            }
        };
        
        private final SettingsDialog d;
        private final ListSelector noHighlightUsers;
        
        public NoHighlightUsers(SettingsDialog d) {
            this.d = d;
            this.noHighlightUsers = d.addListSetting("noHighlightUsers", "No Highlight User", 180, 250, false, true);
        }
        
        @Override
        public JDialog createDialog() {
            return new Dialog();
        }
        
        private class Dialog extends JDialog {

            Dialog() {
                super(d);

                setDefaultCloseOperation(HIDE_ON_CLOSE);
                setTitle("Users to never highlight");
                setLayout(new GridBagLayout());

                GridBagConstraints gbc;

                gbc = d.makeGbc(0, 0, 1, 1);
                add(new JLabel("<html><body style='width:260px;padding:4px;'>Users on this list "
                        + "will never trigger a Highlight. This can be useful e.g. "
                        + "for bots in your channel that repeatedly post messages "
                        + "containing your name."), gbc);

                gbc = d.makeGbc(0, 1, 1, 1);
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 0.5;
                gbc.weighty = 1;
                noHighlightUsers.setDataFormatter(FORMATTER);
                add(noHighlightUsers, gbc);

                JButton closeButton = new JButton(Language.getString("dialog.button.close"));
                closeButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setVisible(false);
                    }
                });
                gbc = d.makeGbc(0, 2, 1, 1);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1;
                gbc.insets = new Insets(5, 5, 5, 5);
                add(closeButton, gbc);

                pack();
                setMinimumSize(getPreferredSize());
            }
        }
        
    }
    
    private static class Substitutes extends LazyDialog {
        
        private final SettingsDialog d;
        private final JCheckBox substitutesEnabled;
        private final ListSelector substitutes;
        
        public Substitutes(SettingsDialog d) {
            this.d = d;
            substitutesEnabled = d.addSimpleBooleanSetting("matchingSubstitutesEnabled");
            substitutes = d.addListSetting("matchingSubstitutes", "Substitutes", 180, 250, true, true);
            substitutes.setChangeListener(value -> {
                updateTestSubstitutes();
            });
        }
        
        public JDialog createDialog() {
            return new Dialog();
        }
        
        private class Dialog extends JDialog {

            private Dialog() {
                super(d);

                setDefaultCloseOperation(HIDE_ON_CLOSE);
                setTitle("Substitutes / Lookalikes");
                setLayout(new GridBagLayout());

                GridBagConstraints gbc;

                gbc = d.makeGbc(0, 0, 2, 1);
                add(new JLabel("<html><body style='width:340px;padding:4px;'>" + SettingsUtil.getInfo("info-substitutes.html", null)), gbc);

                gbc = d.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
                substitutesEnabled.addItemListener(e -> updateTestSubstitutes());
                add(substitutesEnabled, gbc);

                gbc = d.makeGbc(0, 2, 2, 1);
                add(new JLabel("<html><body style='width:340px;padding:4px;padding-top:0'>Independent of this setting the <code>config:s</code> prefix can enable and <code>config:!s</code> disable this feature on a per Highlight-item basis."), gbc);

                JButton addDefaults = new JButton("Add default entries");
                GuiUtil.smallButtonInsets(addDefaults);
                addDefaults.addActionListener(e -> {
                    int result = JOptionPane.showConfirmDialog(rootPane, "This will add 26 (a-z) lookalikes entries. Existing entries will remain.", "Add entries?", JOptionPane.YES_NO_OPTION);
                    if (result == 0) {
                        List<String> data = substitutes.getData();
                        data.addAll(Replacer2.LOOKALIKES);
                        substitutes.setData(data);
                    }
                });
                gbc = d.makeGbc(1, 1, 1, 1, GridBagConstraints.EAST);
                add(addDefaults, gbc);

                gbc = d.makeGbc(0, 3, 2, 1);
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1;
                gbc.weighty = 1;
                substitutes.setEditor(() -> new SubstitutesEditor(this));
                add(substitutes, gbc);

                JButton closeButton = new JButton(Language.getString("dialog.button.close"));
                closeButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setVisible(false);
                    }
                });
                gbc = d.makeGbc(0, 4, 2, 1);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1;
                gbc.insets = new Insets(5, 5, 5, 5);
                add(closeButton, gbc);

                pack();
                setMinimumSize(getPreferredSize());
            }
        }
        
        private void updateTestSubstitutes() {
            HighlighterTester.substitutesItem = Replacer2.create(substitutes.getSettingValue());
            HighlighterTester.substitutesDefault = substitutesEnabled.isSelected();
        }
        
    }
    
    private static class SubstitutesEditor extends JDialog implements StringEditor {
        
        private static final int INPUT_LENGTH_LIMIT = 100*1000;
        
        private final JTextArea itemValue = new JTextArea(4, 20);
        private final JTextArea info = new JTextArea(20, 68);
        private final JButton okButton = new JButton(Language.getString("dialog.button.save"));
        private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
        
        private String result;
        
        public SubstitutesEditor(Dialog owner) {
            super(owner);
            setModal(true);
            setTitle("Editor");
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc = SettingsDialog.makeGbc(0, 0, 2, 1);
            itemValue.setFont(new Font(Font.MONOSPACED, 0, itemValue.getFont().getSize()));
            itemValue.setLineWrap(true);
            GuiUtil.installLengthLimitDocumentFilter(itemValue, INPUT_LENGTH_LIMIT, false);
            
            info.setEditable(false);
            info.setFont(new Font(Font.MONOSPACED, 0, info.getFont().getSize()));
            
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    new JScrollPane(itemValue), new JScrollPane(info));
            gbc = SettingsDialog.makeGbc(0, 1, 2, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(splitPane, gbc);
            
            gbc = SettingsDialog.makeGbc(0, 2, 2, 1);
            add(new JLabel("<html><body style='width:340px;'>Automatically generated characters are indented. Duplicates within this entry are marked with [Duplicate], but shouldn't cause any issues."), gbc);
            
            GuiUtil.addChangeListener(itemValue.getDocument(), e -> updateItem());

            //--------------------------
            // Main buttons
            //--------------------------
            okButton.setMnemonic(KeyEvent.VK_S);
            cancelButton.setMnemonic(KeyEvent.VK_C);
            gbc = GuiUtil.makeGbc(0, 21, 1, 1, GridBagConstraints.EAST);
            gbc.weightx = 1;
            add(okButton, gbc);
            add(cancelButton,
                    GuiUtil.makeGbc(1, 21, 1, 1, GridBagConstraints.EAST));

            okButton.addActionListener(e -> {
                result = itemValue.getText();
                setVisible(false);
            });
            cancelButton.addActionListener(e -> setVisible(false));
            
            pack();
        }
        
        private void updateItem() {
            String text = itemValue.getText();
            if (text.trim().isEmpty()) {
                info.setText("Nothing entered yet");
            }
            else {
                List<Part> split = Replacer2.parse(text);
                StringBuilder b = new StringBuilder();
                Set<String> checkUnique = new HashSet<>();
                for (int i = 0; i < split.size(); i++) {
                    if (i == 0) {
                        b.append("Replace with:");
                        b.append("\n");
                    }
                    else if (i == 1) {
                        b.append("Replace:");
                        b.append("\n");
                    }
                    Part partItem = split.get(i);
                    String part = partItem.text;
                    if (checkUnique.contains(part) && !part.isEmpty()) {
                        b.append("[Duplicate]");
                    }
                    if (i > 1) {
                        checkUnique.add(part);
                    }
                    if (!partItem.valid && !partItem.text.isEmpty()) {
                        b.append("[Invalid]");
                    }
                    if (split.get(i).autoGenerated) {
                        b.append(" ");
                    }
                    b.append(" ");
                    b.append(part);
                    b.append(" : ");
                    IntStream stream = part.trim().codePoints();
                    stream.forEachOrdered(codePoint -> {
                        b.append(String.format("U+%04X", codePoint));
                        b.append("/").append(Character.getName(codePoint));
                        b.append(", ");
                    });
                    b.delete(b.length() - 2, b.length()); // Delete last ", "
                    b.append("\n");
                }
                info.setText(b.toString());
            }
            
            boolean canSave = !text.trim().isEmpty();
            okButton.setEnabled(canSave);
        }
        
        @Override
        public String showDialog(String title, String preset, String info) {
            // Caused issues for Notification editor for some reason
            SwingUtilities.invokeLater(() -> {
                setMinimumSize(getPreferredSize());
                itemValue.setText(preset);
                updateItem();
            });
            setMinimumSize(getPreferredSize());
            setLocationRelativeTo(getParent());
            setVisible(true);
            return result;
        }

        @Override
        public void setLinkLabelListener(LinkLabelListener listener) {
            
        }
        
    }
    
    
    //==========================
    // Test
    //==========================
    
    public static void main(String[] args) {
        SubstitutesEditor editor = new SubstitutesEditor(new JDialog());
        editor.showDialog("abc", "o # o о ο օ", null);
        System.exit(0);
    }
    
}
