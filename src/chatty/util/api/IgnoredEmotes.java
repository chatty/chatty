
package chatty.util.api;

import chatty.util.MiscUtil;
import static chatty.util.MiscUtil.biton;
import chatty.util.StringUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author tduva
 */
public class IgnoredEmotes {
    
    public static final int CHAT = 1;
    public static final int EMOTE_DIALOG = 2;
    public static final int TAB_COMPLETION = 4;
    public static final int ALL = CHAT | EMOTE_DIALOG | TAB_COMPLETION;
    
    private final Set<Item> items = new HashSet<>();
    
    public void setData(Collection<String> data) {
        items.clear();
        for (String input : data) {
            Item parsed = Item.parse(input);
            if (parsed != null && parsed.context > 0) {
                items.add(parsed);
            }
        }
    }
    
    public Collection<String> getData() {
        List<String> result = new ArrayList<>();
        items.forEach(item -> {
            result.add(item.toString());
        });
        return result;
    }
    
    public String add(Emoticon emote, int context) {
        remove(emote);
        Item item = Item.create(emote, context);
        if (item.context > 0) {
            items.add(item);
        }
        return item.toString();
    }
    
    public List<Item> getMatches(Emoticon emote) {
        List<Item> result = new ArrayList<>();
        for (Item item : items) {
            if (item.matches(emote, 0)) {
                result.add(item);
            }
        }
        return result;
    }
    
    public boolean remove(Emoticon emote) {
        return items.removeIf(item -> item.matches(emote, 0));
    }
    
    public boolean isIgnored(Emoticon emote, int context) {
        for (Item item : items) {
            if (item.matches(emote, context)) {
                return true;
            }
        }
        return false;
    }
    
    public static class Item {
        
        public static Item parse(String input) {
            // Kappa id:1234
            // KEKW type:ffz id:12345
            // a:b
            String id = null;
            Emoticon.Type type = null;
            String code = null;
            int context = CHAT | EMOTE_DIALOG | TAB_COMPLETION;
            String[] split = input.split(" ");
            for (String item : split) {
                int prefixStart = item.indexOf(":");
                if (prefixStart > 0 && prefixStart + 1 < item.length()) {
                    String prefix = item.substring(0, prefixStart);
                    String value = item.substring(prefixStart+1);
                    switch (prefix) {
                        case "id":
                            id = value;
                            break;
                        case "type":
                            type = getType(value);
                            break;
                        case "for":
                            context = getContext(value);
                            break;
                    }
                }
                else {
                    code = item;
                }
            }
            if (id != null || code != null) {
                return new Item(code, type, id, context);
            }
            return null;
        }
        
        public static Item create(Emoticon emote, int context) {
            String code = emote.code;
            if (emote instanceof CheerEmoticon) {
                code = ((CheerEmoticon) emote).getSimpleCode();
            }
            return new Item(code, emote.type, emote.stringId, context);
        }
        
        private static Emoticon.Type getType(String value) {
            for (Emoticon.Type type : Emoticon.Type.values()) {
                if (value.equalsIgnoreCase(type.id)) {
                    return type;
                }
            }
            return null;
        }
        
        private static int getContext(String value) {
            int result = 0;
            if (value.contains("c")) {
                result += CHAT;
            }
            if (value.contains("t")) {
                result += TAB_COMPLETION;
            }
            if (value.contains("d")) {
                result += EMOTE_DIALOG;
            }
            return result;
        }
        
        private static String fromContext(int in) {
            if (in == ALL) {
                return null;
            }
            if (in == 0) {
                return "0";
            }
            String result = "";
            if ((in & CHAT) != 0) {
                result += "c";
            }
            if ((in & TAB_COMPLETION) != 0) {
                result += "t";
            }
            if ((in & EMOTE_DIALOG) != 0) {
                result += "d";
            }
            return result;
        }
        
        public final String code;
        public final String id;
        public final Emoticon.Type type;
        public final int context;
        
        private Item(String code, Emoticon.Type type, String id, int context) {
            this.code = code;
            this.type = type;
            this.id = id;
            this.context = context;
        }
        
        public boolean matches(Emoticon emote, int context) {
            if (context > 0 && (this.context & context) == 0) {
                return false;
            }
            if (type != null && type != emote.type) {
                return false;
            }
            if (id != null
                    && (emote.stringId == null || !emote.stringId.equals(id))) {
                return false;
            }
            if (emote instanceof CheerEmoticon) {
                return code != null && ((CheerEmoticon) emote).getSimpleCode().equals(code);
            }
            return code != null && code.equals(emote.code);
        }
        
        @Override
        public String toString() {
            String result = code != null ? code : "";
            result = StringUtil.append(result, " ", "for:", fromContext(context));
            result = StringUtil.append(result, " ", "type:", type != null ? type.id : null);
            result = StringUtil.append(result, " ", "id:", id);
            return result;
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
            final Item other = (Item) obj;
            if (this.context != other.context) {
                return false;
            }
            if (!Objects.equals(this.code, other.code)) {
                return false;
            }
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return this.type == other.type;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + Objects.hashCode(this.code);
            hash = 97 * hash + Objects.hashCode(this.id);
            hash = 97 * hash + Objects.hashCode(this.type);
            hash = 97 * hash + this.context;
            return hash;
        }
        
    }
    
    public static void main(String[] args) {
        Item item = Item.parse("abc type:ffz in:c");
        System.out.println(item);
        Emoticon emote = (new Emoticon.Builder(Emoticon.Type.FFZ, "abc")).build();
        System.out.println(item.matches(emote, CHAT));
        System.out.println(Item.create(emote, 0));
    }
    
}
