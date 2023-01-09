
package chatty.util.commands;

/**
 * Allows reading the given String character by character.
 * 
 * @author tduva
 */
public class StringReader {
    
    private final String string;
    
    private int index = -1;
    
    public StringReader(String string) {
        this.string = string;
    }
    
    /**
     * Checks if there is a next character available.
     * 
     * @return 
     */
    public boolean hasNext() {
        return string.length() > index + 1;
    }
    
    /**
     * Return the next character and advance the read index.
     * 
     * @return 
     */
    public String next() {
        index++;
        return string.substring(index, index+1);
    }
    
    /**
     * Return the next character, but don't advance the read index.
     * 
     * @return 
     */
    public String peek() {
        return string.substring(index+1, index+2);
    }
    
    /**
     * Return the next amount characters, but don't advance the read index. If
     * there aren't amount characters left an empty String is returned.
     * 
     * @param amount The amount of characters to return, must be positive
     * @return 
     */
    public String peek(int amount) {
        if (string.length() > index+amount) {
            return string.substring(index+1, index+amount+1);
        }
        return "";
    }
    
    /**
     * If the given text is equal to the next characters of the same length the
     * index is advanced.
     * 
     * @param text
     * @return true if the text is equal
     */
    public boolean accept(String text) {
        if (peek(text.length()).equals(text)) {
            index += text.length();
            return true;
        }
        return false;
    }
    
    public String last() {
        return string.substring(index, index+1);
    }
    
    /**
     * The current read index.
     * 
     * @return 
     */
    public int pos() {
        return index;
    }
    
    public static void main(String[] args) {
        StringReader reader = new StringReader("abc");
        System.out.println(reader.next());
        System.out.println(reader.peek());
        System.out.println(reader.next());
        System.out.println(reader.next());
        System.out.println(reader.next());
    }
    
}
