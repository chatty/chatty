
package chatty.util.commands;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Custom Commands parser. Returns an Items object containing all the elements
 * of the Custom Command.
 * 
 * @author tduva
 */
public class Parser {

    private final String input;
    private final StringReader reader;
    
    public Parser(String text) {
        input = text;
        reader = new StringReader(text);
    }
    
    /**
     * Parses a Custom Command.
     * 
     * @return
     * @throws ParseException 
     */
    Items parse() throws ParseException {
        return parse(null);
    }
    
    /**
     * Reads in values until it encounters one of the characters defined in the
     * given regex parameter. It won't include the character that made it stop
     * in the result.
     * 
     * @param to Regex that will make it stop (single-character)
     * @return An Items object containing all the parsed elements
     * @throws ParseException If the parser encountered something unexpected
     */
    private Items parse(String to) throws ParseException {
        Items items = new Items();
        while (reader.hasNext() && (to == null || !reader.peek().matches(to))) {
            if (accept("$")) {
                Item item = specialThing();
                if (to == null) {
                    // Top-level item
                    items.add(new SpecialEscape(item));
                }
                else {
                    items.add(item);
                }
            } else if (accept("\\")) {
                if (reader.hasNext()) {
                    // Just read next character as literal
                    items.add(reader.next());
                }
            } else {
                items.add(reader.next());
            }
        }
        items.flush();
        return items;
    }
    
    /**
     * If the parser encountered something unexpected, this will create an error
     * message and throw a ParseException.
     * 
     * @param message
     * @throws ParseException 
     */
    private void error(String message, int offset) throws ParseException {
        throw new ParseException(message, reader.pos() + offset);
    }

    /**
     * A single character that the parser can accept as next character, but
     * won't throw an error if it's not there. If the character is indeed there
     * it will be read (advancing the read index), but not returned.
     * 
     * @param character A single character
     * @return true if the character is the next character, false otherwise
     */
    private boolean accept(String character) {
        if (reader.hasNext() && reader.peek().equals(character)) {
            reader.next();
            return true;
        }
        return false;
    }
    
    private boolean peek(String character) {
        return reader.hasNext() && reader.peek().equals(character);
    }
    
    private boolean acceptMatch(String regex) {
        if (reader.hasNext() && reader.peek().matches(regex)) {
            reader.next();
            return true;
        }
        return false;
    }
    
    /**
     * A single character that the parser expects to be the next character, and
     * will throw an error if it's not there. This will advance the read index.
     * 
     * @param character A single character
     * @throws ParseException 
     */
    private void expect(String character) throws ParseException {
        if (!reader.hasNext() || !reader.next().equals(character)) {
            error("Expected '"+character+"'", 0);
        }
    }
    
    /**
     * Read until it encounters a character not matching the given regex. The
     * character that caused it to stop won't be read.
     *
     * @param regex A regex matching a single character
     * @return The read String, may be empty
     */
    private String readAll(String regex) {
        StringBuilder b = new StringBuilder();
        while (reader.hasNext() && reader.peek().matches(regex)) {
            b.append(reader.next());
        }
        return b.toString();
    }

    /**
     * Read a single character that matches the given regex.
     * 
     * @param regex A regex matching a single character
     * @return The read String, may be empty
     */
    private String readOne(String regex) {
        if (reader.hasNext() && reader.peek().matches(regex)) {
            return reader.next();
        }
        return "";
    }
    
    /**
     * Parse stuff that occurs after a '$'.
     * 
     * @return
     * @throws ParseException 
     */
    private Item specialThing() throws ParseException {
        boolean isRequired = accept("$");
        String type = functionName();
        if (type.isEmpty()) {
            return replacement(isRequired);
        }
        else if (type.equals("if")) {
            return condition(isRequired);
        }
        else if (type.equals("join")) {
            return join(isRequired);
        }
        else if (type.equals("ifeq")) {
            return ifEq(isRequired);
        }
        else if (type.equals("switch")) {
            return switchFunc(isRequired);
        }
        else if (type.equals("lower")) {
            return lower(isRequired);
        }
        else if (type.equals("upper")) {
            return upper(isRequired);
        }
        else if (type.equals("rand")) {
            return rand(isRequired);
        }
        else if (type.equals("randnum")) {
            return randNum(isRequired);
        }
        else if (type.equals("datetime")) {
            return datetime(isRequired);
        }
        else if (type.equals("urlencode")) {
            return urlencode(isRequired);
        }
        else if (type.equals("sort")) {
            return sort(isRequired);
        }
        else if (type.equals("replace")) {
            return replace(isRequired);
        }
        else if (type.equals("is")) {
            return is(isRequired);
        }
        else if (type.equals("get")) {
            return get(isRequired);
        }
        else if (type.equals("calc")) {
            return calc(isRequired);
        }
        else if (type.equals("input")) {
            return input(isRequired);
        }
        else if (type.equals("request")) {
            return request(isRequired);
        }
        else {
            error("Invalid function '"+type+"'", 0);
            return null;
        }
    }
    
    /**
     * $(chan) $(3) $(5-) $3 $3-
     * 
     * @return 
     */
    private Item identifier() throws ParseException {
        String ref = readAll("[a-zA-Z0-9-_]");
        if (ref.isEmpty()) {
            error("Expected identifier", 1);
        }
        Matcher m = Pattern.compile("([0-9]+)(-)?").matcher(ref);
        if (m.matches()) {
            int index = Integer.parseInt(m.group(1));
            if (index == 0) {
                error("Invalid numeric identifier 0", 0);
            }
            boolean toEnd = m.group(2) != null;
            return new RangeIdentifier(index, toEnd);
        } else {
            return new Identifier(ref);
        }
    }
    
    private Item tinyIdentifier() throws ParseException {
        String ref = readOne("[0-9]");
        if (ref.isEmpty()) {
            error("Expected numeric identifier", 1);
        }
        int index = Integer.parseInt(ref);
        if (index == 0) {
            error("Invalid numeric identifer 0", 0);
        }
        boolean toEnd = false;
        if (accept("-")) {
            toEnd = true;
        }
        return new RangeIdentifier(index, toEnd);
    }

    private Item condition(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = peekParam();
        expect(",");
        Items output1 = param();
        Items output2 = null;
        if (accept(",")) {
            output2 = lastParam();
        }
        expect(")");
        return new If(identifier, isRequired, output1, output2);
    }
    
    private Item ifEq(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = peekParam();
        expect(",");
        Items compare = param();
        expect(",");
        Items output1 = param();
        Items output2 = null;
        if (accept(",")) {
            output2 = lastParam();
        }
        expect(")");
        return new IfEq(identifier, isRequired, compare, output1, output2);
    }
    
    private Item switchFunc(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = peekParam();
        Item def = new Items();
        expect(",");
        Map<Item, Item> cases = new LinkedHashMap<>();
        do {
            Item key = parse("[,):]");
            if (accept(":")) {
                if (cases.containsKey(key)) {
                    error("Duplicate case: "+key, 0);
                }
                cases.put(key, param());
            }
            else {
                // Default case must be the last one
                def = key;
                break;
            }
        } while (accept(","));
        expect(")");
        if (cases.isEmpty()) {
            error("No case found", -1);
        }
        return new Switch(identifier, cases, def, isRequired);
    }
    
    private Item join(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = peekParam();
        expect(",");
        Items separator = parse("[)]");
        if (separator.isEmpty()) {
            error("Expected separator string", 1);
        }
        expect(")");
        return new Join(identifier, separator, isRequired);
    }
    
    private Item lower(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = peekParam();
        expect(")");
        return new Lower(identifier, isRequired);
    }
    
    private Item upper(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = peekParam();
        expect(")");
        return new Upper(identifier, isRequired);
    }
    
    private Item rand(boolean isRequired) throws ParseException {
        expect("(");
        List<Item> params = new ArrayList<>();
        do {
            params.add(param());
        } while(accept(","));
        expect(")");
        return new Rand(isRequired, params);
    }
    
    private Item randNum(boolean isRequired) throws ParseException {
        expect("(");
        Item a = param();
        Item b = null;
        if (accept(",")) {
            b = param();
        }
        expect(")");
        return new RandNum(isRequired, a, b);
    }
    
    private Item datetime(boolean isRequired) throws ParseException {
        expect("(");
        Item format = param();
        Item timezone = null;
        Item locale = null;
        if (accept(",")) {
            timezone = param();
        }
        if (accept(",")) {
            locale = param();
        }
        expect(")");
        return new DateTime(format, timezone, locale, isRequired);
    }
    
    private Item urlencode(boolean isRequired) throws ParseException {
        expect("(");
        Item item = param();
        expect(")");
        return new UrlEncode(item, isRequired);
    }
    
    private Item sort(boolean isRequired) throws ParseException {
        expect("(");
        Item item = param();
        Item type = null;
        if (accept(",")) {
            type = param();
        }
        Item sep = null;
        if (accept(",")) {
            sep = param();
        }
        expect(")");
        return new Sort(item, sep, type, isRequired);
    }
    
    private Item replace(boolean isRequired) throws ParseException {
        expect("(");
        Item item = midParam();
        expect(",");
        Item search = midParam();
        expect(",");
        Item replace = param();
        Item type = null;
        if (accept(",")) {
            type = param();
        }
        expect(")");
        return new Replace(item, search, replace, isRequired, type);
    }
    
    private Item is(boolean isRequired) throws ParseException {
        expect("(");
        Item item = param();
        expect(")");
        return new Is(item, isRequired);
    }
    
    private Item get(boolean isRequired) throws ParseException {
        expect("(");
        Item item = param();
        Item item2 = null;
        if (accept(",")) {
            item2 = param();
        }
        expect(")");
        return new Get(item, item2, isRequired);
    }
    
    private Item calc(boolean isRequired) throws ParseException {
        expect("(");
        Item item = param();
        expect(")");
        return new Calc(item, isRequired);
    }
    
    private Item input(boolean isRequired) throws ParseException {
        expect("(");
        Item message = param();
        Item initial = null;
        Item type = null;
        if (accept(",")) {
            initial = param();
        }
        if (accept(",")) {
            type = param();
        }
        expect(")");
        return new Input(type, message, initial, isRequired);
    }
    
    private Item request(boolean isRequired) throws ParseException {
        expect("(");
        Item url = param();
        List<Item> options = new ArrayList<>();
        if (accept(",")) {
            do {
                options.add(param());
            } while (accept(","));
        }
        expect(")");
        return new Request(url, options, isRequired);
    }
    
    private Replacement replacement(boolean isRequired) throws ParseException {
        if (accept("(")) {
            Item identifier = identifier();
            Item args = null;
            if (accept(",")) {
                args = param();
            }
            expect(")");
            return new Replacement(identifier, args, isRequired);
        }
        else {
            Item identifier = tinyIdentifier();
            return new Replacement(identifier, null, isRequired);
        }
    }
    
    private String functionName() {
        return readAll("[a-zA-Z]");
    }
    
    private Items param() throws ParseException {
        return parse("[,)]");
    }
    
    /**
     * For parameters that have required parameters following, so they only
     * expect ",", so other stuff like ")" doesn't have to be escaped if used
     * literal.
     * 
     * @return
     * @throws ParseException 
     */
    private Items midParam() throws ParseException {
        return parse("[,]");
    }
    
    private Items lastParam() throws ParseException {
        return parse("[)]");
    }
    
    private Item peekParam() throws ParseException {
        return peek("$") ? param() : identifier();
    }
    

    
    public static void main(String[] args) {
        Identifier id = new Identifier("abc");
        Literal lit = new Literal("abcd");
        Items items = new Items();
        items.add(id);
        items.add(new Identifier("aijofwe"));
        items.add("_ffweffabc");
        items.add(new Join(new Identifier("cheese"), null, true));
        System.out.println(Item.getIdentifiersWithPrefix("_", id, lit, items));
    }
    
}
