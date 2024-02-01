
package chatty.gui;

import chatty.gui.components.SimplePopup;
import chatty.lang.Language;
import chatty.util.StringUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A document filter that removes text based on a regular expression.
 * 
 * @author tduva
 */
public class RegexDocumentFilter extends DocumentFilter {
    
    private final Pattern pattern;
    private final SimplePopup popup;
    private String latestFiltered = "";
    
    /**
     * Create a new filter. Anything that matches the regex will not be allowed.
     * 
     * @param regex The regex to use, required
     * @param popupOwner The owner of the popup that is shown for invalid input,
     * may be null (in which case no popup is shown)
     */
    public RegexDocumentFilter(String regex, Component popupOwner) {
        pattern = Pattern.compile(regex);
        if (popupOwner != null) {
            popup = new SimplePopup(popupOwner, () -> {
                latestFiltered = "";
            });
        }
        else {
            popup = null;
        }
    }
    
    public String getRegex() {
        return pattern.pattern();
    }
    
    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int off, String str, AttributeSet attr) {
        try {
            fb.insertString(off, getFiltered(str), attr);
        } catch (BadLocationException | NullPointerException ex) {
            
        }
    }
    
    @Override
    public void replace(DocumentFilter.FilterBypass fb, int off, int len, String str, AttributeSet attr) {
        try {
            if (str == null) {
                fb.replace(off, len, str, attr);
            } else {
                fb.replace(off, len, getFiltered(str), attr);
            }
        } catch (BadLocationException | NullPointerException ex) {
            
        }
    }
    
    private String getFiltered(String input) {
        Matcher m = pattern.matcher(input);
        boolean result = m.find();
        if (result) {
            StringBuffer sb = new StringBuffer();
            StringBuilder filtered = new StringBuilder();
            do {
                m.appendReplacement(sb, "");
                filtered.append(m.group());
                result = m.find();
            } while (result);
            m.appendTail(sb);
            showPopup(filtered.toString());
            return sb.toString();
        }
        return input;
    }
    
    private void showPopup(String filtered) {
        if (popup == null) {
            return;
        }
        
        latestFiltered += filtered;
        
        popup.showPopup(String.format("%s '%s'",
                                      Language.getString("dialog.error.invalidInput"),
                                      StringUtil.shortenTo(latestFiltered, 100)));
    }
    
    // For testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            JTextField text = new JTextField();
            ((AbstractDocument) text.getDocument()).setDocumentFilter(new RegexDocumentFilter("[^0-9.]", text));
            frame.add(text, BorderLayout.SOUTH);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }

}