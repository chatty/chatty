
package chatty.util.commands;

import chatty.util.StringUtil;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
class JsonPathItem implements Item {
    
    private static final Logger LOGGER = Logger.getLogger(JsonPathItem.class.getName());

    private final boolean isRequired;
    private final Item path;
    private final Item def;
    
    public JsonPathItem(Item path, Item def, boolean isRequired) {
        this.path = path;
        this.def = def;
        this.isRequired = isRequired;
    }
    
    @Override
    public String replace(Parameters parameters) {
        String value = path.replace(parameters);
        if (!Item.checkReq(isRequired, value)) {
            return null;
        }
        Object data = parameters.getObject("json");
        if (data == null) {
            LOGGER.warning("No JSON object");
            return null;
        }
        Object result;
        try {
            result = new Parser(value).parse().resolve(data);
        }
        catch (ParseException ex) {
            String msg = CustomCommand.makeErrorMessage(ex.getLocalizedMessage(), ex.getErrorOffset(), value, false);
            LOGGER.warning("Error parsing $j(): "+msg);
            return null;
        }
        
        String resultText = result == null ? "" : String.valueOf(result);
        if (resultText.isEmpty() && def != null) {
            resultText = def.replace(parameters);
        }
        if (!Item.checkReq(isRequired, resultText)) {
            return null;
        }
        return resultText;
    }
    
    @Override
    public String toString() {
        return "JsonPath "+path+"/"+def;
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, path, def);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, path, def);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JsonPathItem other = (JsonPathItem) obj;
        if (this.isRequired != other.isRequired) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        return Objects.equals(this.def, other.def);
    }
    
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + (this.isRequired ? 1 : 0);
        hash = 37 * hash + Objects.hashCode(this.path);
        hash = 37 * hash + Objects.hashCode(this.def);
        return hash;
    }
    
    private static interface PathItem {
        
        public Object resolve(Object data);
        
    }
    
    private static class TopItem implements PathItem {
        
        private final List<PathItem> items = new ArrayList<>();
        private StringBuilder key = new StringBuilder();
        
        public void add(PathItem item) {
            items.add(item);
        }
        
        public void addToKey(String text) {
            key.append(text);
        }
        
        public boolean keyFinished() {
            if (key.length() > 0) {
                add(new KeyItem(key.toString()));
                key = new StringBuilder();
                return true;
            }
            return false;
        }
        
        @Override
        public Object resolve(Object data) {
            if (data == null) {
                return null;
            }
            for (PathItem item : items) {
                data = item.resolve(data);
            }
            return data;
        }
        
    }
    
    private static class IndexItem implements PathItem {
        
        private final int index;
        
        public IndexItem(int index) {
            this.index = index;
        }

        @Override
        public Object resolve(Object data) {
            if (data instanceof JSONArray) {
                try {
                    JSONArray array = (JSONArray) data;
                    if (index == -1) {
                        return array.get(array.size() - 1);
                    }
                    return array.get(index);
                }
                catch (IndexOutOfBoundsException ex) {
                    return null;
                }
            }
            return null;
        }
        
    }
    
    private static class KeyItem implements PathItem {
        
        private final String key;
        
        public KeyItem(String key) {
            this.key = key;
        }

        @Override
        public Object resolve(Object data) {
            if (data instanceof JSONObject) {
                JSONObject object = (JSONObject) data;
                return object.get(key);
            }
            /**
             * Collect all values inside an array with the given key. This is
             * instead done with with [collect:...] to be more clear what it
             * does.
             */
//            if (data instanceof JSONArray) {
//                JSONArray result = new JSONArray();
//                for (Object o : (JSONArray) data) {
//                    if (o instanceof JSONObject) {
//                        Object value = ((JSONObject) o).get(key);
//                        if (value != null) {
//                            result.add(value);
//                        }
//                    }
//                }
//                return result;
//            }
            return null;
        }
        
    }
    
    private static class FuncItem implements PathItem {
        
        private final String key;
        
        public FuncItem(String key) {
            this.key = key;
        }

        @Override
        public Object resolve(Object data) {
            switch (key) {
                case "size":
                    if (data instanceof JSONArray) {
                        return ((JSONArray) data).size();
                    }
                    else if (data instanceof JSONObject) {
                        return ((JSONObject) data).size();
                    }
                    else if (data instanceof String) {
                        return ((String) data).length();
                    }
                    break;
                case "unique":
                    if (data instanceof JSONArray) {
                        JSONArray result = new JSONArray();
                        result.addAll(new LinkedHashSet<Object>((JSONArray) data));
                        return result;
                    }
                    break;
            }
            return null;
        }
        
    }
    
    private static class JoinItem implements PathItem {
        
        private final String delimiter;
        
        public JoinItem(String delimiter) {
            this.delimiter = delimiter;
        }

        @Override
        public Object resolve(Object data) {
            if (data instanceof JSONArray) {
                JSONArray array = (JSONArray) data;
                return StringUtil.join(array, delimiter);
            }
            return null;
        }
        
    }
    
    private static class FilterItem implements PathItem {
        
        private final String type;
        private final PathItem path;
        private final Pattern search;
        
        public FilterItem(String type, PathItem path, String search) {
            this.type = type;
            this.path = path;
            this.search = search != null ? Pattern.compile(search) : null;
        }

        @Override
        public Object resolve(Object data) {
            if (data instanceof JSONArray) {
                JSONArray array = (JSONArray) data;
                JSONArray result = new JSONArray();
                for (Object o : array) {
                    Object value = path.resolve(o);
                    if (value != null && (search == null || search.matcher(String.valueOf(value)).matches())) {
                        switch (type) {
                            case "filter":
                                result.add(o);
                                break;
                            case "collect":
                                result.add(value);
                                break;
                        }
                    }
                    if (type.equals("combine") && value instanceof JSONArray) {
                        if (value instanceof JSONArray) {
                            for (Object item : (JSONArray) value) {
                                // Check each item in the array instead of the
                                // value overall
                                if (item != null && (search == null || search.matcher(String.valueOf(item)).matches())) {
                                    result.add(item);
                                }
                            }
                        }
                    }
                }
                return result;
            }
            return null;
        }
        
    }
    
    private static class Parser {

        private final StringReader reader;
        
        public Parser(String text) {
            reader = new StringReader(text);
        }
        
        PathItem parse() throws ParseException {
            return parse(null);
        }
        
        private PathItem parse(String to) throws ParseException {
            TopItem result = new TopItem();
            while (reader.hasNext() && (to == null || !reader.peek().matches(to))) {
                if (reader.accept("[")) {
                    result.keyFinished();
                    result.add(bracketsItem());
                }
                else if (reader.accept("->")) {
                    if (!result.keyFinished()) {
//                        error("Key expected", 0);
                    }
                }
                else {
                    result.addToKey(reader.next());
                }
            }
            result.keyFinished();
            return result;
        }

        private PathItem bracketsItem() throws ParseException {
            PathItem result = null;
            String number = readAll("[0-9]");
            if (!number.isEmpty()) {
                result = new IndexItem(Integer.parseInt(number));
            }
            if (result == null) {
                String name = readAll("[a-z]");
                if (!name.isEmpty()) {
                    switch (name) {
                        case "last":
                            result =  new IndexItem(-1);
                            break;
                        case "unique":
                        case "size":
                            result =  new FuncItem(name);
                            break;
                        case "filter":
                        case "collect":
                        case "combine":
                            expect(":");
                            try {
                                result = filterItem(name);
                            }
                            catch (PatternSyntaxException ex) {
                                error("Invalid regex ("+ex.getLocalizedMessage()+")", 0);
                            }
                            break;
                        case "join":
                            String delimiter = ", ";
                            if (accept(":")) {
                                delimiter = param();
                            }
                            result = new JoinItem(delimiter);
                            break;
                    }
                }
            }
            if (result == null) {
                error("Invalid [ ] content", 0);
            }
            expect("]");
            return result;
        }
        
        private PathItem filterItem(String type) throws ParseException {
            PathItem path = parse("[]=]");
            String search = null;
            if (accept("=")) {
                search = param();
            }
            return new FilterItem(type, path, search);
        }
        
        private String param() throws ParseException {
            if (accept("'")) {
                return quotedLiteral("'");
            }
            return readAll("[^]]");
        }

        /**
         * If the parser encountered something unexpected, this will create an
         * error message and throw a ParseException.
         *
         * @param message
         * @throws ParseException
         */
        private void error(String message, int offset) throws ParseException {
            throw new ParseException(message, reader.pos() + offset);
        }
        
        private boolean accept(String text) {
            return reader.accept(text);
        }

        /**
         * A single character that the parser expects to be the next character,
         * and will throw an error if it's not there. This will advance the read
         * index.
         *
         * @param character A single character
         * @throws ParseException
         */
        private void expect(String character) throws ParseException {
            if (!reader.hasNext() || !reader.next().equals(character)) {
                error("Expected '" + character + "'", 0);
            }
        }

        /**
         * Read until it encounters a character not matching the given regex.
         * The character that caused it to stop won't be read.
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
        
        private String quotedLiteral(String quote) throws ParseException {
            StringBuilder b = new StringBuilder();
            while (reader.hasNext()) {
                if (reader.peek().equals(quote)) {
                    reader.next();
                    if (reader.hasNext() && reader.peek().equals(quote)) {
                        b.append(reader.next());
                    }
                    else {
                        break;
                    }
                }
                else {
                    b.append(reader.next());
                }
            }
            // Ending quote would have already been consumed
            return b.toString();
        }
        
    }
    
}
