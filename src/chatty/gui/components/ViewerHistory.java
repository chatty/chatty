
package chatty.gui.components;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.LaF;
import chatty.gui.components.menus.ContextMenu;
import chatty.gui.components.menus.ContextMenuAdapter;
import chatty.gui.components.menus.ContextMenuListener;
import chatty.gui.components.menus.HistoryContextMenu;
import chatty.lang.Language;
import chatty.util.api.StreamTagManager.StreamTag;
import chatty.util.api.StreamInfo;
import chatty.util.api.StreamInfo.StreamType;
import chatty.util.api.StreamInfoHistoryItem;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Shows a graph with the viewer count history.
 * 
 * @author tduva
 */
public class ViewerHistory extends JComponent {
    
    private static final Logger LOGGER = Logger.getLogger(ViewerHistory.class.getName());
    
    private static final float ONE_HOUR = 60*60*1000;
    
    /**
     * The sizes of the points
     */
    private static final int POINT_SIZE = 5;
    
    private static final Color FIRST_COLOR = new Color(20,180,62);
    
    private static final Color SECOND_COLOR = new Color(0,0,220);
    
    private static final Color SECOND_COLOR_DARK = new Color(160,160,255);
    
    private static final Color OFFLINE_COLOR = new Color(255,140,140);
    
    
//        private static final Color FIRST_COLOR = new Color(0,0,255);
//    
//    private static final Color SECOND_COLOR = new Color(30,160,0);
//    
//    private static final Color OFFLINE_COLOR = new Color(200,180,180);
    
    private static final Color HOVER_COLOR = Color.WHITE;
    /**
     * How much tolerance when hovering over entries with the mouse (how far
     * away the mouse pointer can be for it to be still associated with the
     * entry).
     */
    private static final int HOVER_RADIUS = 18;
    /**
     * The font to use for text.
     */
    private Font font = new Font(Font.DIALOG, Font.PLAIN, 12);
    /**
     * The margin all around the graph area.
     */
    private static final int MARGIN = 8;
    
    /**
     * How long the latest viewercount data should be displayed as "now:".
     */
    private static final int CONSIDERED_AS_NOW = 200*1000;
    
    /**
     * How to format times.
     */
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    
    private Color foreground_color = Color.BLACK;
    
    /**
     * The background color, can be set from the outside thus no constant.
     */
    private Color background_color = new Color(250,250,250);
    
    /**
     * Store the current history.
     */
    private LinkedHashMap<Long,StreamInfoHistoryItem> history;
    
    /*
     * Values that only change when the history is updated, so it's enough to
     * update those then
     */
    private int maxValue;
    private int minValue;
    private long startTime;
    private long endTime;
    private long duration;
    /**
     * Time in milliseconds that is displayed, going back from the newest.
     * Values <= 0 indicate that the whole data currentRange is displayed.
     */
    private long currentRange = 0;
    private long fixedStartTime = 0;
    private long fixedEndTime = 0;
    
    private final Map<String, Long> fixedStartTimes = new HashMap<>();
    private final Map<String, Long> fixedEndTimes = new HashMap<>();
    
    private String currentStream = null;
    
    
    /**
     * Store the actual locations of points on the component, this is updated
     * with every redraw.
     */
    private LinkedHashMap<Point, Long> locations = new LinkedHashMap<>();
    /**
     * Store color for every entry, this is updated when a new history is set.
     */
    private LinkedHashMap<Long, Color> colors = new LinkedHashMap<>();
    
    private final HistoryContextMenu contextMenu = new HistoryContextMenu();
    
    /*
     * Values that affect what is rendered.
     */
    private boolean mouseEntered = false;
    private boolean showInfo = true;
    private long hoverEntry = -1;
    private boolean fixedHoverEntry = false;
    private boolean verticalZoom = false;
    
    private ViewerHistoryListener listener;

    private static class HistoryTest {
        
        // Starting with 1 because selecting fixed range checks for 0 (which it
        // shouldn't be outside testing)
        public long currentTime = 1;
        public LinkedHashMap<Long, StreamInfoHistoryItem> history = new LinkedHashMap<>();
        public long startTime;
        public long picnicStartTime;
        
        public void add(String title, String game, int viewers, StreamType streamType, StreamTag... communities) {
            java.util.List<StreamTag> c;
            if (communities == null) {
                c = null;
            } else {
                c = Arrays.asList(communities);
            }
            history.put(currentTime, new StreamInfoHistoryItem(currentTime,
                    viewers, title, game, streamType, c,
                    startTime, picnicStartTime));
            currentTime += 120*1000;
        }
        
    }
    
    public ViewerHistory() {
        // Test data
        if (Chatty.DEBUG) {
            
            HistoryTest test = new HistoryTest();
            
            
            history = new LinkedHashMap<>();
            StreamTag c1 = new StreamTag("abc", "VarietyStreaming");
            StreamTag c2 = new StreamTag("abc", "Speedrunning");
            StreamTag c3 = new StreamTag("abc", "Pro-Audio");
            
            test.startTime = -5*60*1000;
            test.picnicStartTime = -10*60*1000;
            test.add("Leveling Battlemage", "The Elder Scrolls V: Skyrim", 5, StreamType.LIVE, c1, c2);
            test.add("Leveling Battlemage", "The Elder Scrolls V: Skyrim", 4, StreamType.LIVE, c1, c2);
            test.add("Leveling Battlemage", "The Elder Scrolls V: Skyrim", 4, StreamType.LIVE, c1, c2);
            test.add("Leveling Battlemage", "The Elder Scrolls V: Skyrim", 6, StreamType.LIVE, c1, c2, c3);
            test.add("Leveling Battlemage", "The Elder Scrolls V: Skyrim", 8, StreamType.LIVE, c1, c2, c3);
            test.add("Pause", "The Elder Scrolls V: Skyrim", 5, StreamType.LIVE, c1);
            test.add("Pause", "The Elder Scrolls V: Skyrim", 5, StreamType.LIVE, c1);
            test.add("Pause", "The Elder Scrolls V: Skyrim", 10, StreamType.LIVE, c1);
            test.add("Pause", "The Elder Scrolls V: Skyrim", 8, StreamType.LIVE, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 5, StreamType.WATCH_PARTY, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 5, StreamType.WATCH_PARTY, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 10, StreamType.WATCH_PARTY, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 12, StreamType.WATCH_PARTY, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 14, StreamType.WATCH_PARTY, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 12, StreamType.WATCH_PARTY, c1);
            test.add("Diebesgilde", "The Elder Scrolls V: Skyrim", 18, StreamType.WATCH_PARTY, c1);
            test.add("any% attempts", "Tomb Raider III: Adventures of Lara Croft", 20, StreamType.LIVE, (StreamTag) null);
            test.add("any% attempts", "Tomb Raider III: Adventures of Lara Croft", 34, StreamType.LIVE, c2);
            test.add("any% attempts", "Tomb Raider III: Adventures of Lara Croft", 40, StreamType.LIVE, c2);
            test.add("any% attempts", "Tomb Raider III: Adventures of Lara Croft", 45, StreamType.LIVE, c2);
            test.add("any% attempts", "Tomb Raider III: Adventures of Lara Croft", 59, StreamType.LIVE, c2);
            for (int i=0;i<100;i++) {
                test.add("any% attempts", "Tomb Raider III: Adventures of Lara Croft", 59, StreamType.LIVE, c2);
            }
            
//            history.put((long) 1000*1000, new StreamInfoHistoryItem(5,"Leveling Battlemage","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1000*1000, 1000*1000));
//            history.put((long) 1120*1000, new StreamInfoHistoryItem(4,"Leveling Battlemage","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1120*1000, 1120*1000));
//            history.put((long) 1240*1000, new StreamInfoHistoryItem(4,"Leveling Battlemage","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1240*1000, 1240*1000));
//            history.put((long) 1360*1000, new StreamInfoHistoryItem(6,"Leveling Battlemage","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1360*1000, 1360*1000));
//            history.put((long) 1480*1000, new StreamInfoHistoryItem(8,"Leveling Battlemage","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1480*1000, 1480*1000));
//            history.put((long) 1600*1000, new StreamInfoHistoryItem(8,"Pause","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1600*1000, 1600*1000));
//            history.put((long) 1720*1000, new StreamInfoHistoryItem(10,"Pause","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1720*1000, 1720*1000));
//            history.put((long) 1840*1000, new StreamInfoHistoryItem(12,"Pause","The Elder Scrolls V: Skyrim", StreamType.LIVE, c1, 1840*1000, 1840*1000));
//            history.put((long) 1960*1000, new StreamInfoHistoryItem(12,"Diebesgilde","The Elder Scrolls V: Skyrim", StreamType.WATCH_PARTY, c1, 1960*1000, 1960*1000));
//            history.put((long) 2080*1000, new StreamInfoHistoryItem(18,"Diebesgilde","The Elder Scrolls V: Skyrim", StreamType.WATCH_PARTY, c1, 2080*1000, 2080*1000));
//            history.put((long) 2200*1000, new StreamInfoHistoryItem(20,"Diebesgilde","The Elder Scrolls V: Skyrim", StreamType.WATCH_PARTY, c1, 2200*1000, 2200*1000));
//            history.put((long) 2320*1000, new StreamInfoHistoryItem(22,"Diebesgilde","The Elder Scrolls V: Skyrim", StreamType.WATCH_PARTY, c1, 2320*1000, 2320*1000));
//            history.put((long) 2440*1000, new StreamInfoHistoryItem(40,"Diebesgilde","The Elder Scrolls V: Skyrim", StreamType.WATCH_PARTY, c1, 2440*1000, 2440*1000));
//            history.put((long) 2560*1000, new StreamInfoHistoryItem(72,"Diebesgilde","The Elder Scrolls V: Skyrim", StreamType.WATCH_PARTY, c1, 2560*1000, 2560*1000));
//            history.put((long) 2760*1000, new StreamInfoHistoryItem(72,"any% attempts","Tomb Raider III: Adventures of Lara Croft", StreamType.LIVE, null, 2760*1000, 2760*1000));
//            history.put((long) 2960*1000, new StreamInfoHistoryItem(68,"any% attempts","Tomb Raider III: Adventures of Lara Croft", StreamType.LIVE, c2, 2960*1000, 2960*1000));
//            history.put((long) 3160*1000, new StreamInfoHistoryItem(71,"any% attempts","Tomb Raider III: Adventures of Lara Croft", StreamType.LIVE, c2, 3160*1000, 3160*1000));
            //history.put((long) 2680*1000, new StreamInfoHistoryItem());
            //history.put((long) 2800*1000, new StreamInfoHistoryItem());
            //history.put((long) 2920*1000, new StreamInfoHistoryItem());
            
            //history.put((long) 1000 * 1000, 40);
            //history.put((long) 1300 * 1000, 290);
//        history.put((long)1600*1000,400);
//        history.put((long)2200*1000,90);
//        history.put((long)3000*1000,123);
//        history.put((long)3300*1000,-1);
//        history.put((long)3600*1000,0);
            setHistory("", test.history);
        }
        MyMouseListener mouseListener = new MyMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);
        setOpaque(false);
    }
    
    public void setListener(ViewerHistoryListener listener) {
        this.listener = listener;
    }
    
    
    
    /**
     * Should info (min/max) be shown.
     * 
     * @return 
     */
    private boolean isShowingInfo() {
        if (showInfo) {
            return true;
        }
        else {
            return mouseEntered;
        }
    }
    
    /**
     * Draw the text and graph.
     * 
     * @param g 
     */
    @Override
    public void paintComponent(Graphics g) {
        locations.clear();
        
        // This color is used for everything until drawing the points
        g.setColor(foreground_color);
        
        // Anti-Aliasing
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Font
        FontMetrics fontMetrics = g.getFontMetrics(font);
        int fontHeight = fontMetrics.getHeight();
        g.setFont(font);
        
        int topTextY = fontMetrics.getAscent();
        
        // Margins
        int vMargin = fontHeight + MARGIN;
        int hMargin = MARGIN;

        // Calculate actual usable size
        double width = getWidth() - hMargin * 2;
        double height = getHeight() - vMargin * 2;
        
        boolean drawLowerLine = height > -vMargin;
        
        // If there is any data and no hovered entry is shown, draw current
        // viewercount
        int nowTextX = 0;
        if (history != null && hoverEntry == -1) {
            Integer viewers = history.get(endTime).getViewers();
            long ago = System.currentTimeMillis() - endTime;
            String text;
            if (ago > CONSIDERED_AS_NOW) {
                text = Language.getString("channelInfo.viewers.latest",
                        Helper.formatViewerCount(viewers));
            } else {
                text = Language.getString("channelInfo.viewers.now",
                        Helper.formatViewerCount(viewers));
            }
            if (viewers == -1) {
                text = Language.getString("channelInfo.streamOffline");
            }
            nowTextX = getWidth() - fontMetrics.stringWidth(text);
            g.drawString(text, nowTextX, topTextY);
        }
        
        // Default text when no data is present
        if (history == null || history.size() < 2) {
            String text = Language.getString("channelInfo.viewers.noHistory");
            int textWidth = fontMetrics.stringWidth(text);
            int y = getHeight() / 2 + fontMetrics.getDescent();
            int x = (getWidth() - textWidth) / 2;
            boolean drawInfoText = false;
            if (history != null && y < topTextY+fontHeight+4 && x+textWidth+7 > nowTextX) {
                if (drawLowerLine || nowTextX > textWidth+5) {
                    if (drawLowerLine) {
                        y = getHeight() - 2;
                    } else {
                        y = topTextY;
                    }
                    x = 0;
                    drawInfoText = true;
                }
            } else {
                drawInfoText = true;
            }
            if (drawInfoText) {
                g.drawString(text, x, y);
            }
            return;
        }
        
        //----------
        // From here only when actual data is to be rendered
        
        
        
        
        // Show info on hovered entry
        String maxValueText = Language.getString("channelInfo.viewers.max",
                Helper.formatViewerCount(maxValue));
        int maxValueEnd = fontMetrics.stringWidth(maxValueText);
        boolean displayMaxValue = true;
        
        if (hoverEntry != -1) {
            Integer viewers = history.get(hoverEntry).getViewers();
            Date d = new Date(hoverEntry);
            String text = Language.getString("channelInfo.viewers.hover", Helper.formatViewerCount(viewers))+" ("+sdf.format(d)+")";
            if (viewers == -1) {
                text = Language.getString("channelInfo.streamOffline")+" ("+sdf.format(d)+")";
            }
            int x = getWidth() - fontMetrics.stringWidth(text);
            if (maxValueEnd > x) {
                displayMaxValue = false;
            }
            g.drawString(text, x, topTextY);
        }
        
        String minText = Language.getString("channelInfo.viewers.min",
                Helper.formatViewerCount(minValue));
        int minTextWidth = fontMetrics.stringWidth(minText);
        
        // Draw Times
        if (drawLowerLine) {
            String timeText = makeTimesText(startTime, endTime);
            int timeTextWidth = fontMetrics.stringWidth(timeText);
            int textX = getWidth() - timeTextWidth;
            g.drawString(timeText, textX, getHeight() - 1);

            if (minValue >= 1000 && timeTextWidth + minTextWidth > width) {
                minText = Language.getString("channelInfo.viewers.min",
                        minValue / 1000)+"k";
            }
        }
        
        // Draw min/max if necessary
        if (isShowingInfo()) {
            if (displayMaxValue) {
                g.drawString(maxValueText, 0, topTextY);
            }
            if (drawLowerLine) {
                g.drawString(minText, 0, getHeight() - 1);
            } else if (maxValueEnd + minTextWidth + 29 < nowTextX) {
                g.drawString(minText, maxValueEnd+10, topTextY);
            }
        }
        
        // If height available for the graph is too small, don't draw graph
        if (height < 5) {
            return;
        }

        // Calculation factors for calculating the points location
        int range = maxValue - minValue;
        if (!verticalZoom) {
            range = maxValue;
        }
        if (range == 0) {
            // Prevent division by zero
            range = 1;
        }
        double pixelPerViewer = height / range;
        double pixelPerTime = width / duration;
        
        // Go through all entries and calculate positions
        
        int prevX = -1;
        int prevY = -1;
        StreamInfoHistoryItem prevItem = null;
        Iterator<Entry<Long,StreamInfoHistoryItem>> it = history.entrySet().iterator();
        while (it.hasNext()) { 
            Entry<Long,StreamInfoHistoryItem> entry = it.next();
            
            // Get time and value to draw next
            long time = entry.getKey();
            if (time < startTime || time > endTime) {
                continue;
            }
            long offsetTime = time - startTime;
            
            int viewers = entry.getValue().getViewers();
            if (viewers == -1) {
                viewers = 0;
            }
            
            // Calculate point location
            int x = (int)(hMargin + offsetTime * pixelPerTime);
            int y;
            if (!verticalZoom) {
                y = (int)(-vMargin + getHeight() - (viewers) * pixelPerViewer);
            }
            else {
                y = (int)(-vMargin + getHeight() - (viewers - minValue) * pixelPerViewer);
            }
            
            // Draw connecting line
            if (prevX != -1) {
                if (entry.getValue().getStreamType() != StreamType.LIVE &&
                        prevItem != null && prevItem.getStreamType() != StreamType.LIVE) {
                    g.setColor(Color.LIGHT_GRAY);
                } else {
                    g.setColor(foreground_color);
                }
                g.drawLine(x,y,prevX,prevY);
            }
            
            // Save point coordinates to be able to draw the line next iteration
            prevX = x;
            prevY = y;
            prevItem = entry.getValue();
            
            // Save point locations to draw points and to find entries on hover
            locations.put(new Point(x,y),time);
        }
        
        
        prevItem = null;
        // Draw points (after lines, so they are in front)
        for (Point point : locations.keySet()) {
            int x = point.x;
            int y = point.y;
            long seconds = locations.get(point);
            
            StreamInfoHistoryItem historyObject = history.get(seconds);
            
            // Highlight hovered entry
            if (seconds == hoverEntry) {
                g.setColor(HOVER_COLOR);
            } else {
                // Draw offline points differently
                if (!historyObject.isOnline()) {
                    g.setColor(OFFLINE_COLOR);
                } else {
                    g.setColor(colors.get(seconds));
                }
            }
            int pointSize = POINT_SIZE;
            
            if (prevItem != null && !Objects.equals(prevItem.getCommunities(), historyObject.getCommunities())) {
                pointSize += 1;
                g.fillRect(x - pointSize / 2, y - pointSize / 2, pointSize, pointSize);
            } else {
                g.fillOval(x - pointSize / 2, y - pointSize / 2, pointSize, pointSize);
            }
            
            
            prevItem = historyObject;
        }
    }
    
    /**
     * Make the text for the start and end time, taking into consideration
     * whether a specific range is displayed.
     * 
     * @param startTime
     * @param endTime
     * @return 
     */
    private String makeTimesText(long startTime, long endTime) {
        String startTimeText = makeTimeText(startTime, fixedStartTime);
        String endTimeText = makeTimeText(endTime, fixedEndTime);
        
        String text = startTimeText+" - "+endTimeText;
        if (fixedStartTime <= 0 && currentRange > 0) {
            text += " (" + duration(currentRange) + "h)";
        }
        return text;
    }
    
    /**
     * Create the text for the start or end time, taking into consideration
     * whether it's a fixed time.
     * 
     * @param time The time in milliseconds to display
     * @param fixedTime The corresponding fixed time in milliseconds
     * @return 
     */
    private String makeTimeText(long time, long fixedTime) {
        Date date = new Date(time);
        if (time == fixedTime) {
            return "|"+sdf.format(date)+"|";
        }
        return sdf.format(date);
    }
    
    /**
     * Gets the starting time for the displayed range. If there is a fixed
     * starting time, use that, otherwise check if there is a range and
     * calculate the starting time from that. Otherwise return 0, meaning the
     * data is displayed from the start.
     * 
     * @param range The time in milliseconds that the data should  be displayed,
     *  going backwards, starting from the very end.
     * @return The start of the data displaying range in milliseconds.
     */
    private long getStartAt(long range) {
        if (fixedStartTime > 0) {
            return fixedStartTime;
        }
        if (range <= 0) {
            return 0;
        }
        long end = -1;
        for (long time : history.keySet()) {
            end = time;
        }
        long startAt = end - range;
        if (startAt < 0) {
            startAt = 0;
        }
        return startAt;
    }
    
    private long getEndAt() {
        if (fixedEndTime > 0 && fixedEndTime > fixedStartTime) {
            return fixedEndTime;
        }
        return -1;
    }
    
    /**
     * Update the start/end/duration/min/max variables which can be changed
     * when the data changes as well when the displayed range changes.
     */
    private void updateVars() {
        long startAt = getStartAt(currentRange);
        long endAt = getEndAt();
        int max = 0;
        int min = -1;
        long start = -1;
        long end = -1;
        for (Long time : history.keySet()) {
            if (time < startAt) {
                continue;
            }
            if (endAt > startAt && time > endAt) {
                continue;
            }
            // Start/End time
            if (start == -1) {
                start = time;
            }
            end = time;
            // Max/min value
            StreamInfoHistoryItem historyObj = history.get(time);
            int viewerCount = historyObj.getViewers();
            if (viewerCount < min || min == -1) {
                min = viewerCount;
            }
            if (viewerCount == -1) {
                min = 0;
            }
            if (viewerCount > max) {
                max = viewerCount;
            }
        }
        
        maxValue = max;
        minValue = min;
        startTime = start;
        endTime = end;
        duration = end - start;
    }
    
    /**
     * Updates the map of colors used for rendering. This creates the
     * alternating colors based on the full stream status, which should only
     * be changed when new data is set.
     */
    private void makeColors() {
        colors.clear();
        Iterator<Entry<Long, StreamInfoHistoryItem>> it = history.entrySet().iterator();
        String prevStatus = null;
        Color currentColor = FIRST_COLOR;
        while (it.hasNext()) {
            Entry<Long, StreamInfoHistoryItem> entry = it.next();
            long time = entry.getKey();
            StreamInfoHistoryItem item = entry.getValue();
            String newStatus = item.getStatusAndGame();
            // Only change color if neither the previous nor the new status
            // are null (offline) and the previous and new status are not equal.
            if (prevStatus != null && newStatus != null
                    && !prevStatus.equals(newStatus)) {
                // Change color
                if (currentColor == FIRST_COLOR) {
                    currentColor = !LaF.isDarkTheme() ? SECOND_COLOR : SECOND_COLOR_DARK;
                } else {
                    currentColor = FIRST_COLOR;
                }
            }
            colors.put(time, currentColor);
            // Save this status as previous status, but only if it's not
            // offline.
            if (newStatus != null) {
                prevStatus = newStatus;
            }
        }
    }

    /**
     * Sets a new history dataset, update the variables needed for rendering
     * and repaints the display.
     * 
     * @param stream
     * @param newHistory 
     */
    public void setHistory(String stream, LinkedHashMap<Long,StreamInfoHistoryItem> newHistory) {
        manageChannelSpecificVars(stream);
        // Make a copy so changes are not reflected in this
        history = newHistory;
        // Only update variables when the history contains something, else
        // set to null so nothing is rendered that isn't supposed to
        if (history != null && !history.isEmpty()) {
            updateVars();
            checkVars();
            makeColors();
        } else {
            history = null;
        }
        repaint();
    }
    
    /**
     * Saves current fixed times and loads fixed times for the new channel, if
     * the channel changed.
     * 
     * @param stream 
     */
    private void manageChannelSpecificVars(String stream) {
        if (!stream.equals(currentStream)) {
            if (hoverEntry > 0 && listener != null) {
                listener.noItemSelected();
            }
            hoverEntry = -1;
            fixedHoverEntry = false;
            fixedStartTimes.put(currentStream, fixedStartTime);
            fixedEndTimes.put(currentStream, fixedEndTime);
            currentStream = stream;
            fixedStartTime = 0;
            fixedEndTime = 0;
            if (fixedStartTimes.containsKey(stream)) {
                fixedStartTime = fixedStartTimes.get(stream);
            }
            if (fixedEndTimes.containsKey(stream)) {
                fixedEndTime = fixedEndTimes.get(stream);
            }
        }
    }
    
    /**
     * Checks if start and end times are valid and if not, resets the fixed
     * times.
     */
    private void checkVars() {
        if (startTime == -1 || endTime == -1) {
            fixedStartTime = 0;
            fixedEndTime = 0;
            updateVars();
        }
    }

    /**
     * Set a new foreground color and repaint.
     * 
     * @param color 
     */
    public void setForegroundColor(Color color) {
        foreground_color = color;
        repaint();
    }
    
    public void setBaseFont(Font newFont) {
        font = newFont.deriveFont(Font.PLAIN);
        repaint();
    }
    
    /**
     * Sets the time range to this number of minutes.
     * 
     * @param minutes 
     */
    public void setRange(int minutes) {
        contextMenu.setRange(minutes);
        this.currentRange = minutes*60*1000;
        fixedStartTime = -1;
        fixedEndTime = -1;
        if (history != null) {
            updateVars();
        }
        repaint();
    }
    
    public void setVerticalZoom(boolean zoom) {
        contextMenu.setZoom(zoom);
        verticalZoom = zoom;
        repaint();
    }
    
    public void setDocked(boolean docked) {
        contextMenu.setDocked(docked);
    }

    public void addContextMenuListener(ContextMenuListener l) {
        contextMenu.addContextMenuListener(l);
    }
    
    public void setFixedStartAt(long startAt) {
        if (startAt == endTime) {
            // Don't use last entry as start
            return;
        }
        if (fixedStartTime > 0 && startAt > 0) {
            fixedEndTime = startAt;
        } else {
            fixedStartTime = startAt;
            fixedEndTime = startAt;
        }
        if (history != null) {
            updateVars();
        }
        repaint();
    }
    
    private String duration(long time) {
        float hours = time / ONE_HOUR;
        if (hours < 1) {
            return String.format("%.1f", hours);
        }
        return String.valueOf((int)hours);
    }
    
    private void openContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            contextMenu.show(this, e.getX(), e.getY());
        }
    }
    
    private class MyMouseListener extends MouseAdapter {
        /**
         * Toggle displaying stuff on mouse-click.
         *
         * Left-click: Keep info displayed even when outside the component
         * Right-click: Switch between 0-max and min-max rendering
         *
         * @param e
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    fixedHoverEntry = false;
                    setFixedStartAt(hoverEntry);
                } else {
                    long actualHoverEntry = findHoverEntry(e.getPoint());
                    fixedHoverEntry = actualHoverEntry != -1;
                    if (hoverEntry != actualHoverEntry) {
                        updateHoverEntry(e.getPoint());
                    }
                }
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            mouseEntered = true;
            repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseEntered = false;
            // No entry selected since mouse isn't in the window
            // (entries near the border would still be selected)
            if (fixedHoverEntry) {
                return;
            }
            if (hoverEntry > 0 && listener != null) {
                listener.noItemSelected();
            }
            hoverEntry = -1;
            repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            openContextMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            openContextMenu(e);
        }

        /**
         * Detect if mouse is moved over a point.
         *
         * @param e
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            if (!fixedHoverEntry) {
                updateHoverEntry(e.getPoint());
            }
        }
    }
    
    /**
     * Finds the key (time) of the currently hovered entry, or -1 if none is
     * hovered.
     * 
     * @param p The current position of the mouse.
     * @return 
     */
    private long findHoverEntry(Point p) {
        double smallestDistance = HOVER_RADIUS;
        long foundHoverEntry = -1;
        for (Point point : locations.keySet()) {
            double distance = p.distance(point);
            if (distance < HOVER_RADIUS) {
                if (distance < smallestDistance) {
                    foundHoverEntry = locations.get(point);
                    smallestDistance = distance;
                }
            }
        }
        return foundHoverEntry;
    }
    
    /**
     * Update the hoverEntry with the currently hovered entry, or none if none
     * is hovered. Repaints and informs listeners if something has changed.
     * 
     * @param p Where the mouse is currently.
     */
    private void updateHoverEntry(Point p) {
        long hoverEntryBefore = hoverEntry;
        hoverEntry = findHoverEntry(p);
        // If something has changed, then redraw.
        if (hoverEntry != hoverEntryBefore) {
            repaint();
            if (listener != null) {
                if (hoverEntry == -1) {
                    listener.noItemSelected();
                } else {
                    StreamInfoHistoryItem item = history.get(hoverEntry);
                    if (item == null) {
                        /**
                         * This shouldn't happen, because the hover entry is set
                         * just before and selected from the current data (and
                         * it should all be in the EDT), but apparently it still
                         * happens on rare occasions.
                         */
                        LOGGER.warning("Hovered Entry "+hoverEntry+" was null");
                        hoverEntry = -1;
                    } else {
                        listener.itemSelected(item);
                    }
                }
            }
        }
    }
    
}

