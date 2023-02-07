
package chatty.gui.components.settings;

import chatty.gui.GuiUtil;
import chatty.gui.components.LinkLabel;
import chatty.gui.components.LinkLabelListener;
import chatty.lang.Language;
import chatty.util.LineNumbers;
import chatty.util.SyntaxHighlighter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Simple text editor dialog that can be opened with a description of the edit,
 * a preset value and an optional help text. Auto adjusts the size of the input
 * box.
 * 
 * It should probably be possible to use one instance of this for several
 * purposes, because the most important stuff is set everytime the dialog is
 * opened, however some stuff (like whether the help is currently shown) remains
 * the same.
 * 
 * @author tduva
 */
public class Editor implements StringEditor {
    
    private static final int INPUT_LENGTH_LIMIT = 100*1000;

    private final JDialog dialog;
    private final JLabel label;
    private final JTextArea input;
    private final JScrollPane scrollpane;
    private final JButton okButton = new JButton(Language.getString("dialog.button.save"));
    private final JButton cancelButton = new JButton(Language.getString("dialog.button.cancel"));
    private final JButton testButton = new JButton(Language.getString("dialog.button.test"));
    private final JToggleButton toggleInfoButton = new JToggleButton(Language.getString("dialog.button.help"));
    private final JCheckBox toggleHighlighting = new JCheckBox("Syntax Highlighting");
    private final Window parent;
    private final LinkLabel info;
    
    private DataFormatter<String> formatter;
    private Tester tester;
    private SyntaxHighlighter highlighter;
    private Runnable highlighterUpdate;
    private boolean allowEmpty;
    private boolean showInfoByDefault;

    private String result;

    public Editor(Window parent) {
        dialog = new JDialog(parent);
        this.parent = parent;

        dialog.setTitle("Input");
        dialog.setModal(true);
        GuiUtil.installEscapeCloseOperation(dialog);

        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc;

        // Should contain something to set correct minimum size in constructor
        label = new JLabel("abc");
        gbc = GuiUtil.makeGbc(0, 0, 2, 1, GridBagConstraints.WEST);
        gbc.insets = new Insets(5, 5, 5, 5);
        dialog.add(label, gbc);
        
        gbc = GuiUtil.makeGbc(3, 0, 1, 1, GridBagConstraints.EAST);
        GuiUtil.smallButtonInsets(testButton);
        dialog.add(testButton, gbc);
        testButton.setVisible(false);

        gbc = GuiUtil.makeGbc(0, 1, 4, 1);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 7, 5, 7);
        input = new JTextArea();
        input.getDocument().addDocumentListener(new ChangeListener());
        input.setMargin(new Insets(2, 2, 2, 2));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setText("test");
        // Use monospaced font for easier editing of some kinds of text
        input.setFont(Font.decode(Font.MONOSPACED));
        GuiUtil.installLengthLimitDocumentFilter(input, INPUT_LENGTH_LIMIT, false);
        GuiUtil.resetFocusTraversalKeys(input);
        scrollpane = new JScrollPane(input);
        dialog.add(scrollpane, gbc);
        
        gbc = GuiUtil.makeGbc(0, 4, 4, 1);
        gbc.insets = new Insets(5, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        info = new LinkLabel("", null);
        dialog.add(info, gbc);
        /**
         * Set help invisible by default (do it in constructor to get correct
         * preferred size when setting minimum size)
         */
        info.setVisible(false);

        gbc = GuiUtil.makeGbc(0, 3, 1, 1);
        dialog.add(toggleInfoButton, gbc);
        
        gbc = GuiUtil.makeGbc(1, 3, 1, 1);
        dialog.add(toggleHighlighting, gbc);
        
        gbc = GuiUtil.makeGbc(2, 3, 1, 1);
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1;
        dialog.add(okButton, gbc);

        gbc = GuiUtil.makeGbc(3, 3, 1, 1);
        dialog.add(cancelButton, gbc);

        ActionListener buttonAction = new ButtonAction();
        okButton.addActionListener(buttonAction);
        cancelButton.addActionListener(buttonAction);
        toggleInfoButton.addActionListener(buttonAction);
        testButton.addActionListener(buttonAction);
        
        toggleHighlighting.setVisible(false);
        toggleHighlighting.setSelected(true);
        toggleHighlighting.addItemListener(e -> {
            if (toggleHighlighting.isSelected()) {
                highlighter.setEnabled(true);
                highlighterUpdate.run();
            }
            else {
                input.getHighlighter().removeAllHighlights();
                highlighter.setEnabled(false);
            }
        });

        okButton.setMnemonic(KeyEvent.VK_S);
        cancelButton.setMnemonic(KeyEvent.VK_C);

        //okButton.setToolTipText("Press Enter in inputbox to save");
        cancelButton.setToolTipText("Press ESC to cancel");

        /**
         * Set a minimum width, so it has some width even for values that may
         * be shorter. This should be based on one line height.
         */
        dialog.pack();
        Dimension preferred = dialog.getPreferredSize();
        dialog.setMinimumSize(new Dimension(400, preferred.height));
    }
    
    /**
     * Shows the editor dialog, with {@code title} as description of the action,
     * {@code preset} as preset value and {@code info} as help, which can be
     * toggled by clicking on the Help-button. If not help text is specified,
     * then there will be no Help-button.
     * 
     * @param title The description of what this edit action does
     * @param preset The preset value to fill the inputbox with
     * @param info The help text (use {@code null} to don't use help)
     * @return The edited value or {@code null} if the dialog was closed without
     * saving
     */
    public String showDialog(String title, String preset, String info) {
        input.setText(preset);
        label.setText(title);
        this.info.setText(info);
        if (info == null) {
            this.info.setVisible(false);
        }
        else {
            this.info.setVisible(showInfoByDefault);
        }
        toggleInfoButton.setVisible(info != null);
        toggleInfoButton.setSelected(this.info.isVisible());
        result = null;

        input.requestFocusInWindow();

        // Set initial size, now also based on the preset value
        dialog.pack();
        Dimension p = dialog.getPreferredSize();
        Rectangle screen = parent.getGraphicsConfiguration().getBounds();
        if (p.height > screen.height / 2) {
            dialog.setSize(p.width, screen.height / 2);
        }
        
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        // This will block until closed, during that time stuff can be changed
        
        return result;
    }
    
    /**
     * Sets an optional formatter, which can format the input before determining
     * if there is something to save (which enables/disables the Save-button if
     * {@link setAllowEmpty(boolean)} is set to false).
     * 
     * @param formatter 
     */
    public void setFormatter(DataFormatter<String> formatter) {
        this.formatter = formatter;
    }
    
    public void setTester(Tester tester) {
        this.tester = tester;
        testButton.setVisible(tester != null);
    }
    
    public void setSyntaxHighlighter(SyntaxHighlighter highlighter) {
        if (this.highlighter == null && highlighter != null) {
            this.highlighter = highlighter;
            this.highlighterUpdate = SyntaxHighlighter.install(input, highlighter);
            toggleHighlighting.setVisible(true);
        }
    }
    
    public void setLinkLabelListener(LinkLabelListener listener) {
        info.setListener(listener);
    }
    
    /**
     * Set whether to allow an empty value to be saved. Default is false.
     * 
     * @param allow true if empty values should be able to be saved
     */
    public void setAllowEmpty(boolean allow) {
        allowEmpty = allow;
    }
    
    /**
     * Set whether to allow linebrekas in the value. Otherwise linebreaks are
     * filtered out when editing and replaced by a space. Default is false.
     * 
     * @param allow true to allow linebreaks
     */
    public final void setAllowLinebreaks(boolean allow) {
        GuiUtil.installLengthLimitDocumentFilter(input, INPUT_LENGTH_LIMIT, allow);
        scrollpane.setRowHeaderView(allow ? new LineNumbers(input) : null);
    }
    
    public final void setShowInfoByDefault(boolean show) {
        this.showInfoByDefault = show;
    }
    
    private String format(String input) {
        if (formatter != null && input != null) {
            return formatter.format(input);
        }
        return input;
    }

    /**
     * Checks if the current input isn't empty (after formatting) and enables or
     * diables the "add" button accordingly.
     */
    private void updateOkButton() {
        okButton.setEnabled(isSomethingToSave());
    }

    /**
     * Check if there is currently something to save, based on the allowEmpty
     * property and the current input (possibly after formatting).
     * 
     * @return true if there is something to save (as defined by the set rules),
     * false otherwise
     */
    private boolean isSomethingToSave() {
        if (allowEmpty) {
            return true;
        }
        String currentInput = format(input.getText());
        return currentInput != null && !currentInput.isEmpty();
    }

    /**
     * React on button presses.
     */
    private class ButtonAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == okButton) {
                result = input.getText();
                dialog.setVisible(false);
            } else if (e.getSource() == cancelButton) {
                dialog.setVisible(false);
            } else if (e.getSource() == toggleInfoButton) {
                /**
                 * Show and hide the help. Showing the help means the minimum
                 * size set in the constructor won't really fit anymore, but
                 * that's not too bad.
                 */
                info.setVisible(toggleInfoButton.isSelected());
                dialog.pack();
            } else if (e.getSource() == testButton) {
                if (tester != null) {
                    String changed = tester.test(dialog, testButton, 0, testButton.getHeight(), input.getText());
                    if (changed != null) {
                        input.setText(changed);
                    }
                }
            }
            
        }

    }
    
    /**
     * Checks if the first Dimension is bigger in width and/or height than the
     * second one.
     * 
     * @param d1 The first Dimension
     * @param d2 The second Dimension
     * @return true if the first Dimension is bigger, false otherwise
     */
    private static boolean bigger(Dimension d1, Dimension d2) {
        if (d1.height > d2.height || d1.width > d2.width) {
            return true;
        }
        return false;
    }
    
    /**
     * Adjusts the size of the dialog to the preferred size, but only if that
     * is bigger than the current size.
     */
    private void adjustSize() {
        if (dialog.isVisible()) {
            Dimension p = dialog.getPreferredSize();
            Point bottomRight = dialog.getLocationOnScreen();
            bottomRight.translate(p.width, p.height);
            if (bigger(p, dialog.getSize())
                    && GuiUtil.isPointOnScreen(bottomRight)) {
                dialog.pack();
            }
        }
    }

    /**
     * Adjust size of the dialog when editing the input (so the input box
     * automatically resizes) and update the Save-button state.
     */
    private class ChangeListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateOkButton();
            
            // Wrap in invokeLater() so the size is adjusted correctly after
            // entering text (otherwise it would always be one edit behind)
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    adjustSize();
                }
            });
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updateOkButton();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateOkButton();
        }
    }
    
    /**
     * Test a String value. This could for example show a popup containing
     * information about the given value (whether it parses correctly, it's
     * resulting properties) or another GUI element that represents the given
     * value in some way.
     */
    public interface Tester {
        
        /**
         * 
         * @param parent The parent window
         * @param component The component that triggered the tester
         * @param x 
         * @param y 
         * @param value The value to test
         * @return Changed value or null to keep it the same
         */
        public String test(Window parent, Component component, int x, int y, String value);
    }

}
