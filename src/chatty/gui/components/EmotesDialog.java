
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.MainGui;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.EmoteContextMenu;
import chatty.util.StringUtil;
import chatty.util.api.CheerEmoticon;
import chatty.util.api.Emoticon;
import chatty.util.api.Emoticon.EmoticonImage;
import chatty.util.api.Emoticon.EmoticonUser;
import chatty.util.api.Emoticons;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
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
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

/**
 * Dialog showing emoticons that can be clicked on to insert them in the last
 * active channel inputbox. Also allows to open the emote context menu.
 * 
 * @author tduva
 */
public class EmotesDialog extends JDialog {
    
    public final int UPDATE_EMOTESET_CHANGED = 1;
    public final int UPDATE_CHANNEL_CHANGED = 2;
    public final int UPDATE_FAVORITES = 4;
    
    private static final Insets TITLE_INSETS = new Insets(5,8,0,8);
    private static final Insets SUBTITLE_INSETS = new Insets(6,4,2,4);
    private static final Insets SUBTITLE_INSETS_SMALLER_MARGIN = new Insets(1,2,0,2);
    private static final Insets EMOTE_INSETS = new Insets(4,10,4,10);
    
    private static final String FAVORITE_EMOTES = "Favorites";
    private static final String MY_EMOTES = "My Emotes";
    private static final String CHANNEL_EMOTES = "Channel";
    private static final String TWITCH_EMOTES = "Twitch";
    private static final String OTHER_EMOTES = "Other";
    private static final String EMOJI = "Emoji";
    private static final String EMOTE_DETAILS = "Emote Details";
    private static final String BITS = "B";
    
    private final JPanel emotesPanel;
    private final Emoticons emoteManager;
    
    private final EmoticonUser emoteUser;
    
    private final CardLayout cardLayout = new CardLayout();
    
    private final MouseAdapter mouseListener;
    private final ContextMenuListener contextMenuListener;
    
    private final List<EmotesPanel> panels = new ArrayList<>();
    private final Map<JToggleButton, EmotesPanel> buttons = new HashMap<>();
    private final EmotesPanel defaultPanel;
    
    /**
     * GridBagConstraints for adding titles/emotes.
     */
    private final GridBagConstraints gbc;
    
    private Set<Integer> emotesets = new HashSet<>();
    private String stream;
    private Emoticon detailsEmote;
    private boolean repaint;
    private float scale;
    
    private boolean closeOnDoubleClick = true;
   
    public EmotesDialog(Window owner, Emoticons emotes, final MainGui main, ContextMenuListener contextMenuListener) {
        super(owner);
        
        emoteUser = new Emoticon.EmoticonUser() {

            @Override
            public void iconLoaded() {
//                repaint = true;
                repaint();
            }
        };
//        Timer timer = new Timer(100, new ActionListener() {
//
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                if (repaint) {
//                    repaint();
//                    repaint = false;
//                }
//            }
//        });
//        timer.setRepeats(true);
//        timer.start();
        
        // TODO: Focusable or maybe just when clicked on emote to insert code?
        this.setFocusable(false);
        this.setFocusableWindowState(false);
        this.contextMenuListener = contextMenuListener;
        this.emoteManager = emotes;
        setResizable(true);

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
        defaultPanel = panels.get(1);
        
        // Buttons/Button Panel
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
        
        panels.add(new EmoteDetailPanel(EMOTE_DETAILS, 0));

        mouseListener = new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2 && closeOnDoubleClick) {
                        setVisible(false);
                    } else {
                        Emote label = (Emote) e.getSource();
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
        
        // Emotes
        emotesPanel = new JPanel();
        emotesPanel.setBackground(Color.WHITE);
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
    private static JComponent wrapPanel(JComponent panel) {
        panel.setBackground(Color.WHITE);
        JPanel outer = new JPanel();
        outer.setLayout(new GridBagLayout());
        outer.setBackground(Color.WHITE);
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
            EmoticonImage emote = ((Emote)e.getSource()).emote;
            JPopupMenu m = new EmoteContextMenu(emote, contextMenuListener);
            m.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * Opens the dialog, using the given emotesets and stream.
     *
     * @param emotesets
     * @param stream
     */
    public void showDialog(Set<Integer> emotesets, String stream) {
        if (stream != null && !stream.equals(this.stream)) {
            setUpdated(UPDATE_CHANNEL_CHANGED);
        }
        if (stream != null) {
            this.stream = stream;
        }
        if (emotesets != null && !emotesets.equals(this.emotesets)) {
            setUpdated(UPDATE_EMOTESET_CHANGED);
        }
        this.emotesets = new HashSet<>(emotesets);
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
        if (!isVisible() || emotesets == null || emotesets.equals(this.emotesets)) {
            return;
        }
        this.emotesets = new HashSet<>(emotesets);
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
        String base = "Global/Subscriber/Turbo";
        if (stream == null) {
            setTitle("Emoticons ("+base+")");
        } else {
            setTitle("Emoticons ("+base+"/#"+stream+")");
        }
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
    private static class Emote extends JLabel {
        
        private static final Border BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        
        public final String code;
        public final EmoticonImage emote;
        public final boolean noInsert;

        public Emote(Emoticon emote, MouseListener mouseListener, float scale,
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
    
    
    private abstract class EmotesPanel extends JPanel {
        
        
        
        private boolean shouldUpdate;
        private final String label;
        private final int updateOn;
        
        EmotesPanel(String name, int updateOn) {
            super(new GridBagLayout());
            this.label = name;
            this.updateOn = updateOn;
        }

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
            String stream = emoteManager.getStreamFromEmoteset(emoteset);
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
         * Adds a title (label with seperating line).
         *
         * @param title The text of the title
         */
        void addTitle(String title) {
            JLabel titleLabel = new JLabel(StringUtil.shortenTo(title, 48, 34));
            titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
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
            panel.setBackground(new Color(250, 250, 250));
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
            for (Emoticon emote : emotes) {
                final JLabel label = new Emote(emote, mouseListener, scale, emoteUser);
                panel.add(label);
            }
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = EMOTE_INSETS;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1;
            add(panel, gbc);
            gbc.gridy++;
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
                addTitle("You haven't added any favorite emotes");
                if (emoteManager.getNumNotFoundFavorites() > 0) {
                    addSubtitle("(Emotes may not have been loaded yet.)", false);
                }
            }

            // Sort emotes by emoteset
            List<Emoticon> sorted = new ArrayList<>(emotes);
            Collections.sort(sorted, new SortEmotesByEmoteset());

            // Sort out emotes that the user probably doesn't have access to
            List<Emoticon> subEmotesNotSubbedTo = new ArrayList<>();
            for (Emoticon emote : sorted) {
                if (emote.emoteSet != Emoticon.SET_UNDEFINED && !emotesets.contains(emote.emoteSet)) {
                    subEmotesNotSubbedTo.add(emote);
                }
            }
            sorted.removeAll(subEmotesNotSubbedTo);

            // Add emotes
            addEmotesPanel(sorted);
            if (!subEmotesNotSubbedTo.isEmpty()) {
                addTitle("You need to subscribe to use these emotes:");
                addEmotesPanel(subEmotesNotSubbedTo);
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
            if (emotesets.isEmpty() || (emotesets.size() == 1 
                    && emotesets.iterator().next().equals(Emoticon.SET_GLOBAL))) {
                addTitle("You don't seem to have any sub or turbo emotes");
                if (stream == null) {
                    addSubtitle("(Must join a channel for them to be recognized.)", false);
                }
            }
            // Put turbo emotes at the end
            Set<Integer> turboEmotes = new HashSet<>();
            for (Integer emoteset : emotesets) {
                if (Emoticons.isTurboEmoteset(emoteset)) {
                    turboEmotes.add(emoteset);
                } else if (emoteset != Emoticon.SET_GLOBAL) {
                    addEmotes(emoteset);
                }
            }

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
                addTitle("No Channel.");
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
                                event.put(info, new HashSet<Emoticon>());
                            }
                            event.get(info).add(emote);
                        }
                    } else {
                        regular.add(emote);
                    }
                }
                
                if (channelEmotes.isEmpty()) {
                    addTitle("No emotes found for #" + stream);
                    addSubtitle("No FFZ or BTTV emotes found.", false);
                } else {
                    addEmotes(regular, "Emotes specific to #" + stream);
                    for (String info : event.keySet()) {
                        addEmotes(event.get(info), "Featured " + info);
                    }
                }
                
                // Subscriber Emotes
                int emoteset = emoteManager.getEmotesetFromStream(stream);
                if (emoteset != -1) {
                    Set<Emoticon> subEmotes = emoteManager.getEmoticons(emoteset);
                    if (!subEmotes.isEmpty()) {
                        addTitle("Subscriber emotes of " + stream + " (" + subEmotes.size() + ")");
                        addEmotesPanel(sortEmotes(subEmotes));
                        if (!emotesets.contains(emoteset)) {
                            addSubtitle("(Need to be subscribed to use these.)", true);
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
            addEmotes(emotes, "Global Twitch Emotes");
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

            addEmotes(ffzRegular, "Global FFZ Emotes");
            addEmotes(ffzFeatured, "Global FFZ Emotes [Featured]");
            addEmotes(bttv, "Global BTTV Emotes");
            
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
            
            addTitle("Emote Details: "+emote.code);
            
            
            lgbc.insets = new Insets(5, 7, 5, 7);
            
            JPanel panel = new JPanel();
            panel.setBackground(new Color(250, 250, 250));
            panel.setLayout(new GridBagLayout());
            
            addScaledEmote(emote, panel, 1, "100%");
            if (emote.getWidth()*3+200 < EmotesDialog.this.getWidth() && !emote.isAnimated) {
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
                addInfo(panel2, "Code:", emote.code);
            }
            String featured = emote.subType == Emoticon.SubType.EVENT ? " (Featured)" : "";
            addInfo(panel2, "Type:", emote.type.toString()+featured);
            if (emote.numericId > Emoticon.ID_UNDEFINED) {
                addInfo(panel2, "Emote ID:", ""+emote.numericId);
            }
            if (emote.emoteSet > Emoticon.SET_UNDEFINED) {
                String emoteSetInfo = String.valueOf(emote.emoteSet);
                if (Emoticons.isTurboEmoteset(emote.emoteSet)) {
                    emoteSetInfo += " (Turbo)";
                }
                addInfo(panel2, "Twitch Emoteset:", emoteSetInfo);
            }
            if (emote.emoteSet == Emoticon.SET_UNKNOWN) {
                addInfo(panel2, "Twitch Emoteset:", "unknown");
            }
            if (emote.hasStreamSet() && Helper.validateStream(emote.getStream())) {
                addInfo(panel2, "Channel:", emote.getStream());
            }
            addInfo(panel2, "Usability:", emote.hasStreamRestrictions() ? "Local" : "Global");
            addInfo(panel2, "Regular Size: ", emote.getWidth()+"x"+emote.getHeight());
            if (emote.creator != null) {
                addInfo(panel2, "Emote by:", emote.creator);
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
            add(new JLabel("<html><body style='width:200px;text-align:center;color:#888888'>Right-click on "
                    + "emotes here or in chat to open context-menu with info/options."), gbc);
            
            relayout();
        }
        
        private void addScaledEmote(Emoticon emote, JPanel panel, float scale, String label) {
            lgbc.anchor = GridBagConstraints.CENTER;
            lgbc.gridy = 0;
            panel.add(new Emote(emote, mouseListener, scale, emoteUser), lgbc);
            
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
