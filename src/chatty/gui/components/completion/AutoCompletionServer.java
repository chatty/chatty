
package chatty.gui.components.completion;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;

/**
 * Defines the CompletionServer, which creates the actual search result used
 * for auto completion.
 * 
 * @author tduva
 */
public interface AutoCompletionServer {
    
    /**
     * Should return sorted items that only contain word characters, unless
     * only one item is returned, then any character is allowed.
     * 
     * The type can be any String and is used to make a general decision on what
     * this is (can e.g. be used for different keys used for auto-completion).
     * 
     * The prefix is anything before the actual search term in the original
     * String. This can be used to further narrow down what is actually supposed
     * to be completed.
     * 
     * For example if the user entered the text "/set irc" and initiated a
     * completion, the prefix would be "/set " and the search would be "irc".
     * Based on the prefix "/set ", it can be determined that this should search
     * for setting names starting with "irc", instead of e.g. chat nicknames (if
     * it is defined this way).
     * 
     * @param type The type used to narrow down what to search for
     * @param prefix The prefix used to narrow down what to search for
     * @param search The search item, will never be null or empty
     * @return A CompletionItems object containing the result
     */
    public CompletionItems getCompletionItems(String type, String prefix,
            String search);
    
    /**
     * Decide if the prefix should auto-start the completion (i.e. show the
     * popup). This is called by the completion instance.
     * 
     * The prefix is everything before what is considered the actual completion
     * search term. This could be fairly long, so usually only the end (which
     * would be right before the search term) would have to be checked, although
     * there may be cases where checking the entire prefix would also make
     * sense (for example to check if something is a command, so the input
     * starts with a '/').
     * 
     * @param prefix The prefix to check
     * @return true for auto-start, false to take no action
     */
    public boolean isAutostartPrefix(String prefix);
    
    /**
     * A container for the data the CompletionServer returns.
     */
    public static class CompletionItems {

        public final List<CompletionItem> items;
        public final String prefixToRemove;

        /**
         * Should contain a sorted list of Strings, containing only of word
         * characters, unless only one String is contained in the list, then
         * any character is allowed.
         * 
         * The prefixToRemove denotes the prefix String that shouldn't actually
         * be part of the resulting completed String, but is only used to
         * determine what type of items should be returned for completion. It
         * will thus be removed. Only the length of the prefixToRemove may
         * actually matter. It must not be longer than the actual prefix was.
         *
         * @param items Some HTML characters will be escaped
         * @param prefixToRemove 
         */
        public CompletionItems(List<CompletionItem> items, String prefixToRemove) {
            this.items = items;
            this.prefixToRemove = prefixToRemove;
        }
        
        public static CompletionItems createFromStrings(List<String> items,
                                                        String prefixToRemove) {
            return createFromStrings(items, prefixToRemove, null);
        }
        
        public static CompletionItems createFromStrings(List<String> items,
                                                        String prefixToRemove,
                                                        Map<String, String> info) {
            List<CompletionItem> result = new ArrayList<>();
            for (String item : items) {
                result.add(new CompletionItem(item, info != null ? info.get(item) : null));
            }
            return new CompletionItems(result, prefixToRemove);
        }
        
        /**
         * Creates an object for an empty result.
         */
        public CompletionItems() {
            this.items = new ArrayList<>();
            this.prefixToRemove = "";
        }
        
        /**
         * Add the items and info of the given CompletionItems object to the end
         * of this one.
         * 
         * @param other 
         */
        public void append(CompletionItems other) {
            items.addAll(other.items);
        }
        
        public boolean isEmpty() {
            return items == null || items.isEmpty();
        }
        
    }
    
    public static class CompletionItem implements Comparable<CompletionItem> {
        
        private final String code;
        private final String info;
        
        public CompletionItem(String code, String info) {
            this.code = code;
            this.info = info;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getInfo() {
            return info;
        }
        
        public boolean hasInfo() {
            return info != null && !info.isEmpty();
        }
        
        public ImageIcon getImage(Component c) {
            return null;
        }
        
        public String toString() {
            return code;
        }

        @Override
        public int compareTo(CompletionItem o) {
            return code.compareToIgnoreCase(o.code);
        }
        
    }
    
}

