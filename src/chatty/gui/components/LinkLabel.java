
package chatty.gui.components;

import chatty.util.colors.HtmlColors;
import chatty.gui.laf.LaF;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

/**
 * A label that is implemented using a JEditorPane to allow for clickable links.
 * 
 * @author tduva
 */
public class LinkLabel extends JEditorPane {
    
    private LinkLabelListener listener;
    private Color foreground;
    
    public LinkLabel(String text, LinkLabelListener listener) {
        this.listener = listener;
        setEditable(false);
        setOpaque(false);
        setContentType("text/html");
        setText(text);
        setCaretPosition(0);
        /**
         * This property seems to be related to how font sizes get converted in
         * HTML. When it is enabled some fonts are too small, for example
         * affecting text in code tags. Setting the font size for code tags to
         * 1em seems to work around this issue though.
         *
         * It seems like in some Look&Feel (like FlatLaf) this property is
         * enabled by default while in others it is not. Not sure if this might
         * also look different on different systems or default fonts.
         * 
         * This issue seems to occur in other components when using HTML as well
         * (at least with JLabel).
         */
//        putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.FALSE);
        
        // Link Listener
        this.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    linkClicked(e.getDescription());
                }
            }
        });
        setStyle();
        
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Make sure cursor is at the top, so it doesn't start reading
                // at the bottom (setting to 1 for some reason activates the
                // cursor immediately)
                setCaretPosition(1);
            }
        });
        getAccessibleContext().setAccessibleName("Help Text");
        getAccessibleContext().setAccessibleDescription("");
    }
    
    @Override
    public void setForeground(Color color) {
        this.foreground = color;
        setStyle();
    }
    
    private void setStyle() {
        if (getDocument() == null || !(getDocument() instanceof HTMLDocument)) {
            return;
        }
        JLabel label = new JLabel();
        // Set the font based on a JLabel
        this.setFont(label.getFont());
        Font font = label.getFont();
        String bold = font.getStyle() == Font.BOLD ? "bold" : "normal";
        String color = HtmlColors.getColorString(label.getForeground());
        String linkColor = LaF.getLinkColor();
        String codeColors = "code { background: white; color: black; }";
        if (LaF.isDarkTheme()) {
            codeColors = "code { background: #444444; color: white; }";
        }
        if (foreground != null) {
            color = HtmlColors.getColorString(foreground);
            linkColor = HtmlColors.getColorString(foreground);
        }
        String fontRule = "body { "
                + "font-family: "+font.getFamily()+";"
                + "font-size: "+font.getSize()+";"
                + "font-weight: "+bold+";"
                + "color: "+color+";"
                + "}"
                + "a {"
                + "color: "+linkColor+";"
                + "}"
                + "code { font-size: 1em; }"
                + codeColors;
        ((HTMLDocument)getDocument()).getStyleSheet().addRule(fontRule);
    }
    
    @Override
    public void updateUI() {
        super.updateUI();
        setStyle();
    }
    
    /**
     * Transform [link Link Text] to <a href="link">Link text</a> when setting
     * the text.
     * 
     * @param text 
     */
    @Override
    public final void setText(String text) {
        if (text != null) {
            text = text.replaceAll("\\[([^] ]+) ([^]]+)\\]","<a href=\"$1\">$2</a>");
        }
        super.setText(text);
    }
    
    public void addRule(String css) {
        ((HTMLDocument)getDocument()).getStyleSheet().addRule(css);
    }
    
    public void setListener(LinkLabelListener listener) {
        this.listener = listener;
    }
    
    /**
     * When a link was clicked, split it to get the link type and actual link.
     * A link should be like: type:link (e.g. help:login)
     * 
     * @param link 
     */
    private void linkClicked(String link) {
        String[] parts = link.split(":", 2);
        if (parts.length == 2 && listener != null) {
            listener.linkClicked(parts[0], parts[1]);
        }
    }
    
}
