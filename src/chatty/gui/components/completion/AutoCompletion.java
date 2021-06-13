
package chatty.gui.components.completion;

import chatty.gui.components.completion.AutoCompletionServer.CompletionItem;
import chatty.util.Debugging;
import chatty.util.StringUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

/**
 * Provides text completion in the associated text component. Completion can
 * either be initiated manually via a method or automatically. Items are then
 * selected manually via a method. An optional popup window shows a list of the
 * completion items.
 * 
 * For completion the word under the cursor is used, whereas word refers to all
 * characters that are matched by the hardcoded WORD pattern.
 *
 * The AutoCompletionServer is responsible for providing the completion items
 * and must decide if a certain prefix should automatically initiate completion.
 * 
 * This is probably only fit for shorter texts, because it always works on the
 * whole text. Might have bad performance in huge documents.
 * 
 * If an instance is no longer used {@link cleanUp()} should be called to make
 * sure it can be garbage collected.
 * 
 * @author tduva
 */
public class AutoCompletion {
    
    private static final Logger LOGGER = Logger.getLogger(AutoCompletion.class.getName());

    /**
     * Word pattern to be used to find the start/end of the word to be
     * completed.
     */
    private static final Pattern WORD = Pattern.compile("[^\\s,.:;\\-@#~!\"'$§%&\\/]+");

    //------------
    // References
    //------------
    /**
     * The JTextField the completion is performed in.
     */
    private final JTextComponent textField;
    
    private final AutoCompletionWindow w;
    private final CaretListener caretListener;
    private AutoCompletionServer server;

    //----------
    // Settings
    //----------
    private boolean isEnabled = true;
    private boolean showPopup = true;
    private boolean completeToCommonPrefix = true;
    private boolean appendSpace = false;

    /**
     * Creates a new auto completion object bound to the given JTextComponent.
     *
     * @param textField The JTextComponent to perform the completion on
     */
    public AutoCompletion(JTextComponent textField) {
        this.textField = textField;
        this.w = new AutoCompletionWindow(textField, (clickedIndex, e) -> {
            if (e.isShiftDown()) {
                if (resultIndex != -1) {
                    // If current already completed a word, add next one after
                    startPos = textField.getCaretPosition();
                }
                insertWord(clickedIndex, appendSpace, true);
                // Set new start after the ensured space, so a non-shift click
                // inserts at the correct position
                startPos++;
            } else {
                insertWord(clickedIndex, appendSpace, false);
                if (!SwingUtilities.isMiddleMouseButton(e)) {
                    end();
                }
            }
            resultIndex = clickedIndex;
            updatePopup(false);
        });
        
        caretListener = new CaretListener() {

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
                        updateState();
                    }
                });
            }
        };
        textField.addCaretListener(caretListener);
    }
    
    //==========
    // Settings
    //==========
    
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
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
    
    public void setFont(Font font) {
        w.setFont(font);
    }
    
    public void setForegroundColor(Color color) {
        w.setForegroundColor(color);
    }
    
    public void setBackgroundColor(Color color) {
        w.setBackgroundColor(color);
    }
    
    public void setHighlightColor(Color color) {
        w.setHighlightColor(color);
    }
    
    /**
     * How many results to show in the info popup.
     *
     * @param max The maximum number of results to show
     */
    public void setMaxResultsShown(int max) {
        w.setMaxResultsShown(max);
    }
    
    /**
     * Set the fixed width/height for the list elements. The height is also used
     * for the min icon width of elements.
     * 
     * @param width
     * @param height 
     */
    public void setCellSize(int width, int height) {
        w.setCellSize(width, height);
    }
    
    public void setAppendSpace(boolean append) {
        this.appendSpace = append;
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
    
    public boolean getCompleteToCommonPrefix() {
        return completeToCommonPrefix;
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
    
    //===============
    // Other Methods
    //===============
    
    /**
     * If currently considered to be in a completion process (potentially
     * cycling through results).
     *
     * @return true if currently in a completion, false otherwise
     */
    public boolean inCompletion() {
        return inCompletion;
    }
    
    public void cleanUp() {
        textField.removeCaretListener(caretListener);
        w.cleanUp();
    }

    //=================
    // State variables
    //=================
    
    private boolean inCompletion = false;
    
    //--------------------
    // Track text changes
    //--------------------
    /**
     * The full text as it was after inserting an item for completion.
     */
    private String autoSetText;
    
    /**
     * The text before the latest caret movement, to track changes.
     */
    private String prevText;
    
    /**
     * Index in text that marks the start of the word where the cursor is.
     */
    private int startPos;
    
    /**
     * Index in text that marks the end of the word where the cursor is.
     */
    private int endPos;
    
    /**
     * Index in text when completion was started.
     */
    private int initialStartPos;
    
    /**
     * Current prefix, that is everything before the word the cursor is at.
     */
    private String prefix;
    
    /**
     * Current word, that is the word where the cursor is at. This is used as
     * search for the completion items.
     */
    private String word;
    
    //----------------------
    // Results / Completion
    //----------------------
    /**
     * Type of the current completion.
     */
    private String type;
    
    /**
     * The latest results.
     */
    private AutoCompletionServer.CompletionItems results;
    
    /**
     * The word the latest results were retrieved for.
     */
    private String resultsWord;
    
    /**
     * The prefix the latest results were retrieved for.
     */
    private String resultsPrefix;
    
    /**
     * The current position in the list of results. A value of -1 means no
     * completion has been performed on the results yet.
     */
    private int resultIndex = -1;
    
    /**
     * If true, the prefix to remove has already been removed for this
     * completion.
     */
    private boolean prefixRemoved;
    
    private String commonPrefix;
    
    /**
     * If true, the next result requested will be the first result since this
     * completion has started.
     */
    private boolean firstResult;
    
    //------------------
    // Restore / Cancel
    //------------------
    /**
     * Caret position used to restore to state before completing.
     */
    private int preCaretPos;
    
    /**
     * Text used to restore to state before completing.
     */
    private String preText;
    
    //====================
    // Completion methods
    //====================
    
    /**
     * Update state on caret position changes.
     */
    private void updateState() {
        w.updateHelp(resultIndex);
        String text = textField.getText();
        int caretPos = textField.getCaretPosition();
        Debugging.println("completion", "[Update] %d %s", caretPos, text);
        
        if (text.equals(autoSetText) && caretPos == endPos) {
            // If text/caret changed based on completion, ignore this change
            return;
        }
        // Check before endPos and autoSetText are updated
        boolean movedAfter = text.equals(autoSetText) && caretPos != endPos;
        
        autoSetText = null;
        
        // Find word at cursor (with these default values if none found)
        startPos = -1;
        endPos = -1;
        word = "";
        prefix = "";
        Matcher m = WORD.matcher(text);
        while (m.find()) {
            if (m.start() < caretPos && m.end() >= caretPos) {
                startPos = m.start();
                endPos = m.end();
                word = m.group();
                prefix = text.substring(0, startPos);
            }
        }
        
        Debugging.println("completion", "[Updated] Prefix: '%s' Word: '%s' Caret: %d", prefix, word, caretPos);
        if (startPos == -1 || endPos == -1 || movedAfter) {
            // Didn't find anything usable for search, so just quit
            end();
        } else {
            // Maybe something
            if (resultIndex != -1 || startPos != initialStartPos) {
                // Moved index after completing, so end
                end();
            }
            checkAutostart();
            updateSearch();
        }
        prevText = text;
    }
    
    //-------------------
    // Start/end methods
    //-------------------
    
    private void checkAutostart() {
        if (!inCompletion && showPopup
                && endPos == textField.getCaretPosition()
                && !textField.getText().equals(prevText)
                && server.isAutostartPrefix(prefix)) {
            Debugging.println("completion", "AUTOSTART");
            start("auto");
        }
    }
    
    public void manual(int step, String type) {
        Debugging.println("completion", "[Manual] Step: %d Type: %s InCompletion: %s", step, type, inCompletion);
        if (inCompletion) {
            complete(step);
        } else {
            start(type);
            if (step != 0) {
                complete(step);
            }
        }
    }
    
    private void start(String type) {
        if (!isEnabled) {
            return;
        }
        if (word == null || word.isEmpty()) {
            return;
        }
        Debugging.println("completion", "START");
        this.type = type;
        inCompletion = true;
        prefixRemoved = false;
        resultsWord = null;
        resultsPrefix = null;
        initialStartPos = startPos;
        firstResult = true;
        updateSearch();
    }
    
    private void end() {
        Debugging.println("completion", "END");
        inCompletion = false;
        w.close();
        autoSetText = null;
    }
    
    private void updateSearch() {
        if (!inCompletion) {
            return;
        }
        if (word.isEmpty()) {
            return;
        }
        if (word.equals(resultsWord) && prefix.equals(resultsPrefix)) {
            return;
        }
        results = server.getCompletionItems(type, prefix, word);
        if (results.isEmpty() && firstResult) {
            // End so that another "start" type can be used if no results
            // (e.g. TAB -> no results, Shift-TAB -> results)
            end();
        }
        if (completeToCommonPrefix && results.items.size() > 1 && showPopup) {
            commonPrefix = findPrefixCommonToAll(results.items);
            if (commonPrefix.length() - word.length() == 0 && w.isShowing()) {
                commonPrefix = "";
            }
        } else {
            commonPrefix = "";
        }
        resultsWord = word;
        resultsPrefix = prefix;
        resultIndex = -1;
        firstResult = false;
        showPopup();
    }
    
    //----------------
    // Window methods
    //----------------
    private void showPopup() {
        if (showPopup && !results.items.isEmpty()) {
            w.init(results, commonPrefix, startPos);
            w.show(-1, true);
        } else {
            w.close();
        }
    }
    
    private void updatePopup(boolean scroll) {
        if (showPopup && inCompletion) {
            w.show(resultIndex, scroll);
        } else {
            w.close();
        }
    }
    
    //-----------------
    // Completion step
    //-----------------
    
    private void complete(int step) {
        if (!inCompletion || results == null || results.items.isEmpty()
                || step == 0) {
            return;
        }
        if (resultIndex == -1) {
            // First completion for these results
            preText = textField.getText();
            preCaretPos = textField.getCaretPosition();
        }
        if (resultIndex == -1 && !commonPrefix.isEmpty()) {
            insertWord(commonPrefix, false, false);
            commonPrefix = "";
        } else {
            resultIndex += step;
            if (resultIndex >= results.items.size()) {
                resultIndex = 0;
            } else if (resultIndex < 0) {
                resultIndex = results.items.size() - 1;
            }
            if (resultIndex >= results.items.size()) {
                return;
            }
            if (resultIndex < 0) {
                resultIndex = 0;
            }
            updatePopup(true);
            insertWord(resultIndex, appendSpace, false);
        }
    }
    
    //----------------
    // Modifying text
    //----------------
    /**
     * Insert the item from the given result index. If the index is invalid,
     * nothing is done.
     * 
     * @param index 
     */
    private void insertWord(int index, boolean appendSpace, boolean ensureSpace) {
        if (results.items.size() <= index) {
            return;
        }
        String item = results.items.get(index).getCode();
        insertWord(item, appendSpace, ensureSpace);
    }
    
    /**
     * Insert the given string at the current position.
     * 
     * @param item 
     */
    private void insertWord(String item, boolean appendSpace, boolean ensureSpace) {
        try {
            removePrefix();

            if (appendSpace) {
                item = item+" ";
            }

            // Update text
            String text = textField.getText();
            if (ensureSpace && startPos > 0 && text.charAt(startPos-1) != ' ') {
                item = " "+item;
            }
            String newText = text.substring(0, startPos) + item + text.substring(endPos);
            textField.setText(newText);

            autoSetText = newText;

            // Update caret and endIndex
            int newEnd = startPos + item.length();
            endPos = newEnd;
            textField.setCaretPosition(newEnd);
        } catch (Exception ex) {
            LOGGER.warning("Exception in insertWord: "+ex);
        }
    }
    
    /**
     * Remove the prefix once after starting completion, if a prefix to remove
     * has been returned by the results.
     */
    private void removePrefix() {
        if (results.prefixToRemove == null || results.prefixToRemove.isEmpty()) {
            return;
        }
        if (prefix.endsWith(results.prefixToRemove) && !prefixRemoved) {
            startPos -= results.prefixToRemove.length();
            prefixRemoved = true;
        }
    }
    
    /**
     * Returns the text to the state before completing.
     */
    public void cancel() {
        if (inCompletion && resultIndex > -1) {
            resultIndex = -1;
            prefixRemoved = false;
            updatePopup(true);
            textField.setText(preText);
            textField.setCaretPosition(preCaretPos);
        }
    }

    //===================
    // Utility functions
    //===================
    
    /**
     * Finds the common prefix to the given list of strings.
     *
     * @param input The list of strings to find the common prefix for
     * @return The common prefix, may be empty, or null if the input list is
     * empty
     */
    private static String findPrefixCommonToAll(List<CompletionItem> input) {
        String result = null;
        for (CompletionItem item : input) {
            if (result == null) {
                result = item.getCode();
            } else if (!StringUtil.toLowerCase(item.getCode()).startsWith(StringUtil.toLowerCase(result))) {
                result = findCommonPrefix(item.getCode(), result);
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

}
