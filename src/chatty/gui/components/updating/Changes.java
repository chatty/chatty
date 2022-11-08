
package chatty.gui.components.updating;

import chatty.util.colors.HtmlColors;
import chatty.gui.laf.LaF;
import chatty.gui.UrlOpener;
import chatty.lang.Language;
import chatty.util.DateTime;
import chatty.util.GitHub.Release;
import chatty.util.GitHub.Releases;
import com.github.rjeschke.txtmark.Processor;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

/**
 *
 * @author tduva
 */
public class Changes extends JDialog {
    
    private static final JLabel label = new JLabel();
    
    private final JTextPane textPane;
    
    public void showDialog(Releases releases, Release latest, Release current) {
        String markdown = makeMarkdown(releases, latest, current);
        try {
            String html = Processor.process(markdown);
            textPane.setText(html);
        } catch (Exception ex) {
            textPane.setText("Error parsing: " + ex);
        }
        textPane.setCaretPosition(0);
        
        if (current == null) {
            setTitle("Changes [Latest: "+latest.getVersion()+"]");
        } else {
            setTitle("Changes [Installed: "+current.getVersion()+" -> Latest: "+latest.getVersion()+"]");
        }
        
        setLocationRelativeTo(getParent());
        setVisible(true);
    }
    
    public Changes(Window parent) {
        super(parent);
        
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        
        textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        this.setFont(label.getFont());
        Font font = label.getFont();
        String bold = font.getStyle() == Font.BOLD ? "bold" : "normal";
        String color = HtmlColors.getColorString(label.getForeground());
        String fontRule = "body { "
                + "font-family: "+font.getFamily()+";"
                + "font-size: "+font.getSize()+";"
                + "font-weight: normal;"
                + "color: "+color+";"
                + "padding: 10px;"
                + "margin-top: 0;"
                + "padding-top: 2px;"
                + "}"
                + "a {"
                + "color: "+LaF.getLinkColor()+";"
                + "}"
                + "h2 {"
                + "border-bottom: 1px solid #AAAAAA;"
                + "margin-bottom: 0px;"
                + "}"
                + "h3 {"
                + "margin-bottom: 4px;"
                + "}";
        ((HTMLDocument)textPane.getDocument()).getStyleSheet().addRule(fontRule);
        textPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Jump to another place in the document
                    String url = e.getURL().toString();
                    String protocol = e.getURL().getProtocol();
                    if (protocol.equals("http") || protocol.equals("https")) {
                        UrlOpener.openUrlPrompt(Changes.this, url, true);
                    }
                }
            }
        });
        add(new JScrollPane(textPane), BorderLayout.CENTER);
        
        JButton closeButton = new JButton(Language.getString("dialog.button.close"));
        closeButton.addActionListener(e -> setVisible(false));
        add(closeButton, BorderLayout.SOUTH);

        setSize(800, 600);
    }
    
    private String makeMarkdown(Releases releases, Release latest, Release current) {
        
        StringBuilder b = new StringBuilder();
        if (current == null) {
            b.append("The version you are running is rather old, so this may not include the full changelog since then.");
        } else {
            b.append("Changelog since version ").append(current.getVersion()).append(".");
        }
        if (latest.isBeta()) {
            b.append(" Includes latest betas.");
        }
        b.append("\n\n");
        
        boolean include = false;
        boolean includeBeta = true;
        for (Release r : releases.getReleases()) {
            if (r == latest) {
                include = true;
            } else if (r == current) {
                include = false;
                b.append("\n\n");
                b.append("## ").append(r.getName());
                b.append(" <small>(").append(DateTime.agoText(r.getPublishedAt())).append(")</small>");
                b.append(" <small>[Installed]</small>");
                b.append("\n\n");
                b.append("<em>Currently installed version.</em>");
            }
            
            if (include && !r.isBeta()) {
                includeBeta = false;
            }
            
            if (include && (!r.isBeta() || includeBeta)) {
                b.append("\n\n");
                b.append("## ").append(r.getName());
                b.append(" <small>(").append(DateTime.agoText(r.getPublishedAt())).append(")</small>");
                if (r == latest) {
                    b.append(" <small>[Latest]</small>");
                }
                b.append("\n\n");
                b.append(r.getDescription());
            }
        }
        return b.toString();
    }
    
}
