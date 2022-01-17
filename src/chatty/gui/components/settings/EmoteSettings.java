
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.lang.Language;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonUser;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

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
        ignoredEmotesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        ignoredEmotesButton.addActionListener(e -> {
            ignoredEmotesDialog.setLocationRelativeTo(d);
            ignoredEmotesDialog.setVisible(true);
        });
        main.add(ignoredEmotesButton,
                d.makeGbc(2, 0, 3, 1, GridBagConstraints.EAST));
        
        //------------
        // Emote Size
        //------------
        // Emote Scale Defintion, used for both Chat and Emotes Dialog
        final Map<Long, String> scaleDef = new LinkedHashMap<>();
        for (int i=50;i<=200;i += 10) {
            if (i == 10) {
                scaleDef.put((long)i, "Normal");
            } else {
                scaleDef.put((long)i, (i)+"%");
            }
        }
        
        // Chat Emote Scale
        main.add(new JLabel(Language.getString("settings.emoticons.chatScale")),
                d.makeGbc(0, 3, 1, 1, GridBagConstraints.WEST));
        ComboLongSetting emoteScale = new ComboLongSetting(scaleDef);
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
        ComboLongSetting emoteScaleDialog = new ComboLongSetting(scaleDef);
        d.addLongSetting("emoteScaleDialog", emoteScaleDialog);
        main.add(emoteScaleDialog, d.makeGbc(1, 4, 1, 1, GridBagConstraints.CENTER));
        
        //-------
        // Other
        //-------
        main.add(d.addSimpleBooleanSetting("closeEmoteDialogOnDoubleClick"),
                d.makeGbc(0, 5, 3, 1));
        
        //----------
        // Cheering
        //----------
        Map<String, String> cheeringTypeDef = new LinkedHashMap<>();
        cheeringTypeDef.put("none", Language.getString("settings.emoticons.cheers.option.text"));
        cheeringTypeDef.put("static", Language.getString("settings.emoticons.cheers.option.static"));
        cheeringTypeDef.put("animated", Language.getString("settings.emoticons.cheers.option.animated"));
        ComboStringSetting cheersType = new ComboStringSetting(cheeringTypeDef);
        
        d.addStringSetting("cheersType", cheersType);
        
        main.add(new JLabel(Language.getString("settings.emoticons.cheers")),
                d.makeGbc(0, 6, 1, 1, GridBagConstraints.CENTER));
        main.add(cheersType,
                d.makeGbc(1, 6, 2, 1, GridBagConstraints.WEST));
        
        //--------------------------
        // Animated
        //--------------------------
        main.add(d.addSimpleBooleanSetting("animatedEmotes"),
                d.makeGbc(0, 7, 3, 1, GridBagConstraints.WEST));
        
        //==========
        // FFZ/BTTV
        //==========
        
        JPanel other = addTitledPanel(Language.getString("settings.section.3rdPartyEmotes"), 1);
        other.add(d.addSimpleBooleanSetting("bttvEmotes"),
                d.makeGbcCloser(0, 0, 1, 1, GridBagConstraints.WEST));
        
        other.add(d.addSimpleBooleanSetting("showAnimatedEmotes"),
                d.makeGbc(1, 0, 1, 1, GridBagConstraints.WEST));

        final JCheckBox ffz = d.addSimpleBooleanSetting("ffz");
        other.add(ffz,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzMod = d.addSimpleBooleanSetting("ffzModIcon");
        other.add(ffzMod,
                d.makeGbcSub(0, 2, 1, 1, GridBagConstraints.WEST));
        
        final JCheckBox ffzEvent = d.addSimpleBooleanSetting("ffzEvent");
        other.add(ffzEvent,
                d.makeGbcCloser(1, 2, 1, 1, GridBagConstraints.WEST));
        
        SettingsUtil.addSubsettings(ffz, ffzMod, ffzEvent);

        //=======
        // Emoji
        //=======
        JPanel emojiSettings = addTitledPanel(Language.getString("settings.section.emoji"), 2);
        
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
        localEmotesButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
        localEmotesButton.addActionListener(e -> {
            localEmotesDialog.setLocationRelativeTo(d);
            localEmotesDialog.setVisible(true);
        });
        localEmoteSettings.add(localEmotesButton,
                d.makeGbc(0, 1, 2, 1, GridBagConstraints.WEST));
    }

    public void setData(Collection<Emoticon> data) {
        localEmotesDialog.setData(data);
    }
    
    public Collection<Emoticon> getData() {
        return localEmotesDialog.getData();
    }
    
    private static class IgnoredEmotesDialog extends JDialog {
        
        private IgnoredEmotesDialog(SettingsDialog d) {
            super(d);
            setTitle(Language.getString("settings.ignoredEmotes.title"));
            setModal(true);
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            gbc = d.makeGbc(0, 0, 1, 1);
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1;
            gbc.weighty = 1;
            add(d.addListSetting("ignoredEmotes", "Ignored Emote", 180, 220, false, true), gbc);
            
            gbc = d.makeGbc(1, 0, 1, 1);
            gbc.anchor = GridBagConstraints.NORTH;
            add(new JLabel("<html><body style='width:160px;'>"
                    + "<p style='padding:5px;'>"+Language.getString("settings.ignoredEmotes.info1")+"</p>"
                    + "<p style='padding:5px;'>"+Language.getString("settings.ignoredEmotes.info2")+"</p>"), gbc);
            
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
    
    private static class LocalEmotesDialog extends JDialog {
        
        private final TableEditor<Emoticon> editor;
        
        private LocalEmotesDialog(SettingsDialog d) {
            super(d);
            setTitle("Local Emotes");
            setModal(true);
            setLayout(new GridBagLayout());
            
            editor = new TableEditor<>(TableEditor.SORTING_MODE_SORTED, false);
            editor.setItemEditor(new TableEditor.ItemEditor() {
                @Override
                public Object showEditor(Object preset, Component c, boolean edit, int column) {
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
            editor.setRendererForColumn(0, new EmoteRenderer(new EmoticonUser() {
                @Override
                public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
                    editor.repaint();
                }
            }));
            
            GridBagConstraints gbc;
            gbc = SettingsDialog.makeGbc(0, 1, 1, 1);
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            add(editor, gbc);
            
            pack();
            GuiUtil.installEscapeCloseOperation(this);
        }
        
        public void setData(Collection<Emoticon> data) {
            editor.setData(new ArrayList<>(data));
        }
        
        public List<Emoticon> getData() {
            return editor.getData();
        }
        
    }
    
    public static class EmoteRenderer extends JLabel implements TableCellRenderer {

        private final EmoticonUser emoticonUser;
        
        public EmoteRenderer(EmoticonUser emoticonUser) {
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
