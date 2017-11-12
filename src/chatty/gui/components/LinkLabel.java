
package chatty.gui.components;

import chatty.gui.HtmlColors;
import chatty.gui.LaF;
import java.awt.Color;
import java.awt.Font;
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
    
    private static final JLabel label = new JLabel();
    private LinkLabelListener listener;
    
    public LinkLabel(String text, LinkLabelListener listener) {
        this.listener = listener;
        setEditable(false);
        setOpaque(false);
        setContentType("text/html");
        setText(text);
        
        // Set the font based on a JLabel
        this.setFont(label.getFont());
        Font font = label.getFont();
        String bold = font.getStyle() == Font.BOLD ? "bold" : "normal";
        String color = HtmlColors.getColorString(label.getForeground());
        String fontRule = "body { "
                + "font-family: "+font.getFamily()+";"
                + "font-size: "+font.getSize()+";"
                + "font-weight: "+bold+";"
                + "color: "+color+";"
                + "}"
                + "a {"
                + "color: "+LaF.getLinkColor()+";"
                + "}";
        ((HTMLDocument)getDocument()).getStyleSheet().addRule(fontRule);
        
        // Link Listener
        this.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    linkClicked(e.getDescription());
                }
            }
        });
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
