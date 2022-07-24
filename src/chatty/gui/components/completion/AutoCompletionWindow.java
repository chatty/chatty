
package chatty.gui.components.completion;

import chatty.Helper;
import chatty.gui.components.completion.AutoCompletionServer.CompletionItems;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Provides the window for auto completion.
 * 
 * @author tduva
 */
public class AutoCompletionWindow {
    
    private boolean show;
    private int maxResultsShown = 5;
    private Color foregroundColor = Color.BLACK;
    private Color backgroundColor = new Color(230, 230, 230);
    
    // Listeners/References
    private final ComponentListener componentListener;
    
    private final FocusListener focusListener;
    private Window containingWindow;
    
    private JWindow infoWindow;
    private JList<AutoCompletionServer.CompletionItem> list;
    private final MyListModel<AutoCompletionServer.CompletionItem> listData = new MyListModel<>();
    private final JLabel countLabel = new JLabel();
    private final JLabel helpLabel = new JLabel();
    private JScrollPane infoScroll;
    private final MyRenderer listRenderer = new MyRenderer();
    private int startPos;
    private final JTextComponent textField;
    private final BiConsumer<Integer, MouseEvent> clickListener;
    
    public AutoCompletionWindow(JTextComponent textField, BiConsumer<Integer, MouseEvent> clickListener) {
        this.clickListener = clickListener;
        this.textField = textField;
        /**
         * Hide and show the info popup depending on whether the textfield has
         * focus.
         */
        focusListener = new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                reshow();
            }

            @Override
            public void focusLost(FocusEvent e) {
                hide();
            }
        };
        textField.addFocusListener(focusListener);
        
        /**
         * Listener to attach to the textField and the main containing window,
         * so when any of that moves or gets resized, the info window is hidden.
         * 
         * The componentShown() and componentHidden() methods may not do
         * anything depending on the specific use, but keeping them there just
         * in case.
         */
        componentListener = new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                reposition();
            }

            @Override
            public void componentShown(ComponentEvent e) {
                infoWindow.setVisible(false);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                infoWindow.setVisible(false);
            }
        };
    }
    
    //=================
    // Setting methods
    //=================
    
    public void setFont(Font font) {
        listRenderer.setFont(font);
        countLabel.setFont(font);
        helpLabel.setFont(font.deriveFont((float) (font.getSize() * 0.8)));
    }
    
    public void setForegroundColor(Color color) {
        this.foregroundColor = color;
        listRenderer.setForeground(color);
        countLabel.setForeground(color);
        helpLabel.setForeground(color);
    }
    
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        if (infoWindow != null) {
            list.setBackground(color);
            infoWindow.getContentPane().setBackground(color);
        }
    }
    
    public void setHighlightColor(Color color) {
        listRenderer.setHighlightBackground(color);
    }
    
        /**
     * How many results to show in the info popup.
     * 
     * @param max The maximum number of results to show
     */
    public void setMaxResultsShown(int max) {
        this.maxResultsShown = max;
    }
    
    /**
     * Set the fixed width/height for the list elements. The height is also used
     * for the min icon width of elements.
     * 
     * @param width
     * @param height 
     */
    public void setCellSize(int width, int height) {
        cellWidth = width;
        cellHeight = height;
    }
    
    //===================
    // Show/hide methods
    //===================
    
    protected void init(CompletionItems results, String commonPrefix, int startPos) {
        this.startPos = startPos;
        if (infoWindow == null || !infoWindow.isVisible()) {
            createInfoWindow();
        }
        listRenderer.setCommonPrefix(commonPrefix);
        listData.setData(results.items);
    }

    protected void show(int index, boolean scroll) {
        show = true;
        if (infoWindow == null) {
            return;
        }
        setCompletionInfoIndex(index, listData.getSize(), maxResultsShown, scroll);
        updateHelp(index);
        if (!infoWindow.isVisible()) {
            updateWindowSettings();
            SwingUtilities.invokeLater(() -> {
                setWindowPosition(startPos);
            });
        }
    }
    
    protected boolean isShowing() {
        return infoWindow != null && infoWindow.isVisible();
    }
    
    private void reposition() {
        if (infoWindow != null && infoWindow.isVisible()) {
            setWindowPosition(startPos);
        }
    }
    
    private void setWindowPosition(int index) {
        
        list.setVisibleRowCount(maxResultsShown);
        
        /**
         * Using getMagicCaretPosition() to get the location didn't always seem
         * to work, possibly depending on timing?
         */
        Rectangle r;
        try {
            r = textField.modelToView(index);
        } catch (BadLocationException ex) {
            System.out.println("null lol");
            return;
        }

        // No location found, so don't show window
        if (r == null) {
            return;
        }
        Point location = new Point(r.x, r.y);
        
        infoScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        
        infoWindow.pack();

        // Determine and set new position
        location.x -= 4;
        if (location.x + infoWindow.getWidth() > textField.getWidth()) {
            location.x = textField.getWidth() - infoWindow.getWidth();
        } else if (location.x < 8) {
            location.x = 8;
        }
        location.y -= infoWindow.getHeight();
        SwingUtilities.convertPointToScreen(location, textField);
        infoWindow.setLocation(location);

        infoWindow.setVisible(true);
    }
    
    private void reshow() {
        if (infoWindow != null && !infoWindow.isVisible() && show) {
            infoWindow.setVisible(true);
        }
    }
    
    private void hide() {
        if (infoWindow != null && infoWindow.isVisible()) {
            infoWindow.setVisible(false);
        }
    }

    protected void close() {
        show = false;
        hide();
    }
    
    /**
     * This should be called when the AutoCompletion is no longer used, so it
     * can be gargabe collected.
     */
    public void cleanUp() {
        if (containingWindow != null) {
            containingWindow.removeComponentListener(componentListener);
        }
        textField.removeFocusListener(focusListener);
        if (infoWindow != null) {
            infoWindow.dispose();
            infoWindow = null;
        }
    }
    
    /**
     * TODO: More help texts? Get outside of this class for localization
     * TODO: General cleanup/testing
     * TODO: Double-click inserts several entries? Or something like that..
     * TODO: Popup closes when input switches to next line from completion
     */
    protected void updateHelp(int resultIndex) {
        if (LocalDateTime.now().get(ChronoField.MINUTE_OF_HOUR) > 50) {
            helpLabel.setText("Shift-click to insert several results");
        } else if (resultIndex == -1) {
            helpLabel.setText("Use TAB or mouse to select");
        } else {
            helpLabel.setText("Continue typing to close");
        }
    }

    /**
     * Updates and shows the info popup.
     *
     * @param index
     * @param size
     * @param newPosition
     * @param items
     * @param commonPrefix
     */
    private void setCompletionInfoIndex(int index, int size, int shownCount, boolean scroll) {
        //-------------
        // Count label
        //-------------
        String count;
        if (index == -1) {
            if (shownCount > 0) {
                count = "(" + size + ")";
            } else {
                count = String.valueOf(size);
            }
        } else {
            if (shownCount > 0) {
                count = String.format("(%d/%d)", index + 1, size);
            } else {
                count = String.format("%d/%d", index + 1, size);
            }
        }
        countLabel.setText(count);
        
        //---------------------
        // Selection/Scrolling
        //---------------------
        if (scroll) {
            if (index == -1) {
                list.removeSelectionInterval(0, listData.getSize());
                list.ensureIndexIsVisible(0);
            }
            else if (shownCount > 0) {
                list.setSelectedIndex(index);

                int top = (shownCount - 1) / 2;
                int bottom = (shownCount - 1) / 2 + (shownCount - 1) % 2;

                int from = Math.max(index - top, 0);
                int to = Math.min(index + bottom, size - 1);

                list.scrollRectToVisible(list.getCellBounds(from, to));
            }
        } else {
            // Selection only
            if (index == -1) {
                list.removeSelectionInterval(0, listData.getSize());
            }
            else if (shownCount > 0) {
                list.setSelectedIndex(index);
            }
        }
    }
    
    private void updateWindowSettings() {
        /**
         * Setting fixed cell size improves performance when there are many
         * entries, since the width/height of the list can be easily calculated.
         */
        list.setFixedCellWidth(cellWidth);
        list.setFixedCellHeight(cellHeight);
        listRenderer.setCellHeight(cellHeight);
        // Just use cellHeight for this as well for now, seems to fit well
        listRenderer.setMinIconWidth(cellHeight);
    }

    private int cellHeight = 40;
    private int cellWidth = 300;

    /**
     * Creates the window for the info popup. This should only be run once and
     * then reused, only changing the text and size.
     */
    private void createInfoWindow() {
        if (infoWindow != null) {
            return;
        }
        infoWindow = new JWindow(SwingUtilities.getWindowAncestor(textField));
        infoWindow.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        JPanel contentPane = (JPanel) infoWindow.getContentPane();
        contentPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        contentPane.setBackground(backgroundColor);
        
        //------
        // List
        //------
        list = new MyList<>();
        list.setFixedCellHeight(cellHeight);
//        list.setFixedCellWidth(cellWidth);
        list.setModel(listData);
        list.setCellRenderer(listRenderer);
        list.setFocusable(false);
        list.setBackground(backgroundColor);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index != -1 && list.getCellBounds(index, index).contains(e.getPoint())) {
                    clickListener.accept(index, e);
                }
            }
            
        });
        
        //------------
        // Scrollpane
        //------------
        infoScroll = new JScrollPane(list);
        infoScroll.getVerticalScrollBar().setUnitIncrement(5);
        infoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        infoScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = 2;
        contentPane.add(infoScroll, gbc);
        
        //------
        // Info
        //------
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        helpLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        infoWindow.getContentPane().add(helpLabel, gbc);
        
        countLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        countLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        gbc.gridy = 1;
        gbc.gridx = 1;
        
        infoWindow.getContentPane().add(countLabel, gbc);

        /**
         * Hide the info popup if the textfield or containing window is changed
         * in any way.
         */
        containingWindow = SwingUtilities.getWindowAncestor(textField);
        if (containingWindow != null) {
            containingWindow.addComponentListener(componentListener);
        }
        textField.addComponentListener(componentListener);
    }

    
    
    //================
    // Custom Classes
    //================
    
    /**
     * For stopping animated GIF rendering when popup is closed. Also repaints
     * the entire list for image updates, so that could probably be optimized
     * (although it shouldn't be that bad).
     *
     * @param <T>
     */
    private static class MyList<T> extends JList<T> {

        public boolean imageUpdate(Image img, int infoflags,
                int x, int y, int w, int h) {
            if (!isShowing()) {
                return false;
            }
            // This will repaint the entire list
            return super.imageUpdate(img, infoflags, x, y, w, h);
        }
        
    }
    
    /**
     * List item renderer.
     */
    private static class MyRenderer implements ListCellRenderer {
        
        private final JPanel panel = new JPanel();
        private final MyIcon icon = new MyIcon();
        private final JLabel text = new JLabel();
        
        private Color highlightBackground = Color.LIGHT_GRAY;
        private String commonPrefix = "";
        
        public MyRenderer() {
            panel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTH;
            panel.add(icon, gbc);
            gbc.gridx = 1;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 5, 0, 0);
            gbc.anchor = GridBagConstraints.WEST;
            panel.add(text, gbc);
            panel.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            // This normally shouldn't happen, but it has before (in renderers in general)
            if (value == null) {
                icon.setIcon(null);
                text.setText(null);
                return panel;
            }
            AutoCompletionServer.CompletionItem item = (AutoCompletionServer.CompletionItem)value;
            if (!commonPrefix.isEmpty()) {
                if (item.hasInfo()) {
                    text.setText(String.format("<html><body><u>%s</u>%s&nbsp;<span style='font-size:0.8em;'>(%s)</span>",
                            enc(commonPrefix),
                            enc(item.getCode().substring(commonPrefix.length())),
                            enc(item.getInfo())));
                } else {
                    text.setText(String.format("<html><body><u>%s</u>%s",
                            enc(commonPrefix),
                            enc(item.getCode().substring(commonPrefix.length()))));
                }
            } else {
                if (item.hasInfo()) {
                    text.setText(String.format("<html><body>%s&nbsp;<span style='font-size:0.8em;'>(%s)</span>",
                            enc(item.getCode()),
                            enc(item.getInfo())));
                } else {
                    text.setText(item.getCode());
                }
            }
            panel.getAccessibleContext().setAccessibleName(item.getCode());
            panel.getAccessibleContext().setAccessibleDescription(item.getInfo());
            
            ImageIcon image = item.getImage(list);
            icon.setIcon(image);
            if (image != null) {
                // For animated GIFs
                image.setImageObserver(list);
            }
            if (isSelected) {
                panel.setBackground(highlightBackground);
            } else {
                panel.setBackground(null);
            }
            return panel;
        }
        
        private static String enc(String input) {
            return Helper.htmlspecialchars_encode(input).replace(" ", "&nbsp;");
        }
        
        public void setFont(Font font) {
            text.setFont(font);
        }
        
        public void setForeground(Color color) {
            text.setForeground(color);
        }
        
        public void setHighlightBackground(Color color) {
            highlightBackground = color;
        }
        
        public void setCellHeight(int height) {
            icon.setCellHeight(height);
        }
        
        public void setMinIconWidth(int width) {
            icon.setMinWidth(width);
        }
        
        public void setCommonPrefix(String prefix) {
            if (prefix == null) {
                this.commonPrefix = "";
            } else {
                this.commonPrefix = prefix;
            }
        }
        
    }
    
    /**
     * For aligning the icon in the center, as well as having a minimum width
     * and fixed height.
     */
    private static class MyIcon extends JPanel {
        
        private final JLabel label = new JLabel();
        private ImageIcon icon;
        private int cellHeight = 40;
        private int minWidth = 40;
        
        public void setIcon(ImageIcon icon) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.CENTER;
            setLayout(new GridBagLayout());
            this.icon = icon;
            label.setIcon(icon);
            setBackground(null);
            add(label, gbc);
        }
        
        /**
         * Set the fixed height, which is used when the minimum width has to be
         * enforced.
         * 
         * @param height 
         */
        public void setCellHeight(int height) {
            this.cellHeight = height;
        }
        
        public void setMinWidth(int width) {
            this.minWidth = width;
        }
        
        @Override
        public Dimension getMinimumSize() {
            if (icon != null && icon.getIconWidth() < minWidth) {
                return new Dimension(minWidth, cellHeight);
            }
            return super.getMinimumSize();
        }

        @Override
        public Dimension getPreferredSize() {
            if (icon != null && icon.getIconWidth() < minWidth) {
                return new Dimension(minWidth, cellHeight);
            }
            return super.getPreferredSize();
        }
        
    }
    
    /**
     * Model with bulk add functionality to prevent having to add items one by
     * one, which can impact performance due to the list being updated each
     * time.
     * 
     * @param <T> 
     */
    private static class MyListModel<T> extends AbstractListModel<T> {

        private final List<T> data = new ArrayList<>();
        
        @Override
        public int getSize() {
            return data.size();
        }

        @Override
        public T getElementAt(int index) {
            return data.get(index);
        }
        
        public void clear() {
            int size = data.size();
            if (size > 0) {
                data.clear();
                fireIntervalRemoved(this, 0, size - 1);
            }
        }
        
        public void setData(Collection<T> data) {
            clear();
            this.data.addAll(data);
            fireIntervalAdded(this, 0, data.size() - 1);
        }

    }
    
}
