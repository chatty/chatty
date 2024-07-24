
package chatty.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Matcher;
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
    
    public static String join(Object[] array) {
        return join(Arrays.asList(array), ",");
    }
    
    public static String join(Object[] array, String delimiter) {
        return join(Arrays.asList(array), delimiter);
    }
    
    public static String join(Collection<?> items, String delimiter) {
        return join(items, delimiter, -1, -1);
    }
    
    public static String join(Collection<?> items, String delimiter, Function<Object, String> func) {
        return join(items, delimiter, -1, -1, func);
    }
    
    public static String join(Collection<?> items, String delimiter, int start) {
        return join(items, delimiter, start, -1);
    }
    
    public static String join(Collection<?> items, String delimiter, int start, int end) {
        return join(items, delimiter, start, end, null);
    }
    
    /**
     * Join the items in the given Collection.
     * 
     * @param items The Collection
     * @param delimiter The delimiter, put in between items
     * @param start The index of the first item to include in the result,
     * negative values are interpreted as 0, values larger than the Collection
     * will simply result in an empty result
     * @param end The index after the last item to include in the result,
     * negative values or values larger than the Collection will include all
     * items after the start (when start is 0, then this is the amount of items
     * included)
     * @param func Transform an element to a String
     * @return The resulting String (never null)
     */
    public static String join(Collection<?> items, String delimiter, int start, int end, Function<Object, String> func) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        start = start > -1 ? start : 0;
        end = end > -1 ? end : items.size();
        
        StringBuilder b = new StringBuilder();
        Iterator<?> it = items.iterator();
        int i = 0;
        while (it.hasNext()) {
            String next = func != null ? func.apply(it.next()) : it.next().toString();
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
    
    public static String firstToUpperCase(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1);
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
    
    /**
     * Removes all whitespace from a String.
     * 
     * @param input
     * @return 
     */
    public static String removeWhitespace(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!Character.isWhitespace(c)) {
                b.append(c);
            }
        }
        return b.toString();
    }
    
    /**
     * Removes all whitespace and the characters in the given sorted char array
     * from the String.
     * 
     * <p>
     * The function {@link getCharsFromString(String)} is recommended to make
     * the char array, since characters outside the BMP would not be seen as a
     * single character, but instead as their surrogates, so it could remove
     * parts of other characters as well.
     * </p>
     * <p>
     * This method was chosen for the performance and to keep it simple for it's
     * use-case, where most likely only ASCII characters are being removed. If
     * required, another function that removes codepoints could be added.
     * </p>
     * <p>
     * If the array is not sorted, the result is undefined, as per
     * {@link Arrays#binarySearch(char[], char)}.
     * </p>
     *
     * @param input The input String
     * @param chars Characters that are removed, must be sorted, can be null
     * @return 
     */
    public static String removeWhitespaceAndMore(String input, char[] chars) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!Character.isWhitespace(c)
                    && (chars == null || Arrays.binarySearch(chars, c) < 0)) {
                b.append(c);
            }
        }
        return b.toString();
    }
    
    /**
     * Adds all unique chars that are not a surrogate from the given String to a
     * sorted array.
     * 
     * @param chars
     * @return 
     */
    public static char[] getCharsFromString(String chars) {
        // Collect all unique non-surrogate characters
        Set<Character> unique = new HashSet<>();
        for (int i = 0; i < chars.length(); i++) {
            char c = chars.charAt(i);
            if (!Character.isSurrogate(c)) {
                unique.add(c);
            }
        }
        // Move to sorted array
        char[] result = new char[unique.size()];
        int index = 0;
        for (Character c : unique) {
            result[index] = c;
            index++;
        }
        Arrays.sort(result);
        return result;
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
    
    public static String append(String a, String sep, String bCond, String b) {
        if (a == null || a.isEmpty()) {
            if (!isNullOrEmpty(b)) {
                return bCond+b;
            }
            return b;
        }
        if (b == null || b.isEmpty()) {
            return a;
        }
        if (!isNullOrEmpty(b)) {
            return a+sep+bCond+b;
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
    
    public static final String UTF8_BOM = "\uFEFF";
    
    /**
     * Remove the UTF-8 BOM from the beginning of the input.
     * 
     * @param input
     * @return 
     */
    public static String removeUTF8BOM(String input) {
        if (input != null && input.startsWith(UTF8_BOM)) {
            return input.substring(1);
        }
        return input;
    }
    
    /**
     * Adds linebreaks to the input, in place of existing space characters, so
     * that each resulting line has the given maximum length. If there is no
     * space character where needed a line may be longer. The added linebreaks
     * don't count into the maximum line length.
     *
     * @param input The intput to modify
     * @param maxLineLength The maximum line length in number of characters
     * @param html If true, a "&lt;br /&gt;" will be added instead of a \n
     * @return 
     */
    public static String addLinebreaks(String input, int maxLineLength, boolean html) {
        if (input == null || input.length() <= maxLineLength) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder b = new StringBuilder();
        int lineLength = 0;
        for (int i=0;i<words.length;i++) {
            String word = words[i];
            if (b.length() > 0
                    && lineLength + word.length() > maxLineLength) {
                if (html) {
                    b.append("<br />");
                } else {
                    b.append("\n");
                }
                lineLength = 0;
            } else if (b.length() > 0) {
                b.append(" ");
                lineLength++;
            }
            b.append(word);
            lineLength += word.length();
        }
        return b.toString();
    }
    
    public static String aEmptyb(String value, String a, String b) {
        if (value == null || value.isEmpty()) {
            return a;
        }
        return String.format(b, value);
    }
    
    public static String concats(Object... args) {
        return concat(" ", args);
    }
    
    public static String concat(String sep, Object... args) {
        if (args.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean appended = false;
        for (Object arg : args) {
            if (appended) {
                b.append(sep);
                appended = false;
            }
            if (arg != null) {
                b.append(arg.toString());
                appended = true;
            }
        }
        return b.toString();
    }
    
    public static List<String> split(String input, char splitAt, int limit) {
        return split(input, splitAt, '"', '\\', limit, 1);
    }
    
    public static List<String> split(String input, char splitAt, int limit, int remove) {
        return split(input, splitAt, '"', '\\', limit, remove);
    }
    
    /**
     * Split the given input String by the {@code splitAt} character. Sections
     * enclosed in the {@code quote} character and characters prefixed by the
     * {@code escape} character aren't checked for the {@code splitAt}
     * character.
     * <p>
     * If the quote and escape characters are equal, escaping is no longer
     * possible, only quoting, except for a quote escaping itself (double
     * quotes) to insert a literal quote.
     * <p>
     * Whether quote/escape characters are removed from the result can be
     * controlled by the {@code remove} value:
     * <ul>
     * <li>0 - don't remove
     * <li>1 - remove from all parts, except result number {@code limit} (if
     * {@code limit} > 0)
     * <li>2 - remove from all parts
     * </ul>
     * 
     * @param input The input to be split
     * @param splitAt The split character
     * @param quote The quote character
     * @param escape The escape character, also used to escape the quote
     * character and itself
     * @param limit Maximum number of parts in the result (0 for high limit)
     * @param remove 0 - don't remove quote/escape characters, 1 - remove from
     * all parts (except result number "limit", if limit > 0), 2 - remove from
     * all parts
     * @return
     */
    public static List<String> split(String input, char splitAt, char quote, char escape, int limit, int remove) {
        if (input == null) {
            return null;
        }
        List<String> result = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        boolean escaped = false;
        int consecQuotes = 0;
        limit = Math.abs(limit);
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (c == quote) {
                consecQuotes++;
            }
            else {
                consecQuotes = 0;
            }
            
            // Add one escaped character
            if (escaped) {
                b.append(c);
                escaped = false;
            }
            // Next character escaped
            else if (c == escape && escape != quote) {
                escaped = true;
                if (remove == 0) {
                    b.append(c);
                }
            }
            // Begin and end quoted section
            else if (c == quote) {
                quoted = !quoted;
                if (remove == 0 || (escape == quote && consecQuotes % 2 == 0)) {
                    b.append(c);
                }
            }
            // Split character found, ignore if quoted or max count reached
            else if (c == splitAt && !quoted && result.size()+1 < limit) {
                result.add(b.toString());
                b = new StringBuilder();
                if (result.size()+1 == limit && remove < 2) {
                    // Add remaining text without parsing
                    result.add(input.substring(i+1));
                    return result;
                }
            }
            // Nothing special, just add character
            else {
                b.append(c);
            }
        }
        // Add last
        result.add(b.toString());
        return result;
    }
    
    public static String[] splitLines(String input) {
        return input.split("\r\n|\n|\r");
    }
    
    public static String quote(String input) {
        return quote(input, "\"");
    }
    
    public static String quote(String input, String quote) {
        if (input == null) {
            return null;
        }
        return quote+input.replaceAll(Pattern.quote(quote), quote+quote)+quote;
    }
    
    public static final NullComparator NULL_COMPARATOR = new NullComparator();

    private static class NullComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return 1;
            }
            if (o2 == null) {
                return -1;
            }
            return o1.compareTo(o2);
        }

    }
    
    public static String stringFromInputStream(InputStream inputStream) {
        return stringFromInputStream(inputStream, "UTF-8");
    }
    
    public static String stringFromInputStream(InputStream inputStream, String charset) {
        try (InputStream input = inputStream) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(charset);
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static String randomString(String[] input) {
        if (input.length == 0) {
            return null;
        }
        return input[ThreadLocalRandom.current().nextInt(input.length)];
    }
    
    public static int getLength(String content) {
        if (content == null) {
            return 0;
        }
        return content.length();
    }
    
    /**
     * Gets the given substring or the fallback if an error occurs.
     * 
     * @param input Where to get the substring from
     * @param start The start index (inclusive)
     * @param end The end index (exclusive)
     * @param fallback The fallback to use when an error occurs
     * @return The substring or fallback
     */
    public static String substring(String input, int start, int end, String fallback) {
        try {
            return input.substring(start, end);
        }
        catch (Exception ex) {
            return fallback;
        }
    }
    
    /**
     * See {@link replaceFunc(String, Pattern, Function)}.
     * 
     * @param input
     * @param regex
     * @param func
     * @return 
     */
    public static String replaceFunc(String input, String regex, Function<Matcher, String> func) {
        return replaceFunc(input, Pattern.compile(regex), func);
    }
    
    /**
     * Calls the given function for every match to get the replacement.
     * 
     * @param input The text to apply the replace to
     * @param pattern The Pattern object to use
     * @param func The function to apply to each match
     * @return The input with replacements performed
     */
    public static String replaceFunc(String input, Pattern pattern, Function<Matcher, String> func) {
        StringBuffer b = new StringBuffer();
        Matcher m = pattern.matcher(input);
        while (m.find()) {
            m.appendReplacement(b, Matcher.quoteReplacement(func.apply(m)));
        }
        m.appendTail(b);
        return b.toString();
    }
    
    /**
     * Test the similarity between two Strings.
     * 
     * {@link prepareForSimilarityComparison(String)} should normally be applied
     * to the Strings first.
     * 
     * @param a One String (must not be null)
     * @param b Another String (must not be null)
     * @param min The minimum similarity score the Strings need to reach
     * @param method The comparison algorithm (1 or 2)
     * @return The score if the Strings reach at least min similiarty score, 0
     * otherwise
     */
    public static float checkSimilarity(String a, String b, float min, int method) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1;
        }
        
        float sim;
        if (method == 2) {
            sim = getSimilarity2(a, b);
        }
        else {
            if (getLengthSimilarity(a, b) >= min) {
                sim = getSimilarity(a, b);
            }
            else {
                sim = 0;
            }
        }
        return sim >= min ? sim : 0;
    }
    
    public static float getLengthSimilarity(String a, String b) {
        return Math.min(a.length(), b.length()) / (float) Math.max(a.length(), b.length()) + 0.2f;
    }
    
    /**
     * Prepare for comparison by removing whitespace. This is in a separate
     * function so that it's not applied more often than necessary.
     * 
     * @param input
     * @return 
     */
    public static String prepareForSimilarityComparison(String input, char[] chars) {
        return removeWhitespaceAndMore(input, chars);
    }
    
    /**
     * Calculate the similarity between the given Strings. Basicially splits
     * each String into overlapping 2-long parts (bigrams) and counts how many
     * of them appear in both Strings, normalized by the number of possible
     * parts.
     * 
     * This function does not remove whitespace or change case of the String. If
     * required, this should be done before feeding the Strings into this, which
     * allows the Strings to be changed just once (e.g. if comparing one String
     * to many different Strings).
     * 
     * This is somewhat based on the compareTwoStrings method found here (MIT
     * License):
     * https://github.com/aceakash/string-similarity/blob/master/src/index.js
     * 
     * This appears to be based on the Sørensen–Dice coefficient, however other
     * implementations of it sometimes don't look at the count of each bigram,
     * but instead use a Set of bigrams and compare that (so something like
     * "aa" would be equal to "aaaaaaaaaaaaaa". I'm not sure what the "correct"
     * way to implement it is, however this version appears to work better.
     * 
     * @param a One String (must not be null)
     * @param b Another String (must not be null)
     * @return A float between 0 (not at all similiar) and 1.
     */
    public static float getSimilarity(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 1;
        }
        if (a.length() < 2 || b.length() < 2) {
            return 0;
        }
        //--------------------------
        // First String
        //--------------------------
        // Create a map of bigram counts for the first String
        Map<Integer, Integer> m = new HashMap<>(a.length());
        for (int i = 0; i < a.length() - 1; i++) {
            /**
             * Encoding two chars in one int seemed to have slightly better
             * performance than creating a lot of Strings.
             */
            Integer part = (a.charAt(i) + (a.charAt(i + 1) << 16));
            m.put(part, m.getOrDefault(part, 0) + 1);
        }
        //--------------------------
        // Second String
        //--------------------------
        // Count how many of the bigrams appear in both Strings
        int count = 0;
        for (int i = 0; i < b.length() - 1; i++) {
            Integer part = (b.charAt(i) + (b.charAt(i + 1) << 16));
            int c = m.getOrDefault(part, 0);
            if (c > 0) {
                count++;
                m.put(part, c - 1);
            }
        }
        /**
         * Each String contains "a.length() - 1" bigrams, so this is dividing
         * by the number of total possible bigrams.
         */
        return 2f * count / (a.length() + b.length() - 2);
    }
    
    /**
     * Based on {@link getSimilarity(String, String)}, but doesn't allow
     * duplicate bigrams, so it doesn't matter how often a bigram occurs in a
     * Strings, but rather only whether it occurs at all, which makes it more
     * lenient to differences in repetitions within a String.
     *
     * @param a One String (must not be null)
     * @param b Another String (must not be null)
     * @return A float between 0 (not at all similiar) and 1.
     */
    public static float getSimilarity2(String a, String b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 1;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 1;
        }
        if (a.length() < 2 || b.length() < 2) {
            return 0;
        }
        Set<Integer> setA = new HashSet<>();
        Set<Integer> setB = new HashSet<>();
        for (int i=0;i<a.length() - 1; i++) {
            Integer part = (a.charAt(i) + (a.charAt(i+1) << 16));
            setA.add(part);
        }
        int count = 0;
        for (int i=0;i<b.length() - 1; i++) {
            Integer part = (b.charAt(i) + (b.charAt(i+1) << 16));
            // Count if first occurence in b and also occured in a
            if (setB.add(part) && setA.contains(part)) {
                count++;
            }
        }
        return 2f*count / (setA.size() + setB.size());
    }
    
    public static String plural(String input, int num) {
        if (num == 0 || num > 1) {
            return input+"s";
        }
        return input;
    }
    
    public static String replaceLast(String string, String toReplace, String replacement) {
        int pos = string.lastIndexOf(toReplace);
        if (pos > -1) {
            return string.substring(0, pos)
                    + replacement
                    + string.substring(pos + toReplace.length());
        }
        return string;
    }
    
    public static final void main(String[] args) {
        System.out.println(shortenTo("abcdefghi", 8, 5));
        System.out.println(concats("a", null, "b", null));
    }
    
}