
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.GuiUtil.SimpleMouseListener;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.gui.components.settings.SettingsUtil;
import chatty.lang.Language;
import chatty.util.ChattyMisc;
import chatty.util.Debugging;
import chatty.util.MiscUtil;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotesApi;
import chatty.util.TwitchEmotesApi.EmotesetInfo;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticons;
import chatty.util.api.CachedImage;
import chatty.util.api.CachedImage.ImageType;
import chatty.util.colors.ColorCorrection;
import chatty.util.colors.ColorCorrectionNew;
import chatty.util.colors.HtmlColors;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import chatty.util.api.CachedImage.CachedImageUser;
import chatty.util.api.Emoticon.TypeCategory;
import chatty.util.api.EmoticonFavorites.Favorite;
import chatty.util.api.IgnoredEmotes;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Dialog showing emoticons that can be clicked on to insert them in the last
 * active channel inputbox. Also allows to open the emote context menu.
 * 
 * @author tduva
 */
public class EmotesDialog extends JDialog {
    
    //--------------------
    // Updating Constants
    //--------------------
    public final int UPDATE_EMOTESET_CHANGED = 1;
    public final int UPDATE_CHANNEL_CHANGED = 2;
    public final int UPDATE_FAVORITES = 4;
    
    //------------------
    // Layout Constants
    //------------------
    private static final Insets TITLE_INSETS = new Insets(5,8,0,8);
    private static final Insets SUBTITLE_INSETS = new Insets(6,4,2,4);
    private static final Insets SUBTITLE_INSETS_SMALLER_MARGIN = new Insets(1,2,0,2);
    private static final Insets EMOTE_INSETS = new Insets(4,10,4,10);
    
    //----------------
    // Page Constants
    //----------------
    private static final String FAVORITE_EMOTES = Language.getString("emotesDialog.tab.favorites");
    private static final String MY_EMOTES = Language.getString("emotesDialog.tab.myEmotes");
    private static final String CHANNEL_EMOTES = Language.getString("emotesDialog.tab.channel");
    private static final String TWITCH_EMOTES = Language.getString("emotesDialog.tab.twitch");
    private static final String OTHER_EMOTES = Language.getString("emotesDialog.tab.other");
    private static final String EMOJI = "Emoji";
    private static final String EMOTE_DETAILS = "Emote Details";
    private static final String BITS = "B";
    
    //--------
    // Layout
    //--------
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel emotesPanel;
    private final List<EmotesPanel> panels = new ArrayList<>();
    private final Map<JToggleButton, EmotesPanel> buttons = new HashMap<>();
    private final ButtonGroup buttonGroup;
    private final EmotesPanel defaultPanel;
    private final Color emotesBackground;
    private final Color emotesForeground;
    private final JButton refreshButton = new JButton(new ImageIcon(EmotesDialog.class.getResource("view-refresh.png")));
    private final JPanel topPanel;
    
    /**
     * GridBagConstraints for adding titles/emotes.
     */
    private final GridBagConstraints gbc;
    
    //------------
    // References
    //------------
    private final Emoticons emoteManager;
    private final CachedImageUser emoteUser;
    private final SimpleMouseListener mouseListener;
    private final ContextMenuListener contextMenuListener;
    private final MainGui main;
    
    //------------------
    // State / Settings
    //------------------
    private EmotesPanel currentPanel;
    private Set<String> localUserEmotesets = new HashSet<>();
    private String currentStream;
    private String tempStream;
    private Emoticon detailsEmote;
    private float scale;
    private ImageType imageType = ImageType.ANIMATED_DARK;
    private boolean closeOnDoubleClick = true;
    private final Set<String> hiddenEmotesets = new HashSet<>();
    private String search;
    private JPanel searchPanel;
    private final JToggleButton searchButton;
    
    private static final String SPECIAL_SET_LOCAL = "$ChattyLocalEmotes";
    private static final String SPECIAL_SET_OTHER = "$ChattyOtherEmotes";
    
    public EmotesDialog(Window owner, Emoticons emotes, final MainGui main, ContextMenuListener contextMenuListener) {
        super(owner);
        
        this.main = main;
        emoteUser = new CachedImageUser() {

            @Override
            public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
//                repaint = true;
                repaint();
            }
        };
        
        // TODO: Focusable or maybe just when clicked on emote to insert code?
//        this.setFocusable(false);
//        this.setFocusableWindowState(false);
        this.contextMenuListener = contextMenuListener;
        this.emoteManager = emotes;
        Color bg = HtmlColors.decode(main.getSettings().getString("backgroundColor"));
        // Offset the color a bit so it doesn't merge with the background of
        // chat as much
        if (ColorCorrection.isLightColor(bg)) {
            emotesBackground = ColorCorrectionNew.makeDarker(bg, 0.982f);
        }
        else {
            // Dark background seemed to require a larger difference
            emotesBackground = ColorCorrectionNew.makeDarker(bg, 0.96f);
        }
        emotesForeground = HtmlColors.decode(main.getSettings().getString("foregroundColor"));
        setResizable(true);

        //------------------
        // Add Emote Panels
        //------------------
        panels.add(new FavoritesPanel(FAVORITE_EMOTES,
                UPDATE_CHANNEL_CHANGED | UPDATE_EMOTESET_CHANGED |
                        UPDATE_FAVORITES));
        panels.add(new SubemotesPanel(MY_EMOTES, UPDATE_EMOTESET_CHANGED));
        panels.add(new ChannelEmotesPanel(CHANNEL_EMOTES,
                UPDATE_CHANNEL_CHANGED | UPDATE_EMOTESET_CHANGED));
        panels.add(new TwitchEmotesPanel(TWITCH_EMOTES,0));
        panels.add(new OtherEmotesPanel(OTHER_EMOTES,0));
        if (Chatty.DEBUG) {
            panels.add(new BitsPanel(BITS, 0));
        }
        // Not quite ready yet
        //panels.add(new EmojiPanel(EMOJI, 0));
        
        // Get second panel as default
        defaultPanel = panels.get(1);
        
        //--------------------------
        // Buttons for those panels
        //--------------------------
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                EmotesPanel emotesPanel = buttons.get(e.getSource());
                if (emotesPanel != null) {
                    showPanel(emotesPanel);
                }
            }
        };
        
        buttonGroup = new ButtonGroup();
        topPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        for (EmotesPanel p : panels) {
            JToggleButton button = new JToggleButton(p.label);
            buttons.put(button, p);
            buttonGroup.add(button);
            GuiUtil.smallButtonInsets(button);
            buttonPanel.add(button);
            button.addActionListener(buttonAction);
        }
        int buttonHeight = buttons.keySet().iterator().next().getPreferredSize().height;
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(2, (int)(buttonHeight*0.9)));
        refreshButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        refreshButton.setPreferredSize(new Dimension(buttonHeight, buttonHeight));
        
        searchButton = new JToggleButton(new ImageIcon(EmotesDialog.class.getResource("search.png")));
        searchButton.setMargin(GuiUtil.SMALLER_BUTTON_INSETS);
        searchButton.setPreferredSize(new Dimension(buttonHeight, buttonHeight));
        searchButton.addActionListener(e -> toggleSearch());
        
        buttonPanel.add(sep);
        buttonPanel.add(refreshButton);
        buttonPanel.add(searchButton);
        topPanel.add(buttonPanel, BorderLayout.NORTH);
        add(topPanel, BorderLayout.NORTH);
        
        // Add Details panel, which doesn't have a button
        panels.add(new EmoteDetailPanel(EMOTE_DETAILS, 0));

        //---------------------------------
        // Listener for clicking on emotes
        //---------------------------------
        mouseListener = new SimpleMouseListener() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2 && closeOnDoubleClick && !e.isShiftDown()) {
                        setVisible(false);
                    } else {
                        EmoteLabel label = (EmoteLabel) e.getSource();
                        if (!label.noInsert) {
                            main.insert(Emoticons.toWriteable(label.code), true, true);
                        }
                    }
                }
            }

            @Override
            public void contextMenu(MouseEvent e) {
                openContextMenu(e);
            }
            
        };
        //--------------------------
        // Refresh button
        //--------------------------
        refreshButton.addActionListener(e -> {
            if (currentPanel != null) {
                if (currentPanel.label.equals(MY_EMOTES)) {
                    main.refreshEmotes("user", null);
                }
                else if (currentPanel.label.equals(CHANNEL_EMOTES)) {
                    String stream = tempStream != null ? tempStream : currentStream;
                    main.refreshEmotes("channel", stream);
                }
                else if (currentPanel.label.equals(TWITCH_EMOTES)) {
                    main.refreshEmotes("globaltwitch", null);
                }
                else if (currentPanel.label.equals(OTHER_EMOTES)) {
                    main.refreshEmotes("globalother", null);
                }
                refreshButton.setEnabled(false);
                Timer timer = new Timer(1000, evt -> refreshButton.setEnabled(true));
                timer.setRepeats(false);
                timer.start();
            }
        });
        
        //------------------------------------
        // Add panel holding all emote panels
        //------------------------------------
        // This is using a CardLayout, showing just one of the added panels,
        // using the panel label as constraint to select which one to show
        emotesPanel = new JPanel();
        emotesPanel.setLayout(cardLayout);
        for (EmotesPanel panel : panels) {
            emotesPanel.add(wrapPanel(panel), panel.label);
        }
        //emotesPanel.setSize(0, 0);
        add(emotesPanel, BorderLayout.CENTER);
        
        gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 0;
        
        pack();
        setMinimumSize(getPreferredSize());
        
        setSize(320,300);
    }
    
    public void setCloseOnDoubleClick(boolean enabled) {
        this.closeOnDoubleClick = enabled;
    }

    /**
     * Wrap the given component into a JPanel, which aligns it at the top. There
     * may be an easier/more direct way of doing this. Also add it to a scroll
     * pane.
     * 
     * @param panel
     * @return 
     */
    private JComponent wrapPanel(JComponent panel) {
        panel.setBackground(emotesBackground);
        JPanel outer = new JPanel();
        outer.setLayout(new GridBagLayout());
        outer.setBackground(emotesBackground);
        GridBagConstraints gbcTest = new GridBagConstraints();
        gbcTest.fill = GridBagConstraints.HORIZONTAL;
        gbcTest.weightx = 1;
        gbcTest.weighty = 1;
        gbcTest.anchor = GridBagConstraints.NORTH;
        outer.add(panel, gbcTest);
        //outer.setSize(0, 0);
        
        // Add and configure scroll pane
        JScrollPane scroll = new JScrollPane(outer);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        return scroll;
    }
    
    /**
     * On right-click on an emote, open the appropriate context menu.
     * 
     * @param e 
     */
    private void openContextMenu(MouseEvent e) {
        CachedImage<Emoticon> emote = ((EmoteLabel) e.getSource()).emote;
        JPopupMenu m = new EmoteContextMenu(emote, contextMenuListener);
        m.show(e.getComponent(), e.getX(), e.getY());
    }
    
    private void toggleSearch() {
        if (search != null) {
            closeSearch();
            return;
        }
        search = "";
        
        searchPanel = new JPanel();
        
        JLabel label = new JLabel("Search:");
        JTextField searchField = new JTextField(10);
        label.setLabelFor(searchField);
        
        JButton closeButton = new JButton(Language.getString("dialog.button.close"));
        GuiUtil.smallButtonInsets(closeButton);
        
        GuiUtil.addChangeListener(searchField.getDocument(), e -> {
            search = StringUtil.toLowerCase(searchField.getText());
            update();
        });
        closeButton.addActionListener(e -> {
            closeSearch();
        });
        
        searchPanel.add(label);
        searchPanel.add(searchField);
        searchPanel.add(closeButton);
        
        topPanel.add(searchPanel, BorderLayout.CENTER);
        revalidate();
        searchField.requestFocusInWindow();
    }
    
    private void closeSearch() {
        search = null;
        topPanel.remove(searchPanel);
        searchButton.setSelected(false);
        revalidate();
        update();
    }
    
    /**
     * Opens the dialog, using the given emotesets and stream.
     *
     * @param localUserEmotesets
     * @param stream
     */
    public void showDialog(Set<String> localUserEmotesets, String stream) {
        if (stream != null && !stream.equals(this.currentStream)) {
            setUpdated(UPDATE_CHANNEL_CHANGED);
        }
        if (stream != null) {
            this.currentStream = stream;
        }
        if (localUserEmotesets != null && !localUserEmotesets.equals(this.localUserEmotesets)) {
            setUpdated(UPDATE_EMOTESET_CHANGED);
        }
        this.localUserEmotesets = new HashSet<>(localUserEmotesets);
        updateTitle();
        showEmotes();
        setVisible(true);
        //update(); // Only for testing if the layouting still works if updated
    }
    
    /**
     * The temp stream shows in the Channel tab and provides a button to go back
     * to the dialog's general stream.
     * 
     * @param tempStream 
     */
    public void setTempStream(String tempStream) {
        if (tempStream != null && !tempStream.equals(this.currentStream)) {
            setUpdated(UPDATE_CHANNEL_CHANGED);
            this.tempStream = tempStream;
        }
    }
    
    public void showChannelEmotes() {
        showPanelByName(CHANNEL_EMOTES);
    }
    
    public void showEmoteDetails(Emoticon emote) {
        detailsEmote = emote;
        getPanelByName(EMOTE_DETAILS).updateEmotes();
        showPanelByName(EMOTE_DETAILS);
        buttonGroup.clearSelection();
    }
    
    /**
     * Reloads the current emotes if visible. This can be used if e.g. new
     * emotes have been added.
     */
    public void update() {
        setEmotesUpdated();
        if (isVisible()) {
            showEmotes();
        }
    }

    /**
     * Changes the current stream and updates the channel-specific emotes if
     * necessary.
     * 
     * @param stream The name of the stream
     * @param emotesets
     */
    public void updateStream(String stream, Set<String> emotesets) {
        if (!isVisible()) {
            return;
        }
        if (stream != null && stream.equals(this.currentStream)) {
            return;
        }
        this.currentStream = stream;
        updateTitle();
        setUpdated(UPDATE_CHANNEL_CHANGED);
        showEmotes();
        updateEmotesets(stream, emotesets);
    }
    
    private void setEmotesUpdated() {
        for (EmotesPanel p : panels) {
            p.setUpdated();
        }
    }
    
    private void setUpdated(int what) {
        for (EmotesPanel p : panels) {
            p.setUpdated(what);
        }
    }
    
    /**
     * Updates the emotesets that are used to display the correct subemotes and
     * refreshes the subscriber emotes if necessary.
     *
     * @param stream The stream the emotesets are from
     * @param emotesets The Set of emotesets
     */
    public void updateEmotesets(String stream, Set<String> emotesets) {
        if (!isVisible()
                || emotesets == null
                || emotesets.equals(this.localUserEmotesets)
                || currentStream == null
                || !currentStream.equals(stream)) {
            return;
        }
        this.localUserEmotesets = new HashSet<>(emotesets);
        setUpdated(UPDATE_EMOTESET_CHANGED);
        showEmotes();
    }
    
    public void updateEmotesetInfo() {
        setUpdated(UPDATE_EMOTESET_CHANGED);
        if (isVisible()) {
            showEmotes();
        }
    }
    
    public void favoritesUpdated() {
        setUpdated(UPDATE_FAVORITES);
        if (isVisible()) {
            showEmotes();
        }
    }
    
    /**
     * Sets the scale to show the emoticons at.
     * 
     * @param percentage 
     */
    public void setEmoteScale(int percentage) {
        float newScale = (float)(percentage / 100.0);
        if (newScale != scale) {
            scale = newScale;
            update();
        }
    }
    
    public void setEmoteImageType(ImageType imageType) {
        if (this.imageType != imageType) {
            this.imageType = imageType;
            update();
        }
    }
    
    /**
     * Set emotesets for hidden sections. Does not update already loaded pages.
     * 
     * @param sets 
     */
    public void setHiddenEmotesets(Collection<String> sets) {
        this.hiddenEmotesets.clear();
        this.hiddenEmotesets.addAll(sets);
    }
    
    /**
     * Get the current emotesets for hidden sections.
     * 
     * @return 
     */
    public Collection<String> getHiddenEmotesets() {
        return hiddenEmotesets;
    }

    /**
     * Sets the title according to the current stream.
     */
    private void updateTitle() {
        setTitle(Language.getString("emotesDialog.title", currentStream == null ? "-" : "#"+currentStream));
    }
    
    private void showPanel(EmotesPanel panel) {
        currentPanel = panel;
        panel.update();
        cardLayout.show(emotesPanel, panel.label);
        for (JToggleButton button : buttons.keySet()) {
            if (buttons.get(button) == panel) {
                button.setSelected(true);
            }
        }
        if (!panel.label.equals(EMOTE_DETAILS)) {
            detailsEmote = null;
        }
        updateRefreshButton();
    }
    
    private void updateRefreshButton() {
        if (currentPanel == null) {
            return;
        }
        boolean enabled = currentPanel.label.equals(MY_EMOTES)
                || currentPanel.label.equals(CHANNEL_EMOTES)
                || currentPanel.label.equals(TWITCH_EMOTES)
                || currentPanel.label.equals(OTHER_EMOTES);
        refreshButton.setEnabled(enabled);
        if (enabled) {
            refreshButton.setToolTipText(Language.getString("emotesDialog.refresh"));
        } else {
            refreshButton.setToolTipText(Language.getString("emotesDialog.refreshInactive"));
        }
    }
    
    private void showPanelByName(String name) {
        EmotesPanel panel = getPanelByName(name);
        if (panel != null) {
            showPanel(panel);
        }
    }
    
    private EmotesPanel getPanelByName(String name) {
        for (EmotesPanel panel : panels) {
            if (panel.label.equals(name)) {
                return panel;
            }
        }
        return null;
    }

    /**
     * Shows the selected emotes page (depending on the pressed button),
     * subemotes by default.
     */
    private void showEmotes() {
        if (detailsEmote != null) {
            return;
        }
        for (JToggleButton button : buttons.keySet()) {
            if (button.isSelected()) {
                showPanel(buttons.get(button));
                return;
            }
        }
        // No button selected, so set default
        showPanel(defaultPanel);
    }
    
    /**
     * A single emote displayed in a JLabel. Saves a reference to the actual
     * Emoticon object, so it can be retrieved when opening the context menu.
     */
    private static class EmoteLabel extends JLabel {
        
        private static final Border BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        
        public final String code;
        public final CachedImage<Emoticon> emote;
        public final boolean noInsert;

        public EmoteLabel(Emoticon emote, SimpleMouseListener mouseListener, float scale,
                ImageType imageType, CachedImageUser emoteUser, Border extraBorder) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            GuiUtil.addSimpleMouseListener(this, mouseListener);
            CachedImage<Emoticon> emoteImage = emote.getIcon(scale, 0, imageType, emoteUser);
            this.code = emote.code;
            this.emote = emoteImage;
            setIcon(emoteImage.getImageIcon());
            setToolTipText(emote.code);
            if (emote.type == Emoticon.Type.EMOJI) {
                setToolTipText(emote.getInfos().toString());
            }
            if (emote.subType == Emoticon.SubType.CHEER) {
                setToolTipText(emote.getInfos().toString());
            }
            if (emote.subType == Emoticon.SubType.CHEER) {
                noInsert = true;
            } else {
                noInsert = false;
            }
            if (extraBorder != null) {
                setBorder(BorderFactory.createCompoundBorder(BORDER, extraBorder));
            }
            else {
                setBorder(BORDER);
            }
        }
        
    }
    
    private EmoteLabel createEmoteLabel(Emoticon emote, float scale) {
        return new EmoteLabel(emote, mouseListener, scale, imageType, emoteUser, null);
    }
    
    //==================
    // Emoticon sorting
    //==================
    
    private static class SortEmotes implements Comparator<Emoticon> {
        
        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            int compareType = o1.type.compareTo(o2.type);
            if (compareType != 0) {
                return compareType;
            }
            int compareEmoteset = SORT_EMOTESETS.compare(o1.emoteset, o2.emoteset);
            if (compareEmoteset != 0) {
                return compareEmoteset;
            }
            return o1.code.compareToIgnoreCase(o2.code);
        }
    }
    
    private static final Comparator<String> SORT_EMOTESETS = new SortEmotesets();
    
    private static class SortEmotesets implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            int compareEmoteset = 0;
            // Try emoteset length (amount of digits)
            if (o1 != null && o2 != null) {
                compareEmoteset = o1.length() - o2.length();
            }
            if (compareEmoteset != 0) {
                return compareEmoteset;
            }
            // Try emoteset lexicographically
            return Objects.compare(o1, o2, StringUtil.NULL_COMPARATOR);
        }
        
    }
    
    //==============
    // Emote Panels
    //==============
    /**
     * General emotes panel which has methods for adding emotes in different
     * ways. Override updateEmotes() which is called when emotes should be
     * updated (for example when new data is received).
     */
    private abstract class EmotesPanel extends JPanel {

        private boolean shouldUpdate;
        private final String label;
        private final int updateOn;
        private final List<Emoticon> ignoredEmotes = new ArrayList<>();
        
        EmotesPanel(String name, int updateOn) {
            super(new GridBagLayout());
            this.label = name;
            this.updateOn = updateOn;
        }

        /**
         * Update the emotes on this panel the next time it is shown, but only
         * if the update reason matches what this panel is configured for.
         * 
         * @param reason 
         */
        public void setUpdated(int reason) {
            if ((updateOn & reason) == reason) {
                shouldUpdate = true;
            }
        }
        
        public void setUpdated() {
            shouldUpdate = true;
        }
        
        public void update() {
            if (shouldUpdate) {
                updateEmotes();
            }
            shouldUpdate = false;
        }
        
        protected abstract void updateEmotes();
        
        /**
         * Clears everything to get ready to add emotes.
         */
        void reset() {
            removeAll();
            //panel.setSize(1,1);
            //panel.revalidate();
            gbc.gridy = 0;
            ignoredEmotes.clear();
        }
        
        /**
         * Section should show the title only if at least one of the given sets
         * is hidden.
         * 
         * @param emotesets
         * @return 
         */
        private boolean isHidden(Collection<String> emotesets) {
            for (String set : emotesets) {
                if (hiddenEmotesets.contains(set)) {
                    return true;
                }
            }
            return false;
        }
        
        boolean isHidden(String emoteset) {
            return hiddenEmotesets.contains(emoteset);
        }
        
        /**
         * Adds the emotes of the given emoteset. Includes the name of the
         * stream if available.
         *
         * @param emoteset The emoteset
         * @param allowHide Enable hide feature
         */
        void addEmotesBySet(String emoteset, boolean allowHide) {
            String stream = emoteManager.getLabelByEmoteset(emoteset);
            if (stream == null) {
                stream = "-";
            }
            Set<Emoticon> emotes = emoteManager.getEmoticonsBySet(emoteset);
            String title = String.format("%s [%s]", stream, emoteset);
            EmotesPanel.this.addEmotesSection(emotes, title, emoteset);
        }
        
        /**
         * Adds the given emotesets under a single title, sorted by emoteset.
         * 
         * @param titlePrefix Title prefix (emotesets and emotecount added
         * automatically)
         * @param emotesets The emotesets to display
         * @param allowHide Enable hide feature
         * @return true if any emotes have been added, false otherwise
         */
        boolean addEmotesBySets(String titlePrefix, Set<EmotesetInfo> emotesets, boolean allowHide) {
            List<String> sets = new ArrayList<>();
            for (EmotesetInfo set : emotesets) {
                sets.add(set.emoteset_id);
            }
            Collections.sort(sets);
            List<Emoticon> sorted = new ArrayList<>();
            for (String set : sets) {
                sorted.addAll(emoteManager.getEmoticonsBySet(set));
            }
            Set<String> hideIds = allowHide ? new HashSet<>(sets) : null;
            String title = String.format("%s %s",
                    titlePrefix,
                    StringUtil.shortenTo(sets.toString(), 14));
            return EmotesPanel.this.addEmotesSection(sorted, title, hideIds);
        }
        
        void addTitle(String title) {
            addTitle(title, null, null);
        }
    
        /**
         * Adds a title (label with seperating line).
         *
         * @param title The text of the title
         * @param sets Emotesets that will be added under this title (for hide
         * feature)
         */
        void addTitle(String title, String tooltip, Collection<String> sets) {
            JLabel titleLabel = new JLabel("<html><body style='width:"+(EmotesDialog.this.getWidth() - 80)+"'>"+Helper.htmlspecialchars_encode(title)+"</html>");
            if (tooltip != null) {
                titleLabel.setToolTipText(tooltip);
            }
            titleLabel.setForeground(emotesForeground);
            titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, titleLabel.getForeground()));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = TITLE_INSETS;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(titleLabel, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
            if (sets != null) {
                // If sets are given, allow clicking on title to hide/unhide
                GuiUtil.addSimpleMouseListener(titleLabel, new SimpleMouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            if (isHidden(sets)) {
                                hiddenEmotesets.removeAll(sets);
                            }
                            else {
                                hiddenEmotesets.addAll(sets);
                            }
                            updateEmotes();
                        }
                    }

                });
                titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                
                if (!sets.isEmpty()
                        && !sets.iterator().next().equals(SPECIAL_SET_LOCAL)
                        && !sets.iterator().next().equals(SPECIAL_SET_OTHER)) {
                    GuiUtil.addSimpleMouseListener(titleLabel, new SimpleMouseListener() {

                        @Override
                        public void contextMenu(MouseEvent e) {
                            ContextMenu m = new ContextMenu() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    Set<Emoticon> all = new HashSet<>();
                                    for (String set : sets) {
                                        all.addAll(emoteManager.getEmoticonsBySet(set));
                                    }
                                    if (e.getActionCommand().equals("add")) {
                                        main.localEmotes.add(all);
                                    }
                                    else {
                                        main.localEmotes.remove(all);
                                    }
                                }
                            };
                            m.addItem("add", "Add local emotes");
                            m.addItem("remove", "Remove local emotes");
                            m.show(e.getComponent(), e.getX(), e.getY());
                        }

                    });
                }
            }
        }
        
        void addSubtitle(String title, boolean smallMargin) {
            addSubtitle(title, smallMargin, false);
        }
    
        /**
         * Adds some centered text without a seperating line and with a slightly
         * subdued color.
         *
         * @param title The text to add
         * @param smallMargin If true, uses smaller margins (for use below
         * emotes)
         */
        void addSubtitle(String title, boolean smallMargin, boolean links) {
            JComponent titleLabel;
            if (links) {
                titleLabel = new LinkLabel(title, main.getLinkLabelListener());
            }
            else {
                titleLabel = new JLabel(title);
            }
            // Usually gray should be readable, but just in case
            titleLabel.setForeground(ColorCorrection.correctReadability(Color.GRAY, emotesBackground));
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.insets = smallMargin ? SUBTITLE_INSETS_SMALLER_MARGIN : SUBTITLE_INSETS;
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(titleLabel, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
        }
        
        /**
         * Adds the given emotes to a new panel.
         *
         * @param emotes The emotes to add
         */
        private void addEmotesPanel(Collection<Emoticon> emotes) {
            JPanel panel = new JPanel();
            panel.setBackground(emotesBackground);
            panel.setLayout(new WrapLayout());
            /**
             * Using getParent() twice to get to JScrollPane viewport width,
             * however it still doesn't always seem to work, depending on the
             * width of the dialog. Substracting too much increases the gap in
             * between panels (probably because it layouts the emotes in a
             * narrower but higher panel). Manually resizing the dialog fixes
             * the layout.
             */
            panel.setSize(getParent().getParent().getWidth() - 20, 1);
            //System.out.println(targetPanel.getParent().getParent());

            String prevEmoteset = null;
            String prevEmotesetInfo = null;
            String prevType = null;
            boolean allSameType = isAllSameType(emotes);
            for (Emoticon emote : emotes) {
                boolean separatorAdded = false;
                if (!Objects.equals(prevEmoteset, emote.emoteset)
                        && prevEmoteset != null
                        && (
                            !Objects.equals(emote.getEmotesetInfo(), prevEmotesetInfo)
                            || emote.getEmotesetInfo() == null)
                        ) {
                    // Separator between different emotesets (and thus tiers)
                    panel.add(makeSeparator());
                    addSeparatorEmotesetInfo(panel, emote);
                    separatorAdded = true;
                }
                else if (prevEmoteset == null) {
                    addSeparatorEmotesetInfo(panel, emote);
                }
                // Separator between types (only third-party)
                if (!allSameType
                        && emote.type.category != TypeCategory.OFFICIAL
                        && !Objects.equals(prevType, emote.type.label)) {
                    if (prevType != null && !separatorAdded) {
                        panel.add(makeSeparator());
                    }
                    panel.add(createLabel(emote.type.label));
                }
                prevEmoteset = emote.emoteset;
                prevEmotesetInfo = emote.getEmotesetInfo();
                prevType = emote.type.label;
                panel.add(createEmoteLabel(emote, scale));
            }
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = EMOTE_INSETS;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            add(panel, gbc);
            gbc.gridx = 0;
            gbc.gridy++;
        }
        
        private boolean isAllSameType(Collection<Emoticon> emotes) {
            Emoticon.Type prevType = null;
            for (Emoticon emote : emotes) {
                if (prevType != null && !Objects.equals(prevType, emote.type)) {
                    return false;
                }
                prevType = emote.type;
            }
            return true;
        }
        
        /**
         * Add a label with a short description of the emoteset for some types.
         * 
         * @param panel
         * @param emote 
         */
        private void addSeparatorEmotesetInfo(JPanel panel, Emoticon emote) {
            if (StringUtil.isNullOrEmpty(emote.getEmotesetInfo())) {
                return;
            }
            if (Arrays.asList(new String[]{"Tier 2", "Tier 3", "Bits", "Follower"}).contains(emote.getEmotesetInfo())) {
                panel.add(createLabel(emote.getEmotesetInfo()));
            }
        }
        
        private JLabel createLabel(String text) {
            JLabel label = new JLabel(text);
            label.setForeground(emotesForeground);
            return label;
        }
        
        private JSeparator makeSeparator() {
            JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
            separator.setPreferredSize(new Dimension(2, 24));
            return separator;
        }
        
        void addEmotesSection(Collection<Emoticon> emotes, String title) {
            addEmotesSection(emotes, title, (String) null);
        }
        
        void addEmotesSection(Collection<Emoticon> emotes, String title, String hideId) {
            Set<String> hideIds = null;
            if (hideId != null) {
                hideIds = new HashSet<>(Arrays.asList(new String[]{hideId}));
            }
            addEmotesSection(emotes, title, hideIds);
        }
        
        boolean addEmotesSection(Collection<Emoticon> emotes, String title, Set<String> hideIds) {
            List<Emoticon> sorted = sortEmotes(emotes);
            sorted.removeIf(emote -> {
                boolean ignored = emoteManager.isEmoteIgnored(emote, IgnoredEmotes.EMOTE_DIALOG);
                if (ignored) {
                    ignoredEmotes.add(emote);
                }
                return ignored;
            });
            if (!sorted.isEmpty()) {
                boolean show = hideIds == null || !isHidden(hideIds);
                String fullTitle = filterEmotesForSearch(sorted, title);
                addTitle(fullTitle, null, hideIds);
                if (show && !sorted.isEmpty()) {
                    addEmotesPanel(sorted);
                }
                return show;
            }
            return false;
        }
        
        /**
         * Add ignored emotes section, if there are any ignored emotes on this
         * tab. Must be called once all other emotes on this tab have been
         * added.
         */
        public void addIgnoredEmotes() {
            if (ignoredEmotes.isEmpty()) {
                return;
            }
            List<Emoticon> emotes = new ArrayList<>(ignoredEmotes);
            String title = filterEmotesForSearch(emotes, "Ignored emotes");
            Set<String> hideIds = new HashSet<>(Arrays.asList(new String[]{"$ignored$"}));
            addTitle(title, null, hideIds);
            if (isHidden(hideIds) && !emotes.isEmpty()) {
                addEmotesPanel(emotes);
            }
        }
        
        /**
         * Removes all emotes from the given collection that does not match the
         * current search (if any). Returns the full title with the appropriate
         * emote counts.
         * 
         * @param emotes The emotes, will be modified
         * @param titlePrefix The title before the emote counts
         * @return The full title
         */
        private String filterEmotesForSearch(Collection<Emoticon> emotes, String titlePrefix) {
            int numEmotes = emotes.size();
            if (!StringUtil.isNullOrEmpty(search)) {
                emotes.removeIf(e -> {
                    return !e.code.toLowerCase().contains(search)
                            && !(e.type == Emoticon.Type.EMOJI && e.stringId.contains(search));
                });
            }
            int numEmotesFiltered = emotes.size();
            String fullTitle;
            if (numEmotes == numEmotesFiltered) {
                fullTitle = String.format("%s (%d emotes)",
                        titlePrefix, numEmotes);
            }
            else {
                fullTitle = String.format("%s (%d/%d emotes)",
                        titlePrefix, numEmotesFiltered, numEmotes);
            }
            return fullTitle;
        }

        
        List<Emoticon> sortEmotes(Collection<Emoticon> emotes) {
            // Copy so the original doesn't get changed
            List<Emoticon> sorted = new ArrayList<>(emotes);
            Collections.sort(sorted, new SortEmotes());
            return sorted;
        }
        
        /**
         * Show layout changes after adding emotes.
         */
        void relayout() {
            revalidate();
            repaint();
        }
    }
    
    
    private class FavoritesPanel extends EmotesPanel {

        public FavoritesPanel(String name, int updateOn) {
            super(name, updateOn);
        }

        @Override
        protected void updateEmotes() {
            reset();
            
            List<Emoticon> favs = new ArrayList<>(emoteManager.getFavorites());
            Collections.sort(favs, new SortEmotes());
            
            List<Emoticon> chanSpecificFavs = new ArrayList<>(emoteManager.getUsableEmotesByStream(currentStream));
            chanSpecificFavs.removeIf(emote -> !emoteManager.isFavorite(emote));
            
            if (favs.isEmpty()) {
                addTitle(Language.getString("emotesDialog.noFavorites"));
                addSubtitle(Language.getString("emotesDialog.noFavorites.hint"), false);
            }

            // Sort out emotes that the user probably doesn't have access to
            List<Emoticon> subEmotesNotSubbedTo = new ArrayList<>();
            for (Emoticon emote : favs) {
                if (!emote.hasGlobalEmoteset() && !localUserEmotesets.contains(emote.emoteset)) {
                    subEmotesNotSubbedTo.add(emote);
                }
            }
            favs.removeAll(subEmotesNotSubbedTo);
            
            // Add emotes
            addEmotesSection(favs, Language.getString("emotesDialog.tab.favorites"), "$fav$");
            addEmotesSection(subEmotesNotSubbedTo, Language.getString("emotesDialog.subscriptionRequired"), "$favsNoAccess$");
            addEmotesSection(chanSpecificFavs, Language.getString("emotesDialog.channelSpecificFavorites", Helper.toChannel(currentStream)), "$favsChan$");
            addNotFoundList(chanSpecificFavs);
            
            addIgnoredEmotes();
            relayout();
        }
        
        private void addNotFoundList(Collection<Emoticon> chanSpecificFavs) {
            Collection<Favorite> notFoundFavorites = getFilteredNotFound(chanSpecificFavs);
            if (!notFoundFavorites.isEmpty()) {
                addTitle(Language.getString("emotesDialog.notFoundFavorites") + " (" + notFoundFavorites.size() + ")", null, Arrays.asList("$favsNotFound$"));
                if (isHidden("$favsNotFound$")) {
                    // List
                    JList<Favorite> list = new JList<>();
                    // Make sure it's not too wide, but it will grow to full width
                    list.setFixedCellWidth(10);
                    
                    DefaultListModel<Favorite> data = new DefaultListModel<>();
                    list.setModel(data);
                    notFoundFavorites.forEach(fav -> data.addElement(fav));
                    
                    Runnable removeAction = () -> {
                        // Remove selected entries
                        Collection<Favorite> toRemove = list.getSelectedValuesList();
                        emoteManager.removeFavorites(toRemove);
                        toRemove.forEach(fav -> data.removeElement(fav));
                        // For updating title
                        updateEmotes();
                    };
                    list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removeFavoriteEmotes");
                    list.getActionMap().put("removeFavoriteEmotes", new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            removeAction.run();
                        }
                    });
                    JListActionHelper.install(list, (a, l, s) -> {
                        if (a == JListActionHelper.Action.CONTEXT_MENU) {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem item = new JMenuItem(Language.getString("emotesDialog.button.unfavoriteSelected"));
                            item.addActionListener(e2 -> removeAction.run());
                            menu.add(item);
                            menu.show(list, l.x, l.y);
                        }
                    });
                    
                    // Remove Button
                    JButton removeButton = new JButton(Language.getString("emotesDialog.button.unfavoriteSelected"));
                    removeButton.addActionListener(e -> {
                        removeAction.run();
                    });
                    
                    // Layout
                    JPanel panel = new JPanel(new BorderLayout());
                    panel.add(new JScrollPane(list), BorderLayout.CENTER);
                    panel.add(removeButton, BorderLayout.SOUTH);
                    panel.setOpaque(false);
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.insets = TITLE_INSETS;
                    gbc.anchor = GridBagConstraints.CENTER;
                    gbc.weightx = 1;
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    add(panel, gbc);
                    gbc.gridx = 0;
                    gbc.gridy++;
                }
            }
        }
        
        private Collection<Favorite> getFilteredNotFound(Collection<Emoticon> remove) {
            Collection<Favorite> result = emoteManager.getNotFoundFavorites();
            result.removeIf(fav -> {
                for (Emoticon emote : remove) {
                    if (emote.type == fav.type && fav.id != null && fav.id.equals(emote.stringId)) {
                        return true;
                    }
                }
                return false;
            });
            return result;
        }

    }
    
    private class SubemotesPanel extends EmotesPanel {
        
        public SubemotesPanel(String name, int updateOn) {
            super(name, updateOn);
        }
        
        @Override
        protected void updateEmotes() {
            updateEmotes2();
        }

        private void updateEmotes2() {
            reset();
            if (localUserEmotesets.isEmpty() || (localUserEmotesets.size() == 1 
                    && localUserEmotesets.iterator().next().equals(Emoticon.SET_GLOBAL))) {
                addTitle(Language.getString("emotesDialog.noSubemotes"));
                if (currentStream == null) {
                    addSubtitle(Language.getString("emotesDialog.subEmotesJoinChannel"), false);
                }
            }
            else {
                if (currentStream == null) {
                    addSubtitle("Must join a channel for Emotes to show correctly.", false);
                }
            }
            
            gbc.gridy++;
            
            Set<Emoticon> customLocalEmotes = emoteManager.getCustomLocalEmotes();
            if (!customLocalEmotes.isEmpty()) {
                addEmotesSection(customLocalEmotes, "Local Emotes", SPECIAL_SET_LOCAL);
                if (!isHidden(SPECIAL_SET_LOCAL)) {
                    addSubtitle("Learn about [help-settings:EmoticonsLocal Local Emotes]", true, true);
                }
            }
            
            //-------------------------
            // Sort emotes by emoteset
            //-------------------------
            /**
             * Add all emotes with the same owner id together, to determine the
             * common prefix. This is necessary if some emotesets from the same
             * owner have an unclear prefix.
             * 
             * Currently this is designed to make use of the current code. This
             * whole section should be rewritten once it's clearer what
             * information will be available when the emote APIs are updated or
             * when it seems like they won't be updated anymore anytime soon.
             */
            Map<String, Set<Emoticon>> perOwnerId = new HashMap<>();
            for (String emoteset : localUserEmotesets) {
                EmotesetInfo newInfo = emoteManager.getInfoBySet(emoteset);
                if (newInfo != null && newInfo.stream_id != null) {
                    Set<Emoticon> emotes = emoteManager.getEmoticonsBySet(emoteset);
                    MiscUtil.getSetFromMap(perOwnerId, newInfo.stream_id).addAll(emotes);
                }
            }
            Map<String, String> prefixesByOwnerId = new HashMap<>();
            for (Map.Entry<String, Set<Emoticon>> entry : perOwnerId.entrySet()) {
                String emotePrefix = getPrefix(entry.getValue());
                if (emotePrefix != null) {
                    prefixesByOwnerId.put(entry.getKey(), emotePrefix);
                }
            }
            
            Set<String> turboEmotes = new HashSet<>();
            Map<String, Set<EmotesetInfo>> perStream = new HashMap<>();
            Map<String, Set<EmotesetInfo>> perInfo = new HashMap<>();
            Map<String, Set<EmotesetInfo>> perPrefix = new HashMap<>();
            List<String> unknownEmotesets = new ArrayList<>();
            Set<String> unknownEmotesetsSingle = new HashSet<>();
            for (String emoteset : localUserEmotesets) {
                if (Emoticons.isTurboEmoteset(emoteset)) {
                    // Turbo emotes
                    turboEmotes.add(emoteset);
                }
                else if (!Emoticon.isGlobalEmoteset(emoteset)) {
                    Set<Emoticon> emotes = emoteManager.getEmoticonsBySet(emoteset);
                    String emotePrefix = getPrefix(emotes);
                    EmotesetInfo newInfo = emoteManager.getInfoBySet(emoteset);
                    if (newInfo != null && newInfo.stream_id != null && prefixesByOwnerId.containsKey(newInfo.stream_id)) {
                        emotePrefix = prefixesByOwnerId.get(newInfo.stream_id);
                    }
                    if (emotePrefix == null) {
                        if (emotes.size() == 1) {
                            unknownEmotesetsSingle.add(emoteset);
                        }
                        else {
                            unknownEmotesets.add(emoteset);
                        }
                    }
                    else {
                        String key = emotePrefix + " Emotes";
                        if (!perPrefix.containsKey(key)) {
                            perPrefix.put(key, new HashSet<>());
                        }
                        perPrefix.get(key).add(new EmotesetInfo(emoteset, null, null, null));
                    }
                }
            }
            
            //--------------------------
            // Unknown Emotesets
            //--------------------------
            /**
             * After collecting all perPrefix sets, sort ones that ended up with
             * only one emote into "Other" and add others to perInfo emotes, so
             * they are sorted the same
             */
            Iterator<Map.Entry<String, Set<EmotesetInfo>>> it = perPrefix.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Set<EmotesetInfo>> entry = it.next();
                String key = entry.getKey();
                Set<EmotesetInfo> sets = entry.getValue();
                String emoteset = sets.iterator().next().emoteset_id;
                if (sets.size() == 1 && emoteManager.getEmoticonsBySet(emoteset).size() == 1) {
                    unknownEmotesetsSingle.add(emoteset);
                }
                else {
                    if (perInfo.containsKey(key)) {
                        perInfo.get(key).addAll(sets);
                    }
                    else {
                        perInfo.put(key, sets);
                    }
                }
            }
            
            //------------
            // Add emotes
            //------------
            List<String> sortedStreams = new ArrayList<>(perStream.keySet());
            Collections.sort(sortedStreams, String.CASE_INSENSITIVE_ORDER);
            for (String stream : sortedStreams) {
                addEmotesBySets(stream, perStream.get(stream), true);
            }
            
            List<String> sortedInfos = new ArrayList<>(perInfo.keySet());
            Collections.sort(sortedInfos, String.CASE_INSENSITIVE_ORDER);
            for (String info : sortedInfos) {
                addEmotesBySets(info, perInfo.get(info), true);
            }
            
            Collections.sort(unknownEmotesets, SORT_EMOTESETS);
            for (String emoteset : unknownEmotesets) {
                addEmotesBySet(emoteset, true);
            }
            
            // Unknown emotesets that only contain a single emote should be
            // grouped together
            Set<Emoticon> unknownEmotesGrouped = new HashSet<>();
            for (String emoteset : unknownEmotesetsSingle) {
                unknownEmotesGrouped.addAll(emoteManager.getEmoticonsBySet(emoteset));
            }
            addEmotesSection(unknownEmotesGrouped, Language.getString("emotesDialog.otherSubemotes"), SPECIAL_SET_OTHER);

            String turboSetA = "793";
            String turboSetB = "19194";
            for (String emoteset : turboEmotes) {
                if (emoteset.equals(turboSetB) && turboEmotes.contains(turboSetA)
                        && emoteManager.equalsByCode(turboSetA, turboSetB)) {
                    // Don't show these Turbo/Prime emotes if the user has the
                    // other set as well, and the emotes are equal
                    continue;
                }
                addEmotesBySet(emoteset, true);
            }
            
            addIgnoredEmotes();
            
            // Don't show if there is very little (or nothing) to show/hide
            if (sortedStreams.size() + sortedInfos.size() > 2) {
                addSubtitle("(Tip: Click headings to show/hide emotes)", false);
            }
            
            relayout();
        }
        
    }
    
    private class ChannelEmotesPanel extends EmotesPanel {

        public ChannelEmotesPanel(String name, int updateOn) {
            super(name, updateOn);
        }

        @Override
        protected void updateEmotes() {
            reset();
            if (currentStream == null && tempStream == null) {
                addTitle(Language.getString("emotesDialog.noChannel"));
            } else {
                String stream = tempStream != null ? tempStream : currentStream;
                if (tempStream != null && currentStream != null) {
                    // Temp Stream
                    JButton button = new JButton(Language.getString("emotesDialog.backToChannel", currentStream));
                    button.addActionListener(e -> {
                        tempStream = null;
                        updateEmotes();
                    });
                    add(button, GuiUtil.makeGbc(0, 0, 1, 1));
                    gbc.gridy++;
                }
                
                // FFZ/BTTV
                Set<Emoticon> channelEmotes = emoteManager.getEmoticonsByStream(stream);
                
                // Split Event/Regular emotes into separate structures
                Set<Emoticon> regular = new HashSet<>();
                Map<String, Set<Emoticon>> event = new HashMap<>();
                for (Emoticon emote : channelEmotes) {
                    if (emote.type == Emoticon.Type.FFZ
                            && emote.subType == Emoticon.SubType.EVENT) {
                        for (String info : emote.getInfos()) {
                            if (!event.containsKey(info)) {
                                event.put(info, new HashSet<>());
                            }
                            event.get(info).add(emote);
                        }
                    } else {
                        regular.add(emote);
                    }
                }
                
                if (channelEmotes.isEmpty()) {
                    addTitle(Language.getString("emotesDialog.noChannelEmotes", stream));
                    addSubtitle(Language.getString("emotesDialog.noChannelEmotes2"), false);
                } else {
                    addEmotesSection(regular, Language.getString("emotesDialog.channelEmotes", stream), "$channel$");
                    for (String info : event.keySet()) {
                        addEmotesSection(event.get(info), "Featured " + info);
                    }
                }
                Debugging.println("emoteinfo", "UPDATE %s", stream);
                TwitchEmotesApi.api.requestByStream(stream);
                addSubemotes(stream, emoteManager.getSetsByStream(stream));
            }
            addIgnoredEmotes();
            relayout();
        }
        
        private void addSubemotes(String stream, Set<EmotesetInfo> sets) {
            Debugging.println("emoteinfo", "ADDSUBEMOTES %s", sets);
            // Subscriber Emotes
            if (sets != null && !sets.isEmpty()) {
                boolean streamMatches = stream != null && stream.equals(sets.iterator().next().stream_name);
                if (streamMatches) {
                    // Split subemotes with and without access
                    Set<EmotesetInfo> withAccess = new HashSet<>();
                    Set<EmotesetInfo> noAccess = new HashSet<>();
                    for (EmotesetInfo set : sets) {
                        if (localUserEmotesets.contains(set.emoteset_id)) {
                            withAccess.add(set);
                        } else {
                            noAccess.add(set);
                        }
                    }
                    if (!withAccess.isEmpty()) {
                        addEmotesBySets(Language.getString("emotesDialog.subemotes", stream), withAccess, false);
                    }
                    if (!noAccess.isEmpty()) {
                        if (addEmotesBySets(Language.getString("emotesDialog.subemotes", stream), noAccess, false)) {
                            if (withAccess.isEmpty()) {
                                // No subscription active
                                addSubtitle(Language.getString("emotesDialog.subscriptionRequired2"), true);
                            } else {
                                // Subscription active, but not high enough tier
                                addSubtitle(Language.getString("emotesDialog.subscriptionRequired3"), true);
                            }
                        }
                    }
                    relayout();
                }
            }
        }

    }
    
    private class TwitchEmotesPanel extends EmotesPanel {

        public TwitchEmotesPanel(String name, int updateOn) {
            super(name, updateOn);
        }

        @Override
        protected void updateEmotes() {
            reset();
            Set<Emoticon> emotes = emoteManager.getGlobalTwitchEmotes();
            addEmotesSection(emotes, Language.getString("emotesDialog.globalTwitch"));
            Collection<Emoticon> smilies = emoteManager.getSmilies();
            if (smilies != null) {
                addEmotesSection(smilies, "Smilies");
            }
            addIgnoredEmotes();
            relayout();
        }

    }
    
    private class OtherEmotesPanel extends EmotesPanel {

        public OtherEmotesPanel(String name, int updateOn) {
            super(name, updateOn);
        }

        @Override
        protected void updateEmotes() {
            reset();
            Set<Emoticon> ffz = Emoticons.filterByType(emoteManager.getOtherGlobalEmotes(), Emoticon.Type.FFZ);
            Set<Emoticon> ffzRegular = new HashSet<>();
            Set<Emoticon> ffzFeatured = new HashSet<>();
            for (Emoticon emote : ffz) {
                if (emote.subType == Emoticon.SubType.FEATURE_FRIDAY) {
                    ffzFeatured.add(emote);
                } else {
                    ffzRegular.add(emote);
                }
            }
            Set<Emoticon> bttv = Emoticons.filterByType(emoteManager.getOtherGlobalEmotes(), Emoticon.Type.BTTV);
            Set<Emoticon> seventv = Emoticons.filterByType(emoteManager.getOtherGlobalEmotes(), Emoticon.Type.SEVENTV);

            addEmotesSection(ffzRegular, Language.getString("emotesDialog.globalFFZ"), "$ffz$");
            addEmotesSection(ffzFeatured, Language.getString("emotesDialog.globalFFZ")+" [Featured]", "$ffzFeatured$");
            addEmotesSection(bttv, Language.getString("emotesDialog.globalBTTV"), "$bttv$");
            addEmotesSection(seventv, Language.getString("emotesDialog.globalSevenTV"), "$seventv$");
            
            addIgnoredEmotes();
            
            relayout();
        }
    }
    
    private class BitsPanel extends EmotesPanel {

        public BitsPanel(String name, int updateOn) {
            super(name, updateOn);
        }
        
        @Override
        protected void updateEmotes() {
            reset();
            
            Collection<CheerEmoticon> cheerEmotes = emoteManager.getCheerEmotes();
            Collection<Emoticon> emotes = new ArrayList<>();
            for (CheerEmoticon emote : cheerEmotes) {
                emotes.add((Emoticon)emote);
            }
            //addEmotesPanel(emotes);
            
            relayout();
        }
        
    }
    
    private class EmojiPanel extends EmotesPanel {

        public EmojiPanel(String name, int updateOn) {
            super(name, updateOn);
        }

        @Override
        protected void updateEmotes() {
            reset();
            
            Map<String, Set<Emoticon>> categories = new HashMap<>();
            Set<Emoticon> emotes = emoteManager.getEmoji();
            for (Emoticon emote : emotes) {
                for (String info : emote.getInfos()) {
                    if (!info.startsWith("Category: ")) {
                        continue;
                    }
                    if (!categories.containsKey(info)) {
                        categories.put(info, new HashSet<Emoticon>());
                    }
                    categories.get(info).add(emote);
                }
            }
            for (String category : categories.keySet()) {
                addEmotesSection(categories.get(category), category);
            }
            
            //addEmotes(emoteManager.getEmoji(), "Emoji");
            
            relayout();
        }
    }
    
    private class EmoteDetailPanel extends EmotesPanel {

        public EmoteDetailPanel(String name, int updateOn) {
            super(name, updateOn);
        }
        
        private GridBagConstraints lgbc = new GridBagConstraints();
        
        @Override
        protected void updateEmotes() {
            updateEmotes2();
        }
        
        private void updateEmotes2() {
            Debugging.println("emoteinfo", "EmoteDetailsPanel update");
            reset();
            
            Emoticon emote = detailsEmote;
            if (emote == null) {
                // This may happen when tab was switched, but a request result
                // triggers this again
                return;
            }
            
            //--------------
            // Add elements
            //--------------
            addTitle(Language.getString("emotesDialog.details.title", emote.code));
            
            
            lgbc.insets = new Insets(5, 7, 5, 7);
            
            JPanel panel = new JPanel();
            panel.setBackground(emotesBackground);
            panel.setLayout(new GridBagLayout());
            
            addScaledEmote(emote, panel, 1, "100%");
            if (emote.getWidth()*3+200 < EmotesDialog.this.getWidth()) {
                /**
                 * Don't show middle one if emote is too wide (this won't be too
                 * exact, but should work well enough in this case).
                 */
                addScaledEmote(emote, panel, (float)1.5, "150%");
            }
            addScaledEmote(emote, panel, 2, "200%");
            
            
            JPanel panel2 = new JPanel();
            panel2.setLayout(new GridBagLayout());
            lgbc.gridy = 2;
            lgbc.gridx = 0;
            lgbc.insets = new Insets(4, 4, 4, 4);
            
            if (emote.subType == Emoticon.SubType.CHEER) {
                addInfo(panel2, "", "Cheering Emote");
            } else {
                addInfo(panel2, Language.getString("emotesDialog.details.code"), emote.code);
            }
            String featured = emote.subType == Emoticon.SubType.EVENT ? " (Featured)" : "";
            addInfo(panel2, Language.getString("emotesDialog.details.type"), emote.type.toString()+featured);
            if (emote.type == Emoticon.Type.TWITCH || emote.type == Emoticon.Type.FFZ) {
                addInfo(panel2, Language.getString("emotesDialog.details.id"), emote.stringId);
            }
            if (!emote.hasGlobalEmoteset()) {
                String emoteset = emote.emoteset;
                if (!emoteset.equals(Emoticon.SET_UNKNOWN)) {
                    String info = String.valueOf(emoteset);
                    if (Emoticons.isTurboEmoteset(emoteset)) {
                        info += " (Turbo)";
                    }
                    String orig = TwitchEmotesApi.isModified(emote) ? "Orig. " : "";
                    addInfo(panel2, orig+"Emoteset:", info);
                } else {
                    addInfo(panel2, "Emoteset:", "unknown");
                }
            }
            
            String emoteChannel = emote.getStream();
            if (emoteChannel != null) {
                addInfo(panel2, Language.getString("emotesDialog.details.channel"), emoteChannel);
            }
            addInfo(panel2, Language.getString("emotesDialog.details.usableIn"),
                    emote.hasStreamRestrictions()
                            ? Language.getString("emotesDialog.details.usableInChannel")
                            : Language.getString("emotesDialog.details.usableEverywhere"));
            if (emote.hasGlobalEmoteset()) {
                addInfo(panel2, Language.getString("emotesDialog.details.access"),
                        Language.getString("emotesDialog.details.everyone"));
            } else {
                addInfo(panel2, Language.getString("emotesDialog.details.access"),
                        Language.getString("emotesDialog.details.restricted"));
                if (localUserEmotesets.contains(emote.emoteset)) {
                    addInfo(panel2, "", Language.getString("emotesDialog.details.accessAvailable"));
                }
            }
            addInfo(panel2, Language.getString("emotesDialog.details.size"), emote.getWidth()+"x"+emote.getHeight());
            if (emote.creator != null) {
                addInfo(panel2, Language.getString("emotesDialog.details.by"), emote.creator);
            }
            
            // Info
            if (emote.type == Emoticon.Type.EMOJI && emote.stringId != null) {
                addInfo(panel2, emote.stringId);
                if (emote.stringIdAlias != null) {
                    addInfo(panel2, "("+emote.stringIdAlias+")");
                }
            }
            featured = emote.subType == Emoticon.SubType.EVENT ? "Featured " : "";
            for (String info : emote.getInfos()) {
                addInfo(panel2, featured+info);
            }
            
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = EMOTE_INSETS;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            add(panel, gbc);
            gbc.gridy++;
            add(panel2, gbc);
            gbc.gridy++;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.CENTER;
            add(new JLabel("<html><body style='width:200px;text-align:center;color:#888888'>"
                +Language.getString("emotesDialog.details.info")), gbc);
            
            relayout();
        }
        
        private void addScaledEmote(Emoticon emote, JPanel panel, float scale,
                String label) {
            lgbc.anchor = GridBagConstraints.CENTER;
            lgbc.gridy = 0;
            panel.add(createEmoteLabel(emote, scale), lgbc);
            
            lgbc.gridy = 1;
            JLabel title = new JLabel(label);
            title.setForeground(emotesForeground);
            panel.add(title, lgbc);
            
            lgbc.gridx++;
        }
        
        /**
         * Adds a info line with separated key and value.
         * 
         * @param panel
         * @param key
         * @param value 
         */
        private void addInfo(JPanel panel, String key, String value) {
            lgbc.gridx = 0;
            lgbc.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel(key), lgbc);
            
            lgbc.gridx = 1;
            lgbc.anchor = GridBagConstraints.EAST;
            
            panel.add(new JLabel(StringUtil.shortenTo(value, 35, 20)), lgbc);
            
            lgbc.gridy++;
        }
        
        /**
         * Adds a full-width info line.
         * 
         * @param panel
         * @param value 
         */
        private void addInfo(JPanel panel, String value) {
            lgbc.gridx = 0;
            lgbc.gridwidth = 2;
            lgbc.anchor = GridBagConstraints.CENTER;
            
            panel.add(new JLabel(StringUtil.shortenTo(value, 35, 20)), lgbc);
            
            lgbc.gridwidth = 1;
            lgbc.gridy++;
        }
        
    }
    
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^([A-Za-z][a-z0-9]+)");
    
    private static String getPrefix(String code) {
        Matcher m = PREFIX_PATTERN.matcher(code);
        if (m.find()) {
            return m.group();
        }
        return null;
    }
    
    /**
     * The the emote prefix. Since it can be a bit ambigious what is part of the
     * prefix (especially with numbers or sets that don't really have a prefix)
     * this may not be entirely correct.
     * 
     * It assumes that everything lowercase or number (first character can be
     * uppercase) belongs to the prefix, however it would be an issue when the
     * code after the prefix starts with a number, so there is some leniancy for
     * not all emotes having the same prefix (the shortest will be used most of
     * them have it).
     * 
     * @param emotes
     * @return 
     */
    private static String getPrefix(Collection<Emoticon> emotes) {
        String commonPrefix = null;
        int count = 0;
        for (Emoticon emote : emotes) {
            String prefix = getPrefix(emote.code);
            if (prefix == null) {
                return null;
            }
            if (commonPrefix == null) {
                commonPrefix = prefix;
                count = 1;
            }
            else if (prefix.equals(commonPrefix)) {
                count++;
            }
            else if (prefix.length() < commonPrefix.length()
                    && commonPrefix.startsWith(prefix)) {
                // Always prefer shorter prefix, if it still fits
                commonPrefix = prefix;
                count = 1;
            }

            if (!prefix.startsWith(commonPrefix)) {
                return null;
            }
        }
        if (count >= emotes.size() * 0.8) {
            return commonPrefix;
        }
        return null;
    }
    
}
