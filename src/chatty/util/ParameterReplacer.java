
package chatty.util;

/**
 * Replaces parameters in a String.
 * 
 * @author tduva
 */
public class ParameterReplacer {
    
    private final StringBuilder output = new StringBuilder();
    private StringBuilder buffer = new StringBuilder();
    
    private Object[] parameters;
    
    private boolean startFound;
    private boolean required;
    private boolean escape;
    private boolean toEnd;
    private int index1 = -1;
    private int index2 = -1;
    private boolean invalidParameters;
    
    /**
     * Replaces parameter tokens found in {@code input} with the parameters in
     * the {@code parameters} array. If an array element exists, then it is
     * interpreted and used as a valid parameter, even if it is {@code null} or
     * it's {@code String} representation is empty.
     * 
     * <p>
     * The objects given as parameters are appended using
     * {@link java.lang.StringBuilder#append(Object)} (which turns the Object
     * into a {@code String} using {@link String#valueOf(Object)}).</p>
     *
     * <ul>
     * <li>$1 means replace with the first element in the array</li>
     * <li>$1- means replace with the first to the last element</li>
     * <li>$$1 requirement parameter, will return {@code null} if the array
     * doesn't contain this element</li>
     * <li>\$ to use literal $, \\ to use literal \</li>
     * </ul>
     * 
     * @param input The input to replace, cannot be {@code null}
     * @param parameters The array of {@code Object}s to use a parameters
     * @return The modified {@code input} {@code String} or {@code null} if a
     * required parameter wasn't specified
     */
    public String replace(String input, Object[] parameters) {
        this.parameters = parameters;
        for (int i=0;i<input.length();i++) {
            char c = input.charAt(i);
            if (c == '\\') {
                if (escape) {
                    escape = false;
                    output.append(c);
                } else {
                    escape = true;
                }
            } else if (!escape && c == '$') {
                if (required) {
                    output.append(buffer.subSequence(0, 1));
                    buffer.deleteCharAt(0);
                }
                if (startFound) {
                    required = true;
                }
                startFound = true;
                buffer.append(c);
            } else if (!escape && startFound && isNumber(c)) {
                int index = Character.getNumericValue(c) - 1;
                if (index1 == -1) {
                    index1 = index;
                } else if (toEnd && index > index1) {
                    index2 = index;
                } else {
                    clear();
                    output.append(c);
                }
            } else if (!escape && startFound && index1 != -1 && c == '-') {
                toEnd = true;
                clear();
            } else {
                clear();
                output.append(c);
            }
            if (invalidParameters) {
                break;
            }
        }
        clear();
        if (invalidParameters) {
            return null;
        }
        return output.toString();
    }
    
    /**
     * Checks if a match to replace has been found and appens the appropriate
     * parameter or the buffer if nothing has been found.
     */
    private void clear() {
        if (startFound) {
            if (index1 != -1) {
                //System.out.println(index1+" "+index2+" "+required);
                if (index1 < 0 || index1 >= parameters.length) {
                    if (required) {
                        invalidParameters = true;
                    }
                } else if (toEnd) {
                    output.append(buildRange(parameters, index1, index2));
                } else {
                    output.append(parameters[index1]);
                }
                buffer = new StringBuilder();
            } else {
                dumpBuffer();
            }
        }
        reset();
    }
    
    /**
     * Appends the current buffer contents to the output and clears the buffer.
     */
    private void dumpBuffer() {
        output.append(buffer);
        buffer = new StringBuilder();
    }
    
    /**
     * Reset all the fields that describe a match.
     */
    private void reset() {
        startFound = false;
        required = false;
        escape = false;
        toEnd = false;
        index1 = -1;
        index2 = -1;
    }
    
    /**
     * Checks if the given character is considered a number for replacing
     * parameters.
     *
     * @param c The character
     * @return {@code true} if a number in the range 1-9, {@code false}
     * otherwise
     */
    private static boolean isNumber(char c) {
        return c >= '1' && c <= '9';
    }
    
    /**
     * Constructs a String out of an array of Strings that consists of the
     * array elements with the indices {@code start} to {@code end}.
     * 
     * <p>
     * If {@code end} is {@code -1} or {@code end} &gt; length of the array,
     * then elements from {@code start} to the end of the array are used.</p>
     *
     * @param input The array of Strings
     * @param start The start index
     * @param end The end index
     * @return The constructed {@code String} or {@code null} if {@code start}
     * &lt; 0 or {@code start} &gt; length of the array
     */
    private static String buildRange(Object[] input, int start, int end) {
        if (start < 0 || start > input.length - 1) {
            return null;
        }
        if (end == -1) {
            end = input.length;
        }
        StringBuilder output = new StringBuilder();
        for (int i=start;i<=end;i++) {
            if (i >= input.length) {
                break;
            }
            if (i != start) {
                output.append(" ");
            }
            output.append(input[i]);
        }
        return output.toString();
    }
    
    public static final void main(String[] args) {
        String text = "/me slaps $$1 around a bit with a large trout";
        String parameter = "test";
        String[] parameters = parameter.split(" ");
        //parameters[0] = null;
        //String[] parameters = new String[0];
        ParameterReplacer replacer = new ParameterReplacer();
        //System.out.println(replacer.replace(text, parameters));
    }
    
}
