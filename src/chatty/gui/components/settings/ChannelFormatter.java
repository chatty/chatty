
package chatty.gui.components.settings;

import chatty.util.StringUtil;

public class ChannelFormatter implements DataFormatter<String> {

    /**
     * Prepends the input with a "#" if not already present. Returns
     * {@code null} if the length after prepending is only 1, which means it
     * only consists of the "#" and is invalid.
     *
     * @param input The input to be formatted
     * @return The formatted input, which has the "#" prepended, or {@code null}
     * or any empty String if the input was invalid
     */
    @Override
    public String format(String input) {
        if (input != null && !input.isEmpty()
                && !input.startsWith("#")
                && !input.startsWith("$")) {
            input = "#" + input;
        }
        if (input != null && input.length() == 1) {
            input = null;
        }
        return StringUtil.toLowerCase(input);
    }
    
}
