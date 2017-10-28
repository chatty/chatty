
package chatty.gui.components;

import chatty.Chatty;
import chatty.gui.MainGui;
import chatty.gui.UrlOpener;
import chatty.util.DateTime;
import chatty.util.JSONUtil;
import chatty.util.MiscUtil;
import chatty.util.UrlRequest;
import chatty.util.settings.Settings;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Show short announcements that are requested from a JSON file.
 * 
 * When the "Mark as read" button is pressed, the timestamp of the latest news
 * is stored to prevent those news from being shown as new.
 * 
 * @author tduva
 */
public class NewsDialog extends JDialog {
    
    private static final Logger LOGGER = Logger.getLogger(NewsDialog.class.getName());
    
    private static final String NEWS_URL = "http://chatty.github.io/news.json";
    private static final String NEWS_URL_TEST = "http://127.0.0.1/twitch/news.json";
    
    private static final String SETTING_LAST_READ_TIMESTAMP = "newsLastRead";
    private static final int REQUEST_DELAY = 60*1000;
    private static final int TIMER_DELAY = (int)TimeUnit.HOURS.toMillis(6);
    
    private final MainGui main;
    private final Settings settings;
    
    // State
    private long lastRequested;
    private long latestNewsTimestamp;
    private String cachedNews;
    
    // GUI
    private final JTextPane news;
    private final JButton refreshButton;

    public NewsDialog(MainGui main, final Settings settings) {
        super(main);
        
        this.main = main;
        this.settings = settings;
        
        news = new JTextPane();
        news.setEditable(false);
        news.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String url = e.getURL().toString();
                    String protocol = e.getURL().getProtocol();
                    if (protocol.equals("http") || protocol.equals("https")) {
                        UrlOpener.openUrlPrompt(NewsDialog.this, url, true);
                    }
                }
            }
        });
        news.setContentType("text/html");
        
        add(new JScrollPane(news), BorderLayout.CENTER);
        
        JPanel buttons = new JPanel(new GridBagLayout());
        add(buttons, BorderLayout.SOUTH);
        
        final JButton markRead = new JButton("Mark as read & Close");
        final JButton close = new JButton("Close");
        refreshButton = new JButton(new ImageIcon(NewsDialog.class.getResource("view-refresh.png")));
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.8;
        gbc.insets = new Insets(5, 4, 5, 4);
        buttons.add(refreshButton, gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        gbc.insets = new Insets(5, 4, 5, 4);
        buttons.add(markRead, gbc);
        
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 1, 5, 4);
        buttons.add(close, gbc);
        

        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == markRead) {
                    if (latestNewsTimestamp > 0) {
                        settings.setLong(SETTING_LAST_READ_TIMESTAMP, latestNewsTimestamp);
                        setNews(cachedNews);
                    }
                    setVisible(false);
                } else if (e.getSource() == close) {
                    setVisible(false);
                }
                else if (e.getSource() == refreshButton) {
                    requestNews(false);
                }
            }
        };
        refreshButton.addActionListener(buttonAction);
        markRead.addActionListener(buttonAction);
        close.addActionListener(buttonAction);
        
        pack();
        setMinimumSize(new Dimension(400, 300));
        setTitle("Announcements");
        
        Timer timer = new Timer(TIMER_DELAY, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                autoRequestNews(false);
            }
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    /**
     * Shows the dialog centered on the main GUI and requests the latest news if
     * the request delay has passed.
     */
    public void showDialog() {
        setLocationRelativeTo(main);
        setVisible(true);
        getNews(false);
    }
    
    /**
     * Requests the news only if the auto request setting is enabled.
     * 
     * @param openIfUnread Open the dialog automatically for new news
     */
    public void autoRequestNews(boolean openIfUnread) {
        if (settings.getBoolean("newsAutoRequest")) {
            getNews(openIfUnread);
        }
    }
    
    /**
     * Requests the news from the server if the request delay has passed or
     * load up a cached version.
     * 
     * @param openIfUnread Open up the dialog automatically if new announcements
     * are available
     */
    private void getNews(boolean openIfUnread) {
        if (System.currentTimeMillis() - lastRequested < REQUEST_DELAY) {
            setNews(cachedNews);
            return;
        }
        requestNews(openIfUnread);
    }
    
    /**
     * Requests the announcements from the server.
     */
    private void requestNews(final boolean openIfUnread) {
        news.setText("Loading..");
        latestNewsTimestamp = 0;
        lastRequested = System.currentTimeMillis();
        refreshButton.setEnabled(false);
        
        UrlRequest request = new UrlRequest(Chatty.DEBUG ? NEWS_URL_TEST : NEWS_URL);
        request.async((result, responseCode) -> {
            SwingUtilities.invokeLater(() -> {
                if (responseCode == 200) {
                    int unread = setNews(result);
                    if (openIfUnread && unread > 0) {
                        showDialog();
                    }
                } else {
                    news.setText("Error loading news. (" + responseCode + ")");
                }
                refreshButton.setEnabled(true);
            });
        });
    }
    
    /**
     * Parses the given data and fills the dialog accordingly. This may be
     * either directly from the server or a cached version. Updates the title
     * and main menu notification accordingly.
     * 
     * @param data The JSON containing the announcements
     * @return The number of new announcements
     */
    private int setNews(String data) {
        if (data == null || data.isEmpty()) {
            return 0;
        }
        try {
            int newCount = parseNews(data);
            cachedNews = data;
            if (newCount > 0) {
                setTitle(String.format("Announcements (%d new)", newCount));
                main.setAnnouncementAvailable(true);
            } else {
                setTitle("Announcements");
                main.setAnnouncementAvailable(false);
            }
            return newCount;
        } catch (Exception ex) {
            news.setText("Error loading news.");
            LOGGER.warning(MiscUtil.getStackTrace(ex));
        }
        return 0;
    }
    
    private int parseNews(String text) throws ParseException {

        // Data
        long lastRead = settings.getLong(SETTING_LAST_READ_TIMESTAMP);
        int unreadCount = 0;
        
        JSONParser parser = new JSONParser();
        JSONObject data = (JSONObject)parser.parse(text);
        JSONArray list = (JSONArray)data.get("news");
        
        // HTML
        final SimpleDateFormat DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        StringBuilder sb = new StringBuilder("<html><head><style>"
                + "body { font:sans-serif; font-size: 12pt; margin: 3px; }"
                + "h2 { margin: 12px 0 0 0; font-size: 16pt; }"
                + ".time { font-size: 12pt; color: #333333; font-weight: bold; }"
                + ".new { background-color: yellow; font-size: 12pt; color: #777777; }"
                + ".old { color: #777777; }"
                + "</style></head><body>"
                + "<p style='margin-top:0;background-color:#EEEEEE;padding:3px;'>"
                + ""+parseContent((String)data.get("intro"))
                + "</p>");
        
        for (Object o : list) {
            
            // Data
            JSONObject entry = (JSONObject)o;
            String title = (String)entry.get("title");
            String content = (String)entry.get("content");
            long time_added = ((Number)entry.get("timestamp")).longValue()*1000;
            boolean old = JSONUtil.getBoolean(entry, "old", false);
            
            if (time_added > lastRead && !old) {
                unreadCount++;
            }
            if (time_added > latestNewsTimestamp) {
                latestNewsTimestamp = time_added;
            }
            
            // HTML
            sb.append("<h2>").append(title);
            if (old) {
                sb.append(" <span class='old'>(old)</span>");
            }
            else if (time_added > lastRead) {
                sb.append(" <span class='new'>(new)</span>");
            }
            sb.append("</h2>");
            sb.append(String.format(" <div class=\"time\">%s (%s)</div><p>%s</p>",
                    DATETIME.format(new Date(time_added)),
                    DateTime.agoText(time_added),
                    parseContent(content)));
            
        }
        sb.append("</body></html>");

        // Replace text in dialog
        news.setDocument(news.getEditorKit().createDefaultDocument());
        news.setText(sb.toString());
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                news.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
            }
        });
        
        return unreadCount;
    }
    
    private String parseContent(String content) {
        return content.replaceAll("\\[([^]]+)\\]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");
    }
    
}
