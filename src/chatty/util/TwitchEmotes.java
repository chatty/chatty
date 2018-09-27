
package chatty.util;

import chatty.Chatty;
import chatty.util.UrlRequest.LinesResult;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class TwitchEmotes {
    
    private static final Logger LOGGER = Logger.getLogger(TwitchEmotes.class.getName());
    
    private static final String EMOTESET_URL = "http://tduva.com/res/emotesetinfo.txt";
    
    private static final int CACHED_EMOTICONS_EXPIRE_AFTER = 60 * 60 * 24 * 3;
    private static final String FILE = Chatty.getCacheDirectory() + "emotesets2";
    
    private final SimpleCache2 cache;
    
    /**
     * 
     * 
     * @param listener Cannot be null
     */
    public TwitchEmotes(TwitchEmotesListener listener) {
        cache = new SimpleCache2("EmotesetInfo", FILE, CACHED_EMOTICONS_EXPIRE_AFTER);
        cache.setRefreshLinesCallback(() -> {
            UrlRequest request = new UrlRequest(EMOTESET_URL);
            request.setLabel("EmotesetInfo");
            LinesResult result = request.syncLines();
            if (result.getResponseCode() == 200) {
                return result.getResult();
            }
            return null;
        });
        cache.setHandleDataCallback(data -> {
            EmotesetInfo result = parseLines(data);
            if (result != null) {
                LOGGER.info("[EmotesetInfo] Loaded: "+result);
                listener.emotesetsReceived(result);
                return true;
            }
            return false;
        });
    }
    
    public void load() {
        cache.asyncLoad();
    }
    
    public void refresh() {
        cache.asyncRefresh();
    }
    
    private static EmotesetInfo parseLines(List<String> lines) {
        if (lines == null) {
            return null;
        }
        
        String[] plans = null;
        Map<String, Emoteset[]> stream2set = new HashMap<>();
        Map<Integer, Emoteset> emoteId2set = new HashMap<>();
        Map<Integer, Emoteset> setInfo = new HashMap<>();
        int errors = 0;
        for (String line : lines) {
            if (line.startsWith("//")) {
                Debugging.println("TwitchEmotes Comment: "+line);
            }
            else if (plans == null) {
                plans = line.trim().split(" ");
            }
            else {
                try {
                    if (line.startsWith(":")) {
                        parseNonStream(line, emoteId2set, setInfo);
                    } else {
                        parseStream(line, plans, stream2set, emoteId2set, setInfo);
                    }
                } catch (Exception ex) {
                    errors++;
                    if (errors > 100) {
                        LOGGER.warning("[Emotesets] Too many errors");
                        return null;
                    }
                    LOGGER.warning("[Emotesets] Error parsing '" + line + "': " + ex);
                }
            }
        }
        return new EmotesetInfo(stream2set, emoteId2set, setInfo);
    }
    
    private static void parseNonStream(String line,
            Map<Integer, Emoteset> id2set, Map<Integer, Emoteset> setInf) {
        line = line.trim();
        String[] split = line.split(" ");
        if (line.isEmpty() || split.length < 2) {
            // No data, or only name with no emoteset
            return;
        }
        String info = split[0].substring(1).replace("_", " ");
        for (int i=1;i<split.length;i++) {
            String[] emotesetSplit = split[i].split(":");
            int emoteset = Integer.parseInt(emotesetSplit[0]);
            if (emoteset == -1) {
                continue;
            }
            Emoteset setInfo = new Emoteset(emoteset, null, info);
            if (emotesetSplit.length == 2) {
                List<String> emotesSplit = Arrays.asList(emotesetSplit[1].split(","));
                emotesSplit.forEach(s -> { id2set.put(Integer.parseInt(s), setInfo); });
            }
            setInf.put(emoteset, setInfo);
        }
    }
    
    private static void parseStream(String line, String[] plans,
            Map<String, Emoteset[]> stream2sets,
            Map<Integer, Emoteset> id2set,
            Map<Integer, Emoteset> setInf) {
        String[] split = line.split(" ");
        if (line.trim().isEmpty() || split.length < 2) {
            // No data, or only name with no emoteset
            return;
        }
        String stream = split[0];
        Emoteset[] emotesets = new Emoteset[split.length - 1];
        boolean hasSets = false;
        for (int i=1;i<split.length;i++) {
            // Set
            String[] emotesetSplit = split[i].split(":");
            int emoteset = Integer.parseInt(emotesetSplit[0]);
            if (emoteset == -1) {
                // No valid set id, so continue with next
                continue;
            }
            hasSets = true;
            String plan = plans[i-1];
            Emoteset setInfo = new Emoteset(emoteset, stream, plan);
            if (emotesetSplit.length == 2) {
                List<String> emotesSplit = Arrays.asList(emotesetSplit[1].split(","));
                emotesSplit.forEach(s -> { id2set.put(Integer.parseInt(s), setInfo); });
            }
            emotesets[i-1] = setInfo;
            setInf.put(emoteset, setInfo);
        }
        if (hasSets) {
            // Only add if stream has any sets
            stream2sets.put(stream, emotesets);
        }
    }

    public static interface TwitchEmotesListener {
        public void emotesetsReceived(EmotesetInfo info);
    }
    
    public static class EmotesetInfo {
        
        public static final EmotesetInfo EMPTY = new EmotesetInfo(
                new HashMap<>(), new HashMap<>(), new HashMap<>());
        
        private final Map<String, Emoteset[]> stream2sets;
        private final Map<Integer, Emoteset> emoteId2set;
        private final Map<Integer, Emoteset> setInfo;
        
        public EmotesetInfo(Map<String, Emoteset[]> stream2sets,
                Map<Integer, Emoteset> emoteId2set,
                Map<Integer, Emoteset> set2stream) {
            this.stream2sets = stream2sets;
            this.emoteId2set = emoteId2set;
            this.setInfo = set2stream;
        }
        
        public Set<Emoteset> getEmotesetsByStream(String stream) {
            Emoteset[] sets = stream2sets.get(stream);
            if (sets != null) {
                Set<Emoteset> result = new HashSet<>();
                for (Emoteset set : sets) {
                    // Might have null values if a stream has "-1" sets
                    if (set != null) {
                        result.add(set);
                    }
                }
                return result;
            }
            return null;
        }
        
        public Emoteset getEmotesetInfo(int emoteset) {
            return setInfo.get(emoteset);
        }
        
        public Emoteset getEmotesetInfoByEmoteId(int emoteId) {
            return emoteId2set.get(emoteId);
        }
        
        @Override
        public String toString() {
            return String.format("%,d streams, %,d emotesets, %,d emotes",
                    stream2sets.size(),
                    setInfo.size(),
                    emoteId2set.size());
        }
        
    }
    
    public static class Emoteset {
        public final int emoteset_id;
        public final String product;
        public final String stream;
        
        public Emoteset(int emoteset_id, String stream, String tier) {
            this.emoteset_id = emoteset_id;
            this.product = tier;
            this.stream = stream;
        }
        
        @Override
        public String toString() {
            return stream+" "+product+" "+emoteset_id;
        }
    }
    
    public static boolean hasAccessTo(Set<Integer> accessTo, Set<Emoteset> emotesets) {
        for (Emoteset set : emotesets) {
            if (!accessTo.contains(set.emoteset_id)) {
                return false;
            }
        }
        return true;
    }
    
    // Just some testing stuff
    public static void main(String[] args) {
        String file = "H:/coding/java/netbeans/Chatty/util/ChattyUtil/emotesetinfo.txt";
        long start = System.currentTimeMillis();
        System.out.println(file);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            final EmotesetInfo info = parseLines(lines);
            System.out.println(info);
            
            long start2 = System.currentTimeMillis();
            System.out.println("---- Parsed, test some of the results ----");
            Consumer<String> test = new Consumer<String>() {

                @Override
                public void accept(String t) {
                    System.out.println(t+": "+info.getEmotesetsByStream(t));
                }
            };
            test.accept("tailsgaming");
            test.accept("wolfricgames");
            test.accept("6t4bites_aus");
            System.out.println(info.getEmotesetInfo(19655));
            System.out.println(info.getEmotesetInfo(12345));
            System.out.println(info.getEmotesetInfo(33));
            System.out.println("----");
            System.out.println((System.currentTimeMillis() - start)+"ms total, "+(System.currentTimeMillis() - start2)+"ms tests");
            
            // To keep program running for testing
            //(new Scanner(System.in)).next();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}
