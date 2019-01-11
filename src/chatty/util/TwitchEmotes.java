
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
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
    
    public static EmotesetInfo parseLines(List<String> lines) {
        if (lines == null) {
            return null;
        }
        
        String[] plans = null;
        ArrayList<Emoteset> bySet = new ArrayList<>();
        ArrayList<Emoteset[]> byStream = new ArrayList<>();
        ArrayList<EmoteIdAndSet> byId = new ArrayList<>();
        int errors = 0;
        for (String line : lines) {
            if (line.startsWith("//")) {
                // Comment
            }
            else if (plans == null) {
                // First non-comment line
                plans = line.trim().split(" ");
            }
            else {
                try {
                    if (line.startsWith(":")) {
                        parseNonStream(line, byId, bySet);
                    } else {
                        parseStream(line, plans, byStream, byId, bySet);
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
        Collections.sort(byStream, EmotesetInfo.BY_STREAM);
        Collections.sort(byId, EmotesetInfo.BY_ID);
        Collections.sort(bySet, EmotesetInfo.BY_SET);
        byStream.trimToSize();
        byId.trimToSize();
        bySet.trimToSize();
        return new EmotesetInfo(byStream, byId, bySet);
    }
    
    private static void parseNonStream(String line,
            List<EmoteIdAndSet> byId, List<Emoteset> bySet) {
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
            // Stream is null, but this it not added to byStream, so it's fine
            Emoteset setInfo = new Emoteset(emoteset, null, info);
            if (emotesetSplit.length == 2) {
                List<String> emotesSplit = Arrays.asList(emotesetSplit[1].split(","));
                emotesSplit.forEach(s -> {
                    byId.add(new EmoteIdAndSet(Integer.parseInt(s), setInfo));
                });
            }
            bySet.add(setInfo);
        }
    }
    
    private static void parseStream(String line, String[] plans,
            List<Emoteset[]> byStream,
            List<EmoteIdAndSet> byId,
            List<Emoteset> bySet) {
        String[] split = line.split(" ");
        if (line.trim().isEmpty() || split.length < 2) {
            // No data, or only name with no emoteset
            return;
        }
        String stream = split[0];
        Emoteset[] emotesets = new Emoteset[split.length - 1];
        boolean hasSets = false;
        boolean hasNull = false;
        for (int i=1;i<split.length;i++) {
            // Set
            String[] emotesetSplit = split[i].split(":");
            int emoteset = Integer.parseInt(emotesetSplit[0]);
            if (emoteset == -1) {
                // No valid set id, so continue with next
                hasNull = true;
                continue;
            }
            hasSets = true;
            String plan = plans[i-1];
            Emoteset setInfo = new Emoteset(emoteset, stream, plan);
            if (emotesetSplit.length == 2) {
                List<String> emotesSplit = Arrays.asList(emotesetSplit[1].split(","));
                emotesSplit.forEach(s -> {
                    byId.add(new EmoteIdAndSet(Integer.parseInt(s), setInfo));
                });
            }
            emotesets[i-1] = setInfo;
            bySet.add(setInfo);
        }
        if (hasSets) {
            // Only add if stream has any sets
            if (hasNull) {
                // Remove null elements (mostly this probably isn't necessary,
                // since something like this doesn't seem to be in the data, but
                // just in case)
                emotesets = Arrays.stream(emotesets).filter(e -> e != null).toArray(Emoteset[]::new);
            }
            byStream.add(emotesets);
        }
    }

    public static interface TwitchEmotesListener {
        public void emotesetsReceived(EmotesetInfo info);
    }
    
    public static class EmotesetInfo {
        
        public static final EmotesetInfo EMPTY = new EmotesetInfo(
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        
        static final Comparator<Emoteset> BY_SET = new Comparator<Emoteset>() {

            @Override
            public int compare(Emoteset o1, Emoteset o2) {
                return Integer.compare(o1.emoteset_id, o2.emoteset_id);
            }
        };
        
        static final Comparator<Emoteset[]> BY_STREAM = new Comparator<Emoteset[]>() {

            @Override
            public int compare(Emoteset[] o1, Emoteset[] o2) {
                return o1[0].stream.compareTo(o2[0].stream);
            }
        };
        
        static final Comparator<EmoteIdAndSet> BY_ID = new Comparator<EmoteIdAndSet>() {

            @Override
            public int compare(EmoteIdAndSet o1, EmoteIdAndSet o2) {
                return Integer.compare(o1.id, o2.id);
            }
        };
        
        private final List<Emoteset> bySet;
        private final List<Emoteset[]> byStream;
        private final List<EmoteIdAndSet> byId;
        
        public EmotesetInfo(List<Emoteset[]> byStream,
                List<EmoteIdAndSet> byId,
                List<Emoteset> bySet) {
            this.bySet = bySet;
            this.byStream = byStream;
            this.byId = byId;
        }
        
        public Set<Emoteset> getEmotesetsByStream(String stream) {
            Emoteset[] search = new Emoteset[]{new Emoteset(-1, stream, null)};
            int index = Collections.binarySearch(byStream, search, BY_STREAM);
            if (index < 0) {
                return null;
            }
            Emoteset[] sets = byStream.get(index);
            Set<Emoteset> result = new HashSet<>();
            for (Emoteset set : sets) {
                // Might have null values if a stream has "-1" sets
                if (set != null) {
                    result.add(set);
                }
            }
            return result;
        }
        
        public Emoteset getEmotesetInfo(int emoteset) {
            Emoteset search = new Emoteset(emoteset, null, null);
            int index = Collections.binarySearch(bySet, search, BY_SET);
            if (index < 0) {
                return null;
            }
            return bySet.get(index);
        }
        
        public Emoteset getEmotesetInfoByEmoteId(int emoteId) {
            EmoteIdAndSet search = new EmoteIdAndSet(emoteId, null);
            int index = Collections.binarySearch(byId, search, BY_ID);
            if (index < 0) {
                return null;
            }
            return byId.get(index).set;
        }
        
        @Override
        public String toString() {
            return String.format("%,d streams, %,d emotesets, %,d emotes",
                    byStream.size(),
                    bySet.size(),
                    byId.size());
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
    
    public static class EmoteIdAndSet {
        
        public final int id;
        public final Emoteset set;
        
        public EmoteIdAndSet(int id, Emoteset set) {
            this.id = id;
            this.set = set;
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
    public static void main(String[] args) throws Exception {
        String file = "H:/coding/java/netbeans/Chatty/util/ChattyUtil/emotesets/emotesetinfo_sorted.txt";
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
            Consumer<String> test2 = new Consumer() {

                @Override
                public void accept(Object t) {
                    for (String l : lines) {
                        if (l.startsWith(t+" ")) {
                            System.out.println(l);
                            return;
                        }
//                        if (l.contains(",158757,")) {
//                            System.out.println(l);
//                            return;
//                        };
                    }
                    System.out.println("Not found");
                }
            };
            
            System.out.println("BY STREAM");
            test.accept("tailsgaming");
            test.accept("wolfricgames");
            test.accept("6t4bites_aus");
            System.out.println("BY SET");
            System.out.println(info.getEmotesetInfo(19655));
            System.out.println(info.getEmotesetInfo(12345));
            System.out.println(info.getEmotesetInfo(12345));
            System.out.println(info.getEmotesetInfo(12345));
            System.out.println(info.getEmotesetInfo(12345));
            System.out.println(info.getEmotesetInfo(12345));
            System.out.println(info.getEmotesetInfo(33));
            System.out.println("BY ID");
            System.out.println(info.getEmotesetInfoByEmoteId(425618));
            System.out.println(info.getEmotesetInfoByEmoteId(1537200));
            System.out.println("----");
//            test2.accept("tailsgaming");
//            test2.accept("elorie");
            System.out.println("----");
            System.out.println((System.currentTimeMillis() - start)+"ms total, "+(System.currentTimeMillis() - start2)+"ms tests");
//            Thread.sleep(8000);
//            test2.accept("elorie");
//            test2.accept("elorie");
//            test2.accept("elorie");
//            test2.accept("elorie");
            // To keep program running for testing
            
            (new Scanner(System.in)).next();
            System.out.println(info);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
}
