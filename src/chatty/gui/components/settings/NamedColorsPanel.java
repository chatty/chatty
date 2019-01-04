package chatty.gui.components.settings;

import chatty.util.colors.HtmlColors;
import chatty.gui.NamedColor;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.colorchooser.AbstractColorChooserPanel;

/**
 * A grid of colors that can be selected by clicking on them and show their
 * color name and values on hover.
 * 
 * @author tduva
 */
class NamedColorsPanel extends AbstractColorChooserPanel {
    
    private final List<NamedColor> namedColors = HtmlColors.getNamedColors();
    private final ColorArea colorArea = new ColorArea();
    
    private NamedColor selectedColor = null;

    @Override
    public void updateChooser() {
        selectedColor = null;
        Color color = getColorFromModel();
        for (NamedColor namedColor : namedColors) {
            if (namedColor.equals(color)) {
                selectedColor = namedColor;
                break;
            }
        }
        colorArea.repaint();
    }

    @Override
    protected void buildChooser() {
        setLayout(new BorderLayout());
        //add(new JLabel("Click color to select."), BorderLayout.NORTH);
        add(colorArea, BorderLayout.CENTER);
        setPreferredSize(new Dimension(600,300));
        setMinimumSize(new Dimension(600,300));
        setMaximumSize(new Dimension(600,500));
        //System.out.println(namedColors.size());
    }

    @Override
    public String getDisplayName() {
        return "Named Colors";
    }

    @Override
    public Icon getSmallDisplayIcon() {
        return null;
    }

    @Override
    public Icon getLargeDisplayIcon() {
        return null;
    }
    
    /**
     * The actual area where the colors are drawn and can be selected.
     */
    private class ColorArea extends JComponent implements MouseMotionListener, MouseListener {
        
        private static final int PADDING = 2;
        private static final int MARGIN = 10;
        private static final int MARGIN_TOP = 40;
        private static final int COLS = 10;
        private static final int ROWS = 15;
        
        private final Font FONT = new Font("Arial", Font.PLAIN, 12);
        private final Font TITLE_FONT = new Font("Arial", Font.BOLD, 15);
        private final Color BACKGROUND_COLOR = new Color(250,250,250);
        
        private final List<ColorToDraw> colorsToDraw = new ArrayList<>();
        
        private ColorToDraw hoveredColor;
        private int height;
        private int width;
        private int elementWidth;
        private int elementHeight;
        private int compHeight;
        private int compWidth;

        ColorArea() {
            addMouseMotionListener(this);
            addMouseListener(this);
            makePositions();
        }
        
        /**
         * Calculate {@code Rectangle}s to draw the colors in and save the
         * {@code NamedColor}s and {@code Rectangle}s in {@code ColorToDraw}
         * objects.
         */
        private void makePositions() {
            // Only recalculate if size has changed
            if (compHeight == getHeight() && compWidth == getWidth()) {
                return;
            }
            compHeight = getHeight();
            compWidth = getWidth();

            int cols = COLS;
            int rows = ROWS;
            
            colorsToDraw.clear();
            
            int padding = PADDING;
            int margin = MARGIN;
            int marginTop = MARGIN_TOP;
            
            int x = margin;
            int y = marginTop;
            
            width = getWidth() - (cols - 1) * padding - margin*2;
            height = getHeight() - (rows - 1) * padding - margin - marginTop;
            
            elementWidth = width / cols;
            elementHeight = height / rows;
            
            Iterator<NamedColor> it = namedColors.iterator();
            
            for (int col=0;col<cols;col++) {
                for (int row=0;row<rows;row++) {
                    if (it.hasNext()) {
                        NamedColor c = it.next();
                        Rectangle r = new Rectangle(x, y, elementWidth, elementHeight);
                        colorsToDraw.add(new ColorToDraw(c, r));
                    }
                    y += elementHeight + padding;
                }
                x += elementWidth + padding;
                y = marginTop;
            }
        }
        
        /**
         * Draw a single {@code ColorToDraw} object, using its {@code Rectangle}
         * to draw a rectangle of the color.
         * 
         * @param g
         * @param color 
         */
        private void drawColor(Graphics g, ColorToDraw color) {
            Rectangle r = color.r;
            Color c = color.color;
            int x = r.x;
            int y = r.y;
            int w = r.width;
            int h = r.height;
            
            // Draw color rectangle and border
            g.setColor(c);
            g.fillRect(x, y, w, h);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, w, h);

            // Draw different border if this is the selected color
            g.setColor(Color.DARK_GRAY);
            if (c.equals(selectedColor)) {
                g.drawRect(x, y, w, h);
                g.drawRect(x - 1, y - 1, w + 2, h + 2);
            }
        }
        
        private void drawHoveredColor(Graphics g) {
            if (hoveredColor == null) {
                return;
            }
            
            // Get color coordinates
            Rectangle r = hoveredColor.r;
            int x = r.x;
            int y = r.y;
            int h = r.height;
            
            // Create description text and calculate text width and height
            String text = hoveredColor.color.getName()+" ("+hoveredColor.color.getRgbString()+")";
            FontMetrics fontMetrics = g.getFontMetrics(FONT);
            g.setFont(FONT);
            int textWidth = fontMetrics.stringWidth(text);
            int textHeight = fontMetrics.getHeight();
            
            // Change position if text doesn't fit
            if (x + textWidth > width) {
                x = x - (x + textWidth - width);
            }
            
            // Draw text background
            g.setColor(Color.BLACK);
            g.fillRect(x - 5, y - h - 8, textWidth + 10, textHeight + 8);
            g.setColor(Color.WHITE);
            g.fillRect(x - 4, y - h - 7, textWidth + 8, textHeight + 6);

            // Draw text
            g.setColor(Color.BLACK);
            g.drawString(text, x, y - h + textHeight - 7);
        }
        
        private void drawTitle(Graphics g) {
            FontMetrics fontMetrics = g.getFontMetrics(TITLE_FONT);
            g.setFont(TITLE_FONT);
            g.setColor(Color.BLACK);
            String text = "Click Color to select (X11 colors)";
            int textWidth = fontMetrics.stringWidth(text);
            int y = 10 + fontMetrics.getHeight();
            int x = getWidth() / 2 - textWidth / 2;
            g.drawString(text, x, y);
        }
        
        @Override
        public void paintComponent(Graphics g) {
            makePositions();
            
            // Anti-Aliasing
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw stuff that should be behind first
            
            // Draw background
            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Draw title and colors
            drawTitle(g);
            for (ColorToDraw c : colorsToDraw) {
                drawColor(g, c);
            }
            drawHoveredColor(g);
        }
        
        /**
         * Find the {@code ColorToDraw} that is drawn on this {@code Point}.
         * 
         * @param p The point to get the color from
         * @return The {@code ColorToDraw} or {@code null} if no color is drawn
         * at that point
         */
        private ColorToDraw getColorFromPoint(Point p) {
            for (ColorToDraw color : colorsToDraw) {
                if (color.r.contains(p)) {
                    return color;
                }
            }
            return null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            hoveredColor = null;
            ColorToDraw c = getColorFromPoint(e.getPoint());
            if (c != null) {
                hoveredColor = c;
            }
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            
        }

        @Override
        public void mousePressed(MouseEvent e) {
            ColorToDraw c = getColorFromPoint(e.getPoint());
            if (c != null) {
                getColorSelectionModel().setSelectedColor(c.color);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            
        }

        @Override
        public void mouseExited(MouseEvent e) {
            
        }
    }
    
    /**
     * Saves a {@code NamedColor} together with a {@code Rectangle} where to
     * draw it.
     */
    private static class ColorToDraw {

        public final NamedColor color;
        public final Rectangle r;
        
        ColorToDraw(NamedColor color, Rectangle r) {
            this.color = color;
            this.r = r;
        }
    }
    
}