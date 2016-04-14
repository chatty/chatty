
package chatty.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Replaces Strings based on a Map. Creates a single regex to search for parts
 * that should be replaced.
 * 
 * @author tduva
 */
public class Replacer {

    private final String[] replacementValues;
    private final Pattern pattern;
    
    /**
     * Create a Replacer that replaces the keys in the Map with their values.
     * The keys are compiled into a Pattern as alternatives, so each key has to
     * be a valid regex as to not interfere with the other keys.
     *
     * @param replacements The map of replacements
     * @throws IllegalArgumentException when one of the replacement keys does
     * not compile to a Pattern
     */
    public Replacer(Map<String, String> replacements) {
        this.replacementValues = new String[replacements.size()];
        StringBuilder sb = new StringBuilder("");
        int i = 0;
        for (String item : replacements.keySet()) {
            try {
                Pattern.compile(item);
            } catch (PatternSyntaxException ex) {
                throw new IllegalArgumentException("Invalid replacement pattern.", ex);
            }
            if (sb.length() != 0) {
                sb.append("|");
            }
            sb.append("(").append(item).append(")");
            
            // Add the replacement values to the array in the same order as the
            // groups
            replacementValues[i] = replacements.get(item);
            i++;
        }
        pattern = Pattern.compile(sb.toString());
    }
    
    /**
     * Replaces anything in the input String, based on the Map specified for
     * this Replacer.
     * 
     * @param input
     * @return 
     */
    public String replace(String input) {
        int lastAppendPos = 0;
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(input, lastAppendPos, matcher.start());
            sb.append(getReplacement(matcher));
            lastAppendPos = matcher.end();
        }
        sb.append(input, lastAppendPos, input.length());
        return sb.toString();
    }
    
    /**
     * Gets the replacement for the given Matcher. Gets the index of the group
     * that matched and returns the replacement value for that index from the
     * array of replacements.
     * 
     * @param matcher
     * @return 
     */
    private String getReplacement(Matcher matcher) {
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null) {
                return replacementValues[--i];
            }
        }
        return null;
    }
}