
package chatty.gui.components.help;

import chatty.Chatty;
import chatty.gui.UrlOpener;
import chatty.util.UrlRequest;
import chatty.util.WrapHistory;
import chatty.util.api.CachedManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTMLDocument;

/**
 * Simple Frame that shows a HTML page as About/Help.
 * 
 * @author tduva
 */
public class About extends JFrame implements ActionListener {
    
    private static final Logger LOGGER = Logger.getLogger(About.class.getName());
    
    private static final String BASE_HELP_URL = Chatty.WEBSITE+"/help/";
    private static final String DEFAULT_PAGE = "help.html";
    
    private final JButton historyBackButton;
    private final JButton historyForwardButton;
    private final JTextField currentLocation;
    private final JTextPane textPane = new JTextPane();
    private final JScrollBar scrollbar;
    private String referenceAfterLoad;
    private int scrollPositionAfterLoad;
    private String currentPage = "";
    private final WrapHistory<HistoryItem> history = new WrapHistory<>(20);
    
    private String patreonHtml;
    
    public About() {
        setTitle("About/Help - Chatty");
        
        // Text pane
        JScrollPane scroll = new JScrollPane(textPane);
        scroll.getVerticalScrollBar().setUnitIncrement(40);
        scrollbar = scroll.getVerticalScrollBar();
        textPane.setEditable(false);
        // Explicitly set to white, as long as the HTML pages themselves are
        textPane.setBackground(Color.WHITE);
        // Prevent scrolling when changing HTML
        ((DefaultCaret)textPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        textPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Jump to another place in the document
                    String url = e.getURL().toString();
                    String protocol = e.getURL().getProtocol();
                    if (protocol.equals("http") || protocol.equals("https")
                            || protocol.equals("mailto")) {
                        UrlOpener.openUrlPrompt(About.this, url, true);
                    } else if (protocol.equals("file") || protocol.equals("jar")) {
                        String path = e.getURL().getFile();
                        String file = path.substring(path.lastIndexOf("/")+1);
                        open(file, e.getURL().getRef());
                    } else {
                        jumpTo(e.getURL().getRef());
                    }
                }
            }
        });
        
        // Listener to do stuff when the page loaded
        textPane.addPropertyChangeListener("page", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                pageLoaded();
            }
        });

        // ActionListener for all the navigation buttons
        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                HistoryItem cameFrom = history.current();
                HistoryItem item = null;
                if (e.getActionCommand().equals("historyBack")) {
                    item = history.backward();
                } else if (e.getActionCommand().equals("historyForward")) {
                    item = history.forward();
                } else if (e.getActionCommand().equals("home")) {
                    open(null, null);
                } else if (e.getActionCommand().equals("up")) {
                    open(currentPage, "");
                } else if (e.getActionCommand().equals("web")) {
                    String url = BASE_HELP_URL+currentLocation.getText();
                    UrlOpener.openUrlPrompt(About.this, url, true);
                }
                
                // Change to valid history item
                if (item != null) {
                    open(item.page, item.ref, item.scrollPosition, cameFrom);
                }
            }
        };
        
        // Toolbar with navigation buttons and location info (text field)
        JToolBar toolbar = new JToolBar();
        historyBackButton = makeButton("go-previous.png", "historyBack", "Go back");
        historyBackButton.addActionListener(buttonAction);
        historyForwardButton = makeButton("go-next.png", "historyForward", "Go forward");
        historyForwardButton.addActionListener(buttonAction);
        JButton homeButton = makeButton("go-home.png", "home", "Main Help Page");
        homeButton.addActionListener(buttonAction);
        JButton upButton = makeButton("go-up.png", "up", "Scroll To Top");
        upButton.addActionListener(buttonAction);
        JButton webButton = makeButton("go-web.png", "web", "Open in online help");
        webButton.addActionListener(buttonAction);
        
        currentLocation = new JTextField(20);
        currentLocation.setEditable(false);
        JPanel currentLocationPanel = new JPanel(new BorderLayout());
        currentLocationPanel.setBorder(BorderFactory.createEmptyBorder(4, 5, 5, 5));
        currentLocationPanel.setOpaque(false);
        currentLocationPanel.add(currentLocation);
        toolbar.setFloatable(false);
        toolbar.add(historyBackButton);
        toolbar.add(historyForwardButton);
        toolbar.add(homeButton);
        toolbar.add(upButton);
        toolbar.addSeparator();
        toolbar.add(currentLocationPanel);
        toolbar.add(webButton);
        toolbar.setMargin(new Insets(5,3,2,3));
        add(toolbar, BorderLayout.PAGE_START);
        
        updateHistoryButtons();
        
        add(scroll);
        scroll.setPreferredSize(new Dimension(680,500));
        open(null, null);
        
        // Close button
        JButton button = new JButton("Close");
        button.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (Chatty.DEBUG) {
                    reload();
                } else {
                    setVisible(false);
                }
            }
        });
        add(button,BorderLayout.SOUTH);

        pack();
        
        CachedManager m = new CachedManager(Chatty.getPathCreate(Chatty.PathType.CACHE).resolve("patreon").toString(), 60*60*24*3, "Patreon") {
            
            @Override
            public boolean handleData(String data) {
                if (data != null) {
                    patreonHtml = data;
                    return true;
                }
                return false;
            }
        };
        if (!m.load()) {
            UrlRequest request = new UrlRequest("https://tduva.com/res/patrons");
            request.async((result, responseCode) -> {
                if (responseCode == 200 && result != null) {
                    m.dataReceived(result, false);
                }
            });
        }
    }
    
    private JButton makeButton(String image, String action, String tooltip) {
        JButton button = new JButton();
        button.setActionCommand(action);
        button.setIcon(new ImageIcon(About.class.getResource(image)));
        button.setToolTipText(tooltip);
        return button;
    }
    
    /**
     * Loads the given page into the window. This only changes the page, not
     * the position in the page, although loading the page can trigger just
     * that.
     * 
     * @param page 
     */
    private void loadPage(String page) {
        try {
            textPane.setPage(getClass().getResource(page));
            currentPage = page;
        }
        catch (IOException ex) {
            LOGGER.warning("Invalid page: " + page + " (" + ex.getLocalizedMessage() + ")");
        }
    }

    /**
     * Close window (or reload page if in DEBUG mode)
     * 
     * @param e 
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
    
    public final void open(String page, String ref) {
        open(page, ref, -1, null);
    }
    
    /**
     * Open a page and go to a position (ref, which is an anchor in the HTML
     * document) on that page.
     * 
     * @param page The filename of the page to load, loads the default page if
     * this is {@code null}
     * @param ref The position to jump to, sets to empty String if {@code null}
     * @param scrollPosition What scroll position to jump to, if not -1
     * @param cameFrom The HistoryItem that was current before this was called
     * (when navigating through the history, history.current() won't be the
     * still current page, but the one that will be opened)
     */
    private void open(String page, String ref, int scrollPosition,
            HistoryItem cameFrom) {
        if (ref == null) {
            ref = "";
        }
        ref = ref.replace(" ", "_");
        if (page == null) {
            page = DEFAULT_PAGE;
        }
        
        // Set scroll position to current page, which is either cameFrom, if
        // available (e.g. when navigating through history), or just
        // history.current() (which should be correct when not navigating
        // through history)
        if (cameFrom != null) {
            cameFrom.setScrollPosition(scrollbar.getValue());
        } else if (history.current() != null) {
            history.current().setScrollPosition(scrollbar.getValue());
        }
        
        if (currentPage != null && currentPage.equals(page)) {
            // If already on this page, just jump to the position
            jumpTo(ref);
            if (scrollPosition != -1) {
                scrollbar.setValue(scrollPosition);
            }
        } else {
            // Else save position for later, so it can jump there after loading
            referenceAfterLoad = ref;
            scrollPositionAfterLoad = scrollPosition;
        }
        loadPage(page);
        
        // Add opened location to history (doesn't change if it's the same as
        // it is already on this position in the history)
        HistoryItem location = new HistoryItem(page, ref);
        history.addIfNew(location);
        updateHistoryButtons();
        
        currentLocation.setText(location.toString());
    }
    
    /**
     * Enable/disable the history buttons based on whether there is something
     * to go to in the history.
     */
    private void updateHistoryButtons() {
        historyForwardButton.setEnabled(history.hasNext());
        historyBackButton.setEnabled(history.hasPrevious());
    }
    
    /**
     * Jump to the given position (ref) in the current page
     * 
     * @param ref The position to jump to, if it is an empty String, then it
     * will scroll to the very top
     * @throws NullPointerException If {@code ref} is {@code null}
     */
    private void jumpTo(final String ref) {
        if (ref.isEmpty()) {
            textPane.scrollRectToVisible(new Rectangle(0,0,1,1));
        } else {
            textPane.scrollToReference(ref);
        }
    }
    
    /**
     * Once the page is loaded, jump to the position, if one is saved for this
     * page.
     */
    private void pageLoaded() {
        HTMLDocument doc = (HTMLDocument) textPane.getDocument();
        Element patreonElement = doc.getElement("patreon");
        if (patreonElement != null) {
            try {
                doc.setInnerHTML(patreonElement, patreonHtml);
            }
            catch (Exception ex) {
                LOGGER.warning("Error changing HTML: " + ex);
            }
        }
        
        if (scrollPositionAfterLoad != -1) {
            scrollbar.setValue(scrollPositionAfterLoad);
            scrollPositionAfterLoad = -1;
        }
        else if (referenceAfterLoad != null) {
            jumpTo(referenceAfterLoad);
            referenceAfterLoad = null;
        }
    }
    
    /**
     * Reloads the current page and jumps to the same position. This is only
     * used for testing purposes, because usually the pages shouldn't change
     * while the program is running.
     */
    private void reload() {
        Document doc = textPane.getDocument();
        doc.putProperty(Document.StreamDescriptionProperty, null);
        scrollPositionAfterLoad = scrollbar.getValue();
        loadPage(currentPage);
    }
    
    /**
     * Saves a combination of page and ref which denotes a location in the help.
     * Used for the history.
     */
    private static class HistoryItem {
        
        private final String page;
        private final String ref;
        private int scrollPosition = -1;
        
        HistoryItem(String page, String ref) {
            this.page = page;
            this.ref = ref;
        }
        
        public void setScrollPosition(int position) {
            this.scrollPosition = position;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof HistoryItem) {
                HistoryItem obj2 = (HistoryItem)obj;
                return page.equals(obj2.page) && ref.equals(obj2.ref);
            }
            return false;
        }
        
        @Override
        public String toString() {
            if (ref.isEmpty()) {
                return page;
            }
            return page+"#"+ref;
        }
        
    }
}
