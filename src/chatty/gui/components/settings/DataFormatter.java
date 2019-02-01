
package chatty.gui.components.settings;

/**
 * Change the format of the given input.
 * 
 * @author tduva
 * @param <E>
 */
public interface DataFormatter<E> {
    
    public static final DataFormatter<String> TRIM = new DataFormatter<String>() {

        @Override
        public String format(String input) {
            if (input != null) {
                return input.trim();
            }
            return null;
        }
    };
    
    /**
     * The input is changed somehow, then returned changed or the same,
     * depending on the implementation of the formatter.
     *
     * @param input The input to format, should not be null
     * @return The formatted input (may also be {@code null}, depending on the
     * implementation of the formatter)
     */
    public E format(E input);
}
