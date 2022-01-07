
package chatty.gui;

import chatty.util.dnd.DockContentContainer;
import chatty.util.dnd.DockManager;
import chatty.util.dnd.DockTabComponent;
import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.BaseTabbedPaneUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.border.Border;

/**
 * Dock content container that provides a styled tab component with various
 * features. For examples used to indicate new messages or live stream status.
 * 
 * @author tduva
 * @param <T>
 */
public class DockStyledTabContainer<T extends JComponent> extends DockContentContainer<T> {

        //--------------------------
        // Setting Constants
        //--------------------------
        // Used to encode several booleans in one integer
        public final static int BOLD = 1 << 0;
        public final static int ITALIC = 1 << 1;
        public final static int COLOR1 = 1 << 2;
        public final static int COLOR2 = 1 << 3;
        public final static int DOT1 = 1 << 4;
        public final static int DOT2 = 1 << 5;
        public final static int ASTERISK = 1 << 6;
        public final static int LINE = 1 << 7;
        public final static int CUSTOM_COLOR = 1 << 8;
        public final static int CUSTOM_COLOR_START_BIT = 16;
        
        //--------------------------
        // Borders
        //--------------------------
        private static final Border BORDER_INACTIVE = BorderFactory.createEmptyBorder(0, 3, 0, 3);
        private static final Border BORDER_ACTIVE = BorderFactory.createEmptyBorder(0, 0, 0, 6);
        
        //--------------------------
        // References
        //--------------------------
        private final TabComponent tab;
        
        //--------------------------
        // Status variables
        //--------------------------
        private boolean isLive;
        private long liveSetting;
        private boolean hasMessages;
        private long messageSetting;
        private boolean hasHighlight;
        private long highlightSetting;
        private boolean hasStatus;
        private long statusSetting;
        private boolean isActive;
        private long activeSetting;
        private boolean isJoining;
        
        //--------------------------
        // Properties
        //--------------------------
        private Color foreground;
        private Color defaultForeground;
        private Font defaultFont;
        private boolean fontNoBold;
        private boolean fontNoItalic;
        private boolean fontBold;
        private boolean fontItalic;
        private Border border;
        private String suffix;
        
        //--------------------------
        // Drawing variables
        //--------------------------
        private boolean dot1;
        private boolean dot2;
        private boolean line;
        private Color dot1Color;
        private Color dot2Color;
        private Color lineColor;
        
        public DockStyledTabContainer(T content, String title, DockManager m) {
            super(title, content, m);
            tab = new TabComponent();
            defaultForeground = getForegroundColor();
            defaultFont = tab.getFont();
        }
        
        public void setActive(boolean isActive) {
            if (isActive != this.isActive) {
                this.isActive = isActive;
                update();
            }
        }
        
        public void setLive(boolean isLive) {
            if (isLive != this.isLive) {
                this.isLive = isLive;
                update();
            }
        }
        
        public boolean isLive() {
            return isLive;
        }
        
        public void setNewMessage(boolean hasMessage) {
            if (hasMessage != this.hasMessages) {
                this.hasMessages = hasMessage;
                update();
            }
        }
        
        public boolean hasNewMessages() {
            return hasMessages;
        }
        
        public void setNewHighlight(boolean hasHighlight) {
            if (hasHighlight != this.hasHighlight) {
                this.hasHighlight = hasHighlight;
                update();
            }
        }
        
        public boolean hasNewHighlight() {
            return hasHighlight;
        }
        
        public void setNewStatus(boolean newStatus) {
            if (newStatus != this.hasStatus) {
                this.hasStatus = newStatus;
                update();
            }
        }
        
        public void resetNew() {
            if (hasMessages || hasHighlight || hasStatus) {
                hasMessages = false;
                hasHighlight = false;
                hasStatus = false;
                update();
            }
        }

        public void setJoining(boolean isJoining) {
            if (isJoining != this.isJoining) {
                this.isJoining = isJoining;
                update();
            }
        }
        
        public boolean isJoining() {
            return isJoining;
        }
        
        public void setSettings(long liveSetting, long messageSetting, long highlightSetting, long statusSetting, long activeSetting, long maxWidth) {
            this.liveSetting = liveSetting;
            this.messageSetting = messageSetting;
            this.highlightSetting = highlightSetting;
            this.statusSetting = statusSetting;
            this.activeSetting = activeSetting;
            tab.setMaxWidth(maxWidth);
            update();
        }
        
        private void update() {
            border = null;
            dot1 = false;
            dot2 = false;
            line = false;
            fontNoItalic = false;
            fontNoBold = false;
            fontItalic = false;
            fontBold = false;
            dot1Color = new Color(255, 100, 100);
            dot2Color = new Color(100, 100, 255);
            lineColor = defaultForeground;
            foreground = defaultForeground;
            suffix = "";
            update(isLive, liveSetting);
            update(hasMessages, messageSetting);
            update(hasStatus, statusSetting);
            update(hasHighlight, highlightSetting);
            update(isActive, activeSetting);
            int fontStyle = defaultFont.getStyle();
            if (fontNoItalic) {
                fontStyle = fontStyle & ~Font.ITALIC;
            }
            if (fontNoBold) {
                fontStyle = fontStyle & ~Font.BOLD;
            }
            if (fontItalic) {
                fontStyle = fontStyle | Font.ITALIC;
            }
            if (fontBold) {
                fontStyle = fontStyle | Font.BOLD;
            }
            tab.setBorder(border);
            tab.setFont(defaultFont.deriveFont(fontStyle));
            // Color
            if (isJoining && foreground != null) {
                foreground = new Color(foreground.getRed(), foreground.getGreen(), foreground.getBlue(), 140);
            }
            if (!Objects.equals(foreground, tab.getForeground())) {
                tab.setForeground(foreground);
            }
            // Title
            String newText = getTitle()+suffix;
            if (!newText.equals(tab.getText())) {
                tab.setText(newText);
            }
        }
        
        private void update(boolean enabled, long setting) {
            if (enabled) {
                Color customColor = null;
                if (isEnabled(setting, CUSTOM_COLOR)) {
                    customColor = decodeColor(setting, CUSTOM_COLOR_START_BIT);
                }
                if (isEnabled(setting, DOT1)) {
                    border = BORDER_ACTIVE;
                    dot1 = true;
                    if (customColor != null) {
                        dot1Color = customColor;
                    }
                }
                if (isEnabled(setting, DOT2)) {
                    border = BORDER_ACTIVE;
                    dot2 = true;
                    if (customColor != null) {
                        dot2Color = customColor;
                    }
                }
                if (isEnabled(setting, LINE)) {
                    line = true;
                    if (customColor != null) {
                        lineColor = customColor;
                    }
                }
                if (isEnabled(setting, ITALIC)) {
                    fontItalic = true;
                }
                if (isEnabled(setting, BOLD)) {
                    fontBold = true;
                }
                if (isEnabled(setting, COLOR1)) {
                    if (customColor != null) {
                        foreground = customColor;
                    }
                    else {
                        foreground = LaF.getTabForegroundUnread();
                    }
                }
                if (isEnabled(setting, COLOR2)) {
                    if (customColor != null) {
                        foreground = customColor;
                    }
                    else {
                        foreground = LaF.getTabForegroundHighlight();
                    }
                }
                if (isEnabled(setting, ASTERISK)) {
                    suffix = "*";
                }
            }
            else {
                if (isEnabled(setting, DOT1) && border == null) {
                    border = BORDER_INACTIVE;
                }
                if (isEnabled(setting, DOT2) && border == null) {
                    border = BORDER_INACTIVE;
                }
                if (isEnabled(setting, ITALIC)) {
                    fontNoItalic = true;
                }
                if (isEnabled(setting, BOLD)) {
                    fontNoBold = true;
                }
            }
        }
        
        /**
         * Check if the given option is enabled in the setting value.
         * 
         * @param settingValue
         * @param optionConstant
         * @return 
         */
        private boolean isEnabled(long settingValue, long optionConstant) {
            return (settingValue & optionConstant) != 0;
        }
        
        public static long encodeColor(Color color, long value, int startBit) {
            value |= (long) color.getRed() << startBit;
            value |= (long) color.getGreen() << startBit + 8;
            value |= (long) color.getBlue() << startBit + 16;
            return value;
        }

        public static Color decodeColor(long value, int startBit) {
            int red = (int) ((value >> startBit) & ((1 << 8) - 1));
            int green = (int) ((value >> startBit + 8) & ((1 << 8) - 1));
            int blue = (int) ((value >> startBit + 16) & ((1 << 8) - 1));
            return new Color(red, green, blue);
        }
        
        @Override
        public void setTitle(String title) {
            if (title.isEmpty()) {
                title = "-";
            }
            super.setTitle(title);
            tab.setText(title);
        }
        
        @Override
        public void setForegroundColor(Color color) {
            tab.setForeground(color);
        }
        
        /**
         * Provides a customized component that draws the tab.
         * 
         * @return 
         */
        @Override
        public DockTabComponent getTabComponent() {
            return tab;
        }

        private class TabComponent extends JLabel implements DockTabComponent {
            
            private long maxWidth;
            
            @Override
            public void paintComponent(Graphics g) {
                int b = (int) (getHeight() * 0.2);
                if (dot1) {
                    g.setColor(dot1Color);
                    g.fillRect(getWidth() - 4, b, 4, 4);
                }
                if (dot2) {
                    g.setColor(dot2Color);
                    g.fillRect(getWidth() - 4, b+6, 4, 4);
                }
//                if (line && getTitle().equals("#tduvatest")) {
////                    if (getTitle().equals("#tduvatest")) {
//                        g.setColor(new Color(defaultForeground.getRed(), defaultForeground.getGreen(), defaultForeground.getBlue(), 180));
//                        int a = 10;
//                        g.drawLine(10, 0, getWidth() - a - 1, 0);
//                        for (int x = 0; x < a; x++) {
//                            int alpha1 = 40 + x * 14;
//                            int alpha2 = 180 - x * 14;
////                            System.out.println(x+" "+alpha1+" "+alpha2);
//                            g.setColor(new Color(defaultForeground.getRed(), defaultForeground.getGreen(), defaultForeground.getBlue(), alpha1));
//                            g.drawLine(x, 0, x, 0);
//                            g.setColor(new Color(defaultForeground.getRed(), defaultForeground.getGreen(), defaultForeground.getBlue(), alpha2));
//                            g.drawLine(x + getWidth() - a, 0, x + getWidth() - a, 0);
//                        }
//                    }
//                    else {
//                        g.setColor(new Color(defaultForeground.getRed(), defaultForeground.getGreen(), defaultForeground.getBlue(), 180));
//                        g.drawLine(0, 0, getWidth(), 0);
//                    }
//                }
                if (line) {
                    paintLine(g, 0, 1, 180, 20);
                    paintLine(g, 1, 4, 100, 0);
                }
                super.paintComponent(g);
            }
            
            private void paintLine(Graphics g, int y, int fadeLength, int baseAlpha, int lowestAlpha) {
                fadeLength = Math.min(fadeLength, getWidth() / 3);
                fadeLength = fadeLength > 0 ? fadeLength : 1;
                int alphaStep = (baseAlpha - lowestAlpha) / fadeLength;
                g.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), baseAlpha));
                // Draw most of the line
                g.drawLine(fadeLength, y, getWidth() - fadeLength - 1, y);
                for (int i = 0; i < fadeLength; i++) {
                    // Draw one pixel each left and right
                    int xLeft = i;
                    int xRight = getWidth() - 1 - i;
                    int pixelAlpha = lowestAlpha + i * alphaStep;
                    g.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), pixelAlpha));
                    g.drawLine(xLeft, y, xLeft, y);
                    g.drawLine(xRight, y, xRight, y);
//                    System.out.println(xLeft + " " + xRight + " " + pixelAlpha);
                }
            }

            /**
             * Called by the tab pane when a relevant state might have changed
             * (e.g. tab changed).
             * 
             * @param pane
             * @param index 
             */
            @Override
            public void update(JTabbedPane pane, int index) {
                boolean changed = false;
                
                //--------------------------
                // Color
                //--------------------------
                Color foreground;
                if (pane.getUI() instanceof BaseTabbedPaneUI && pane.getSelectedIndex() == index) {
                    // Special for JTattoo LaF
                    foreground = AbstractLookAndFeel.getTheme().getTabSelectionForegroundColor();
                }
                else {
                    foreground = pane.getForegroundAt(index);
                }
                if (!Objects.equals(foreground, defaultForeground)) {
                    defaultForeground = foreground;
                    changed = true;
                }
                
                //--------------------------
                // Font
                //--------------------------
                Font font = pane.getFont();
                if (!Objects.equals(font, defaultFont)) {
                    defaultFont = font;
                    changed = true;
                }
                
                //--------------------------
                // Update
                //--------------------------
                if (changed) {
                    DockStyledTabContainer.this.update();
                }
            }
            
            @Override
            public JComponent getComponent() {
                return this;
            }
            
            public void setMaxWidth(long maxWidth) {
                this.maxWidth = maxWidth;
            }
            
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                if (maxWidth > 0 && d.width > maxWidth) {
                    return new Dimension((int) maxWidth, d.height);
                }
                return d;
            }
            
        }

    }
