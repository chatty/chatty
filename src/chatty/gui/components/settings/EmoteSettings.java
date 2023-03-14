
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.api.Emoticon;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import chatty.util.api.CachedImage.CachedImageUser;
import chatty.util.api.IgnoredEmotes;
import chatty.util.seventv.WebPUtil;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;

/**
 *
 * @author tduva
 */
public class EmoteSettings extends SettingsPanel {
    
    protected final LocalEmotesDialog localEmotesDialog;
    
    protected EmoteSettings(SettingsDialog d) {
        
        //==================
        // General Settings
        //==================
        JPanel main = addTitledPanel(Language.getString("settings.section.emoticons"), 0);

        main.add(
                d.addSimpleBooleanSetting("emoticonsEnabled"),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        IgnoredEmotesDialog ignoredEmotesDialog = new IgnoredEmotesDialog(d);
        JButton ignoredEmotesButton = new JButton(Language.getString("settings.emoticons.button.editIgnored"));
        GuiUtil.smallButtonInsets(ignoredEmotesButton);
        ignoredEmotesButton.addActionListener(e -> {
            ignoredEmotesDialog.show(d);
        });
        main.add(ignoredEmotesButton,
                d.makeGbc(2, 0, 3, 1, GridBagConstraints.EAST));
        
        //------------
        // Emote Size
        //------------
        // Chat Emote Scale
        main.add(new JLabel(Language.getString("settings.emoticons.chatScale")),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScale = new ComboLongSetting(makeScaleValues());
        d.addLongSetting("emoteScale", emoteScale);
        main.add(emoteScale, d.makeGbc(1, 3, 1, 1, GridBagConstraints.CENTER));
        
        // Maximum Emote Height (Chat)
        main.add(new JLabel(Language.getString("settings.emoticons.maxHeight")),
                d.makeGbc(2, 3, 1, 1, GridBagConstraints.EAST));
        main.add(d.addSimpleLongSetting("emoteMaxHeight", 3, true),
                d.makeGbc(3, 3, 1, 1, GridBagConstraints.WEST));
        main.add(new JLabel(Language.getString("settings.emoticons.maxHeightPixels")),
                d.makeGbc(4, 3, 1, 1, GridBagConstraints.WEST));
        
        // Emotes Dialog Emote Scale
        main.add(new JLabel(Language.getString("settings.emoticons.dialogScale")),
                d.makeGbc(0, 4, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScaleDialog = new ComboLongSetting(makeScaleValues());
        d.addLongSetting("emoteScaleDialog", emoteScaleDialog);
        main.add(emoteScaleDialog, d.makeGbc(1, 4, 1, 1, GridBagConstraints.CENTER));
        
        //-------
        // Other
        //-------
        main.add(d.addSimpleBooleanSetting("closeEmoteDialogOnDoubleClick"),
                d.makeGbc(0, 5, 3, 1));
        
        //--------------------------
        // Animations
        //--------------------------
        ComboLongSetting animatePauseSetting = d.addComboLongSetting("animationPause", 0, 1, 2);
        ComboLongSetting animatePauseFrameSetting = d.addComboLongSetting("animationPauseFrame", 0, 1, 2);
        
        SettingsUtil.addSubsettings(animatePauseSetting, s -> s != 2, animatePauseFrameSetting);
        
        SettingsUtil.addLabeledComponent(main, "animationPause",
                0, 6, 4, GridBagConstraints.WEST,
                animatePauseSetting);
        
        SettingsUtil.addLabeledComponent(main, "animationPauseFrame",
                0, 7, 4, GridBagConstraints.WEST,
                animatePauseFrameSetting);
        
        //--------------------------
        // WebP
        //--------------------------
        JLabel webpTest = new JLabel("WebP not supported.");
        main.add(webpTest,
                SettingsDialog.makeGbc(2, 8, 3, 1, GridBagConstraints.WEST));
        WebPUtil.runIfWebPAvailable(() -> {
            webpTest.setText("WebP is supported.");
        });
        
        main.add(
                d.addSimpleBooleanSetting("webp"),
                d.makeGbc(0, 8, 2, 1, GridBagConstraints.WEST));
        
        //==========================
        // Provider specific
        //==========================
        JPanel providerSettingsPanel = addTitledPanel("Provider-specific Emote Settings", 1);
        
        JTabbedPane providerSettingsTabs = new JTabbedPane();
        JPanel twitchSettings = new JPanel(new GridBagLayout());
        JPanel ffzSettings = new JPanel(new GridBagLayout());
        JPanel bttvSettings = new JPanel(new GridBagLayout());
        JPanel seventvSettings = new JPanel(new GridBagLayout());
        JPanel emojiSettings = new JPanel(new GridBagLayout());
        providerSettingsTabs.addTab("Twitch", twitchSettings);
        providerSettingsTabs.addTab("FFZ", ffzSettings);
        providerSettingsTabs.addTab("BTTV", bttvSettings);
        providerSettingsTabs.addTab("7TV", seventvSettings);
        providerSettingsTabs.addTab(Language.getString("settings.section.emoji"), emojiSettings);
        
        GridBagConstraints gbc = d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        providerSettingsPanel.add(providerSettingsTabs, gbc);
        
        //--------------------------
        // Twitch
        //--------------------------
        
        // Cheers
        Map<String, String> cheeringTypeDef = new LinkedHashMap<>();
        cheeringTypeDef.put("none", Language.getString("settings.emoticons.cheers.option.text"));
        cheeringTypeDef.put("static", Language.getString("settings.emoticons.cheers.option.static"));
        cheeringTypeDef.put("animated", Language.getString("settings.emoticons.cheers.option.animated"));
        ComboStringSetting cheersType = new ComboStringSetting(cheeringTypeDef);
        
        d.addStringSetting("cheersType", cheersType);
        
        twitchSettings.add(new JLabel(Language.getString("settings.emoticons.cheers")),
                d.makeGbc(0, 6, 1, 1, GridBagConstraints.CENTER));
        twitchSettings.add(cheersType,
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.WEST));
        
        // Animated
        twitchSettings.add(d.addSimpleBooleanSetting("animatedEmotes"),
                d.makeGbc(0, 7, 3, 1, GridBagConstraints.WEST));
        
        SettingsUtil.topAlign(twitchSettings, 10);
        
        //--------------------------
        // FFZ
        //--------------------------
        final JCheckBox ffz = d.addSimpleBooleanSetting("ffz");
        ffzSettings.add(ffz,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzMod = d.addSimpleBooleanSetting("ffzModIcon");
        ffzSettings.add(ffzMod,
                d.makeGbcSub(0, 2, 1, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzEvent = d.addSimpleBooleanSetting("ffzEvent");
        ffzSettings.add(ffzEvent,
                d.makeGbcSub(0, 3, 1, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(ffz, ffzMod, ffzEvent);
        
        SettingsUtil.topAlign(ffzSettings, 10);
        
        //--------------------------
        // BTTV
        //--------------------------
        bttvSettings.add(d.addSimpleBooleanSetting("bttvEmotes"),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        SettingsUtil.topAlign(bttvSettings, 10);
        
        //--------------------------
        // SevenTV
        //--------------------------
        seventvSettings.add(d.addSimpleBooleanSetting("seventv"),
                SettingsDialog.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        
        SettingsUtil.topAlign(seventvSettings, 10);

        //--------------------------
        // Emoji
        //--------------------------
        
        Map<String, String> emojiSetDef = new LinkedHashMap<>();
        emojiSetDef.put("twemoji", "Twemoji (Twitter)");
        emojiSetDef.put("e1", "Emoji One [outdated]");
        emojiSetDef.put("none", Language.getString("settings.emoji.option.none"));
        ComboStringSetting emojiSet = new ComboStringSetting(emojiSetDef);
        
        d.addStringSetting("emoji", emojiSet);
        
        emojiSettings.add(new JLabel(Language.getString("settings.emoji.set")),
                d.makeGbc(0, 0, 1, 1, GridBagConstraints.WEST));
        emojiSettings.add(emojiSet,
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));
        
        emojiSettings.add(d.addSimpleBooleanSetting("emojiReplace"),
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addLabeledComponent(emojiSettings, "emojiZWJ", 0, 2, 1, GridBagConstraints.WEST, d.addComboLongSetting("emojiZWJ", 0, 1, 2));
        
        SettingsUtil.topAlign(emojiSettings, 10);
        
        //==========================
        // Local Emotes
        //==========================
        JPanel localEmoteSettings = addTitledPanel(Language.getString("settings.section.localEmotes"), 3);
        
        localEmoteSettings.add(new LinkLabel(SettingConstants.HTML_PREFIX+"Emotes configured in this section will always be available in your sent messages, however other people will only see them in your messages if you actually have access to them. [help-settings:EmoticonsLocal Learn More]", d.getLinkLabelListener()),
                d.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST));
        
        Map<Long, String> smiliesDef = new LinkedHashMap<>();
        smiliesDef.put(0L, "Off");
        smiliesDef.put(1L, "Robot");
        smiliesDef.put(2L, "Glitch");
        smiliesDef.put(3L, "Monkey");
        smiliesDef.put(10L, "Robot (Set automatically)");
        smiliesDef.put(20L, "Glitch (Set automatically)");
        smiliesDef.put(30L, "Monkey (Set automatically)");
        
        ComboLongSetting smilies = new ComboLongSetting(smiliesDef);
        d.addLongSetting("smilies", smilies);
        
        SettingsUtil.addLabeledComponent(localEmoteSettings, "smilies", 0, 2, 1, GridBagConstraints.WEST, smilies);
        
        localEmotesDialog = new LocalEmotesDialog(d);
        JButton localEmotesButton = new JButton("View Local Emotes");
        GuiUtil.smallButtonInsets(localEmotesButton);
        localEmotesButton.addActionListener(e -> {
            localEmotesDialog.show(d);
        });
        localEmoteSettings.add(localEmotesButton,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
    }
    
    public static Map<Long, String> makeScaleValues() {
        final Map<Long, String> scaleDef = new LinkedHashMap<>();
        for (int i=50;i<=200;i += 10) {
            if (i == 10) {
                scaleDef.put((long)i, "Normal");
            } else {
                scaleDef.put((long)i, (i)+"%");
            }
        }
        return scaleDef;
    }

    public void setData(Collection<Emoticon> data) {
        localEmotesDialog.setData(data);
    }
    
    public Collection<Emoticon> getData() {
        return localEmotesDialog.getData();
    }
    
    private static class IgnoredEmotesDialog extends LazyDialog {
        
        private final SettingsDialog d;
        private final ListSelector setting;
        
        private IgnoredEmotesDialog(SettingsDialog d) {
            this.d = d;
            this.setting = d.addListSetting("ignoredEmotes", "Ignored Emote", 280, 280, false, true);
        }

        @Override
        public JDialog createDialog() {
            return new Dialog();
        }

        private class Dialog extends JDialog {

            private Dialog() {
                super(d);
                setTitle(Language.getString("settings.ignoredEmotes.title"));
                setModal(true);
                setLayout(new GridBagLayout());

                GridBagConstraints gbc;

                gbc = d.makeGbc(0, 0, 1, 1);
                gbc.fill = GridBagConstraints.BOTH;
                gbc.weightx = 1;
                gbc.weighty = 1;
                add(setting, gbc);

                gbc = d.makeGbc(1, 0, 1, 1);
                gbc.anchor = GridBagConstraints.NORTH;
                add(new JLabel("<html><body style='width:240px;'>"
                        + "<p style='padding:5px;'>"+Language.getString("settings.ignoredEmotes.info1")+"</p>"
                                                + "<p style='padding:5px;'>In addition to the name you can provide the following restrictions:</p>"
                        + "<ul style='padding:5px;padding-left:0'>"
                        + "<li><code>for:t</code> - Only hide from TAB Completion (<code>c</code> - Chat, <code>d</code> - Emote Dialog, <code>t</code> - TAB Completion)</li>"
                        + "<li><code>type:ffz</code> - Must be an FFZ emote (<code>twitch</code>, <code>bttv</code>, <code>7tv</code>)</li>"
                        + "<li><code>id:123</code> - Emote must have the given id</li>"
                        + "</ul>"
                        + "<p style='padding:5px;'>" + Language.getString("settings.ignoredEmotes.info2") + "</p>"), gbc);

                JButton closeButton = new JButton(Language.getString("dialog.button.close"));
                closeButton.addActionListener(e -> {
                    setVisible(false);
                });
                gbc = d.makeGbc(0, 1, 2, 1);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = 1;
                add(closeButton, gbc);

                pack();
                GuiUtil.installEscapeCloseOperation(this);
            }

        }

    }
    
    private static EditIgnoredEmote editIgnoredEmoteDialog;
    
    public static IgnoredEmotes.Item showIgnoredEmoteEditDialog(Window owner, Component comp, Emoticon emote, Collection<IgnoredEmotes.Item> currentMatches) {
        if (editIgnoredEmoteDialog == null) {
            editIgnoredEmoteDialog = new EditIgnoredEmote(owner);
        }
        return editIgnoredEmoteDialog.showDialog(comp, emote, currentMatches);
    }
    
    public static class EditIgnoredEmote extends JDialog {
        
        private final JPanel beforePanel;
        private final JCheckBox chatB = new JCheckBox(Language.getString("settings.ignoredEmotes.for.chat"));
        private final JCheckBox dialogB = new JCheckBox(Language.getString("settings.ignoredEmotes.for.emoteDialog"));
        private final JCheckBox completionB = new JCheckBox(Language.getString("settings.ignoredEmotes.for.completion"));
        
        private final JCheckBox chat = new JCheckBox(Language.getString("settings.ignoredEmotes.for.chat"));
        private final JCheckBox dialog = new JCheckBox(Language.getString("settings.ignoredEmotes.for.emoteDialog"));
        private final JCheckBox completion = new JCheckBox(Language.getString("settings.ignoredEmotes.for.completion"));
        
        private final JTextArea before = new JTextArea();
        private final JTextField after = new JTextField();
        
        private final JLabel info = new JLabel();
        
        private final JToggleButton ignoredButton;
        private final JToggleButton unignoredButton;
        
        private Emoticon emote;
        private boolean save;
        private IgnoredEmotes.Item result;
        
        public EditIgnoredEmote(Window owner) {
            super(owner);
            setModal(true);
            setTitle(Language.getString("settings.ignoredEmotes.title"));
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            beforePanel = new JPanel(new GridBagLayout());
            
            before.setColumns(20);
            before.setEditable(false);
            before.setBackground(UIManager.getColor("TextField.inactiveBackground"));
            before.setBorder(after.getBorder());
            
            after.setEditable(false);
            after.setColumns(20);
            
            before.setFont(Font.decode(Font.MONOSPACED));
            after.setFont(Font.decode(Font.MONOSPACED));
            
            gbc = SettingsDialog.makeGbc(0, 0, 2, 1);
            add(info, gbc);
            
            gbc = SettingsDialog.makeGbc(0, 0, 2, 1, GridBagConstraints.CENTER);
            JLabel beforeLabel = new JLabel(Language.getString("settings.ignoredEmotes.before"));
            beforePanel.add(beforeLabel, gbc);
            
            chatB.setEnabled(false);
            dialogB.setEnabled(false);
            completionB.setEnabled(false);
            
            gbc = SettingsDialog.makeGbc(0, 1, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            beforePanel.add(before, gbc);
            
            gbc = SettingsDialog.makeGbcCloser(0, 2, 2, 1, GridBagConstraints.WEST);
            beforePanel.add(chatB, gbc);
            gbc = SettingsDialog.makeGbcCloser(0, 3, 2, 1, GridBagConstraints.WEST);
            beforePanel.add(dialogB, gbc);
            gbc = SettingsDialog.makeGbcCloser(0, 4, 2, 1, GridBagConstraints.WEST);
            beforePanel.add(completionB, gbc);
            
            gbc = SettingsDialog.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST);
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 0, 5, 0);
            add(beforePanel, gbc);
            
            gbc = SettingsDialog.makeGbc(0, 2, 2, 1);
            add(new JLabel(Language.getString("settings.ignoredEmotes.after")), gbc);
            
            gbc = SettingsDialog.makeGbc(0, 3, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(after, gbc);
            
            JPanel buttonPanel = new JPanel();
            ignoredButton = new JToggleButton(Language.getString("settings.ignoredEmotes.button.allIgnored"));
            GuiUtil.smallButtonInsets(ignoredButton);
            unignoredButton = new JToggleButton(Language.getString("settings.ignoredEmotes.button.notIgnored"));
            GuiUtil.smallButtonInsets(unignoredButton);
            
            buttonPanel.add(ignoredButton);
            buttonPanel.add(unignoredButton);
            gbc = SettingsDialog.makeGbcCloser(0, 4, 2, 1, GridBagConstraints.WEST);
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            add(buttonPanel, gbc);
            
            ignoredButton.addActionListener(e -> {
                boolean selected = ignoredButton.isSelected();
                chat.setSelected(selected);
                dialog.setSelected(selected);
                completion.setSelected(selected);
            });
            unignoredButton.addActionListener(e -> {
                boolean selected = unignoredButton.isSelected();
                chat.setSelected(!selected);
                dialog.setSelected(!selected);
                completion.setSelected(!selected);
            });
            
            gbc = SettingsDialog.makeGbcCloser(0, 5, 2, 1, GridBagConstraints.WEST);
            add(chat, gbc);
            gbc = SettingsDialog.makeGbcCloser(0, 6, 2, 1, GridBagConstraints.WEST);
            add(dialog, gbc);
            gbc = SettingsDialog.makeGbcCloser(0, 7, 2, 1, GridBagConstraints.WEST);
            add(completion, gbc);
            
            JButton okButton = new JButton(Language.getString("dialog.button.ok"));
            okButton.addActionListener(e -> {
                save = true;
                dispose();
            });
            gbc = SettingsDialog.makeGbc(0, 8, 1, 1, GridBagConstraints.WEST);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            add(okButton, gbc);
            
            JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
            cancelButton.addActionListener(e -> {
                dispose();
            });
            gbc = SettingsDialog.makeGbc(1, 8, 1, 1, GridBagConstraints.WEST);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 0.3;
            add(cancelButton, gbc);
            
            chat.setSelected(true);
            dialog.setSelected(true);
            completion.setSelected(true);
            
            chat.addItemListener(e -> updateResult());
            dialog.addItemListener(e -> updateResult());
            completion.addItemListener(e -> updateResult());
        }
        
        private void updateResult() {
            int in = 0;
            in += chat.isSelected() ? IgnoredEmotes.CHAT : 0;
            in += dialog.isSelected() ? IgnoredEmotes.EMOTE_DIALOG : 0;
            in += completion.isSelected() ? IgnoredEmotes.TAB_COMPLETION : 0;
            ignoredButton.setSelected(in == IgnoredEmotes.ALL);
            unignoredButton.setSelected(in == 0);
            IgnoredEmotes.Item item = IgnoredEmotes.Item.create(emote, in);
            if (in == 0) {
                after.setText("<not ignored>");
            }
            else {
                after.setText(item.toString());
                after.setCaretPosition(0);
            }
            result = item;
        }
        
        public IgnoredEmotes.Item showDialog(Component parent, Emoticon emote, Collection<IgnoredEmotes.Item> beforeData) {
            this.emote = emote;
            beforePanel.setVisible(!beforeData.isEmpty());
            info.setText("<html><body style='width:300px'>"
                    + Language.getString("settings.ignoredEmotes.editInfo", emote) + "<br><br>"
                    + Language.getString("settings.ignoredEmotes.editInfo2"));
            if (!beforeData.isEmpty()) {
                before.setText(StringUtil.join(beforeData, "\n"));
                before.setCaretPosition(0);
                int combinedIn = 0;
                for (IgnoredEmotes.Item item : beforeData) {
                    combinedIn |= item.context;
                }
                chatB.setSelected((combinedIn & IgnoredEmotes.CHAT) != 0);
                dialogB.setSelected((combinedIn & IgnoredEmotes.EMOTE_DIALOG) != 0);
                completionB.setSelected((combinedIn & IgnoredEmotes.TAB_COMPLETION) != 0);
            }
            result = null;
            updateResult();
            pack();
            setLocationRelativeTo(parent);
            save = false;
            setVisible(true);
            if (save) {
                return result;
            }
            return null;
        }
        
    }
    
    public static void main(String[] args) {
        List<IgnoredEmotes.Item> matches = new ArrayList<>();
        matches.add(IgnoredEmotes.Item.parse("Kappa for:c"));
        matches.add(IgnoredEmotes.Item.parse("Kappa for:t"));
        Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, "Kappa", null);
//        b.setStringId("123abc");
        System.out.println(new EditIgnoredEmote(null).showDialog(null, b.build(), matches));
        System.exit(0);
    }
    
    private static class LocalEmotesDialog extends LazyDialog {
        

        
        private final TableEditor<Emoticon> editor;
        private final SettingsDialog d;
        
        private LocalEmotesDialog(SettingsDialog d) {
            this.d = d;
            this.editor = new TableEditor<>(TableEditor.SORTING_MODE_SORTED, false);
            editor.setItemEditor(() -> new TableEditor.ItemEditor<Emoticon>() {
                @Override
                public Emoticon showEditor(Emoticon preset, Component c, boolean edit, int column) {
                    JOptionPane.showMessageDialog(c, "Emotes should be added through the Emote Context Menu (e.g. right-click on an Emote in chat).");
                    return null;
                }
            });
            editor.setModel(new ListTableModel<Emoticon>(new String[]{"Image", "Code", "Id"}) {

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    switch (columnIndex) {
                        case 0: return get(rowIndex);
                        case 1: return get(rowIndex).code;
                        case 2: return get(rowIndex).stringId;
                    }
                    return null;
                }
            });
            editor.setRendererForColumn(0, new EmoteRenderer(new CachedImageUser() {
                @Override
                public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
                    editor.repaint();
                }
            }));
        }
        
        @Override
        public JDialog createDialog() {
            return new Dialog();
        }
        
        private class Dialog extends JDialog {

            private Dialog() {
                super(d);
                setTitle("Local Emotes");
                setModal(true);
                setLayout(new GridBagLayout());

                GridBagConstraints gbc;
                gbc = SettingsDialog.makeGbc(0, 1, 1, 1);
                gbc.weightx = 1;
                gbc.weighty = 1;
                gbc.fill = GridBagConstraints.BOTH;
                add(editor, gbc);

                pack();
                GuiUtil.installEscapeCloseOperation(this);
            }
        }
        
        public void setData(Collection<Emoticon> data) {
            editor.setData(new ArrayList<>(data));
        }
        
        public List<Emoticon> getData() {
            return editor.getData();
        }
        
    }
    
    public static class EmoteRenderer extends JLabel implements TableCellRenderer {

        private final CachedImageUser emoticonUser;
        
        public EmoteRenderer(CachedImageUser emoticonUser) {
            this.emoticonUser = emoticonUser;
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            
            // Just return if null
            if (value == null) {
                return this;
            }
            
            Emoticon emote = (Emoticon) value;
            setIcon(emote.getIcon(emoticonUser).getImageIcon());
            return this;
        }

    }
    
}
