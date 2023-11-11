
package chatty.util;

import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class FileUtil {
    
    public static final Pattern ILLEGAL_FILENAME_CHARACTERS_PATTERN = Pattern.compile("[/\\n\\r\\t\\x00\\f`?*\\\\<>|\\\"\\\\:]");
    
}
