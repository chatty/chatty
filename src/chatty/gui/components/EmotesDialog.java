
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.util.colors.HtmlColors;
import chatty.gui.LaF;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotes;
import chatty.util.TwitchEmotes.Emoteset;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticon.EmoticonUser;
import chatty.util.api.Emoticons;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
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
import javax.swing.border.Border;

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
    private final EmotesPanel defaultPanel;
    private final Color emotesBackground;
    
    /**
     * GridBagConstraints for adding titles/emotes.
     */
    private final GridBagConstraints gbc;
    
    //------------
    // References
    //------------
    private final Emoticons emoteManager;
    private final EmoticonUser emoteUser;
    private final MouseAdapter mouseListener;
    private final ContextMenuListener contextMenuListener;
    
    //------------------
    // State / Settings
    //------------------
    private Set<Integer> localUserEmotesets = new HashSet<>();
    private String stream;
    private Emoticon detailsEmote;
    private float scale;
    private boolean closeOnDoubleClick = true;
   
    
    public EmotesDialog(Window owner, Emoticons emotes, final MainGui main, ContextMenuListener contextMenuListener) {
        super(owner);
        
        emoteUser = new Emoticon.EmoticonUser() {

            @Override
            public void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged) {
//                repaint = true;
                repaint();
            }
        };
        
        // TODO: Focusable or maybe just when clicked on emote to insert code?
        this.setFocusable(false);
        this.setFocusableWindowState(false);
        this.contextMenuListener = contextMenuListener;
        this.emoteManager = emotes;
        emotesBackground = LaF.isDarkTheme() ? new Color(38, 38, 38) : new Color(250, 250, 250);
        //emotesBackground = new JPanel().getBackground().brighter();
        //emotesBackground = brighter(new JPanel().getBackground(), 0.8);
        //emotesBackground = HtmlColors.decode(main.getSettings().getString("backgroundColor"));
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
        
        ButtonGroup buttonGroup = new ButtonGroup();
        JPanel buttonPanel = new JPanel();
        for (EmotesPanel p : panels) {
            JToggleButton button = new JToggleButton(p.label);
            buttons.put(button, p);
            buttonGroup.add(button);
            button.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            buttonPanel.add(button);
            button.addActionListener(buttonAction);
        }
        add(buttonPanel, BorderLayout.NORTH);
        
        // Add Details panel, which doesn't have a button
        panels.add(new EmoteDetailPanel(EMOTE_DETAILS, 0));

        //---------------------------------
        // Listener for clicking on emotes
        //---------------------------------
        mouseListener = new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2 && closeOnDoubleClick) {
                        setVisible(false);
                    } else {
                        EmoteLabel label = (EmoteLabel) e.getSource();
                        if (!label.noInsert) {
                            main.insert(Emoticons.toWriteable(label.code), true);
                        }
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                openContextMenu(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                openContextMenu(e);
            }
            
        };
        
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
        if (e.isPopupTrigger()) {
            EmoticonImage emote = ((EmoteLabel)e.getSource()).emote;
            JPopupMenu m = new EmoteContextMenu(emote, contextMenuListener);
            m.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * Opens the dialog, using the given emotesets and stream.
     *
     * @param localUserEmotesets
     * @param stream
     */
    public void showDialog(Set<Integer> localUserEmotesets, String stream) {
        if (stream != null && !stream.equals(this.stream)) {
            setUpdated(UPDATE_CHANNEL_CHANGED);
        }
        if (stream != null) {
            this.stream = stream;
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
    
    public void showChannelEmotes() {
        showPanelByName(CHANNEL_EMOTES);
    }
    
    public void showEmoteDetails(Emoticon emote) {
        detailsEmote = emote;
        getPanelByName(EMOTE_DETAILS).updateEmotes();
        showPanelByName(EMOTE_DETAILS);
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
     */
    public void updateStream(String stream) {
        if (!isVisible()) {
            return;
        }
        if (stream != null && stream.equals(this.stream)) {
            return;
        }
        this.stream = stream;
        updateTitle();
        setUpdated(UPDATE_CHANNEL_CHANGED);
        showEmotes();
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
     * @param emotesets The Set of emotesets
     */
    public void updateEmotesets(Set<Integer> emotesets) {
        if (!isVisible() || emotesets == null || emotesets.equals(this.localUserEmotesets)) {
            return;
        }
        this.localUserEmotesets = new HashSet<>(emotesets);
        setUpdated(UPDATE_EMOTESET_CHANGED);
        showEmotes();
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

    /**
     * Sets the title according to the current stream.
     */
    private void updateTitle() {
        setTitle(Language.getString("emotesDialog.title", stream == null ? "-" : "#"+stream));
    }
    
    private void showPanel(EmotesPanel panel) {
        panel.update();
        cardLayout.show(emotesPanel, panel.label);
        for (JToggleButton button : buttons.keySet()) {
            if (buttons.get(button) == panel) {
                button.setSelected(true);
            }
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
        
        private static final Border BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        
        public final String code;
        public final EmoticonImage emote;
        public final boolean noInsert;

        public EmoteLabel(Emoticon emote, MouseListener mouseListener, float scale,
                EmoticonUser emoteUser) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(mouseListener);
            EmoticonImage emoteImage = emote.getIcon(scale, 0, emoteUser);
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
            setBorder(BORDER);
        }
        
    }
    
    //==================
    // Emoticon sorting
    //==================
    private static class SortEmotesByEmoteset implements Comparator<Emoticon> {

        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            return o1.emoteSet - o2.emoteSet;
        }
        
    }
    
    private static class SortEmotesByTypeAndName implements Comparator<Emoticon> {
        
        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            int compareType = o1.type.compareTo(o2.type);
            if (compareType == 0) {
                return o1.code.compareToIgnoreCase(o2.code);
            }
            return compareType;
        }
    }
    
    private static class SortEmotesByEmotesetAndName implements Comparator<Emoticon> {
        
        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            int compareEmoteset = o1.emoteSet - o2.emoteSet;
            if (compareEmoteset == 0) {
                return o1.code.compareToIgnoreCase(o2.code);
            }
            return compareEmoteset;
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
        }
    
        /**
         * Adds the emotes of the given emoteset. Includes the name of the
         * stream if available.
         *
         * @param emoteset The emoteset
         */
        void addEmotes(int emoteset) {
            String stream = emoteManager.getLabelByEmoteset(emoteset);
            if (stream == null) {
                stream = "-";
            }
            Set<Emoticon> emotes = emoteManager.getEmoticons(emoteset);
            List<Emoticon> sorted = new ArrayList<>(emotes);
            Collections.sort(sorted, new SortEmotesByTypeAndName());
            addTitle(stream + " [" + emoteset + "] (" + emotes.size() + " emotes)");
            addEmotesPanel(sorted);
        }
        
        /**
         * Adds the given emotesets under a single title, sorted by emoteset.
         * 
         * @param titlePrefix Title prefix (emotesets and emotecount added
         * automatically)
         * @param emotesets The emotesets to display
         * @return true if any emotes have been added, false otherwise
         */
        boolean addEmotes(String titlePrefix, Set<Emoteset> emotesets) {
            List<Integer> sets = new ArrayList<>();
            for (Emoteset set : emotesets) {
                sets.add(set.emoteset_id);
            }
            Collections.sort(sets);
            List<Emoticon> sorted = new ArrayList<>();
            for (int set : sets) {
                sorted.addAll(emoteManager.getEmoticons(set));
            }
            Collections.sort(sorted, new SortEmotesByEmotesetAndName());
            addTitle(String.format("%s %s (%d emotes)",
                    titlePrefix,
                    sets,
                    sorted.size()));
            addEmotesPanel(sorted);
            return !sorted.isEmpty();
        }
    
        /**
         * Adds a title (label with seperating line).
         *
         * @param title The text of the title
         */
        void addTitle(String title) {
            JLabel titleLabel = new JLabel(StringUtil.shortenTo(title, 48, 34));
            titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, titleLabel.getForeground()));
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = TITLE_INSETS;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            add(titleLabel, gbc);
            gbc.gridy++;
        }
    
        /**
         * Adds some centered text without a seperating line and with a slightly
         * subdued color.
         *
         * @param title The text to add
         * @param smallMargin If true, uses smaller margins (for use below
         * emotes)
         */
        void addSubtitle(String title, boolean smallMargin) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setForeground(Color.GRAY);
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            gbc.insets = smallMargin ? SUBTITLE_INSETS_SMALLER_MARGIN : SUBTITLE_INSETS;
            gbc.anchor = GridBagConstraints.CENTER;
            add(titleLabel, gbc);
            gbc.gridy++;
        }
        
        /**
         * Adds the given emotes to a new panel.
         *
         * @param emotes The emotes to add
         */
        void addEmotesPanel(Collection<Emoticon> emotes) {
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

            int prevEmoteset = -1;
            for (Emoticon emote : emotes) {
                Emoticons.addInfo(emoteManager.getEmotesetInfo(), emote);
                if (prevEmoteset != emote.emoteSet && prevEmoteset != -1) {
                    // Separator between different emotesets (and thus tiers)
                    panel.add(makeSeparator());
                }
                prevEmoteset = emote.emoteSet;
                final JLabel label = new EmoteLabel(emote, mouseListener, scale, emoteUser);
                panel.add(label);
            }
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = EMOTE_INSETS;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            add(panel, gbc);
            gbc.gridy++;
        }
        
        private JSeparator makeSeparator() {
            JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
            separator.setPreferredSize(new Dimension(2, 24));
            return separator;
        }
        
        void addEmotes(Collection<Emoticon> emotes, String title) {
            if (!emotes.isEmpty()) {
                addTitle(title+" ("+emotes.size()+" emotes)");
                addEmotesPanel(sortEmotes(emotes));
            }
        }
        
        Collection<Emoticon> sortEmotes(Collection<Emoticon> emotes) {
            // Copy so the original doesn't get changed
            List<Emoticon> sorted = new ArrayList<>(emotes);
            Collections.sort(sorted, new SortEmotesByTypeAndName());
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
            Set<Emoticon> emotes = emoteManager.getFavorites();
            if (emotes.isEmpty()) {
                addTitle(Language.getString("emotesDialog.noFavorites"));
                addSubtitle(Language.getString("emotesDialog.noFavorites.hint"), false);
            }

            // Sort emotes by emoteset
            List<Emoticon> sorted = new ArrayList<>(emotes);
            Collections.sort(sorted, new SortEmotesByEmotesetAndName());

            // Sort out emotes that the user probably doesn't have access to
            List<Emoticon> subEmotesNotSubbedTo = new ArrayList<>();
            List<Emoticon> notFoundFavorites = new ArrayList<>();
            for (Emoticon emote : sorted) {
                if (emote.type == Emoticon.Type.NOT_FOUND_FAVORITE) {
                    notFoundFavorites.add(emote);
                }
                else if (!emote.hasGlobalEmoteset() && !localUserEmotesets.contains(emote.emoteSet)) {
                    subEmotesNotSubbedTo.add(emote);
                }
            }
            sorted.removeAll(subEmotesNotSubbedTo);
            sorted.removeAll(notFoundFavorites);
            
            // Add emotes
            addEmotesPanel(sorted);
            if (!subEmotesNotSubbedTo.isEmpty()) {
                addTitle(Language.getString("emotesDialog.subscriptionRequired"));
                addEmotesPanel(subEmotesNotSubbedTo);
            }
            if (!notFoundFavorites.isEmpty()) {
                addTitle(Language.getString("emotesDialog.notFoundFavorites"));
                addEmotesPanel(notFoundFavorites);
                addSubtitle(Language.getString("emotesDialog.favoriteCmInfo"), true);
            }
            relayout();
        }

    }
    
    private class SubemotesPanel extends EmotesPanel {
        
        public SubemotesPanel(String name, int updateOn) {
            super(name, updateOn);
        }

        @Override
        protected void updateEmotes() {
            reset();
            if (localUserEmotesets.isEmpty() || (localUserEmotesets.size() == 1 
                    && localUserEmotesets.iterator().next().equals(Emoticon.SET_GLOBAL))) {
                addTitle(Language.getString("emotesDialog.noSubemotes"));
                if (stream == null) {
                    addSubtitle(Language.getString("emotesDialog.subEmotesJoinChannel"), false);
                }
            }
            
            //-------------------------
            // Sort emotes by emoteset
            //-------------------------
            Set<Integer> turboEmotes = new HashSet<>();
            Map<String, Set<Emoteset>> perStream = new HashMap<>();
            Map<String, Set<Emoteset>> perInfo = new HashMap<>();
            Set<Integer> unknownEmotesets = new HashSet<>();
            Set<Integer> unknownEmotesetsSingle = new HashSet<>();
            for (Integer emoteset : localUserEmotesets) {
                if (Emoticons.isTurboEmoteset(emoteset)) {
                    // Turbo emotes
                    turboEmotes.add(emoteset);
                } else if (emoteset != Emoticon.SET_GLOBAL) {
                    Emoteset info = emoteManager.getInfoByEmoteset(emoteset);
                    if (info != null) {
                        if (info.stream == null) {
                            // No stream name, probably special emoteset
                            String key = info.product+" Emotes";
                            if (!perInfo.containsKey(key)) {
                                perInfo.put(key, new HashSet<>());
                            }
                            perInfo.get(key).add(info);
                        } else {
                            // Stream name known
                            if (!perStream.containsKey(info.stream)) {
                                perStream.put(info.stream, new HashSet<>());
                            }
                            perStream.get(info.stream).add(info);
                        }
                    } else {
                        // Unknown emoteset
                        if (emoteManager.getEmoticons(emoteset).size() == 1) {
                            unknownEmotesetsSingle.add(emoteset);
                        } else {
                            unknownEmotesets.add(emoteset);
                        }
                    }
                }
            }
            
            //------------
            // Add emotes
            //------------
            for (String stream : perStream.keySet()) {
                addEmotes(stream, perStream.get(stream));
            }
            
            for (String info : perInfo.keySet()) {
                addEmotes(info, perInfo.get(info));
            }
            
            for (Integer emoteset : unknownEmotesets) {
                addEmotes(emoteset);
            }
            
            // Unknown emotesets that only contain a single emote should be
            // grouped together
            Set<Emoticon> unknownEmotesGrouped = new HashSet<>();
            for (int emoteset : unknownEmotesetsSingle) {
                unknownEmotesGrouped.addAll(emoteManager.getEmoticons(emoteset));
            }
            addEmotes(unknownEmotesGrouped, Language.getString("emotesDialog.otherSubemotes"));

            int turboSetA = 793;
            int turboSetB = 19194;
            for (Integer emoteset : turboEmotes) {
                if (emoteset == turboSetB && turboEmotes.contains(turboSetA)
                        && emoteManager.equalsByCode(turboSetA, turboSetB)) {
                    // Don't show these Turbo/Prime emotes if the user has the
                    // other set as well, and the emotes are equal
                    continue;
                }
                addEmotes(emoteset);
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
            if (stream == null) {
                addTitle(Language.getString("emotesDialog.noChannel"));
            } else {
                // FFZ/BTTV
                Set<Emoticon> channelEmotes = emoteManager.getEmoticons(stream);
                
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
                    addEmotes(regular, Language.getString("emotesDialog.channelEmotes", stream));
                    for (String info : event.keySet()) {
                        addEmotes(event.get(info), "Featured " + info);
                    }
                }
                
                // Subscriber Emotes
                Set<Emoteset> sets = emoteManager.getEmotesetsByStream(stream);
                if (sets != null && !sets.isEmpty()) {
                    if (addEmotes(Language.getString("emotesDialog.subemotes", stream), sets)) {
                        if (!TwitchEmotes.hasAccessTo(localUserEmotesets, sets)) {
                            addSubtitle(Language.getString("emotesDialog.subscriptionRequired2"), true);
                        }
                    }
                }
            }
            relayout();
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
            addEmotes(emotes, Language.getString("emotesDialog.globalTwitch"));
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

            addEmotes(ffzRegular, Language.getString("emotesDialog.globalFFZ"));
            addEmotes(ffzFeatured, Language.getString("emotesDialog.globalFFZ")+" [Featured]");
            addEmotes(bttv, Language.getString("emotesDialog.globalBTTV"));
            
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
            addEmotesPanel(emotes);
            
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
                addEmotes(categories.get(category), category);
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
            reset();
            
            Emoticon emote = detailsEmote;
            
            addTitle(Language.getString("emotesDialog.details.title", emote.code));
            
            
            lgbc.insets = new Insets(5, 7, 5, 7);
            
            JPanel panel = new JPanel();
            panel.setBackground(emotesBackground);
            panel.setLayout(new GridBagLayout());
            
            addScaledEmote(emote, panel, 1, "100%");
            if (emote.getWidth()*3+200 < EmotesDialog.this.getWidth() && !emote.isAnimated()) {
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
            if (emote.numericId > Emoticon.ID_UNDEFINED) {
                addInfo(panel2, Language.getString("emotesDialog.details.id"), ""+emote.numericId);
            }
            if (!emote.hasGlobalEmoteset()) {
                String emoteSetInfo = String.valueOf(emote.emoteSet);
                if (Emoticons.isTurboEmoteset(emote.emoteSet)) {
                    emoteSetInfo += " (Turbo)";
                }
                Emoteset setInfo = emoteManager.getInfoByEmoteset(emote.emoteSet);
                if (setInfo != null) {
                    emoteSetInfo += " ("+setInfo.product+")";
                }
                if (emote.emoteSet == Emoticon.SET_UNKNOWN) {
                    addInfo(panel2, "Emoteset:", "unknown");
                } else {
                    addInfo(panel2, "Emoteset:", emoteSetInfo);
                }
            }
            if (emote.hasStreamSet() && Helper.isValidStream(emote.getStream())) {
                addInfo(panel2, Language.getString("emotesDialog.details.channel"), emote.getStream());
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
                if (localUserEmotesets.contains(emote.emoteSet)) {
                    addInfo(panel2, "", Language.getString("emotesDialog.details.accessAvailable"));
                }
            }
            addInfo(panel2, Language.getString("emotesDialog.details.size"), emote.getWidth()+"x"+emote.getHeight());
            if (emote.creator != null) {
                addInfo(panel2, Language.getString("emotesDialog.details.by"), emote.creator);
            }
            
            // Info
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
            panel.add(new EmoteLabel(emote, mouseListener, scale, emoteUser), lgbc);
            
            lgbc.gridy = 1;
            panel.add(new JLabel(label), lgbc);
            
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
    
}
