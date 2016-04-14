
package chatty.gui.components;

import java.util.ArrayList;
import java.util.List;

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
     * @param search The search item
     * @return A CompletionItems object containing the result
     */
    public CompletionItems getCompletionItems(String type, String prefix,
            String search);
    
    /**
     * A container for the data the CompletionServer returns.
     */
    public static class CompletionItems {

        public final List<String> items;
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
         * @param items
         * @param prefixToRemove 
         */
        public CompletionItems(List<String> items, String prefixToRemove) {
            this.items = items;
            this.prefixToRemove = prefixToRemove;
        }
        
        /**
         * Creates an object for an empty result.
         */
        public CompletionItems() {
            this.items = new ArrayList<>();
            this.prefixToRemove = "";
        }
    }
}

