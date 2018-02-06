
package chatty.gui.components;

import chatty.gui.HtmlColors;
import chatty.gui.components.AutoCompletionServer.CompletionItems;
import chatty.util.StringUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

/**
 * Provides the feature to complete text when the user performs a certain action
 * (e.g. pressing TAB, although that is controlled from outside this class).
 * 
 * This is probably only fit for shorter texts, because it always works on the
 * whole text. Might have bad performance in huge documents if it where modified
 * to work in another context.
 * 
 * If this is not used anymore, you should call {@link cleanUp()} to make sure
 * it can get gargabe collected.
 * 
 * @author tduva
 */
public class AutoCompletion {

    /**
     * Word pattern to be used to find the start/end of the word to be
     * completed.
     */
    private static final Pattern WORD = Pattern.compile("[^\\s,.:-@#+~!\"'$§%&\\/]+");

    /**
     * The JTextField the completion is performed in.
     */
    private final JTextComponent textField;

    // Settings
    private int maxResultsShown = 5;
    private boolean showPopup = true;
    private boolean completeToCommonPrefix = true;

    // State variables
    private boolean inCompletion = false;
    private String completionType;
    private AutoCompletionServer server;
    private String prevCompletion = null;
    private int prevCompletionIndex = 0;
    private int prevStart = 0;
    private String prevCompletionText = null;
    private int prevCaretPos;
    private AutoCompletionServer.CompletionItems prevCompletionItems;
    private String prevCommonPrefix;
    private String textBefore;
    private int caretPosBefore;

    // GUI elements for info display
    private JWindow infoWindow;
    private JLabel infoLabel;
    
    private final ComponentListener componentListener;
    private Window containingWindow;

    /**
     * Creates a new auto completion object bound to the given JTextField.
     *
     * @param textField The JTextField to perform the completion on
     */
    public AutoCompletion(JTextComponent textField) {
        this.textField = textField;
        textField.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(final CaretEvent e) {
                /**
                 * invokeLater because according to the Java Tutorial,
                 * caretUpdate isn't necessarily called in the EDT. Also it
                 * might help to wait for prevCaretPos to be updated to the
                 * value we want here to prevent the info window from closing
                 * when we don't want to, which would cause flickering.
                 */
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        if (e.getDot() != prevCaretPos) {
                            hideCompletionInfoWindow();
                            inCompletion = false;
                        }
                    }
                });
            }
        });
        
        /**
         * Hide and show the info popup depending on whether the textfield has
         * focus.
         */
        textField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                reshowCompletionInfoWindow();
            }

            @Override
            public void focusLost(FocusEvent e) {
                hideCompletionInfoWindow();
            }
        });
        
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
                infoWindow.setVisible(false);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                infoWindow.setVisible(false);
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

    /**
     * How many results to show in the info popup.
     * 
     * @param max The maximum number of results to show
     */
    public void setMaxResultsShown(int max) {
        this.maxResultsShown = max;
    }

    /**
     * Show the info popup during completion. This is also a prerequisite for
     * {@link setCompleteToCommonPrefix(boolean)}.
     * 
     * @param show Whether to show the info popup during completion
     */
    public void setShowPopup(boolean show) {
        this.showPopup = show;
    }

    /**
     * If enabled, completes to the common prefix of all matched results first,
     * allow to cycle through or refine the search. This is only enabled if
     * {@link setShowPopup(boolean)} is enabled as well.
     * 
     * <p>If only one match is found, this does nothing.</p>
     * 
     * <p>For example entering "j" while the matching results are "josh",
     * "joshimuz" and "joshua" would first complete to "josh". Following
     * completions will cycle through the results as usual, entering more text
     * and completing again starts a new completion (as usual), allowing to
     * refine the results.</p>
     * 
     * @param common Whether to complete to common prefix first
     */
    public void setCompleteToCommonPrefix(boolean common) {
        this.completeToCommonPrefix = common;
    }

    /**
     * Sets the {@link AudoCompletionServer}, which provides the actual
     * completion items. If this is not set or set to null, then the completion
     * does nothing.
     *
     * @param server The CompletionServer to use for completion
     */
    public void setCompletionServer(AutoCompletionServer server) {
        this.server = server;
    }
    
    /**
     * If currently considered to be in a completion process (potentially
     * cycling through results).
     *
     * @return true if currently in a completion, false otherwise
     */
    public boolean inCompletion() {
        return inCompletion;
    }
    
    /**
     * Returns the last used completion type as specified in
     * {@link doAutoCompletion(String, boolean)}.
     * 
     * @return 
     */
    public String getCompletionType() {
        return completionType;
    }

    /**
     * Cancels the current completion, which means the state of the text is
     * returned to what it was before completion and the info popup is closed if
     * necessary.
     */
    public void cancelAutoCompletion() {
        if (inCompletion) {
            textField.setText(textBefore);
            textField.setCaretPosition(caretPosBefore);
            prevCompletion = null;
            prevCompletionIndex = 0;
            inCompletion = false;
        }
    }

    /**
     * Do completion for the given type. Automatically takes the caret position
     * and text from the associated JTextField to perform the completion. The
     * type is used by the CompletionServer (which provides the actual
     * completion items) to help determine what items to return.
     *
     * @param type The type, can be any string the AudoCompletionServer
     * understands
     * @param forward Whether to cycle forward through results, moves backwards
     * otherwise
     */
    public void doAutoCompletion(String type, boolean forward) {
        if (server == null) {
            return;
        }

        // Get current state
        int pos = textField.getCaretPosition();
        String text = textField.getText();
        
        //-----------------
        // Index in result
        //-----------------
        // Gets reset if new completion
        int index = prevCompletionIndex;

        if (forward) {
            index++;
        } else {
            index--;
        }
        
        //-----------------
        // New Completion?
        //-----------------
        // If text was manually edited after the previous completion, start
        // fresh, which means it counts as a new completion
        boolean newCompletion = false;
        if (!text.equals(prevCompletionText) || !inCompletion
                || (prevCompletionItems != null && prevCompletionItems.items.size() == 1)) {
            prevCompletion = null;
            index = 0;
            newCompletion = true;
        }

        //-------------------------
        // Current word in textbox
        //-------------------------
        // Find start and end of the word based on where the caret is
        int end = findWordEnd(text, pos);
        int start = prevStart;
        if (newCompletion) {
            // This is necessary if a prefix was removed which separated the
            // word from previous characters (in the same completion)
            start = findWordStart(text, pos);
        }

        // Get the word
        String word = text.substring(start, end);
        if (word.isEmpty()) {
            return;
        }
        String actualWord = word;

        if (prevCompletion != null) {
            word = prevCompletion;
        }

        String prefix = "";
        if (start > 0) {
            prefix = text.substring(0, start);
        }

        //-------------
        // Get results
        //-------------
        AutoCompletionServer.CompletionItems results;
        if (newCompletion) {
            // Get new list of completion items
            results = findResults(type, prefix, word);
        } else {
            // Use current completion items if still in the same completion
            results = prevCompletionItems;
        }

        //---------------
        // Remove prefix
        //---------------
        List<String> items = results.items;
        if (prefix.endsWith(results.prefixToRemove) && newCompletion) {
            /**
             * Only remove prefix if it is still present. This might not be the
             * case if only the initial search contains that prefix, but not the
             * result items.
             */
            start -= results.prefixToRemove.length();
            actualWord = results.prefixToRemove + actualWord;
        }

        //------------------
        // Results checking
        //------------------
        
        // If no matches were found, quit now
        if (items.isEmpty() || (items.size() == 1 && items.get(0).equals(word))) {
            return;
        }
        
        // If previous completion reached the end, start from the beginning
        if (index >= items.size()) {
            index = 0;
        } else if (index < 0) {
            index = items.size() - 1;
        }

        // Get the replacement value for this completion step
        String nick = items.get(index);

        //---------------
        // Common prefix
        //---------------
        String commonPrefix = "";
        if (!newCompletion && prevCommonPrefix != null) {
            commonPrefix = prevCommonPrefix;
        } //System.out.println(prevCompletionIndex+" "+prevCompletion+" "+prevCompletionText);
        else if (items.size() > 1 && prevCompletion == null && showPopup
                && completeToCommonPrefix) {
            commonPrefix = findPrefixCommonToAll(items);
            if (!commonPrefix.isEmpty() && !nick.equalsIgnoreCase(commonPrefix)) {
                nick = commonPrefix;
                index = -1;
            }
        }
        if (newCompletion) {
            textBefore = text;
            caretPosBefore = pos;
        }

        //------------------------
        // Set new text and caret
        //------------------------
        // Create new text and set it
        String newText = text.substring(0, start) + nick + text.substring(end);
        textField.setText(newText);

        // Set caret at the end of the new word
        int newEnd = end + (nick.length() - actualWord.length());
        prevCaretPos = newEnd;
        textField.setCaretPosition(newEnd);
        
        //System.out.println("'"+prefix+"'"+nick.codePointCount(0, nick.length())+" "+nick.length()+" "+actualWord+" "+end+" "+newEnd+" "+textField.getText().length());

        //------------
        // Info popup
        //------------
        if (showPopup) {
            // Will only do something if more than one item was found or item
            // info has to be displayed
            showCompletionInfo(index, prevCompletion == null,
                    results, commonPrefix);
        }

        /**
         * Set variables for next completion (for cycling through completion
         * items and not actually a new completion)
         */
        prevCompletion = word;
        prevCompletionIndex = index;
        prevStart = start;
        prevCompletionText = newText;
        prevCompletionItems = results;
        prevCommonPrefix = commonPrefix;
        completionType = type;
        inCompletion = true;
    }
    


    /**
     * Create search result for the given search. Finds all words that start
     * with the given search.
     *
     * @param nicks An Array of nicknames
     * @param search The String to search for
     * @return A List of search results
     */
    private AutoCompletionServer.CompletionItems findResults(String type, String prefix, String search) {
        // Already returns a sorted result
        return server.getCompletionItems(type, prefix, search);
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
    private void showCompletionInfo(final int index,
            final boolean newPosition, final CompletionItems results,
            final String commonPrefix) {
        
        final List<String> items = results.items;
        final int size = items.size();
        // Don't show info popup if there is only one entry and no info for it
        if (size == 1 && !results.hasInfo(items.get(0))) {
            return;
        }
        /**
         * Using invokeLater probably so the state is already updated after
         * completion.
         */
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                int maxShown = maxResultsShown;

                StringBuilder b = new StringBuilder("<html><body style='padding:0 2 0 1;color:black'>");
                int end = -1;
                if (maxShown > 0) {
                    end = addNames(b, index, maxShown, results, commonPrefix);
                }
                String more = "";
                if (end != -1 && size - 1 > end) {
                    more = ", " + (size - end - 1) + " more";
                }
                if (maxShown > 0) {
                    b.append("<div style='padding:2 3 2 3;'>");
                } else {
                    b.append("<div style=''>");
                }
                if (index == -1) {
                    if (maxShown > 0) {
                        b.append("(" + size + " total" + more + ")");
                    } else {
                        b.append(size);
                    }
                } else {
                    if (maxShown > 0) {
                        b.append("(" + (index + 1) + "/" + size + more + ")");
                    } else {
                        b.append((index + 1) + "/" + size);
                    }
                }
                b.append("</div>");

                showInfoWindow(b.toString(), newPosition);
            }
        });
    }

    /**
     * Creates the info text containing the current completion items.
     *
     * @param b The StringBuilder to add the text to
     * @param index The index we are at cycling through the items
     * @param maxShown How many items to show at max
     * @param items The actual items
     * @param commonPrefix The common prefix of the items, to highlight
     * @return The index of the last item that was added
     */
    private int addNames(StringBuilder b, int index, int maxShown,
            CompletionItems results, String commonPrefix) {
        
        List<String> items = results.items;
        
        int left = maxShown - 1;

        int start = index - left / 2;
        left -= left / 2;
        if (start < 0) {
            left += -start;
            start = 0;
        }
        int end = index + left;
        left = 0;
        if (end >= items.size()) {
            left += end + 1 - items.size();
            end = items.size() - 1;
        }
        if (left > 0) {
            start -= left;
            if (start < 0) {
                start = 0;
            }
        }

        b.append("<div style=''>");
        for (int i = start; i <= end; i++) {
            String item = items.get(i);
            b.append("<span ");
            if (i == index) {
                b.append("style='background-color:#CCCCCC;'>");
                b.append(item);
            } else {
                b.append(">");
                if (commonPrefix.length() > 0) {
                    int length = commonPrefix.length();
                    b.append("<span style='background-color:#DDDDDD;'>");
                    b.append(item.substring(0, length)).append("</span>");
                    b.append(item.substring(length));
                } else {
                    b.append(item);
                }
            }
            if (results.hasInfo(item)) {
                b.append(" <span style='color:#555555'>(").append(results.getInfo(item)).append(")</span>");
            }
            b.append("</span><br />");
        }

        b.append("</div>");
        return end;
    }

    private Point prevCaretLocation;

    /**
     * Position the info popup according to the current caret location and show
     * it.
     *
     * @param infoText The info text to show
     * @param newPosition
     */
    private void showInfoWindow(String infoText, boolean newPosition) {
        if (infoWindow == null) {
            createInfoWindow();
        }
        Point location = prevCaretLocation;
        if (location == null || newPosition) {
            location = textField.getCaret().getMagicCaretPosition();
        }

        // No location found, so don't show window
        if (location == null) {
            return;
        }
        // Save a copy, because location is modified in-place
        prevCaretLocation = new Point(location);

        // Get size before setting new values
        int prevHeight = infoWindow.getHeight();
        int prevWidth = infoWindow.getWidth();

        // Get new size
        infoLabel.setText(infoText);
        Dimension preferredSize = infoWindow.getPreferredSize();

        // Set size depending on previous size
        if (prevWidth > preferredSize.width && !newPosition) {
            infoWindow.setSize(prevWidth, preferredSize.height);
        } else {
            infoWindow.setSize(preferredSize);
        }

        // If height of the window changed, need to reposition it
        if (prevHeight != infoWindow.getHeight()
                || prevWidth != infoWindow.getWidth()) {
            newPosition = true;
        }

        if (newPosition || !infoWindow.isVisible()) {
            // Determine and set new position
            location.x -= infoWindow.getWidth() / 4;
            if (location.x + infoWindow.getWidth() > textField.getWidth()) {
                location.x = textField.getWidth() - infoWindow.getWidth();
            } else if (location.x < 8) {
                location.x = 8;
            }
            location.y -= infoWindow.getHeight();
            SwingUtilities.convertPointToScreen(location, textField);
            infoWindow.setLocation(location);
        }
        infoWindow.setVisible(true);
    }

    /**
     * Creates the window for the info popup. This should only be run once and
     * then reused, only changing the text and size.
     */
    private void createInfoWindow() {
        infoWindow = new JWindow(SwingUtilities.getWindowAncestor(textField));
        infoLabel = new JLabel();
        infoWindow.add(infoLabel);
        JPanel contentPane = (JPanel) infoWindow.getContentPane();
        Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(2, 4, 2, 4));
        contentPane.setBorder(border);
        contentPane.setBackground(HtmlColors.decode("#EEEEEE"));
        infoLabel.setFont(textField.getFont());

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

    private void reshowCompletionInfoWindow() {
        if (infoWindow != null && !infoWindow.isVisible() && inCompletion) {
            infoWindow.setVisible(true);
        }
    }

    private void hideCompletionInfoWindow() {
        if (infoWindow != null && infoWindow.isVisible()) {
            infoWindow.setVisible(false);
        }
    }

    /**
     * Find the end of the word at the given position.
     *
     * @param text The full text
     * @param pos The position to find the word at (cursor position)
     * @return The index of the last character of the word
     */
    private int findWordEnd(String text, int pos) {
        int end = -1;

        // Find last word character from the current position
        Matcher m = WORD.matcher(text);
        if (pos > 0) {
            pos--;
        }
        if (m.find(pos)) {
            end = m.end();
        }

        // If position is already at the end of the text, use the text length
        if (text.length() == pos) {
            end = text.length();
        }

        // If no end was found, default to the end of the text
        if (end == -1) {
            end = text.length();
        }
        return end;
    }

    /**
     * Find the beginning of the word at the given position.
     *
     * @param text The full text
     * @param pos The position to find the word at (the cursor position)
     * @return The index of the first character of the word
     */
    private static int findWordStart(String text, int pos) {
        /**
         * Search "backwards" from the given position, finding the first word
         * character, by actually checking matches from the start and finding
         * the last one that is in front of the given position.
         */
        Matcher m = WORD.matcher(text);
        int temp = -1;
        while (m.find()) {
            if (m.start() > pos) {
                /**
                 * This match is always past the given position, so use the
                 * previous one.
                 */
                break;
            } else {
                // This is always the previous match start position
                temp = m.start();
            }
        }
        // Use whatever match was found, or -1 if none was found
        int start = temp;
        if (start == -1) {
            start = 0;
        }
        return start;
    }

    /**
     * Finds the common prefix to the given list of strings.
     *
     * @param input The list of strings to find the common prefix for
     * @return The common prefix, may be empty, or null if the input list is
     * empty
     */
    private static String findPrefixCommonToAll(List<String> input) {
        String result = null;
        for (String item : input) {
            if (result == null) {
                result = item;
            } else if (!StringUtil.toLowerCase(item).startsWith(StringUtil.toLowerCase(result))) {
                result = findCommonPrefix(item, result);
                if (result.isEmpty()) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Finds the common prefix between two strings.
     *
     * @param a One string
     * @param b The other string
     * @return The common prefix, may be empty
     */
    private static String findCommonPrefix(String a, String b) {
        int minLength = Math.min(a.length(), b.length());
        for (int i = 0; i < minLength; i++) {
            if (StringUtil.toLowerCase(a).charAt(i) != StringUtil.toLowerCase(b).charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a.substring(0, minLength);
    }
    
    /**
     * This should be called when the AutoCompletion is no longer used, so it
     * can be gargabe collected.
     */
    public void cleanUp() {
        if (containingWindow != null) {
            containingWindow.removeComponentListener(componentListener);
        }
        infoWindow = null;
    }

}
