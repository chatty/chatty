
package chatty.util.commands;

import chatty.util.StringUtil;
import java.text.ParseException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class CustomCommand {
    
    private final String name;
    private final String chan;
    private final Items items;
    private final String raw;
    private final String error;
    private final String singleLineError;

    private CustomCommand(String name, String chan, Items items, String raw) {
        this.items = items;
        this.error = null;
        this.singleLineError = null;
        this.name = name;
        this.chan = chan;
        this.raw = raw;
    }

    private CustomCommand(String name, String chan, String error, String singleLineError, String raw) {
        this.items = null;
        this.error = error;
        this.singleLineError = singleLineError;
        this.name = name;
        this.chan = chan;
        this.raw = raw;
    }
    
    public String replace(Parameters parameters) {
        return items.replace(parameters);
    }
    
    /**
     * The raw input this command was created with.
     * 
     * @return 
     */
    public String getRaw() {
        return raw;
    }
    
    @Override
    public String toString() {
        if (items != null) {
            return items.toString();
        }
        return error;
    }
    
    /**
     * Get the error formatted with linebreaks. The error position in the
     * excerpt is point at from below.
     * 
     * @return The error message, or null when there is no error
     */
    public String getError() {
        return error;
    }
    
    /**
     * Check if there is an error.
     * 
     * @return true there is an error, false otherwise
     */
    public boolean hasError() {
        return error != null;
    }
    
    /**
     * Get the error formatted without linebreaks. The error position in the
     * excerpt is marked in the excerpt itself.
     * 
     * @return The error message, or null when there is no error
     */
    public String getSingleLineError() {
        return singleLineError;
    }
    
    public boolean hasName() {
        return !StringUtil.isNullOrEmpty(name);
    }
    
    public String getName() {
        return name;
    }
    
    public boolean hasChan() {
        return !StringUtil.isNullOrEmpty(chan);
    }
    
    public String getChan() {
        return chan;
    }
    
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return items.getIdentifiersWithPrefix(prefix);
    }
    
    /**
     * Returns all non-numeric identifiers that are part of top-level required
     * replacements. Since it only searches the top-level, not all identifiers
     * that can result in an "insufficient parameters" when executed may be
     * returned (this can be useful for something like $rand(), where it's only
     * decided at execution time what identifier is actually used).
     * 
     * @return 
     */
    public Set<String> getRequiredIdentifiers() {
        return items.getRequiredIdentifiers();
    }
    
    public String getCommandName() {
        if (items != null && !items.isEmpty()) {
            Item firstToken = items.getItem(0);
            if (firstToken instanceof Literal) {
                String text = ((Literal)firstToken).getLiteral();
                if (text.startsWith("/")) {
                    if (text.contains(" ")) {
                        return text.substring(1, text.indexOf(" "));
                    } else {
                        return text.substring(1);
                    }
                }
            }
        }
        return null;
    }
    
    public static CustomCommand parse(String input) {
        return parseCustom(null, null, input, "$", "\\");
    }
    
    public static CustomCommand parseCustom(String input, String special, String escape) {
        return parseCustom(null, null, input, special, escape);
    }

    public static CustomCommand parse(String name, String chan, String input) {
        return parseCustom(name, chan, input, "$", "\\");
    }
    
    public static CustomCommand parseCustom(String name, String chan, String input, String special, String escape) {
        if (special == null) {
            special = "$";
        }
        if (escape == null) {
            escape = "\\";
        }
        Parser parser = new Parser(input, special, escape);
        try {
            return new CustomCommand(name, chan, parser.parse(), input);
        } catch (ParseException ex) {
            return new CustomCommand(
                    name, chan,
                    makeErrorMessage(ex.getLocalizedMessage(), ex.getErrorOffset(), input, false),
                    makeErrorMessage(ex.getLocalizedMessage(), ex.getErrorOffset(), input, true),
                    input
            );
        }
    }
    
    /**
     * Creates an error message containing the error description, position and
     * an excerpt of the input with the position marked.
     * 
     * Optionally it can either format the message to be on several lines, with
     * the error position pointed at from below, or output just one line, with
     * the error position pointed at from the right in the excerpt itself.
     * 
     * @param input The original input
     * @param pos The position of the error
     * @param error The error description
     * @param singleLine No linebreaks in resulting message
     * @return The error message
     */
    public static String makeErrorMessage(String error, int pos, String input,
            boolean singleLine) {
        if (pos == -1) {
            // For errors before parsing started
            return error;
        }
        final int before = 30;
        final int after = 20;
        final String dotdot = "[..]";
        
        int start = pos > before+dotdot.length() ? pos - before : 0;
        int end = input.length() > pos + after + dotdot.length() ? pos + after : input.length();
        int displayPos = pos - start;
        String excerpt = input.substring(start, end);
        if (start > 0) {
            excerpt = dotdot+excerpt;
            displayPos += dotdot.length();
        }
        if (end < input.length()) {
            excerpt = excerpt+dotdot;
        }
        if (singleLine) {
            String excerpt1 = excerpt.substring(0, displayPos);
            int endExcerptStart = displayPos + 1;
            if (endExcerptStart > excerpt.length()) {
                // If the pos is right at the end, there may not be an extra
                // character to show/point at, so restrict to length
                endExcerptStart = excerpt.length();
            }
            String excerpt2 = excerpt.substring(displayPos, endExcerptStart);
            String excerpt3 = excerpt.substring(endExcerptStart);
            return String.format("%s at pos %s (\"%s%s<<<%s\")",
                error,
                pos+1,
                excerpt1,
                excerpt2,
                excerpt3);
        }
        return String.format("%s at pos %s\n %s\n %s^",
                error,
                pos+1,
                excerpt,
                String.join("", Collections.nCopies(displayPos, " ")));
    }
    
    public static CustomCommand createDefault(String commandName) {
        return parse(commandName+" $1-");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CustomCommand other = (CustomCommand) obj;
        if (!Objects.equals(this.items, other.items)) {
            return false;
        }
        if (!Objects.equals(this.error, other.error)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + Objects.hashCode(this.items);
        hash = 73 * hash + Objects.hashCode(this.error);
        return hash;
    }

    public static void main(String[] args) {
        CustomCommand command = CustomCommand.parse("$lower(abc) $$1- $$(blah) fewaf $afwe");
        System.out.println(command.error);
        System.out.println(command.getRequiredIdentifiers());
    }
    
}
