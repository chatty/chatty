
package chatty.gui.components;

import chatty.Helper;
import chatty.gui.LinkListener;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.textpane.LinkController;
import chatty.gui.components.textpane.WrapLabelView;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTML;

/**
 * Simple text pane that turns URLs and SRL channels into clickable links.
 * 
 * @author tduva
 */
public class ExtendedTextPane extends JTextPane {
    
    private static final Logger LOGGER = Logger.getLogger(ExtendedTextPane.class.getName());
    
    private final StyledDocument doc;
    
    /**
     * The matcher for finding URLs.
     */
    private static final Matcher urlMatcher = Helper.getUrlPattern().matcher("");
    
    /**
     * The regex and matcher for finding SRL channels (e.g. #srl-abc).
     */
    private static final String srlRegex = "#srl-([a-z0-9]{2,6})";
    private static final Matcher srlMatcher
            = Pattern.compile(srlRegex).matcher("");
    
    /**
     * To build an SRL race URL.
     */
    private static final String SRL_URL = "http://speedrunslive.com/race/?id=";
    
    private final LinkController linkController;
    
    public ExtendedTextPane() {
        setEditorKit(new MyEditorKit());
        linkController = new LinkController();
        this.addMouseListener(linkController);
        this.addMouseMotionListener(linkController);
        doc = this.getStyledDocument();
    }
    
    public void setLinkListener(LinkListener listener) {
        linkController.setLinkListener(listener);
    }
    
    public void setContextMenuListener(ContextMenuListener listener) {
        linkController.setContextMenuListener(listener);
    }
    
    /**
     * Set this text pane to a new text. Removes any previous content and adds
     * the new text while adding clickablel links.
     * 
     * @param text 
     */
    @Override
    public void setText(String text) {
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ex) {
            LOGGER.warning("Bad location");
        }
        printSpecials(text);
    }
    
    /**
     * Print special stuff in the text like links and emoticons differently.
     * 
     * First a map of all special stuff that can be found in the text is built,
     * in a way that stuff doesn't overlap with previously found stuff.
     * 
     * Then all the special stuff in this map is printed accordingly, while
     * printing the stuff inbetween with regular style.
     * 
     * @param text 
     */
    protected void printSpecials(String text) {
        // Where stuff was found
        TreeMap<Integer,Integer> ranges = new TreeMap<>();
        // The style of the stuff (basicially metadata)
        HashMap<Integer,MutableAttributeSet> rangesStyle = new HashMap<>();
        
        findLinks(text, ranges, rangesStyle);
        findSrl(text, ranges, rangesStyle);

        // Actually print everything
        int lastPrintedPos = 0;
        Iterator<Map.Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Map.Entry<Integer, Integer> range = rangesIt.next();
            int start = range.getKey();
            int end = range.getValue();
            if (start > lastPrintedPos) {
                // If there is anything between the special stuff, print that
                // first as regular text
                print(text.substring(lastPrintedPos, start), null);
            }
            print(text.substring(start, end + 1),rangesStyle.get(start));
            lastPrintedPos = end + 1;
        }
        // If anything is left, print that as well as regular text
        if (lastPrintedPos < text.length()) {
            print(text.substring(lastPrintedPos), null);
        }
        
    }
    
    /**
     * Finds all URLs and saves them to be printed as clickable links.
     * 
     * @param text The text to find the URLs in.
     * @param ranges The ranges in the text that are already taken by other
     * links.
     * @param rangesStyle 
     */
    private void findLinks(String text, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        // Find links
        urlMatcher.reset(text);
        while (urlMatcher.find()) {
            int start = urlMatcher.start();
            int end = urlMatcher.end() - 1;
            if (!inRanges(start, ranges) && !inRanges(end,ranges)) {
                String foundUrl = urlMatcher.group();
                
                if (foundUrl.contains("..")) {
                    continue;
                }
                
                // Check if URL contains ( ) like http://example.com/test(abc)
                // or is just contained in ( ) like (http://example.com)
                // (of course this won't work perfectly, but it should be ok)
                if (foundUrl.endsWith(")") && !foundUrl.contains("(")) {
                    foundUrl = foundUrl.substring(0, foundUrl.length() - 1);
                    end--;
                }
                
                if (checkUrl(foundUrl)) {
                    ranges.put(start, end);
                    if (!foundUrl.startsWith("http")) {
                        foundUrl = "http://"+foundUrl;
                    }
                    rangesStyle.put(start, url(foundUrl));
                }
            }
        }
    }
    
    /**
     * Finds all SRL channels and saves them to be printed as clickable links.
     * 
     * @param text The text to find the SRL channels in.
     * @param ranges The ranges in the text that are already take by other
     * links.
     * @param rangesStyle The style associated with a range (metadata).
     */
    private void findSrl(String text, Map<Integer, Integer> ranges,
            Map<Integer, MutableAttributeSet> rangesStyle) {
        srlMatcher.reset(text);
        while (srlMatcher.find()) {
            int start = srlMatcher.start();
            int end = srlMatcher.end() - 1;
            if (!inRanges(start, ranges) && !inRanges(end, ranges)) {
                String foundSrl = srlMatcher.group();
                String url = SRL_URL+foundSrl;
                ranges.put(start, end);
                rangesStyle.put(start, url(url));
            }
        }
    }
    
    /**
     * Print the given text with the given style. Used to be able to output
     * links.
     * 
     * @param text
     * @param printStyle 
     */
    private void print(String text, AttributeSet printStyle) {
        try {
            doc.insertString(doc.getLength(), text, printStyle);
        } catch (BadLocationException e) {
            LOGGER.warning("Bad location");
        }
    }
    
    /**
     * Checks if the given integer is within the range of any of the key=value
     * pairs of the Map (inclusive).
     * 
     * @param i
     * @param ranges
     * @return 
     */
    private boolean inRanges(int i, Map<Integer,Integer> ranges) {
        Iterator<Map.Entry<Integer, Integer>> rangesIt = ranges.entrySet().iterator();
        while (rangesIt.hasNext()) {
            Map.Entry<Integer, Integer> range = rangesIt.next();
            if (i >= range.getKey() && i <= range.getValue()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the Url can be later used as a URI.
     * 
     * @param uriToCheck
     * @return 
     */
    private boolean checkUrl(String uriToCheck) {
        try {
            new URI(uriToCheck);
        } catch (URISyntaxException ex) {
            return false;
        }
        return true;
    }
    
    /**
     * Make a link style for the given URL.
     *
     * @param url
     * @return
     */
    public MutableAttributeSet url(String url) {
        SimpleAttributeSet urlStyle = new SimpleAttributeSet();
        StyleConstants.setUnderline(urlStyle, true);
        urlStyle.addAttribute(HTML.Attribute.HREF, url);
        return urlStyle;
    }

    /**
     * Replaces one view to wrap long words.
     */
    private static class MyEditorKit extends StyledEditorKit {

        @Override
        public ViewFactory getViewFactory() {
            return new StyledViewFactory();
        }

        static class StyledViewFactory implements ViewFactory {

            @Override
            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null) {
                    if (kind.equals(AbstractDocument.ContentElementName)) {
                        return new WrapLabelView(elem);
                    } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                        return new ParagraphView(elem);
                    } else if (kind.equals(AbstractDocument.SectionElementName)) {
                        return new BoxView(elem, View.Y_AXIS);
                    } else if (kind.equals(StyleConstants.ComponentElementName)) {
                        return new ComponentView(elem);
                    } else if (kind.equals(StyleConstants.IconElementName)) {
                        return new IconView(elem);
                    }
                }
                return new LabelView(elem);
            }
        }
    }
}