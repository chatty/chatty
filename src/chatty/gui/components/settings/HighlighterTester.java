
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.Highlighter;
import chatty.gui.Highlighter.HighlightItem;
import chatty.gui.Highlighter.Match;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
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

    private final static String INFO = "<html><body style='width: 300px;padding:5px;'>"
            + "Test the entered 'Item' pattern with the 'Test' text. Disregards non-text related prefixes.";
    
    private final static String INFO_BLACKLIST = "<br /><br />"
            + "Anything matched by the <code>Blacklist</code> pattern "
            + "(indicated by strike-through) cannot cause a match. This can "
            + "be used to test how an entry on the Highlight Blacklist would "
            + "affect Highlighting.";
    
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
    
    private final MutableAttributeSet matchAttr1 = new SimpleAttributeSet();
    private final MutableAttributeSet matchAttr2 = new SimpleAttributeSet();
    private final MutableAttributeSet defaultAttr = new SimpleAttributeSet();
    private final MutableAttributeSet blacklistAttr = new SimpleAttributeSet();
    
    private boolean blacklistPreset;
    private String result;
    private ActionListener addToBlacklistListener;
    
    private HighlightItem highlightItem;
    private HighlightItem blacklistItem;
    
    public HighlighterTester(Window owner, boolean showBlacklist) {
        super(owner);
        
        setTitle("Test and Edit");
        
        // Styles
        StyleConstants.setBackground(matchAttr1, Color.BLUE);
        StyleConstants.setForeground(matchAttr1, Color.WHITE);
        StyleConstants.setBackground(matchAttr2, Color.MAGENTA);
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
        add(itemValue, gbc);
        
        if (showBlacklist) {
            add(new JLabel("Blacklist:"),
                    GuiUtil.makeGbc(0, 2, 1, 1));

            gbc = GuiUtil.makeGbc(1, 2, 2, 1);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            blacklistValue.setFont(Font.decode(Font.MONOSPACED));
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
        testInput.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERS‌​AL_KEYS, null);
        testInput.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERS‌​AL_KEYS, null);
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
        
        gbc = GuiUtil.makeGbc(0, 7, 3, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(10, 5, 5, 5);
        add(infoText, gbc);
        
        okButton.addActionListener(e -> {
            result = blacklistPreset ? blacklistValue.getText() : itemValue.getText();
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
    }
    
    private void updateItem() {
        String value = itemValue.getText();
        if (value.isEmpty()) {
            highlightItem = null;
        } else {
            highlightItem = new HighlightItem(value);
        }
        updateInfoText();
        updateMatches(doc);
    }
    
    private void updateBlacklistItem() {
        String value = blacklistValue.getText();
        blacklistItem = null;
        if (!value.isEmpty()) {
            HighlightItem newItem = new HighlightItem(value);
            if (!newItem.hasError()) {
                blacklistItem = newItem;
            }
            addToBlacklistButton.setEnabled(!newItem.hasError());
        } else {
            addToBlacklistButton.setEnabled(false);
        }
        updateInfoText();
        updateMatches(doc);
    }
    
    private void updateInfoText() {
        Highlighter.Blacklist blacklist = null;
        if (blacklistItem != null) {
            blacklist = new Highlighter.Blacklist(null, testInput.getText(), Arrays.asList(new HighlightItem[]{blacklistItem}), true);
        }
        if (highlightItem == null) {
            testResult.setText("No pattern.");
        } else if (highlightItem.hasError()) {
            testResult.setText("Invalid pattern.");
        } else if (highlightItem.matches(null, testInput.getText(), null, true, blacklist)) {
            testResult.setText("Matched.");
        } else {
            testResult.setText("No match.");
        }
    }

    private void updateMatches(StyledDocument doc) {
        try {
            // Reset to default styles
            doc.setCharacterAttributes(0, doc.getLength(), defaultAttr, true);
            
            // Regular item
            if (highlightItem != null) {
                List<Match> matches = highlightItem.getTextMatches(testInput.getText());
                for (int i = 0; i < matches.size(); i++) {
                    Match m = matches.get(i);
                    if (i % 2 == 0) {
                        doc.setCharacterAttributes(m.start, m.end - m.start, matchAttr1, false);
                    } else {
                        doc.setCharacterAttributes(m.start, m.end - m.start, matchAttr2, false);
                    }
                }
            }
            
            // Blacklist item
            if (blacklistItem != null) {
                List<Match> blacklistMatches = blacklistItem.getTextMatches(testInput.getText());
                for (int i = 0; i < blacklistMatches.size(); i++) {
                    Match m = blacklistMatches.get(i);
                    doc.setCharacterAttributes(m.start, m.end - m.start, blacklistAttr, false);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private static final Border EDITING_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(1, 2, 1, 2));
    private static final Border DEFAULT_BORDER = new JTextField().getBorder();
    
    public void setBlacklistPreset(boolean blacklistPreset) {
        this.blacklistPreset = blacklistPreset;
        updateEditIndicator();
    }
    
    private void updateEditIndicator() {
        if (blacklistPreset) {
            blacklistValue.setBorder(EDITING_BORDER);
            itemValue.setBorder(DEFAULT_BORDER);
        } else {
            blacklistValue.setBorder(DEFAULT_BORDER);
            itemValue.setBorder(EDITING_BORDER);
        }
    }
    
    private void updateTestText() {
        if (testInput.getText().isEmpty() || testInput.getText().startsWith(TEST_PRESET)) {
            if (highlightItem != null) {
                testInput.setText(TEST_PRESET_EXAMPLE+highlightItem.getTextWithoutPrefix());
            } else {
                testInput.setText(TEST_PRESET);
            }
        }
    }

    @Override
    public String showDialog(String title, String preset, String info) {
        if (blacklistPreset) {
            blacklistValue.requestFocusInWindow();
        } else {
            itemValue.requestFocusInWindow();
        }
        setTitle(title);
        this.result = null;
        infoText.setText(info);
        infoText.setVisible(info != null);
        if (blacklistPreset) {
            blacklistValue.setText(preset);
        } else {
            itemValue.setText(preset);
        }
        updateItem();
        updateBlacklistItem();
        updateTestText();
        revalidate();
        pack();
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
    
    public static final String INFO2 = "<html><body style='width:300px;font-weight:normal;'>"
            + "Quick reference (see [help-settings:Highlight help] for more):"
            + "<ul style='margin-left:30px'>"
            + "<li><code>Bets open</code> - 'Bets open' anywhere in the message"
            + "<li><code>cs:Bets open</code> - Same, but case-sensitive</li>"
            + "<li><code>w:Clip</code> - 'Clip' as a word, so e.g. not 'Clipped'</li>"
            + "<li><code>wcs:Clip</code> - Same, but case-sensitive</li>"
            + "<li><code>start:!slot</code> - Message beginning with '!slot'</li>"
            + "<li><code>re*:(?i)^!\\w+$</code> - Regular expression, anywhere</li>"
            + "</ul>"
            + "Meta prefixes (in front of text matching):"
            + "<ul>"
            + "<li><code>chan:joshimuz</code> - Restrict to channel 'joshimuz'</li>"
            + "<li><code>user:Elorie</code> - Restrict to user 'Elorie'</li>"
            + "<li><code>cat:vip</code> - Restrict to users in category 'vip'</li>"
            + "<li><code>config:info</code> - Match info messages</li>"
            + "</ul>"
            + "Example: <code>user:botimuz cs:Bets open</code>";
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HighlighterTester tester = new HighlighterTester(null, true);
            tester.setAddToBlacklistListener(e -> {
                System.out.println(e);
            });
            //tester.setDefaultCloseOperation(EXIT_ON_CLOSE);
            //tester.setBlacklistPreset(true);
            System.out.println(tester.showDialog("Highlight Item", "user:botimuz cs:Bets open", INFO2));
            System.exit(0);
        });
    }
    
}
