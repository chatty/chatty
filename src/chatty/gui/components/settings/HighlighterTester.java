
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.gui.Highlighter.Match;
import chatty.gui.MainGui;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.commands.CustomCommand;
import chatty.util.irc.MsgTags;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Dialog to check if the entered pattern matches the test text. This can be
 * used for developing patterns for the Highlight and Ignore lists.
 * 
 * @author tduva
 */
public class HighlighterTester extends JDialog implements StringEditor {
    
    public static Map<String, CustomCommand> testPresets;
    
    private static final int MAX_INPUT_LENGTH = 50*1000;

    private final static String TEST_PRESET = "Enter test text.";
    private final static String TEST_PRESET_EXAMPLE = TEST_PRESET+" Example: ";
    
    private final JButton okButton = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
    private final JButton addToBlacklistButton = new JButton("Add to Blacklist");
    private final JTextField itemValue = new JTextField(40);
    private final JTextField blacklistValue = new JTextField(40);
    private final JTextPane testInput = new JTextPane();
    private final DefaultStyledDocument doc = new DefaultStyledDocument();
    private final JLabel testResult = new JLabel("Abc");
    private final LinkLabel infoText = new LinkLabel("", null);
    private final JTextArea parseResult = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();
    private final ItemFields itemFields;
    
    private final MutableAttributeSet matchAttr1 = new SimpleAttributeSet();
    private final MutableAttributeSet matchAttr2 = new SimpleAttributeSet();
    private final MutableAttributeSet defaultAttr = new SimpleAttributeSet();
    private final MutableAttributeSet blacklistAttr = new SimpleAttributeSet();
    
    private final String type;
    
    private boolean editingBlacklistItem;
    private boolean allowEmpty;
    private String result;
    private ActionListener addToBlacklistListener;
    
    /**
     * Previous automatically set test text.
     */
    private String prevTestText;
    
    private HighlightItem highlightItem;
    private HighlightItem blacklistItem;
    
    public HighlighterTester(Window owner, boolean showBlacklist, String type) {
        super(owner);
        this.type = type;
        
        setTitle("Test and Edit");
        
        // Styles
        StyleConstants.setBackground(matchAttr1, Color.BLUE);
        StyleConstants.setForeground(matchAttr1, Color.WHITE);
        StyleConstants.setBackground(matchAttr2, new Color(0, 140, 100));
        StyleConstants.setForeground(matchAttr2, Color.WHITE);
        StyleConstants.setStrikeThrough(blacklistAttr, true);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        final Insets leftInsets = new Insets(5, 16, 5, 5);
        
        JPanel main = new JPanel(new GridBagLayout());
        
        gbc = GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(10, 5, 0, 5);
        main.add(new JLabel("Full "+type+" item:"),
                gbc);
        
        gbc = GuiUtil.makeGbc(1, 1, 1, 1, GridBagConstraints.EAST);
        gbc.insets = new Insets(10, 5, 0, 5);
        main.add(new JLabel("(You may edit it here or below.)"),
                gbc);
        
        gbc = GuiUtil.makeGbc(0, 2, 2, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = leftInsets;
        gbc.weightx = 1;
        itemValue.setFont(Font.decode(Font.MONOSPACED));
        GuiUtil.installLengthLimitDocumentFilter(itemValue, MAX_INPUT_LENGTH, false);
        main.add(itemValue, gbc);
        
        itemFields = new ItemFields(itemValue);
        itemFields.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(1, 4, 1, 4)));
        gbc = GuiUtil.makeGbc(0, 4, 2, 1);
        gbc.insets = leftInsets;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        main.add(itemFields, gbc);
        
        //--------------------------
        // Blacklist
        //--------------------------
        if (showBlacklist) {
            gbc = GuiUtil.makeGbc(0, 11, 2, 1, GridBagConstraints.LINE_START);
            gbc.insets = new Insets(10, 5, 0, 5);
            JLabel blacklistLabel = new JLabel("Blacklist item:");
            blacklistLabel.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "You can enter a single blacklist item here to test how it would affect text matches if it were on the blacklist."));
            main.add(blacklistLabel,
                    gbc);

            gbc = GuiUtil.makeGbc(0, 12, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = leftInsets;
            blacklistValue.setFont(Font.decode(Font.MONOSPACED));
            GuiUtil.installLengthLimitDocumentFilter(blacklistValue, MAX_INPUT_LENGTH, false);
            main.add(blacklistValue, gbc);
            
            gbc = GuiUtil.makeGbc(0, 13, 2, 1);
            gbc.insets = new Insets(0, 5, 4, 5);
            addToBlacklistButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            main.add(addToBlacklistButton, gbc);
            addToBlacklistButton.setVisible(false);
            addToBlacklistButton.addActionListener(e -> {
                if (addToBlacklistListener != null) {
                    addToBlacklistListener.actionPerformed(new ActionEvent(
                            addToBlacklistButton,
                            ActionEvent.ACTION_FIRST,
                            blacklistValue.getText()));
                    addToBlacklistButton.setEnabled(false);
                }
            });
        }
        
        //--------------------------
        // Test field
        //--------------------------
        gbc = GuiUtil.makeGbc(0, 14, 1, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(10, 5, 0, 5);
        JLabel testLabel = new JLabel("Test text:");
        testLabel.setToolTipText("Non-text requirements are ignored for testing.");
        main.add(testLabel,
                gbc);
        
//        gbc = GuiUtil.makeGbc(1, 14, 1, 1, GridBagConstraints.EAST);
//        gbc.insets = new Insets(10, 5, 0, 5);
//        main.add(new JLabel("(Non-text requirements ignored.)"),
//                gbc);
        
        gbc = GuiUtil.makeGbc(0, 15, 2, 1);
        gbc.insets = leftInsets;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        testInput.setDocument(doc);
        testInput.setPreferredSize(new Dimension(0, 50));
        testInput.setFont(Font.decode(Font.MONOSPACED));
        // Enable focus traversal keys
        GuiUtil.resetFocusTraversalKeys(testInput);
        GuiUtil.installLengthLimitDocumentFilter(testInput, 1000, false);
        main.add(new JScrollPane(testInput),
                gbc);
        
        gbc = GuiUtil.makeGbc(0, 16, 2, 1, GridBagConstraints.WEST);
        gbc.insets = leftInsets;
        main.add(testResult, gbc);

        gbc = GuiUtil.makeGbc(0, 10, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 4, 0, 4);
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        add(main, gbc);
        
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
        
        //--------------------------
        // Help/Result
        //--------------------------
        JPanel helpPanel = new JPanel();
        helpPanel.add(infoText);
        
        JPanel parseResultPanel = new JPanel(new BorderLayout());
        JLabel parseResultInfo = new JLabel(HighlightSettings.INFO_HEADER
                + "Shows parsing information and the requirements for a match (each line's condition has to be satisified).");
        parseResultInfo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        parseResultPanel.add(parseResultInfo,
                BorderLayout.NORTH);
        parseResultPanel.add(new JScrollPane(parseResult),
                BorderLayout.CENTER);
        parseResult.setFont(new Font(Font.MONOSPACED, 0, parseResult.getFont().getSize()));
        parseResult.setEditable(false);
        parseResult.setLineWrap(true);
        parseResult.setWrapStyleWord(true);
        
        tabs.add("Help", helpPanel);
        tabs.add("Parse Result", parseResultPanel);
        
        gbc = GuiUtil.makeGbc(0, 20, 3, 1, GridBagConstraints.CENTER);
        gbc.insets = new Insets(10, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0.5;
        add(tabs, gbc);
        
        //--------------------------
        // Listeners and stuff
        //--------------------------
        okButton.addActionListener(e -> {
            result = editingBlacklistItem ? blacklistValue.getText() : itemValue.getText();
            setVisible(false);
        });
        cancelButton.addActionListener(e -> setVisible(false));
        
        /**
         * Use DocumentFilter to be able to change the Document without
         * triggering and endless loop.
         */
        doc.setDocumentFilter(new DocumentFilter() {
            
            @Override
            public void insertString(DocumentFilter.FilterBypass fb, int offset, String string,
                    AttributeSet attr) throws BadLocationException {
                fb.insertString(offset, StringUtil.removeLinebreakCharacters(string), attr);
                updateMatches((DefaultStyledDocument)fb.getDocument());
                updateInfoText();
            }
            
            @Override
            public void remove(DocumentFilter.FilterBypass fb, int offset, int length) throws
                    BadLocationException {
                fb.remove(offset, length);
                updateMatches((DefaultStyledDocument)fb.getDocument());
                updateInfoText();
            }

            @Override
            public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                fb.replace(offset, length, StringUtil.removeLinebreakCharacters(text), attrs);
                updateMatches((DefaultStyledDocument)fb.getDocument());
                updateInfoText();
            }
            
        });
        
        addDocumentListener(itemValue, e -> {
            updateItem();
        });
        
        addDocumentListener(blacklistValue, e -> {
            updateBlacklistItem();
        });
        
        updateEditIndicator();
        updateSaveButton();
        
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(getPreferredSize());
    }
    
    private static void addDocumentListener(JTextField textField, Consumer<DocumentEvent> action) {
        textField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                action.accept(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.accept(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.accept(e);
            }
        });
    }
    
    private HighlightItem createItem(String value) {
        return new HighlightItem(value, type, false,
                testPresets != null ? testPresets : new HashMap<>());
    }
    
    private void updateItem() {
        String value = itemValue.getText();
        if (value.isEmpty()) {
            highlightItem = null;
        } else {
            highlightItem = createItem(value);
        }
        updateParseResult();
        updateInfoText();
        updateMatches(doc);
        updateSaveButton();
        updateTestText();
        
        itemFields.update();
//        System.out.println(highlightItem.getMatchInfo());
    }
    
    private void updateBlacklistItem() {
        String value = blacklistValue.getText();
        if (value.isEmpty()) {
            blacklistItem = null;
            addToBlacklistButton.setEnabled(false);
        } else {
            blacklistItem = createItem(value);
            addToBlacklistButton.setEnabled(!blacklistItem.hasError());
        }
        updateParseResult();
        updateInfoText();
        updateMatches(doc);
        updateSaveButton();
        updateTestText();
    }
        
    public void updateParseResult() {
        String text = "";
        boolean warningIcon = false;
        if (highlightItem != null) {
            if (highlightItem.patternThrowsError()) {
                warningIcon = true;
            }
            if (highlightItem.getMainPrefix() == null
                    && highlightItem.getTextWithoutPrefix().matches("^\\+?!?\\w+:.*")) {
                text += "[!] The beginning of the text has the format of a prefix "
                        + "(even though it's not one as of yet), so to keep it safe for "
                        + "the future, it is "
                        + "recommended to prefix 'text:' (or another Text Matching "
                        + "Prefix) to indicate no more prefixes are to come:\n\n"
                        + " text:" + highlightItem.getTextWithoutPrefix() + "\n\n";
                warningIcon = true;
            }
        }
        if (warningIcon) {
            int size = tabs.getFontMetrics(tabs.getFont()).getHeight();
            Icon icon = GuiUtil.getFallbackIcon(UIManager.getIcon("OptionPane.warningIcon"), MainGui.class, "warning.png");
            tabs.setIconAt(1, GuiUtil.getScaledIcon(icon, size, size));
        }
        else {
            tabs.setIconAt(1, null);
        }
        if (highlightItem != null) {
            text += highlightItem.getMatchInfo();
        }
        if (blacklistItem != null) {
            text += "\n### Blacklist ###\n"+blacklistItem.getMatchInfo();
        }
        parseResult.setText(text);
    }
    
    private void updateInfoText() {
        Highlighter.Blacklist blacklist = null;
        if (blacklistItem != null) {
            // Match ANY type, same as the other matching in this (ignoring
            // non-text prefixes)
            blacklist = new Highlighter.Blacklist(HighlightItem.Type.ANY, testInput.getText(), null,
                    null, null, null, MsgTags.EMPTY, Arrays.asList(new HighlightItem[]{blacklistItem}));
        }
        if (highlightItem == null) {
            testResult.setText("Empty item.");
        } else if (highlightItem.hasError()) {
            testResult.setText("Error: "+highlightItem.getError());
        } else if (highlightItem.matchesTest(testInput.getText(), blacklist)) {
            testResult.setText("Matched.");
        } else {
            String failedReason = highlightItem.getFailedReason();
            if (failedReason == null) {
                testResult.setText("No match.");
            }
            else {
                testResult.setText("No match: "+failedReason);
            }
            if (highlightItem.hasMatchingError()) {
                testResult.setText("Error: "+highlightItem.getMatchingError());
            }
        }
        if (blacklistItem != null && blacklistItem.hasError()) {
            testResult.setText("Blacklist error: "+blacklistItem.getError());
        }
    }

    private void updateMatches(StyledDocument doc) {
        try {
            // Reset to default styles
            doc.setCharacterAttributes(0, doc.getLength(), defaultAttr, true);
            
            // Regular item
            if (highlightItem != null) {
                List<Match> matches = highlightItem.getTextMatches(testInput.getText());
                if (matches != null) {
                    for (int i = 0; i < matches.size(); i++) {
                        Match m = matches.get(i);
                        if (i % 2 == 0) {
                            doc.setCharacterAttributes(m.start, m.end - m.start, matchAttr1, false);
                        } else {
                            doc.setCharacterAttributes(m.start, m.end - m.start, matchAttr2, false);
                        }
                    }
                }
            }
            
            // Blacklist item
            if (blacklistItem != null) {
                List<Match> blacklistMatches = blacklistItem.getTextMatches(testInput.getText());
                if (blacklistMatches != null) {
                    for (int i = 0; i < blacklistMatches.size(); i++) {
                        Match m = blacklistMatches.get(i);
                        doc.setCharacterAttributes(m.start, m.end - m.start, blacklistAttr, false);
                    }
                } else if (blacklistItem.matchesTest(testInput.getText(), null)) {
                    doc.setCharacterAttributes(0, doc.getLength(), blacklistAttr, false);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static final Border EDITING_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(1, 2, 1, 2));
    private static final Border DEFAULT_BORDER = new JTextField().getBorder();
    
    /**
     * Set to true if this instance is used for editing a blacklist item, which
     * sets the preset to the Blacklist field and also returns that value, as
     * well as changes some other related behaviour.
     * 
     * @param editingBlacklistItem 
     */
    public void setEditingBlacklistItem(boolean editingBlacklistItem) {
        this.editingBlacklistItem = editingBlacklistItem;
        updateEditIndicator();
    }
    
    public void setAllowEmpty(boolean allowEmpty) {
        this.allowEmpty = allowEmpty;
    }
    
    private void updateEditIndicator() {
        if (editingBlacklistItem) {
            blacklistValue.setBorder(EDITING_BORDER);
            itemValue.setBorder(DEFAULT_BORDER);
        } else {
            blacklistValue.setBorder(DEFAULT_BORDER);
            itemValue.setBorder(EDITING_BORDER);
        }
    }
    
    private void updateTestText() {
        HighlightItem item = editingBlacklistItem ? blacklistItem : highlightItem;
        boolean matches = item != null && item.matchesAny(testInput.getText(), null);
        if (!matches && (testInput.getText().isEmpty() || testInput.getText().equals(prevTestText))) {
            if (item != null && item.getTextWithoutPrefix().length() < 20) {
                testInput.setText(TEST_PRESET_EXAMPLE+item.getTextWithoutPrefix());
            } else {
                testInput.setText(TEST_PRESET);
            }
            prevTestText = testInput.getText();
        }
    }
    
    private void updateSaveButton() {
        if (editingBlacklistItem) {
            okButton.setEnabled(blacklistItem != null && !blacklistItem.hasError());
        } else {
            okButton.setEnabled((highlightItem != null && !highlightItem.hasError()) || allowEmpty);
        }
    }

    @Override
    public String showDialog(String title, String preset, String info) {
        if (editingBlacklistItem) {
            blacklistValue.requestFocusInWindow();
        } else {
            itemValue.requestFocusInWindow();
        }
        setTitle(title);
        result = null;
        infoText.setText(info);
        // Reset so it doesn't affect sizing
        itemValue.setText(null);
        blacklistValue.setText(null);
        parseResult.setText(null);
        revalidate();
        pack();
        // Caused issues for Notification editor for some reason
        SwingUtilities.invokeLater(() -> {
            setMinimumSize(getPreferredSize());
            // Set after setting size, so that Parse Result does not affect it
            if (editingBlacklistItem) {
                blacklistValue.setText(preset);
            }
            else {
                itemValue.setText(preset);
            }
            updateItem();
            updateBlacklistItem();
            updateTestText();
        });
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(getParent());
        setVisible(true);
        return result;
    }

    @Override
    public void setLinkLabelListener(LinkLabelListener listener) {
        this.infoText.setListener(listener);
        this.itemFields.setLinkLabelListener(listener);
    }
    
    public void setAddToBlacklistListener(ActionListener listener) {
        this.addToBlacklistListener  = listener;
        if (listener != null) {
            addToBlacklistButton.setVisible(true);
        }
    }
    
    private static class ItemFields extends JPanel {
        
        private final JTextComponent item;
        
        private final JTextField metaPrefixes = new JTextField();
        private final LinkLabel metaPrefixesLabel;
        private final JLabel metaPrefixesError = new JLabel();
        
        private final LinkLabel textMatchLabel;
        private final JTextField mainPrefix;
        private final JTextField mainText = new JTextField();
        private final MatchOptions matchOptions = new MatchOptions();
        
        private boolean updating;
        
        ItemFields(JTextComponent item) {
            this.item = item;
            
            setLayout(new GridBagLayout());
            
            GridBagConstraints gbc;
            
            //--------------------------
            // Meta Prefixes
            //--------------------------
            JPanel metaPrefixesLabelPanel = new JPanel(new GridBagLayout());
            gbc = GuiUtil.makeGbc(0, 4, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            metaPrefixesLabel = new LinkLabel("[help-settings:Highlight_Meta_Matching Meta Prefixes:]", null);
            metaPrefixesLabel.setMargin(null);
            metaPrefixesLabelPanel.add(metaPrefixesLabel, gbc);

            gbc = GuiUtil.makeGbc(1, 4, 1, 1);
            gbc.insets = new Insets(0, 0, 0, 0);
            metaPrefixesLabelPanel.add(metaPrefixesError, gbc);

            gbc = GuiUtil.makeGbc(0, 4, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 0, 5);
            add(metaPrefixesLabelPanel, gbc);

            gbc = GuiUtil.makeGbc(0, 5, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            metaPrefixes.setFont(Font.decode(Font.MONOSPACED));
            add(metaPrefixes, gbc);

            //--------------------------
            // Text Match
            //--------------------------
            gbc = GuiUtil.makeGbc(0, 7, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(5, 5, 0, 5);
            textMatchLabel = new LinkLabel("[help-settings:Highlight_Matching Text Match:]", null);
            textMatchLabel.setMargin(null);
            add(textMatchLabel, gbc);

            JPanel textMatchPanel = new JPanel();
            textMatchPanel.setLayout(new GridBagLayout());
            gbc = GuiUtil.makeGbc(0, 0, 1, 1);
            mainPrefix = new JTextField(8) {
                
                // Prevent field from shrinking in case of long text match
                @Override
                public Dimension getMinimumSize() {
                    return getPreferredSize();
                }
                
            };
            mainPrefix.setFont(Font.decode(Font.MONOSPACED));
            mainPrefix.setEditable(false);
            textMatchPanel.add(mainPrefix, gbc);
            gbc = GuiUtil.makeGbc(1, 0, 1, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            mainText.setFont(Font.decode(Font.MONOSPACED));
            textMatchPanel.add(mainText, gbc);

            gbc = GuiUtil.makeGbc(0, 8, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 0, 0);
            add(textMatchPanel, gbc);

            gbc = GuiUtil.makeGbc(0, 9, 2, 1);
            gbc.insets = new Insets(0, 0, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            add(matchOptions, gbc);
            
            matchOptions.setListener(p -> {
                if (p != null) {
                    mainPrefix.setText(p);
                    constructValue();
                }
            });
            
            addDocumentListener(metaPrefixes, e -> {
                constructValue();
            });

            addDocumentListener(mainText, e -> {
                constructValue();
            });
        }
        
        private void constructValue() {
            if (updating) {
                return;
            }
            updating = true;
            HighlightItem metaItem = new HighlightItem(metaPrefixes.getText(), "", false, null);
            String mPrefixes = metaItem.getMetaPrefixes();
            String mPrefixesExtra = metaItem.getRaw().substring(metaItem.getMetaPrefixes().length()).trim();
            String newValue = mPrefixes;
            if (!mPrefixesExtra.isEmpty()) {
                metaPrefixesError.setText("Not a meta prefix: " + StringUtil.shortenTo(mPrefixesExtra, 20));
            }
            else {
                metaPrefixesError.setText(null);
            }
            String main = "";
            if (!mainText.getText().trim().isEmpty()) {
                main = mainPrefix.getText() + mainText.getText();
            }
            newValue = StringUtil.append(newValue, " ", main);
            item.setText(newValue);
            updating = false;
        }
        
        public void setLinkLabelListener(LinkLabelListener listener) {
            metaPrefixesLabel.setListener(listener);
            textMatchLabel.setListener(listener);
        }
        
        public void update() {
            if (updating) {
                return;
            }
            // Empty type to disable presets
            HighlightItem i = new HighlightItem(item.getText(), "", false, null);
            updating = true;
            String mPrefix = i.getMainPrefix();
            if (StringUtil.isNullOrEmpty(mPrefix)) {
                mPrefix = "text:";
            }
            mainPrefix.setText(mPrefix);
            matchOptions.setPrefix(mPrefix);
            mainText.setText(i.getTextWithoutPrefix());
            metaPrefixes.setText(i.getMetaPrefixes());
            updating = false;
        }
        
    }
    
    /**
     * Provides a panel of checkboxes with options that can be selected to set
     * a text matchting prefix. Checkboxes are automatically disabled to prevent
     * combinations of options for which a prefix doesn't exist.
     * 
     * <p>Special checkboxes are "Other" (which is used for prefixes that aren't
     * supported by this) and "Inverted" (which applies the same to all
     * prefixes, supported or not).
     */
    private static class MatchOptions extends JPanel {
        
        private enum Prefix {
            
            /**
             * Defines which properties each prefix stands for and thus the
             * relationship between the checkbox selection and the prefixes (as
             * well as which checkboxes are disabled based on which selection).
             * The "Other" bit isn't used here, but only in the special OTHER
             * constant.
             * 
             * Other, Regex, Case-sensitive, Word, Start
             */
            TEXT("text:", 0b0000),
            CS("cs:", 0b0100),
            W("w:", 0b0010),
            WCS("wcs:", 0b0110),
            START("start:", 0b0001),
            STARTW("startw:", 0b0011),
            REG("reg:", 0b1100),
            REGI("regi:", 0b1000),
            REGW("regw:", 0b1110),
            REGWI("regwi:", 0b1010);

            public final String name;
            public final int options;
            
            Prefix(String prefix, int options) {
                this.name = prefix;
                this.options = options;
            }
            
        }
        
        private static final int OTHER = 0b10000;
        
        private final JCheckBox regex = new JCheckBox("Regex");
        private final JCheckBox caseSensitive = new JCheckBox("Case-sensitive");
        private final JCheckBox word = new JCheckBox("Word");
        private final JCheckBox start = new JCheckBox("Start");
        private final JCheckBox inverted = new JCheckBox("Inverted");
        private final JCheckBox other = new JCheckBox("Other");
        
        private final List<JCheckBox> checkboxes = new ArrayList<>();
        
        private Consumer<String> listener;
        
        /**
         * True when setting the state of checkboxes that isn't initiated by the
         * user, to prevent unwanted update notifications.
         */
        private boolean setInProgress;
        
        /**
         * The prefix that was last set. This can be returned for unsupported
         * "Other" prefixes.
         */
        private String lastSetPrefix;
        
        MatchOptions() {
            setLayout(new FlowLayout(FlowLayout.LEADING, 5, 0));
            
            regex.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "Use Regular Expressions"));
            caseSensitive.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "Case-sentitive text matching (e.g. 'Test' would not be the same as 'TEST')"));
            word.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "The beginning and end must be at a word boundary (e.g. 'w:test' would match 'This is a test!' but not 'This is testing!')"));
            start.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "The text must be at the beginning of the message (e.g. 'start:!quote' would match when '!quote' is at the beginning of the message)"));
            inverted.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "The match is inverted (e.g. '!start:quote' would match when there is no '!quote' at the beginning of the message)"));
            other.setToolTipText(SettingsUtil.addTooltipLinebreaks(
                    "This is checked when prefixes that can't be selected here are used (unchecking switches the prefix and allows you to use the other checkboxes again)"));
            
            add(regex);
            add(caseSensitive);
            add(word);
            add(start);
            add(inverted);
            add(other);
            
            /**
             * Must be added in this order so that the Option "enabled"
             * parameter matches.
             */
            checkboxes.add(start);
            checkboxes.add(word);
            checkboxes.add(caseSensitive);
            checkboxes.add(regex);
            checkboxes.add(other);
            
            for (JCheckBox check : checkboxes) {
                check.addItemListener(e -> {
                    updateSelected();
                });
            }
            inverted.addItemListener(e -> updateSelected());
        }
        
        /**
         * Update based on the current selected state of the checkboxes.
         */
        private void updateSelected() {
            if (listener != null && !setInProgress) {
                updateDisabled();
                listener.accept(getPrefix());
            }
        }
        
        /**
         * Set a new selected state for the checkboxes.
         * 
         * @param selected 
         */
        private void setSelectedOptions(int selected) {
            for (int i=0;i<checkboxes.size();i++) {
                checkboxes.get(i).setSelected((selected & (1 << i)) != 0);
            }
            updateDisabled();
        }
        
        /**
         * Update which checkboxes should be disabled based on the currently
         * selected checkboxes. Not all combinations of options have a prefix,
         * so this disables all checkboxes that don't fit the current selection.
         */
        private void updateDisabled() {
            setInProgress = true;
            
            // Find all options that also have the current selection selected
            int enabled = getSelectedOptions();
            int possibleOptions = enabled;
            for (Prefix option : Prefix.values()) {
                // Check if current selection bits are set (enabled 0 selects all)
                if ((option.options & enabled) == enabled) {
                    possibleOptions = possibleOptions | option.options;
                }
            }
            
            // Disable all options that don't fit the current selection
            for (int i=0;i<checkboxes.size();i++) {
                boolean optionPossible = (possibleOptions & (1 << i)) != 0;
                checkboxes.get(i).setEnabled(optionPossible);
                if (!optionPossible) {
                    checkboxes.get(i).setSelected(false);
                }
            }
            
            setInProgress = false;
        }
        
        /**
         * Gets which options are currently selected.
         * 
         * @return An int with the appropriate bits set
         */
        private int getSelectedOptions() {
            int selected = 0;
            for (int i=0;i<checkboxes.size();i++) {
                selected += checkboxes.get(i).isSelected() ? 1 << i : 0;
            }
            return selected;
        }
        
        /**
         * Sets the current prefix which may change the currently selected
         * options. If the prefix is not supported, the "Other" option is
         * selected and (aside from the "Inverted" option) the set prefix will
         * be returned by {@link getPrefix()}.
         * 
         * @param prefix 
         */
        public void setPrefix(String prefix) {
            setInProgress = true;
            // Inverted
            inverted.setSelected(prefix.startsWith("!"));
            prefix = prefix.startsWith("!") ? prefix.substring(1) : prefix;
            // Set prefix and options
            this.lastSetPrefix = prefix;
            boolean prefixFound = false;
            for (Prefix p : Prefix.values()) {
                if (p.name.equals(prefix)) {
                    setSelectedOptions(p.options);
                    prefixFound = true;
                }
            }
            if (!prefixFound) {
                setSelectedOptions(OTHER);
            }
            setInProgress = false;
        }
        
        /**
         * Get the prefix based on the currently selected options.
         * 
         * @return The current prefix
         */
        public String getPrefix() {
            // Default to last set
            String result = lastSetPrefix;
            int selectedOptions = getSelectedOptions();
            for (Prefix p : Prefix.values()) {
                if (p.options == selectedOptions) {
                    result = p.name;
                }
            }
            return inverted.isSelected() ? "!"+result : result;
        }
        
        /**
         * The listener receives the current prefix when the selection changes
         * (aside from changes like when the prefix is set).
         * 
         * @param listener 
         */
        public void setListener(Consumer<String> listener) {
            this.listener = listener;
        }
        
    }
    
    public static final String TEST_INFO = "<html><body style='width:300px;font-weight:normal;'>"
            + "Quick reference (see [help-settings:Highlight help] for more):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>Bets open</code> - 'Bets open' anywhere in the message"
            + "<li><code>cs:Bets open</code> - Same, but case-sensitive</li>"
            + "<li><code>w:Clip</code> - 'Clip' as a word, so e.g. not 'Clipped'</li>"
            + "<li><code>wcs:Clip</code> - Same, but case-sensitive</li>"
            + "<li><code>start:!slot</code> - Message beginning with '!slot'</li>"
            + "<li><code>reg:(?i)^!\\w+$</code> - Regular expression (Regex)</li>"
            + "</ul>"
            + "Meta prefixes (in front of text matching):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>chan:joshimuz</code> - Restrict to channel 'joshimuz'</li>"
            + "<li><code>user:Elorie</code> - Restrict to user 'Elorie'</li>"
            + "<li><code>cat:vip</code> - Restrict to users in category 'vip'</li>"
            + "<li><code>config:info</code> - Match info messages</li>"
            + "</ul>";
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HighlighterTester.testPresets = Highlighter.HighlightItem.makePresets(Arrays.asList(new String[]{
                "_test $replace($1-,$\"\\\\!\",$\"[\\W_]*?\",reg)"
            }));
            HighlighterTester tester = new HighlighterTester(null, true, "highlight");
            tester.setAddToBlacklistListener(e -> {
                System.out.println(e);
            });
            //tester.setDefaultCloseOperation(EXIT_ON_CLOSE);
            //tester.setEditingBlacklistItem(true);
            tester.setEditingBlacklistItem(false);
            System.out.println(tester.showDialog("Highlight Item", "!start:abc", TEST_INFO));
            System.exit(0);
        });
    }
    
}
