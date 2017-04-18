
package chatty.util.commands;

import chatty.util.StringUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
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
    
    public Items parse() throws ParseException {
        return parse(null);
    }
    
    private Items parse(String to) throws ParseException {
        Items items = new Items();
        while (reader.hasNext() && (to == null || !reader.peek().matches(to))) {
            if (accept("$")) {
                items.add(specialThing());
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
    
    private void error(String message) throws ParseException {
        int pos = reader.pos() + 1;
        String errorPos = input.substring(0, pos)+"<"+pos+">"+input.substring(pos, input.length());
        throw new ParseException(message+" at pos <"+pos+"> ["+errorPos+"]", reader.pos());
    }

    private boolean accept(String character) {
        if (reader.hasNext() && reader.peek().equals(character)) {
            reader.next();
            return true;
        }
        return false;
    }
    
    private boolean acceptMatch(String regex) {
        if (reader.hasNext() && reader.peek().matches(regex)) {
            reader.next();
            return true;
        }
        return false;
    }
    
    private void expect(String character) throws ParseException {
        if (!reader.hasNext() || !reader.next().equals(character)) {
            error("Expected "+character);
        }
    }
    
    private Item specialThing() throws ParseException {
        boolean isRequired = false;
        if (accept("$")) {
            isRequired = true;
        }
        String type = name();
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
        else {
            error("Invalid function '"+type+"'");
            return null;
        }
    }
    
    /**
     * $(chan) $(3) $(5-) $3 $3-
     * 
     * @return 
     */
    private Item identifier() throws ParseException {
        String ref = read("[a-zA-Z0-9-_]");
        Matcher m = Pattern.compile("([0-9])+(-)?").matcher(ref);
        if (ref.isEmpty()) {
            error("Expected identifier");
        }
        if (m.matches()) {
            int index = Integer.parseInt(m.group(1));
            if (index == 0) {
                error("Invalid index 0");
            }
            boolean toEnd = m.group(2) != null;
            return new RangeIdentifier(index, toEnd);
        } else {
            return new Identifier(ref);
        }
    }
    
    private Item tinyIdentifier() throws ParseException {
        String ref = read("[0-9]");
        if (ref.isEmpty()) {
            error("Expected number");
        }
        int index = Integer.parseInt(ref);
        if (index == 0) {
            error("Invalid index 0");
        }
        boolean toEnd = false;
        if (accept("-")) {
            toEnd = true;
        }
        return new RangeIdentifier(index, toEnd);
    }

    private Item condition(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = identifier();
        expect(",");
        Items output1 = param();
        Items output2 = null;
        if (accept(",")) {
            output2 = lastParam();
        }
        expect(")");
        return new Condition(identifier, isRequired, output1, output2);
    }
    
    private Item ifEq(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = identifier();
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
    
    private Item join(boolean isRequired) throws ParseException {
        expect("(");
        Item identifier = identifier();
        expect(",");
        Items separator = parse("[)]");
        if (separator.isEmpty()) {
            error("Expected separator string");
        }
        expect(")");
        return new Join(identifier, separator, isRequired);
    }
    
    private Replacement replacement(boolean isRequired) throws ParseException {
        if (accept("(")) {
            Item identifier = identifier();
            expect(")");
            return new Replacement(identifier, isRequired);
        }
        else {
            Item identifier = tinyIdentifier();
            return new Replacement(identifier, isRequired);
        }
    }
    
    private String name() {
        return read("[a-zA-Z]");
    }
    
    private Items param() throws ParseException {
        return parse("[,)]");
    }
    
    private Items lastParam() throws ParseException {
        return parse("[)]");
    }
    
    private String read(String regex) {
        StringBuilder b = new StringBuilder();
        while (reader.hasNext() && reader.peek().matches(regex)) {
            b.append(reader.next());
        }
        return b.toString();
    }
    
    
    static interface Item {
        
        /**
         * Return the text with any special tokens replaced with the given
         * parameters, depending on the individual implementation of the Item.
         * For example a Replacement would simply look up the value of the
         * parameter and return that, a Literal would just return it's text
         * unchanged.
         * 
         * A null return value indicates that a required parameter was not
         * found, which mostly means that the entire process should be aborted.
         * If a non-required parameter was not found, then an empty String may
         * be returned.
         * 
         * @param parameters
         * @return 
         */
        public String replace(Parameters parameters);
        
        /**
         * Returns all identifiers that start with the given prefix (can be
         * empty to return all).
         * 
         * @param prefix
         * @return A set of identifiers, empty if none are found
         */
        public Set<String> getIdentifiersWithPrefix(String prefix);
        
        public static Set<String> getIdentifiersWithPrefix(String prefix, Object... input) {
            Set<String> output = new HashSet<>();
            for (Object value : input) {
                if (value != null) {
                    if (value instanceof String) {
                        if (((String)value).startsWith(prefix)) {
                            output.add((String)value);
                        }
                    } else if (value instanceof Item) {
                        Set<String> value2 = ((Item)value).getIdentifiersWithPrefix(prefix);
                        if (value2 != null) {
                            output.addAll(value2);
                        }
                    }
                }
            }
            return output;
        }
        
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
    
    static class Literal implements Item {

        private final String literal;
        
        public Literal(String literal) {
            this.literal = literal;
        }
        
        @Override
        public String replace(Parameters parameters) {
            return literal;
        }
        
        @Override
        public String toString() {
            return "'"+literal+"'";
        }
        
        public String getLiteral() {
            return literal;
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return null;
        }
        
    }
    
    static class Replacement implements Item {

        private final boolean isRequired;
        private final Item identifier;
        
        public Replacement(Item name, boolean isRequired) {
            this.identifier = name;
            this.isRequired = isRequired;
        }
        
        @Override
        public String replace(Parameters parameters) {
            String value = identifier.replace(parameters);
            if (value != null && !value.isEmpty()) {
                return value;
            }
            return isRequired ? null : "";
        }
        
        @Override
        public String toString() {
            return (isRequired ? "$" : "")+identifier.toString();
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return identifier.getIdentifiersWithPrefix(prefix);
        }
        
    }
    
    static class Condition implements Item {

        private final boolean isRequired;
        private final Item identifier;
        private final Items output1;
        // May be null
        private final Items output2;
        
        public Condition(Item name, boolean isRequired, Items output1, Items output2) {
            this.isRequired = isRequired;
            this.identifier = name;
            this.output1 = output1;
            this.output2 = output2;
        }
        
        @Override
        public String replace(Parameters parameters) {
            String value = identifier.replace(parameters);
            if (value != null && !value.isEmpty()) {
                return output1.replace(parameters, isRequired);
            }
            if (output2 != null) {
                return output2.replace(parameters, isRequired);
            }
            return isRequired ? null : "";
        }
        
        @Override
        public String toString() {
            return "If "+identifier+" ? "+output1+" : "+output2;
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return Item.getIdentifiersWithPrefix(prefix, identifier, output1, output2);
        }
        
    }
    
    static class IfEq implements Item {

        private final boolean isRequired;
        private final Item identifier;
        private final Items compare;
        private final Items output1;
        // May be null
        private final Items output2;
        
        public IfEq(Item identifier, boolean isRequired, Items compare,
                Items output1, Items output2) {
            this.identifier = identifier;
            this.isRequired = isRequired;
            this.compare = compare;
            this.output1 = output1;
            this.output2 = output2;
        }
        
        @Override
        public String replace(Parameters parameters) {
            String value = identifier.replace(parameters);
            String compareTo = compare.replace(parameters);
            if (Objects.equals(value, compareTo)) {
                return output1.replace(parameters, isRequired);
            }
            if (output2 != null) {
                return output2.replace(parameters, isRequired);
            }
            return isRequired ? null : "";
        }

        @Override
        public String toString() {
            return "If "+identifier+" == "+compare+" ? "+output1+" : "+output2;
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return Item.getIdentifiersWithPrefix(prefix, identifier, compare, output1, output2);
        }

    }
    
    static class Items implements Item {
        
        private final List<Item> collection;
        private StringBuilder builder = new StringBuilder();
        
        public Items() {
            this.collection = new ArrayList<>();
        }
        
        public Items(List<Item> collection) {
            this.collection = collection;
        }
        
        public void add(Item item) {
            flush();
            this.collection.add(item);
        }
        
        public void add(String literal) {
            builder.append(literal);
        }
        
        public Item getItem(int index) {
            return collection.get(index);
        }
        
        public void flush() {
            if (builder.length() > 0) {
                collection.add(new Literal(builder.toString()));
                builder = new StringBuilder();
            }
        }
        
        public boolean isEmpty() {
            return collection.isEmpty();
        }
        
        public String replace(Parameters parameters, boolean isRequired) {
            String result = replace(parameters);
            return result != null || isRequired ? result : "";
        }

        @Override
        public String replace(Parameters parameters) {
            StringBuilder b = new StringBuilder();
            for (Item item : collection) {
                String replaced = item.replace(parameters);
                if (replaced == null) {
                    return null;
                }
                b.append(replaced);
            }
            return b.toString();
        }
        
        @Override
        public String toString() {
            return collection.toString();
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return Item.getIdentifiersWithPrefix(prefix, collection.toArray());
        }
        
    }
    
    static class RangeIdentifier implements Item {
        
        private final int index;
        private final boolean toEnd;
        
        public RangeIdentifier(int index, boolean toEnd) {
            this.index = index;
            this.toEnd = toEnd;
        }

        @Override
        public String replace(Parameters parameters) {
            Collection<String> range = parameters.getRange(index-1, toEnd);
            if (range == null) {
                return null;
            }
            return StringUtil.join(range, " ");
        }
        
        @Override
        public String toString() {
            return "$"+index+(toEnd ? "-" : "");
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return null;
        }
        
    }
    
    static class Join implements Item {
        
        private final boolean isRequired;
        private final Item identifier;
        private final Items separator;
        
        public Join(Item identifier, Items separator, boolean isRequired) {
            this.identifier = identifier;
            this.separator = separator;
            this.isRequired = isRequired;
        }

        @Override
        public String replace(Parameters parameters) {
            String value = identifier.replace(parameters);
            if (value != null && !value.isEmpty()) {
                String sep = separator.replace(parameters);
                if (sep == null) {
                    return null;
                }
                return value.replaceAll(" ", sep);
            }
            if (isRequired) {
                return null;
            }
            return "";
        }
        
        @Override
        public String toString() {
            return "JOIN:"+identifier+"/"+separator;
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return Item.getIdentifiersWithPrefix(prefix, identifier, separator);
        }
        
    }
    
    static class Identifier implements Item {
    
        private final String name;
        
        public Identifier(String name) {
            this.name = name.toLowerCase();
        }

        @Override
        public String replace(Parameters parameters) {
            return parameters.get(name);
        }
        
        @Override
        public String toString() {
            return "$"+name;
        }

        @Override
        public Set<String> getIdentifiersWithPrefix(String prefix) {
            return Item.getIdentifiersWithPrefix(prefix, name);
        }
        
    }
    
}
