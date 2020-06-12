
package chatty.gui.components.settings;

import chatty.Helper;
import chatty.gui.GuiUtil;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.gui.Highlighter.Match;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.StringUtil;
import chatty.util.irc.MsgTags;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
    
    private static final int MAX_INPUT_LENGTH = 50*1000;

    private final static String INFO = "<html><body style='width: 300px;padding:5px;'>"
            + "Test the entered 'Item' pattern with the 'Test' text. Disregards non-text related prefixes.";
    
    private final static String INFO_BLACKLIST = "<br /><br />"
            + "Test a single <code>Blacklist</code> pattern (showing as "
            + "strike-through), that if added to the Highlight Blacklist "
            + "will prevent whatever it matches from triggering Highlights.";
    
    private final static String TEST_PRESET = "Enter test text.";
    private final static String TEST_PRESET_EXAMPLE = TEST_PRESET+" Example: ";
    
    private final JButton okButton = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
    private final JButton addToBlacklistButton = new JButton("Add to Highlight Blacklist");
    private final JTextField itemValue = new JTextField(40);
    private final JTextField blacklistValue = new JTextField(40);
    private final JTextPane testInput = new JTextPane();
    private final DefaultStyledDocument doc = new DefaultStyledDocument();
    private final JLabel testResult = new JLabel("Abc");
    private final LinkLabel infoText = new LinkLabel("", null);
    private final JTextArea parseResult = new JTextArea();
    private final JTabbedPane tabs = new JTabbedPane();
    
    private final MutableAttributeSet matchAttr1 = new SimpleAttributeSet();
    private final MutableAttributeSet matchAttr2 = new SimpleAttributeSet();
    private final MutableAttributeSet defaultAttr = new SimpleAttributeSet();
    private final MutableAttributeSet blacklistAttr = new SimpleAttributeSet();
    
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
    
    public HighlighterTester(Window owner, boolean showBlacklist) {
        super(owner);
        
        setTitle("Test and Edit");
        
        // Styles
        StyleConstants.setBackground(matchAttr1, Color.BLUE);
        StyleConstants.setForeground(matchAttr1, Color.WHITE);
        StyleConstants.setBackground(matchAttr2, new Color(0, 140, 100));
        StyleConstants.setForeground(matchAttr2, Color.WHITE);
        StyleConstants.setStrikeThrough(blacklistAttr, true);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc;
        
        add(new JLabel(INFO+(showBlacklist ? INFO_BLACKLIST : "")),
                GuiUtil.makeGbc(0, 0, 3, 1, GridBagConstraints.CENTER));
        
        add(new JLabel("Item:"),
                GuiUtil.makeGbc(0, 1, 1, 1, GridBagConstraints.EAST));
        
        gbc = GuiUtil.makeGbc(1, 1, 2, 1);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        itemValue.setFont(Font.decode(Font.MONOSPACED));
        GuiUtil.installLengthLimitDocumentFilter(itemValue, MAX_INPUT_LENGTH, false);
        add(itemValue, gbc);
        
        if (showBlacklist) {
            add(new JLabel("Blacklist:"),
                    GuiUtil.makeGbc(0, 2, 1, 1));

            gbc = GuiUtil.makeGbc(1, 2, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            blacklistValue.setFont(Font.decode(Font.MONOSPACED));
            GuiUtil.installLengthLimitDocumentFilter(blacklistValue, MAX_INPUT_LENGTH, false);
            add(blacklistValue, gbc);
            
            gbc = GuiUtil.makeGbc(1, 3, 2, 1);
            gbc.insets = new Insets(0, 5, 4, 5);
            addToBlacklistButton.setMargin(GuiUtil.SMALL_BUTTON_INSETS);
            add(addToBlacklistButton, gbc);
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
        
        add(new JLabel("Test:"),
                GuiUtil.makeGbc(0, 4, 1, 1, GridBagConstraints.FIRST_LINE_END));
        
        gbc = GuiUtil.makeGbc(1, 4, 2, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        testInput.setDocument(doc);
        testInput.setPreferredSize(new Dimension(0, 50));
        testInput.setFont(Font.decode(Font.MONOSPACED));
        // Enable focus traversal keys
        testInput.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        testInput.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        GuiUtil.installLengthLimitDocumentFilter(testInput, 1000, false);
        add(new JScrollPane(testInput),
                gbc);
        
        add(testResult,
                GuiUtil.makeGbc(1, 5, 2, 1, GridBagConstraints.WEST));

        okButton.setMnemonic(KeyEvent.VK_S);
        cancelButton.setMnemonic(KeyEvent.VK_C);
        gbc = GuiUtil.makeGbc(1, 6, 1, 1, GridBagConstraints.EAST);
        gbc.weightx = 1;
        add(okButton, gbc);
        add(cancelButton,
                GuiUtil.makeGbc(2, 6, 1, 1, GridBagConstraints.EAST));
        
        JPanel helpPanel = new JPanel();
        helpPanel.add(infoText);
        
        JPanel parseResultPanel = new JPanel(new BorderLayout());
        JLabel parseResultInfo = new JLabel(HighlightSettings.INFO_HEADER
                + "This shows what requirements for a match Chatty has found "
                + "in what you entered (each line's condition has to be "
                + "satisified for a successful match, although for testing here "
                + "non-text matching prefixes are ignored).");
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
        
        gbc = GuiUtil.makeGbc(0, 7, 3, 1, GridBagConstraints.CENTER);
        gbc.insets = new Insets(10, 5, 5, 5);
        add(tabs, gbc);
        
        okButton.addActionListener(e -> {
            result = editingBlacklistItem ? blacklistValue.getText() : itemValue.getText();
            setVisible(false);
        });
        cancelButton.addActionListener(e -> setVisible(false));
        
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setMinimumSize(getPreferredSize());
        
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
        
        itemValue.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateItem();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateItem();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateItem();
            }
        });
        
        blacklistValue.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateBlacklistItem();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateBlacklistItem();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateBlacklistItem();
            }
        });
        
        updateEditIndicator();
        updateSaveButton();
    }
    
    private void updateItem() {
        String value = itemValue.getText();
        if (value.isEmpty()) {
            highlightItem = null;
        } else {
            highlightItem = new HighlightItem(value, false);
        }
        updateParseResult();
        updateInfoText();
        updateMatches(doc);
        updateSaveButton();
        updateTestText();
//        System.out.println(highlightItem.getMatchInfo());
    }
    
    private void updateBlacklistItem() {
        String value = blacklistValue.getText();
        if (value.isEmpty()) {
            blacklistItem = null;
            addToBlacklistButton.setEnabled(false);
        } else {
            blacklistItem = new HighlightItem(value, false);
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
            tabs.setIconAt(1, GuiUtil.getScaledIcon(UIManager.getIcon("OptionPane.warningIcon"), size, size));
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
            testResult.setText("Invalid regex: "+highlightItem.getError());
        } else if (highlightItem.matchesTest(testInput.getText(), blacklist)) {
            testResult.setText("Matched.");
        } else {
            String failedReason = highlightItem.getFailedReason();
            if (failedReason == null) {
                testResult.setText("No match.");
            }
            else {
                testResult.setText("No match, due to: "+failedReason);
            }
        }
        if (blacklistItem != null && blacklistItem.hasError()) {
            testResult.setText("Invalid blacklist pattern: "+blacklistItem.getError());
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
                } else {
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
            if (item != null) {
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
        this.result = null;
        infoText.setText(info);
        if (editingBlacklistItem) {
            blacklistValue.setText(preset);
        } else {
            itemValue.setText(preset);
        }
        updateItem();
        updateBlacklistItem();
        updateTestText();
        revalidate();
        pack();
        // Caused issues for Notification editor for some reason
        SwingUtilities.invokeLater(() -> {
            setMinimumSize(getPreferredSize());
        });
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(getParent());
        setVisible(true);
        return result;
    }

    @Override
    public void setLinkLabelListener(LinkLabelListener listener) {
        this.infoText.setListener(listener);
    }
    
    public void setAddToBlacklistListener(ActionListener listener) {
        this.addToBlacklistListener  = listener;
        if (listener != null) {
            addToBlacklistButton.setVisible(true);
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
            HighlighterTester tester = new HighlighterTester(null, true);
            tester.setAddToBlacklistListener(e -> {
                System.out.println(e);
            });
            //tester.setDefaultCloseOperation(EXIT_ON_CLOSE);
            //tester.setEditingBlacklistItem(true);
            System.out.println(tester.showDialog("Highlight Item", "regw:[a-z]+", TEST_INFO));
            System.exit(0);
        });
    }
    
}
