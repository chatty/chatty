
package chatty.gui.notifications;

import chatty.Helper;
import chatty.gui.MainGui;
import chatty.util.ActivityListener;
import chatty.util.ActivityTracker;
import chatty.util.IconManager;
import chatty.util.colors.ColorCorrection;
import chatty.util.colors.ColorCorrectionNew;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;

/**
 * A notification which is displayed in a window.
 * 
 * @author tduva
 */
public class NotificationWindow {
    
    private static final int SECOND = 1000;
    
    private static final ImageIcon ICON = IconManager.getNotificationIcon();

    private static final Border PADDING_BORDER = BorderFactory.createEmptyBorder(9, 9, 9, 8);
    private static final Border LINE_BORDER_DARK = BorderFactory.createLineBorder(new Color(50, 50, 50), 1);
    private static final Border BORDER_DARK = BorderFactory.createCompoundBorder(LINE_BORDER_DARK, PADDING_BORDER);

    private static final int MAX_WIDTH = 190;

    private static final String HTML = "<html><body style='font-weight: normal;'>";
    private static final String HTML_FIXED_WIDTH = "<html><body style='width:" + MAX_WIDTH + "px;font-weight: normal;'>";
    
    /**
     * Maximum number of characters to display as message
     */
    private static final int MAX_MESSAGE_LENGTH = 250;
    
    private static final float DEFAULT_OPACITY = 0.99f;

    private static final int UPDATE_TIME_INTERVAL = 1*60*1000;
    
    private static final boolean SHORTER_AFTER_ACTIVITY = true;
    private static final int SHORTER_CUTOFF = 2*SECOND;
    
    /**
     * Times
     */
    private int timeout = 10*1000;
    private int fallbackTimeout = 30*60*1000;
    private int activityTime = 60*1000;
    
    private long visibleSince = 0;
    private final long createdAt;
    
    private boolean closed = false;

    /**
     * References
     */
    private final JWindow window;
    private final NotificationWindowListener listener;
    
    private boolean translucencySupported = false;
    private HideMethod hideMethod = HideMethod.FADE_OUT;
    
    private final JLabel timeLabel;

    /**
     * Timers
     */
    private Timer fadeOutTimer;
    private Timer fallbackTimer;
    private Timer regularTimer;
    private final Timer updateTimeTimer;
    
    private ActivityListener activityListener;

    /**
     * A notification.
     * 
     * @param title
     * @param message
     * @param foreground
     * @param background
     * @param listener Listener that gets informed about the notification state
     */
    public NotificationWindow(String title,
            String message,
            Color foreground,
            Color background,
            final NotificationWindowListener listener) {
        // Window
        window = new JWindow();
        window.setFocusable(false);
        window.setFocusableWindowState(false);
        window.setAutoRequestFocus(false);
        window.setAlwaysOnTop(true);
        window.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        checkTranslucencySupport();
        setOpacity(DEFAULT_OPACITY);

        // Panel
        final JPanel panel = new JPanel(new GridBagLayout());
        /**
         * Keep black-ish (as before) for light backgrounds, but modify
         * background color for dark backgrounds (might look a bit better than
         * a grey around the same lightness, tried a few different methods).
         * 
         * Only do on dark enough colors, so there is always enough contrast
         * between the border and the background.
         */
        if (ColorCorrection.getBrightness(background) < 100) {
            Color bColor = ColorCorrectionNew.toLightness(background, 140);
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(bColor), PADDING_BORDER));
        } else {
            panel.setBorder(BORDER_DARK);
        }
        panel.setOpaque(true);
        panel.setBackground(background);

        GridBagConstraints gbc = new GridBagConstraints();

        // Icon
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(ICON);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(2, 0, 0, 5);
        panel.add(iconLabel, gbc);
        
        timeLabel = new JLabel("55m");
        timeLabel.setFont(timeLabel.getFont().deriveFont(10.0f));
        timeLabel.setForeground(ColorCorrectionNew.offset(foreground, 0.7f));
        //gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(timeLabel, gbc);
        
        // Content Label
        title = Helper.htmlspecialchars_encode(title);
        message = Helper.htmlspecialchars_encode(message);
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH)+"[..]";
        }
        JLabel content = new JLabel(makeContent(title, message, false));
        content.setForeground(foreground);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        panel.add(content, gbc);

        // Finish Window
        window.add(panel);
        window.setMaximumSize(new Dimension(MAX_WIDTH, 100));
        
        // Set fixed width if window width exceeds the max width
        if (window.getPreferredSize().width > MAX_WIDTH) {
            content.setText(makeContent(title, message, true));
        }
        
        // Listeners
        this.listener = listener;
        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    if (listener != null) {
                        listener.notificationAction(NotificationWindow.this);
                    }
                }
                close();
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                NotificationWindow.this.mouseEntered();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                NotificationWindow.this.mouseExited();
            }
        });
        window.pack();
        
        createdAt = System.currentTimeMillis();
        updateCreatedTime();
        
        // Update Time Timer
        updateTimeTimer = new Timer(UPDATE_TIME_INTERVAL, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateCreatedTime();
            }
        });
        updateTimeTimer.setRepeats(true);
        updateTimeTimer.start();
    }
    

    
    public long getVisibleTime() {
        return System.currentTimeMillis() - visibleSince;
    }

    public boolean isVisible() {
        return visibleSince > 0 && !closed;
    }
    
    public void setActivityTime(int time) {
        this.activityTime = time;
    }
    
    public void setHideMethod(HideMethod method) {
        this.hideMethod = method;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public void setLocation(Point location) {
        window.setLocation(location);
    }

    public int getHeight() {
        return window.getHeight();
    }

    public Dimension getSize() {
        return window.getSize();
    }

    public void moveVertical(int offset) {
        Point location = window.getLocation();
        location.y += offset;
        window.setLocation(location);
    }

    public void show() {
        if (activityTime <= 0 || ActivityTracker.getLastActivityAgo() < activityTime) {
            startTimer(false);
        } else {
            activityListener = new MyActivityListener();
            ActivityTracker.addActivityListener(activityListener);
        }
        startFallbackTimer();
        visibleSince = System.currentTimeMillis();
        window.setVisible(true);
    }

    /**
     * Perform the configured hide action.
     */
    public void hide() {
        switch (hideMethod) {
            case FADE_OUT:
                fadeOut();
                break;
            case CLOSE:
                close();
                break;
        }
    }
    
    /**
     * Sets the time for the fallback timer and restarts if it is already
     * running.
     *
     * @param timeout
     */
    public void setFallbackTimeout(int timeout) {
        fallbackTimeout = timeout;
        if (fallbackTimer != null && fallbackTimer.isRunning()) {
            fallbackTimer.stop();
            startFallbackTimer();
        }
    }

    /**
     * Close the window immediately and cleanup.
     */
    public void close() {
        stopTimers();
        closed = true;
        window.dispose();
        if (listener != null) {
            listener.notificationRemoved(this);
        }
        if (activityListener != null) {
            ActivityTracker.removeActivityListener(activityListener);
        }
    }
    
    /**
     * Start the timer to fade out the window.
     */
    private void fadeOut() {
        if (fadeOutTimer == null) {
            fadeOutTimer = new Timer(80, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    performFadeOut();
                }
            });
            fadeOutTimer.start();
        }
    }
    


    /**
     * Starts the regular timer that will hide the window after it's finished.
     */
    private void startTimer(boolean afterActivity) {
        if (regularTimer == null && !closed) {
            int timerDelay = timeout;
            if (SHORTER_AFTER_ACTIVITY && afterActivity
                    && timerDelay > SHORTER_CUTOFF) {
                timerDelay *= 0.5;
                if (timerDelay < SHORTER_CUTOFF) {
                    timerDelay = SHORTER_CUTOFF;
                }
            }
            regularTimer = new Timer(timerDelay, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    hide();
                }
            });
            regularTimer.setRepeats(false);
            regularTimer.start();
        }
    }

    private void startFallbackTimer() {
        fallbackTimer = new Timer(fallbackTimeout, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hide();
            }
        });
        fallbackTimer.setRepeats(false);
        fallbackTimer.start();
    }

    private String makeContent(String title, String message, boolean fixedWidth) {
        return (fixedWidth ? HTML_FIXED_WIDTH : HTML) + "<p style='margin-bottom: 1px;'><b>" + title + "</b></p>"
                + "<p>" + message + "</p>";
    }

    /**
     * Lower the opacity by a bit or close the window if opacity is low enough.
     */
    private void performFadeOut() {
        if (!translucencySupported) {
            close();
        } else {
            float opacity = window.getOpacity();
            if (opacity < 0.1) {
                close();
            } else {
                setOpacity(opacity - 0.05f);
            }
        }
    }
    
    /**
     * Sets the opacity of the window, if supported.
     * 
     * @param opacity 
     */
    private void setOpacity(float opacity) {
        if (translucencySupported) {
            if (opacity < 0) {
                opacity = 0;
            }
            window.setOpacity(opacity);
        }
    }

    /**
     * Check if translucecncy is supported on this device.
     */
    private void checkTranslucencySupport() {
        translucencySupported = window.getGraphicsConfiguration().getDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
    }
    

    
    private void mouseEntered() {
        setOpacity(DEFAULT_OPACITY);
        stopTimers();
    }
    
    private void mouseExited() {
        if (!closed && regularTimer != null) {
            regularTimer.restart();
        }
    }

    /**
     * Stops all timers that are running.
     */
    private void stopTimers() {
        stopTimer(fadeOutTimer);
        stopTimer(regularTimer);
        stopTimer(fallbackTimer);
        stopTimer(updateTimeTimer);
        fadeOutTimer = null;
    }

    /**
     * Stops the given timer if it is running.
     * 
     * @param timer 
     */
    private void stopTimer(Timer timer) {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
    }

    private void updateCreatedTime() {
        long ago = (System.currentTimeMillis() - createdAt) / 1000;
        timeLabel.setText(makeTimeString(ago));
    }

    private String makeTimeString(long time) {
        if (time < 60) {
            return "";
        }
        long minutes = time / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        return hours + "h";
    }
    
    public enum HideMethod {
        FADE_OUT, CLOSE
    }
    
    private class MyActivityListener implements ActivityListener {

        @Override
        public void activity() {
            startTimer(true);
        }
    }
}
