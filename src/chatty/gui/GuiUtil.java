
package chatty.gui;

import chatty.Helper;
import chatty.util.MiscUtil;
import chatty.util.ProcessManager;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;

/**
 * Some Utility functions or constants for GUI related stuff
 * 
 * @author tduva
 */
public class GuiUtil {
    
    private static final Logger LOGGER = Logger.getLogger(GuiUtil.class.getName());
    
    public final static Insets NORMAL_BUTTON_INSETS = new Insets(2, 14, 2, 14);
    public final static Insets SMALL_BUTTON_INSETS = new Insets(-1, 10, -1, 10);
    public final static Insets SPECIAL_BUTTON_INSETS = new Insets(2, 12, 2, 6);
    public final static Insets SPECIAL_SMALL_BUTTON_INSETS = new Insets(-1, 12, -1, 6);
    
    private static final String CLOSE_DIALOG_ACTION_MAP_KEY = "CLOSE_DIALOG_ACTION_MAP_KEY";
    private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    
    
    public static void installEscapeCloseOperation(final JDialog dialog) {
    Action closingAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispatchEvent(
                        new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING
                        ));
            }
        };
        
        JRootPane root = dialog.getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                ESCAPE_STROKE,
                CLOSE_DIALOG_ACTION_MAP_KEY);
        root.getActionMap().put(CLOSE_DIALOG_ACTION_MAP_KEY, closingAction);
    }
    
    /**
     * Shows a JOptionPane that doesn't steal focus when opened, but is
     * focusable afterwards.
     * 
     * @param parent The parent Component
     * @param title The title
     * @param message The message
     * @param messageType The type of message as in JOptionPane
     * @param optionType The option type as in JOptionPane
     * @param options The options as in JOptionPane
     * @return The selected option or -1 if none was selected
     */
    public static int showNonAutoFocusOptionPane(Component parent, String title, String message,
            int messageType, int optionType, Object[] options) {
        JOptionPane p = new JOptionPane(message, messageType, optionType);
        p.setOptions(options);
        final JDialog d = p.createDialog(parent, title);
        d.setAutoRequestFocus(false);
        d.setFocusableWindowState(false);
        // Make focusable after showing the dialog, so that it can be focused
        // by the user, but doesn't steal focus from the user when it opens.
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                d.setFocusableWindowState(true);
            }
        });
        d.setVisible(true);
        // Find index of result
        Object value = p.getValue();
        for (int i = 0; i < options.length; i++) {
            if (options[i] == value) {
                return i;
            }
        }
        return -1;
    }
    
    public static void showNonModalMessage(Component parent, String title, String message, int type) {
        showNonModalMessage(parent, title, message, type, false);
    }
    
    public static void showNonModalMessage(Component parent, String title, String message, int type, boolean allowHtml) {
        if (!allowHtml) {
            message = Helper.htmlspecialchars_encode(message);
        }
        message = "<html><body style='font-family: Monospaced;width:400px;'>"+message;
        JOptionPane pane = new JOptionPane(message, type);
        JDialog dialog = pane.createDialog(parent, title);
        dialog.setModal(false);
        dialog.setVisible(true);
    }
    
    /**
     * Checks if the given {@code Point} is on a screen. The point can be moved
     * horizontally before checking by specifying a {@code xOffset}. The
     * original point is not modified.
     * 
     * @param p The {@code Point} to check
     * @param xOffset The horizontal offset in pixels
     * @return {@code true} if the point is on screen, {@code false} otherwise
     */
    public static boolean isPointOnScreen(Point p, int xOffset) {
        Point moved = new Point(p.x + xOffset, p.y);
        return isPointOnScreen(moved);
    }
    
    /**
     * Checks if the given {@code Point} is on a screen.
     * 
     * @param p The {@code Point} to check
     * @return {@code true} if the point is on screen, {@code false} otherwise
     */
    public static boolean isPointOnScreen(Point p) {
        GraphicsDevice[] screens = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices();
        for (GraphicsDevice screen : screens) {
            if (screen.getDefaultConfiguration().getBounds().contains(p)) {
                return true;
            }
        }
        return false;
    }
    
    public static GridBagConstraints makeGbc(int x, int y, int w, int h) {
        return makeGbc(x, y, w, h, GridBagConstraints.EAST);
    }
    
    public static GridBagConstraints makeGbc(int x, int y, int w, int h, int anchor) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.anchor = anchor;
        gbc.insets = new Insets(5, 5, 5, 5);
        return gbc;
    }
    
    public static JPanel northWrap(JPanel panel) {
        JPanel container = new JPanel(new BorderLayout());
        container.add(panel, BorderLayout.NORTH);
        return container;
    }
    
    /**
     * Output the text of the subelements of the given element.
     *
     * @param line
     */
    public static void debugLineContents(Element line) {
        Document doc = line.getDocument();
        System.out.print("[");
        for (int i = 0; i < line.getElementCount(); i++) {
            Element l = line.getElement(i);
            //System.out.println(l);
            try {
                System.out.print("'" + doc.getText(l.getStartOffset(), l.getEndOffset() - l.getStartOffset()) + "'");

            } catch (BadLocationException ex) {
                System.out.println("Bad location");
            }
        }
        System.out.println("]");
    }
    
    /**
     * Detect retina display.
     * 
     * https://stackoverflow.com/questions/20767708/how-do-you-detect-a-retina-display-in-java
     * 
     * @return 
     */
    public static boolean hasRetinaDisplay() {
        Object obj = Toolkit.getDefaultToolkit().getDesktopProperty(
                "apple.awt.contentScaleFactor");
        if (obj instanceof Float) {
            int scale = ((Float)obj).intValue();
            return (scale == 2); // 1 indicates a regular mac display.
        }
        return false;
    }
    
    /**
     * Recursively set the font size of the given component and all
     * subcomponents.
     * 
     * @param fontSize
     * @param component 
     */
    public static void setFontSize(float fontSize, Component component) {
        if (fontSize <= 0) {
            return;
        }
        if (component instanceof Container) {
            synchronized(component.getTreeLock()) {
                for (Component c : ((Container) component).getComponents()) {
                    GuiUtil.setFontSize(fontSize, c);
                }
            }
        }
        component.setFont(component.getFont().deriveFont(fontSize));
    }
    
    /**
     * Returns the current sort keys of the given table encoded in a String.
     * 
     * <p>This is intended to be used together with
     * {@link setSortingForTable(JTable, String)}.</p>
     *
     * @param table
     * @return 
     */
    public static String getSortingFromTable(JTable table) {
        List<? extends RowSorter.SortKey> keys = table.getRowSorter().getSortKeys();
        String result = "";
        for (RowSorter.SortKey key : keys) {
            int order = 0;
            if (key.getSortOrder() == SortOrder.ASCENDING) {
                order = 1;
            } else if (key.getSortOrder() == SortOrder.DESCENDING) {
                order = 2;
            }
            result += String.format("%s:%s;", key.getColumn(), order);
        }
        return result;
    }
    
    /**
     * Sets the sort keys for the RowSorter of the given JTable. Doesn't change
     * the sorting if the sorting parameter doesn't contain any valid sort key.
     * 
     * <p>This is intended to be used together with
     * {@link getSortingFromTable(JTable)}.</p>
     *
     * @param table
     * @param sorting 
     */
    public static void setSortingForTable(JTable table, String sorting) {
        List<RowSorter.SortKey> keys = new ArrayList<>();
        StringTokenizer t = new StringTokenizer(sorting, ";");
        while (t.hasMoreTokens()) {
            String[] split = t.nextToken().split(":");
            if (split.length == 2) {
                try {
                    int rowId = Integer.parseInt(split[0]);
                    int orderId = Integer.parseInt(split[1]);
                    SortOrder order;
                    switch (orderId) {
                        case 1: order = SortOrder.ASCENDING; break;
                        case 2: order = SortOrder.DESCENDING; break;
                        default: order = SortOrder.UNSORTED;
                    }
                    keys.add(new RowSorter.SortKey(rowId, order));
                } catch (NumberFormatException ex) {
                    // Just don't add anything
                }
            }
        }
        try {
            if (!keys.isEmpty()) {
                table.getRowSorter().setSortKeys(keys);
            }
        } catch (IllegalArgumentException ex) {
            // Don't change sorting
        }
    }
    
    /**
     * Adds the Copy/Paste/Cut shortcuts for Mac (Command instead of Ctrl).
     * 
     * <p>Normally the Look&Feel should do that automatically, but for some
     * reason it doesn't seem to do it.</p>
     */
    public static void addMacKeyboardActions() {
        if (MiscUtil.OS_MAC) {
            addMacKeyboardActionsTo("TextField.focusInputMap");
            addMacKeyboardActionsTo("TextArea.focusInputMap");
            addMacKeyboardActionsTo("TextPane.focusInputMap");
        }
    }
    
    /**
     * Based on: https://stackoverflow.com/a/7253059/2375667
     */
    private static void addMacKeyboardActionsTo(String key) {
        InputMap im = (InputMap) UIManager.get(key);

        // Copy/paste actions
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);

        // Navigation actions
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK), DefaultEditorKit.beginLineAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK), DefaultEditorKit.endLineAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK), DefaultEditorKit.previousWordAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK), DefaultEditorKit.nextWordAction);

        // Navigation selection actions
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.selectionBeginLineAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.selectionEndLineAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.selectionPreviousWordAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), DefaultEditorKit.selectionNextWordAction);

        // Other actions
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
    }
    
    public static void showCommandNotification(String commandText, String title,
            String message, String channel) {
        CustomCommand command = CustomCommand.parse(commandText);

        Parameters param = Parameters.create("");
        param.put("title", title.replace("\"", "\\\""));
        param.put("message", message.replace("\"", "\\\""));
        param.put("channel", channel);

        ProcessManager.execute(command.replace(param), "Notification");
    }
    
    /**
     * Java 8u161/162 introduced a bug that causes high CPU usage when a
     * JTextField/JTextArea is focused as first component after the window is
     * focused.
     * 
     * This workaround aims to prevent this by rejecting that focus change if it
     * occurs and focusing another component first, then focusing the original
     * text component.
     * 
     * This may or may not actually work, but it seemed fine in testing.
     */
    public static void installTextComponentFocusWorkaround() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addVetoableChangeListener(new VetoableChangeListener() {

            private boolean rejectNext = false;
            private JComponent target;

            @Override
            public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
                if (evt.getNewValue() != null) {
                    if (rejectNext && evt.getPropertyName().equals("focusOwner")) {
                        if (evt.getNewValue() instanceof JTextComponent) {
                            JComponent component = (JComponent) evt.getNewValue();
                            // Move focus up, this usually moves it to the
                            // window itself
                            KeyboardFocusManager.getCurrentKeyboardFocusManager().upFocusCycle(component);
                            target = component;
                            LOGGER.info("[Focus] Rejected JTextComponent focus");
                            // Reject focus change as well, otherwise this
                            // didn't seem to work
                            throw new PropertyVetoException("Rejected JTextComponent focus", evt);
                        } else {
                            // If anything else was focused, no need to reject
                            // anymore, change focus back if necessary
                            rejectNext = false;
                            if (target != null) {
                                LOGGER.info("[Focus] Temp: " + evt.getNewValue());
                                target.requestFocus();
                                target = null;
                            }
                        }
                    } else if (evt.getPropertyName().equals("focusedWindow")) {
                        // Next focus on a text component should be rejected
                        LOGGER.info("[Focus] Window focused");
                        rejectNext = true;
                    }
                }

                // Debug
                String oldV = evt.getOldValue() != null ? evt.getOldValue().getClass().toString() : null;
                String newV = evt.getNewValue() != null ? evt.getNewValue().getClass().toString() : null;
                //System.out.println(evt.getPropertyName()+": "+oldV+" -> "+newV);
            }
        });
    }
    
}
