
package chatty.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 *
 * @author tduva
 */
public class StringUtil {
    
    /**
     * Tries to turn the given Object into a List of Strings.
     * 
     * If the given Object is a List, go through all items and copy those
     * that are Strings into a new List of Strings.
     * 
     * @param obj
     * @return 
     */
    public static List<String> getStringList(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List)obj) {
                if (item instanceof String) {
                    result.add((String)item);
                }
            }
        }
        return result;
    }
    
    public static String join(String[] array) {
        return join(Arrays.asList(array), ",");
    }
    
    public static String join(Collection<?> items, String delimiter) {
        return join(items, delimiter, -1, -1);
    }
    
    public static String join(Collection<?> items, String delimiter, int start) {
        return join(items, delimiter, start, -1);
    }
    
    public static String join(Collection<?> items, String delimiter, int start, int end) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        start = start > -1 ? start : 0;
        end = end > -1 ? end : items.size();
        
        StringBuilder b = new StringBuilder();
        Iterator<?> it = items.iterator();
        int i = 0;
        while (it.hasNext()) {
            String next = it.next().toString();
            if (i >= start && i < end) {
                b.append(next);
                if (it.hasNext() && i+1 < end) {
                    b.append(delimiter);
                }
            }
            i++;
        }
        return b.toString();
    }
    
    /**
     * Shortens the given {@code input} to the {@code max} length. Only changes
     * the {@code input} if it actually exceeds {@code max} length, but if it
     * does, the returning text is 2 shorter than {@code max} because it also adds
     * ".." where it shortened the text.
     * 
     * Positive {@code max} length values shorten the {@code input} at the end,
     * negative values shorten the {@code input} at the start.
     * 
     * @param input The {@code String} to shorten
     * @param max The maximum length the String should have after this
     * @return The modified {@code String} if {@code input} exceeds the
     * {@code max} length, the original value otherwise
     */
    public static String shortenTo(String input, int max) {
        if (input != null && input.length() > Math.abs(max)) {
            if (max > 2) {
                return input.substring(0, max-2)+"..";
            } else if (max < -2) {
                return ".."+input.substring(input.length() + max + 2 ); // abcd      -3
            } else {
                return "..";
            }
        }
        return input;
    }
    
    public static String shortenTo(String input, int max, int min) {
        if (input != null && input.length() > max) {
            if (min+2 > max) {
                min = max-2;
            }
            if (max > 2) {
                String start = input.substring(0, min);
                String end = input.substring(input.length() - (max - min - 2));
                return start+".."+end;
            } else {
                return "..";
            }
        }
        return input;
    }
    
    public static String trim(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }
    
    public static String nullToString(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }
    
    public static String toLowerCase(String s) {
        return s != null ? s.toLowerCase(Locale.ENGLISH) : null;
    }
    
    /**
     * Removes leading and trailing whitespace and removes and duplicate
     * whitespace in the middle. Due to the way it works, it also replaces any
     * whitespace characters that are not a space with a space (e.g. tabs).
     * 
     * @param s The String
     * @see removeDuplicateWhitespace(String text)
     * @return The modified String or null if the given String was null
     */
    public static String trimAll(String s) {
        if (s == null) {
            return s;
        }
        return removeDuplicateWhitespace(s).trim();
    }
    
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    
    /**
     * Replaces all occurences of one or more consecutive whitespace characters
     * with a single space. So it also replaces any whitespace characters that
     * are not a space with a space (e.g. tabs).
     * 
     * @param text
     * @return 
     */
    public static String removeDuplicateWhitespace(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ");
    }
    
    private static final Pattern LINEBREAK_CHARACTERS = Pattern.compile("[\\r\\n]+");
    
    /**
     * Removes any linebreak characters from the given String and replaces them
     * with a space. Consecutive linebreak characters are replaced with only a
     * single space.
     * 
     * @param s The String (can be empty or null)
     * @return The modified String or null if the given String was null
     */
    public static String removeLinebreakCharacters(String s) {
        if (s == null) {
            return null;
        }
        return LINEBREAK_CHARACTERS.matcher(s).replaceAll(" ");
    }
    
    public static String append(String a, String sep, String b) {
        if (a == null || a.isEmpty()) {
            return b;
        }
        if (b == null || b.isEmpty()) {
            return a;
        }
        return a+sep+b;
    }
    
    /**
     * Checks if any of the String arguments is null or empty.
     * 
     * @param input A number of String arguments
     * @return true if at least one of the arguments is null or empty, false
     * otherwise
     */
    public static boolean isNullOrEmpty(String... input) {
        if (input == null) {
            return true;
        }
        for (String s : input) {
            if (s == null || s.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    public static final void main(String[] args) {
        System.out.println(shortenTo("abcdefghi", 8, 5));
    }
    
}