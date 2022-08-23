
package chatty.util.api;

import chatty.Chatty;
import chatty.Helper;
import chatty.gui.emoji.EmojiUtil;
import chatty.util.CombinedEmoticon;
import chatty.util.CombinedIterator;
import chatty.util.LogUtil;
import chatty.util.MiscUtil;
import chatty.util.StringUtil;
import chatty.util.TwitchEmotesApi.EmotesetInfo;
import chatty.util.settings.Settings;
import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.Timer;

/**
 * Add emoticons and get a list of them matching a certain emoteset.
 * 
 * <p>
 * Emotes are sorted into the {@code emoticons} map if they don't have a stream
 * restriction and into the {@code streamEmoticons} map if they have a stream
 * restriction. Emoticons in {@code streamEmoticons} may still have an emoteset
 * or other restrictions that should be checked. This is only done this way to
 * retrieve and iterate over a relatively small subset of all emotes.
 * </p>
 * 
 * <p>
 * This is generally not thread-safe and all methods should be only used from
 * the same thread (like in this case probably the EDT).
 * </p>
 * 
 * @author tduva
 */
public class Emoticons {
    
    private static final Logger LOGGER = Logger.getLogger(Emoticons.class.getName());
    
    private static final Map<String, String> EMOTICONS_MAP = new HashMap<>();
    
    static {
        EMOTICONS_MAP.put("B-?\\)", "B)");
        EMOTICONS_MAP.put("R-?\\)", "R)");
        EMOTICONS_MAP.put("\\:-?D", ":D");
        EMOTICONS_MAP.put("\\;-?\\)", ";)");
        EMOTICONS_MAP.put("\\:-?(o|O)", ":O");
        EMOTICONS_MAP.put("\\:-?\\)", ":)");
        EMOTICONS_MAP.put("\\;-?(p|P)", ";p");
        EMOTICONS_MAP.put("[oO](_|\\.)[oO]", "O_o");
        EMOTICONS_MAP.put(">\\(", ">(");
        EMOTICONS_MAP.put("\\:-?(?:\\/|\\\\)(?!\\/)", ":/");
        EMOTICONS_MAP.put("\\:-?\\(", ":(");
        EMOTICONS_MAP.put("\\:-?(p|P)", ":p");
        EMOTICONS_MAP.put("\\:-?[z|Z|\\|]", ":|");
        EMOTICONS_MAP.put(":-?(?:7|L)", ":7");
        EMOTICONS_MAP.put("\\:>", ":>");
        EMOTICONS_MAP.put("\\:-?(S|s)", ":S");
        EMOTICONS_MAP.put("#-?[\\\\/]", "#/");
        EMOTICONS_MAP.put("<\\]", "<]");
        EMOTICONS_MAP.put("\\:-?[\\\\/]", ":\\");
        EMOTICONS_MAP.put("\\:-?\\)", ":)");
    }
    
    /**
     * Emoticons associated with an emoteset (Twitch Emotes and others).
     */
    private final HashMap<String, HashSet<Emoticon>> emoticonsByEmoteset = new HashMap<>();
    
    /**
     * All successfully loaded Custom Emotes.
     */
    private final Set<Emoticon> customEmotes = new HashSet<>();
    
    /**
     * Custom Emotes for lookup by id. All of these are also in customEmotes.
     */
    private final Map<String, Emoticon> customEmotesById = new HashMap<>();
    
    /**
     * Emoji should be sorted by length, so that longer Emoji (which can be
     * combinations of several short Emoji characters) are checked first.
     */
    private final Set<Emoticon> emoji = new TreeSet<>(new Comparator<Emoticon>() {
            @Override
            public int compare(Emoticon s1, Emoticon s2) {
                int cmp = Integer.compare(s2.code.length(), s1.code.length());
                return cmp != 0 ? cmp : s1.code.compareTo(s2.code);
            }
        });
    
    /**
     * Only global Twitch emotes.
     */
    private final Set<Emoticon> globalTwitchEmotes = new HashSet<>();
    
    private final CheersUtil cheers = new CheersUtil();
    
    /**
     * Only other (FFZ/BTTV) global emotes.
     */
    private final Set<Emoticon> otherGlobalEmotes = new HashSet<>();
    
    /**
     * Manually added Twitch Emotes that aren't available through the API, for
     * sending messages. These are added to usable emotes as normal for actual
     * use, this Set is only used for displaying them in the Emote Dialog and
     * such.
     */
    private final Set<Emoticon> localEmotes = new HashSet<>();
    
    private final Set<Emoticon> smilies = new HashSet<>();
    
    /**
     * All loaded Twitch Emotes, by their Twitch Emote Id.
     */
    private final HashMap<String,Emoticon> twitchEmotesById = new HashMap<>();
    
    /**
     * Emoticons restricted to a channel (FrankerFaceZ/BTTV).
     */
    private final HashMap<String,HashSet<Emoticon>> streamEmoticons = new HashMap<>();
    
    private final Map<String, Emoticon> combinedEmotes = new HashMap<>();
    
    //===============
    // Usable Emotes
    //===============
    // Used for TAB completion
    
    /**
     * Global emotes the local user has access to.
     */
    private final GlobalEmotes usableGlobalEmotes = new GlobalEmotes();
    
    /**
     * Channel-specific emotes the local user has access to. Needs to be in a
     * separate map if the user may not have access to all channel-specific
     * emotes, which is rather unusual, but some BTTV may have an emoteset as
     * requirement.
     */
    private final Map<String, Set<Emoticon>> usableStreamEmotes = new HashMap<>();
    
    /**
     * Used to check what emotes the local user has access to for completion. If
     * this changes, it checks usableGlobalEmotes again. May not contain
     * channel-specific (follower) emotesets.
     */
    private Set<String> localEmotesets = new HashSet<>();
    
    /**
     * Contain all emotesets from all channels, may contain emotesets the user
     * has no longer access to since not all channels are necessarily updated.
     */
    private Set<String> allLocalEmotesets = new HashSet<>();
    
    private Set<String> allHelixEmoteIds = new HashSet<>();
    
    //==================
    // Meta Information
    //==================
    
    private static final HashSet<Emoticon> EMPTY_SET = new HashSet<>();
    
    private final IgnoredEmotes ignoredEmotes = new IgnoredEmotes();
    
    private final EmoticonFavorites favorites = new EmoticonFavorites();
    
    private static final int DEFAULT_IMAGE_EXPIRE_MINUTES = 4*60;
    private static final int FASTER_IMAGE_EXPIRE_MINUTES = 1*60;
    
    public Emoticons() {
        Timer timer = new Timer(1*60*60*1000, e -> {
            int imageExpireMinutes = DEFAULT_IMAGE_EXPIRE_MINUTES;
            if (LogUtil.getMemoryPercentageOfMax() > 80) {
                imageExpireMinutes = FASTER_IMAGE_EXPIRE_MINUTES;
            }
            int removedCount = 0;
            removedCount += clearOldEmoticonImages(twitchEmotesById.values(), imageExpireMinutes);
            removedCount += clearOldEmoticonImages(otherGlobalEmotes, imageExpireMinutes);
            for (Set<Emoticon> emotes : streamEmoticons.values()) {
                removedCount += clearOldEmoticonImages(emotes, imageExpireMinutes);
            }
            LOGGER.info(String.format("Cleared %d unused emoticon images (%dm)",
                    removedCount, imageExpireMinutes));
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    private final Map<String, Set<EmotesetInfo>> twitchEmotesByStream = new HashMap<>();
    private final Map<String, EmotesetInfo> infoBySet = new HashMap<>();
    
    public Set<EmotesetInfo> getSetsByStream(String stream) {
        return twitchEmotesByStream.get(stream);
    }
    
    public EmotesetInfo getInfoBySet(String set) {
        return infoBySet.get(set);
    }
    
    public void updateEmoticons(EmoticonUpdate update) {
        removeEmoticons(update);
        if (!update.emotesToAdd.isEmpty()) {
            addEmoticons(update.emotesToAdd);
        }
        if (update.source == EmoticonUpdate.Source.HELIX_CHANNEL) {
            // All emotes should contain emoteset info and the same stream
            Set<EmotesetInfo> sets = new HashSet<>();
            String stream = null;
            for (Emoticon emote : update.emotesToAdd) {
                EmotesetInfo info = new EmotesetInfo(emote.emoteset, emote.getStream(), null, emote.getEmotesetInfo());
                sets.add(info);
                stream = emote.getStream();
            }
            if (stream != null) {
                twitchEmotesByStream.put(stream, sets);
            }
        }
        if (update.setInfos != null) {
            for (EmotesetInfo info : update.setInfos) {
                infoBySet.put(info.emoteset_id, info);
            }
        }
        if (update.source == EmoticonUpdate.Source.HELIX_CHANNEL
                || update.source == EmoticonUpdate.Source.HELIX_SETS) {
            update.emotesToAdd.forEach(emote -> allHelixEmoteIds.add(emote.stringId));
        }
    }
    
    private void removeEmoticons(EmoticonUpdate update) {
        // If used for other types as well, may have to handle favorites
        if (update.typeToRemove == null) {
            return;
        }
        int removedCount = 0;
        if (update.typeToRemove == Emoticon.Type.FFZ
                || update.typeToRemove == Emoticon.Type.BTTV
                || update.typeToRemove == Emoticon.Type.SEVENTV) {
            Iterator<Emoticon> it;
            if (update.roomToRemove == null) {
                // Global Non-Twitch
                it = otherGlobalEmotes.iterator();
            }
            else {
                // Channel-specific
                if (!streamEmoticons.containsKey(update.roomToRemove)) {
                    return;
                }
                it = streamEmoticons.get(update.roomToRemove).iterator();
            }
            // Check selected for removal
            while (it.hasNext()) {
                Emoticon emote = it.next();
                if (emote.type == update.typeToRemove) {
                    if (update.subTypeToRemove == null
                            || emote.subType == update.subTypeToRemove) {
                        it.remove();
                        usableGlobalEmotes.remove(emote);
                        if (update.roomToRemove != null &&
                                usableStreamEmotes.containsKey(update.roomToRemove)) {
                            usableStreamEmotes.get(update.roomToRemove).remove(emote);
                        }
                        removedCount++;
                    }
                }
            }
        }
        if (update.typeToRemove == Emoticon.Type.TWITCH
                && update.setsToRemove != null) {
            for (String set : update.setsToRemove) {
                Set<Emoticon> removed = emoticonsByEmoteset.remove(set);
                if (removed != null) {
                    removedCount += removed.size();
                    removed.forEach(e -> usableGlobalEmotes.remove(e));
                }
            }
        }
        if (update.typeToRemove == Emoticon.Type.CUSTOM2) {
            removedCount += localEmotes.size();
            for (Emoticon emote : localEmotes) {
                usableGlobalEmotes.remove(emote);
            }
            localEmotes.clear();
        }
        if (removedCount >= 0) {
            LOGGER.info(String.format(Locale.ROOT, "Removed %d emotes (%s/%s/%s/%s)",
                    removedCount,
                    update.typeToRemove,
                    update.subTypeToRemove,
                    update.roomToRemove,
                    update.setsToRemove));
        }
    }
    
    /**
     * Adds the given emoticons and sorts them into different maps, depending
     * on their restrictions.
     * 
     * <ul>
     * <li>If they have a stream restriction, they will be put in the
     * {@code streamEmoticons} map, with the stream name as the key. If there
     * is more than one stream in the restriction, it will be added under
     * several keys.</li>
     * <li>If there is no stream restriction, then they will be put in the
     * {@code emoticons} map, with the emoteset as the key. If no emoteset is
     * defined, then {@code null} will be used as key.</li>
     * </ul>
     * 
     * <p>
     * This is not thread-safe, so it should only be called from the EDT.
     * </p>
     * 
     * @param newEmoticons 
     */
    public void addEmoticons(Set<Emoticon> newEmoticons) {
        for (Emoticon emote : newEmoticons) {
            Set<String> channelRestrictions = emote.getStreamRestrictions();
            if (channelRestrictions != null) {
                // With channel restriction
                for (String channel : channelRestrictions) {
                    // Create channel set if necessary
                    if (!streamEmoticons.containsKey(channel)) {
                        streamEmoticons.put(channel, new HashSet<Emoticon>());
                    }
                    addEmote(streamEmoticons.get(channel), emote);
                }
            } else {
                if (emote.hasGlobalEmoteset()) {
                    // Global emotes
                    if (emote.type == Emoticon.Type.TWITCH) {
                        addEmote(globalTwitchEmotes, emote);
                    } else if (emote.type == Emoticon.Type.CUSTOM2) {
                        addEmote(localEmotes, emote);
                    } else {
                        addEmote(otherGlobalEmotes, emote);
                    }
                } else {
                    // Emoteset based
                    String emoteset = emote.emoteset;
                    if (!emoticonsByEmoteset.containsKey(emoteset)) {
                        emoticonsByEmoteset.put(emoteset, new HashSet<>());
                    }
                    addEmote(emoticonsByEmoteset.get(emoteset), emote);
                }
            }
            // By Twitch Emote ID
            if (emote.type == Emoticon.Type.TWITCH && emote.stringId != null) {
                twitchEmotesById.put(emote.stringId, emote);
            }
        }
        LOGGER.info(String.format("Added %d emotes. Now %d emotesets, %d channels, %d Twitch Global, %d Other Global.",
                newEmoticons.size(),
                emoticonsByEmoteset.size(),
                streamEmoticons.size(),
                globalTwitchEmotes.size(),
                otherGlobalEmotes.size()));
        findFavorites();
    }
    
    /**
     * Helper method to add an emote to a Collection.
     * 
     * @param collection
     * @param emote 
     */
    private void addEmote(Collection<Emoticon> collection, Emoticon emote) {
        /**
         * Add emotes the local user has access to (e.g. TAB Completion).
         */
        if (!emote.hasStreamRestrictions()) {
            if (emote.hasGlobalEmoteset() || localEmotesets.contains(emote.emoteset)) {
                /**
                 * Remove first to ensure emote is updated. When new emotes are
                 * received from the API, the ones used for received messages
                 * (twitchEmotesById), as well as other collections, are updated
                 * with the new object, so this one should be too, so that the
                 * same object is used for sent messages (especially to have
                 * animated emotes synchronized).
                 */
                usableGlobalEmotes.remove(emote);
                usableGlobalEmotes.add(emote);
            }
        }
        else {
            if (emote.hasGlobalEmoteset() || allLocalEmotesets.contains(emote.emoteset)) {
                for (String stream : emote.getStreamRestrictions()) {
                    getUsableStreamEmotesSet(stream).remove(emote);
                    getUsableStreamEmotesSet(stream).add(emote);
                }
            }
        }
        
        /**
         * Add to collection.
         */
        collection.remove(emote);
        collection.add(emote);
    }
    
    /**
     * Add emote received by message tags but not actually added from JSON.
     * 
     * @param emote 
     */
    public void addTempEmoticon(Emoticon emote) {
        twitchEmotesById.put(emote.stringId, emote);
    }
    
    public void setSmilies(Set<Emoticon> emotes) {
        smilies.clear();
        if (emotes != null) {
            smilies.addAll(emotes);
            LOGGER.info("Set " + smilies.size() + " smilies");
        }
    }
    
    public Set<Emoticon> getSmilies() {
        if (smilies != null) {
            return smilies;
        }
        return EMPTY_SET;
    }
    
    public void setLocalEmotes(Collection<Emoticon> emotes) {
        EmoticonUpdate.Builder b = new EmoticonUpdate.Builder(new HashSet<>(emotes));
        b.setTypeToRemove(Emoticon.Type.CUSTOM2);
        updateEmoticons(b.build());
    }
    
    public Set<Emoticon> getCustomLocalEmotes() {
        return localEmotes;
    }
    
    public boolean canAddCustomLocal(Emoticon emote) {
        return (emote.type == Emoticon.Type.TWITCH || emote.type == Emoticon.Type.CUSTOM2)
                && (emote.subType == null || emote.subType == Emoticon.SubType.REGULAR)
                && emote.stringId != null;
    }
    
    public boolean isCustomLocal(Emoticon emote) {
        if (emote.type == Emoticon.Type.CUSTOM2) {
            return true;
        }
        for (Emoticon presentEmote : localEmotes) {
            if (presentEmote.stringId.equals(emote.stringId)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isHelixEmoteId(Emoticon emote) {
        return allHelixEmoteIds.contains(emote.stringId);
    }
    
    private static int clearOldEmoticonImages(Collection<Emoticon> emotes,
                                              int imageExpireMinutes) {
        int removedCount = 0;
        for (Emoticon emote : emotes) {
            removedCount += emote.clearOldImages(imageExpireMinutes);
        }
        return removedCount;
    }

    public Set<Emoticon> getCustomEmotes() {
        return customEmotes;
    }
    
    public Emoticon getCustomEmoteById(String id) {
        return customEmotesById.get(id);
    }
    
    /**
     * Get the sorted set of all Emoji. Note that this returns the original set,
     * so it should not be modified and only be accessed out of the EDT.
     * 
     * @return 
     */
    public Set<Emoticon> getEmoji() {
        return emoji;
    }
    
    /**
     * Gets a list of all emoticons that don't have an emoteset associated
     * with them. This returns the original Set, so it should not be modified.
     * 
     * @return 
     */
    public Set<Emoticon> getGlobalTwitchEmotes() {
        return globalTwitchEmotes;
    }
    
    public Set<CheerEmoticon> getCheerEmotes() {
        return cheers.get();
    }
    
    /**
     * Get a String with all Cheer emoticons, or just Cheer emoticons specific
     * to the given stream.
     * 
     * @param stream Only return emotes specific to this stream, or null for all
     * @return 
     */
    public String getCheerEmotesString(String stream) {
        return cheers.getString(stream);
    }
    
    public Set<Emoticon> getOtherGlobalEmotes() {
        return otherGlobalEmotes;
    }
    
    public HashMap<String, Emoticon> getEmoticonsById() {
        return twitchEmotesById;
    }
    
    /**
     * Gets a list of emoticons that are associated with the given emoteset.
     * This returns the original Set, so it should not be modified. Does not
     * return emotes for the global emoteset (0).
     *
     * @param emoteSet
     * @return
     */
    public HashSet<Emoticon> getEmoticonsBySet(String emoteSet) {
        HashSet<Emoticon> result = emoticonsByEmoteset.get(emoteSet);
        if (result == null) {
            result = EMPTY_SET;
        }
        return result;
    }
    
    /**
     * Gets a list of emoticons that are associated with the given channel. This
     * returns the original Set, so it should not be modified.
     *
     * @param stream The name of the channel
     * @return
     */
    public HashSet<Emoticon> getEmoticonsByStream(String stream) {
        HashSet<Emoticon> result = streamEmoticons.get(stream);
        if (result == null) {
            result = EMPTY_SET;
        }
        return result;
    }
    
    public Emoticon getCombinedEmote(List<Emoticon> emotes) {
        emotes = new ArrayList<>(emotes);
        String code = CombinedEmoticon.getCode(emotes);
        Emoticon emote = combinedEmotes.get(code);
        if (emote != null) {
            return emote;
        }
        emote = CombinedEmoticon.create(emotes, code);
        combinedEmotes.put(code, emote);
        return emote;
    }
    
    public Set<Emoticon> getUsableGlobalTwitchEmotes() {
        return usableGlobalEmotes.getTwitch();
    }
    
    public Set<Emoticon> getUsableGlobalOtherEmotes() {
        return usableGlobalEmotes.getOther();
    }
    
    public Collection<Emoticon> getUsableEmotesByStream(String stream) {
        Collection<Emoticon> names = usableStreamEmotes.get(stream);
        return names == null ? EMPTY_SET : names;
    }
    
    /**
     * Update Twitch Emotes usable for the local user. This only updates based
     * on emotesets, other emotes (like FFZ) must not be removed by this.
     *
     * @param emotesets 
     * @param allEmotesets 
     */
    public void updateLocalEmotes(Set<String> emotesets, Set<String> allEmotesets) {
        /**
         * Global emotes use the "localEmotesets", which should more likely be
         * up-to-date in regards to non-channel-specific emotesets.
         */
        if (!this.localEmotesets.equals(emotesets)) {
            this.localEmotesets = emotesets;
            //--------------------------
            // By set
            //--------------------------
            // Remove emotes not having current sets (and not being global)
            Iterator<Emoticon> it = usableGlobalEmotes.iterator();
            while (it.hasNext()) {
                Emoticon emote = it.next();
                if (!emote.hasGlobalEmoteset() && !localEmotesets.contains(emote.emoteset)) {
                    it.remove();
                }
            }
            // Add all emotes for current sets
            for (String emoteset : emotesets) {
                for (Emoticon emote : getEmoticonsBySet(emoteset)) {
                    usableGlobalEmotes.add(emote);
                }
            }
        }
        /**
         * Channel-specific emotes use the "allEmotesets", which is less likely
         * to be up-to-date in regards to non-channel-specific emotesets, but
         * is more likely to contain all channel-specific emotesets.
         */
        if (!this.allLocalEmotesets.equals(allEmotesets)) {
            this.allLocalEmotesets = allEmotesets;
            //--------------------------
            // By stream
            //--------------------------
            for (Map.Entry<String, Set<Emoticon>> entry : usableStreamEmotes.entrySet()) {
                // For each stream in usable emotes
                // Remove any non-accessible
                Iterator<Emoticon> itStream = entry.getValue().iterator();
                while (itStream.hasNext()) {
                    Emoticon emote = itStream.next();
                    if (!emote.hasGlobalEmoteset() && !allLocalEmotesets.contains(emote.emoteset)) {
                        itStream.remove();
                    }
                }
            }
            for (Map.Entry<String, HashSet<Emoticon>> entry : streamEmoticons.entrySet()) {
                // For each stream in all per-stream emotes
                String stream  = entry.getKey();
                // Add all accessible
                for (Emoticon emote : entry.getValue()) {
                    if (emote.hasGlobalEmoteset() || allLocalEmotesets.contains(emote.emoteset)) {
                        getUsableStreamEmotesSet(stream).add(emote);
                    }
                }
            }
        }
    }
    
    private Set<Emoticon> getUsableStreamEmotesSet(String stream) {
        if (!usableStreamEmotes.containsKey(stream)) {
            /**
             * Sort Twitch emotes first so that Follower emotes are preferred.
             */
            usableStreamEmotes.put(stream, new TreeSet<>(new SortEmotesByTypeAndCode()));
        }
        return usableStreamEmotes.get(stream);
    }
    
    private static class SortEmotesByTypeAndCode implements Comparator<Emoticon> {
        
        @Override
        public int compare(Emoticon o1, Emoticon o2) {
            int compareType = o1.type.compareTo(o2.type);
            if (compareType != 0) {
                return compareType;
            }
            return o1.code.compareTo(o2.code);
        }
    }
    
    public Set<String> getLocalEmotesets() {
        return localEmotesets;
    }
    
    public Set<String> getAllLocalEmotesets() {
        return allLocalEmotesets;
    }
    
    private static final List<String> TURBO_EMOTESETS = Arrays.asList(new String[]{
        "33", "42", "457", "793", "19194"});
    
    /**
     * Checks whether the given emoteset is a turbo emoteset. This may be
     * incomplete.
     * 
     * @param emoteset The emoteset to check
     * @return true when it is a turbo emoteset, false otherwise
     */
    public static boolean isTurboEmoteset(String emoteset) {
        if (emoteset == null) {
            return false;
        }
        return TURBO_EMOTESETS.contains(emoteset);
    }
    
    private static final Pattern SPLIT_EMOTESETS = Pattern.compile(",");
    
    /**
     * Parses a comma-separated list of emotesets. Empty or whitespace only
     * values are ignored. Returns null if no emotesets found.
     * 
     * @param input
     * @return A set of emotesets, or null
     */
    public static Set<String> parseEmotesets(String input) {
        if (input == null) {
            return null;
        }
        Set<String> result = null;
        String[] split = SPLIT_EMOTESETS.split(input);
        for (String set : split) {
            if (!set.trim().isEmpty()) {
                if (result == null) {
                    result = new HashSet<>();
                }
                result.add(set);
            }
        }
        return result;
    }
    
    /**
     * Gets the name of the stream the given emoteset is associated with. This
     * of course only works if the emoteset data was actually successfully
     * requested before calling this.
     * 
     * @param emoteset The emoteset
     * @return The name of the stream, or null if none could be found for this
     * emoteset
     */
    public String getLabelByEmoteset(String emoteset) {
        if (isTurboEmoteset(emoteset)) {
            return "Turbo/Prime Emotes";
        }
        return null;
    }
    
    //================
    // Ignored Emotes
    //================
    /**
     * Replaces the ignored emotes list with the given data.
     * 
     * @param ignoredEmotes A Collection of emote codes to ignore
     */
    public void setIgnoredEmotes(Collection<String> ignoredEmotes) {
        this.ignoredEmotes.setData(ignoredEmotes);
    }
    
    /**
     * Adds the given emote code to the list of ignored emotes.
     * 
     * @param emote The emote to edit the ignore state for
     * @param context In what context to ignore the emote, can be 0 to unignore
     * @param settings A reference to the settings
     */
    public void setEmoteIgnored(Emoticon emote, int context, Settings settings) {
        ignoredEmotes.add(emote, context);
        settings.putList("ignoredEmotes", ignoredEmotes.getData());
    }
    
    /**
     * Get all ignore items that match the given emote.
     * 
     * @param emote The emote to find items for
     * @return A list of matches, may be empty, never null
     */
    public Collection<IgnoredEmotes.Item> getIgnoredEmoteMatches(Emoticon emote) {
        return ignoredEmotes.getMatches(emote);
    }
    
    /**
     * Check if the given emote is on the list of ignored emotes. Compares the
     * emote code to the codes on the list.
     *
     * @param emote The Emoticon to check
     * @param context In what context the emote has to be ignored, 0 for any
     * @return true if the emote is ignored, false otherwise
     */
    public boolean isEmoteIgnored(Emoticon emote, int context) {
        return ignoredEmotes.isIgnored(emote, context);
    }
    
    /**
     * Displays some information about the emotes that match the given emote
     * code (usually just one, but might be several if emotes with the same code
     * exist for different emotesets or channels).
     * 
     * @param emoteCode A single emote code to find the emote for
     * @return A String with a description of the found emote(s)
     */
    public String getEmoteInfo(String emoteCode) {
        if (emoteCode == null) {
            return "No emote specified.";
        }
        Set<Emoticon> found = findMatchingEmoticons(emoteCode);
        if (found.isEmpty()) {
            return "No matching emote found.";
        }
        StringBuilder b = new StringBuilder();
        b.append("Found ").append(found.size());
        if (found.size() == 1) {
            b.append(" emote");
        } else {
            b.append(" emotes");
        }
        b.append(" for '").append(emoteCode).append("': ");
        String sep = "";
        for (Emoticon emote : found) {
            Set<String> streams = emote.getStreamRestrictions();
            b.append(sep+"'"+emote.code+"'"
                +" / Type: "+emote.type+" / "
                +(emote.hasGlobalEmoteset()
                    ? "Usable by everyone"
                    : ("Emoteset: "+emote.emoteset
                      +" ("+getLabelByEmoteset(emote.emoteset)+")"))
                
                +(streams == null
                    ? " / Usable in all channels"
                    : " / Only usable in: "+streams));
            sep = " ### ";
        }
        
        return b.toString();
    }
    
    /**
     * Finds all emotes matching the given emote code.
     * 
     * @param emoteCode
     * @return 
     */
    public Set<Emoticon> findMatchingEmoticons(String emoteCode) {
        Set<Emoticon> found = new HashSet<>();
        found.addAll(findMatchingEmoticons(emoteCode, emoticonsByEmoteset.values()));
        found.addAll(findMatchingEmoticons(emoteCode, streamEmoticons.values()));
        return found;
    }
    
    /**
     * Finds all emotes matching the given emote code within the given data.
     * 
     * @param emoteCode
     * @param values
     * @return 
     */
    public Set<Emoticon> findMatchingEmoticons(String emoteCode,
            Collection<HashSet<Emoticon>> values) {
        Set<Emoticon> found = new HashSet<>();
        for (Collection<Emoticon> emotes : values) {
            for (Emoticon emote : emotes) {
                if (emote.getMatcher(emoteCode).matches()) {
                    found.add(emote);
                }
            }
        }
        return found;
    }
    
    /**
     * Creates a new Set that only contains the subset of the given emotes that
     * are of the given type (e.g. Twitch, FFZ or BTTV).
     * 
     * @param emotes The emotes to filter
     * @param type The emote type to allow through the filter
     * @return A new Set containing the emotes of the given type
     */
    public static final Set<Emoticon> filterByType(Set<Emoticon> emotes,
            Emoticon.Type type) {
        Set<Emoticon> filtered = new HashSet<>();
        for (Emoticon emote : emotes) {
            if (emote.type == type) {
                filtered.add(emote);
            }
        }
        return filtered;
    }
    
    public boolean equalsByCode(String setA, String setB) {
        Collection<Emoticon> a = getEmoticonsBySet(setA);
        Collection<Emoticon> b = getEmoticonsBySet(setB);
        return equalsByCode(a, b);
    }
    
    public static final boolean equalsByCode(Collection<Emoticon> a, Collection<Emoticon> b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.size() != b.size()) {
            return false;
        }
        for (Emoticon emoteA : a) {
            boolean found = false;
            for (Emoticon emoteB : b) {
                if (emoteA.code.equals(emoteB.code)) {
                    found = true;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
    
    public static final String toWriteable(String emoteCode) {
        String writeable = EMOTICONS_MAP.get(emoteCode);
        if (writeable == null) {
            return emoteCode;
        }
        return writeable;
    }
    
    public static final String toRegex(String emoteCode) {
        for (Map.Entry<String, String> entry : EMOTICONS_MAP.entrySet()) {
            if (entry.getValue().equals(emoteCode) || emoteCode.matches(entry.getKey())) {
                return entry.getKey();
            }
        }
        return emoteCode;
    }
    
    
    //===========
    // Favorites
    //===========
    
    public void addFavorite(Emoticon emote) {
        favorites.addFavorite(emote);
    }
    
    public void removeFavorite(Emoticon emote) {
        favorites.removeFavorite(emote);
    }
    
    public void removeFavorites(Collection<EmoticonFavorites.Favorite> toRemove) {
        favorites.removeFavorites(toRemove);
    }
    
    public boolean isFavorite(Emoticon emote) {
        return favorites.isFavorite(emote);
    }
    
    public Set<Emoticon> getFavorites() {
        return favorites.getFavorites();
    }
    
    public Collection<EmoticonFavorites.Favorite> getNotFoundFavorites() {
        return favorites.getNotFound();
    }
    
    public void loadFavoritesFromSettings(Settings settings) {
        favorites.loadFavoritesFromSettings(settings);
        findFavorites();
    }
    
    private void findFavorites() {
        favorites.find(twitchEmotesById, otherGlobalEmotes, emoji, customEmotes, localEmotes);
    }
    
    public Set<String> getFavoritesNonGlobalEmotesets() {
        return favorites.getNonGlobalEmotesets();
    }
    
    public void saveFavoritesToSettings(Settings settings) {
        favorites.saveFavoritesToSettings(settings);
    }
    
    //===============
    // Custom Emotes
    //===============
    
    /**
     * Loads custom emotes from the emotes.txt file in the settings directory.
     * 
     * Each line can have one emote. See {@link loadCustomEmote(String)} for the
     * parsing of each line.
     */
    public void loadCustomEmotes() {
        customEmotes.clear();
        customEmotesById.clear();
        
        Path file = Paths.get(Chatty.getUserDataDirectory()+"emotes.txt");
        try (BufferedReader r = Files.newBufferedReader(file, Charset.forName("UTF-8"))) {
            int countLoaded = 0;
            String line;
            boolean firstLine = true;
            while ((line = r.readLine()) != null) {
                if (firstLine) {
                    // Remove BOM, if present
                    line = StringUtil.removeUTF8BOM(line);
                    firstLine = false;
                }
                if (loadCustomEmote(line)) {
                    countLoaded++;
                }
            }
            LOGGER.info("Loaded "+countLoaded+" custom emotes");
            findFavorites();
        } catch (IOException ex) {
            LOGGER.info("Didn't load custom emotes: "+ex);
        }
    }
    
    /**
     * Parse a single line of custom emotes. Allows a number of options that are
     * in the form "option-key:option-value" which basicially can be in any
     * place in the line as long as they are separated by whitespace. The first
     * and second part without recognized option is used as code and url
     * respectively. Unknown parts (non-options) after the code/url have been
     * found are ignored.
     * 
     * Lines starting with "#" are ignored.
     *
     * @param line The line, cannot be null
     * @return true if an emote was found and added, false otherwise
     */
    private boolean loadCustomEmote(String line) {

        // Comment
        if (line.startsWith("#")) {
            return false;
        }
        
        String code = null;
        boolean literal = true;
        String url = null;
        String emoteset = Emoticon.SET_NONE;
        String id = null;
        // Use Dimension because it's easier to check if one value is set
        Dimension size = null;
        String streamRestriction = null;
        
        String[] split = line.trim().split("\\s+");
        for (int i=0;i<split.length;i++) {
            String item = split[i];
            
            if (item.startsWith("re:") && item.length() > "re:".length()) {
                literal = false;
                code = item.substring("re:".length());
            } else if (item.startsWith("id:")) {
                id = item.substring("id:".length());
            } else if (item.startsWith("set:")) {
                emoteset = item.substring("set:".length());
            } else if (item.startsWith("chan:") && item.length() > "chan:".length()) {
                streamRestriction = Helper.toStream(item.substring("chan:".length()));
            } else if (item.startsWith("size:")) {
                // All other data is found, so this must be the size
                try {
                    String[] sizes = item.substring("size:".length()).split("x");
                    int width = Integer.parseInt(sizes[0]);
                    int height = Integer.parseInt(sizes[1]);
                    // Set here so it's only set if both values are correct
                    size = new Dimension(width, height);
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                    // Do nothing if the format isn't right
                }
            } else {
                // No prefixes anymore, so check which one was set already
                // The order of this is important
                if (code == null) {
                    code = item;
                } else if (url == null) {
                    url = item;
                    if (!item.startsWith("http")) {
                        try {
                            Path path = Paths.get(Chatty.getUserDataDirectory()).resolve(Paths.get(url));
                            url = path.toUri().toURL().toString();
                        } catch (MalformedURLException ex) {
                            url = null;
                        }
                    }
                } else {
                    // Just ignore other stuff
                }
            }
        }
        
        // With code and URL found we can add the emote, other stuff is optional
        if (code != null && url != null) {
            Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.CUSTOM, code, url);
            b.setLiteral(literal).setEmoteset(emoteset);
            b.setStringId(id);
            if (size != null) {
                b.setSize(size.width, size.height);
            }
            b.addStreamRestriction(streamRestriction);
            Emoticon emote = b.build();
            customEmotes.add(emote);
            if (id != null) {
                customEmotesById.put(id, emote);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Makes a String containing info about the currently loaded custom emotes
     * over several lines.
     * 
     * @return The info text
     */
    public String getCustomEmotesInfo() {
        if (customEmotes.isEmpty()) {
            return "No custom emotes loaded";
        }
        StringBuilder b = new StringBuilder(customEmotes.size()+" custom emotes loaded:\n");
        for (Emoticon emote : customEmotes) {
            List<String> info = new ArrayList<>();
            if (emote.hasStreamRestrictions()) {
                info.add("#"+emote.getStreamRestrictions().iterator().next());
            }
            if (emote.emoteset != null) {
                info.add("set:"+emote.emoteset);
            }
            if (emote.stringId != null) {
                info.add("id:"+emote.stringId);
            }
            b.append("\""+emote.code+"\" ");
            if (info.size() > 0) {
                b.append(info);
            }
            b.append("\n   ");
            b.append(emote.url);
            b.append("\n");
        }
        b.delete(b.length()-1, b.length());
        return b.toString();
    }
    
    /**
     * Parses the Twitch emotes tag into an easier usable format.
     * 
     * @param tag The value of the emotes tag, can be null if no emotes are
     * supplied
     * @return The TagEmotes object containing the Map of emotes and ranges, or
     * null if the tag value was null (used for local messages for example,
     * indicating twitch emotes have to be parsed using regex)
     */
    public static TagEmotes parseEmotesTag(String tag) {
        Map<Integer, TagEmote> result = new HashMap<>();
        if (tag == null) {
            return null;
        }
        String[] emotes = tag.split("/");
        for (String emote : emotes) {
            // Go through all emotes
            int idEnd = emote.indexOf(":");
            if (idEnd > 0) {
                try {
                    /**
                     * Parse the emote id and ranges. If the format isn't as
                     * expected, this emote or at least any further ranges for
                     * this emote are ignored.
                     */
                    String id = emote.substring(0, idEnd);
                    String[] emoteRanges = emote.substring(idEnd+1).split(",");
                    
                    // Save all ranges for this emote
                    for (String range : emoteRanges) {
                        String[] rangeSplit = range.split("-");
                        int start = Integer.parseInt(rangeSplit[0]);
                        int end = Integer.parseInt(rangeSplit[1]);
                        if (end > start && start >= 0) {
                            // Every start index should only appear once anyway,
                            // so it can be used as key
                            result.put(start, new TagEmote(id, end));
                        }
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                    // Simply ignore error and continue with the next emote
                }
            }
        }
        return new TagEmotes(result);
    }
    
    public static void main(String[] args) {
        System.out.println(parseEmotesTag("131:1-2,4-5/43:1-7"));
        Set<Emoticon> a = new HashSet<>();
        Set<Emoticon> b = new HashSet<>();
        a.add(testBuild("abc"));
        b.add(testBuild("abc"));
        b.add(testBuild("abcd"));
        System.out.println(equalsByCode(a, b));
    }
    
    private static Emoticon testBuild(String code) {
       Emoticon.Builder b = new Emoticon.Builder(Emoticon.Type.TWITCH, code, "");
       return b.build();
    }
    
    /**
     * Wrapping class for the Map of emote ids and their ranges.
     */
    public static class TagEmotes {
        public final Map<Integer, TagEmote> emotes;
        
        public TagEmotes(Map<Integer, TagEmote> emotes) {
            this.emotes = emotes;
        }
        
        @Override
        public String toString() {
            return emotes.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TagEmotes other = (TagEmotes) obj;
            if (!Objects.equals(this.emotes, other.emotes)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.emotes);
            return hash;
        }
    }
    
    /**
     * Tag Emote with an id and a range end (the start should be the key
     * referring to this object.
     */
    public static class TagEmote {
        
        public final String id;
        public final int end;
        
        public TagEmote(String id, int end) {
            this.id = id;
            this.end = end;
        }
        
        @Override
        public String toString() {
            return ">"+id+":"+end;
        }
    }
    
    private volatile Map<Pattern, String> emojiReplacement;
    
    public void addEmoji(String sourceId) {
        emoji.clear();
        emoji.addAll(EmojiUtil.makeEmoticons(sourceId));
        Map<Pattern, String> replacements = new HashMap<>();
        for (Emoticon e : emoji) {
            if (e.stringId != null) {
                replacements.put(Pattern.compile(e.stringId, Pattern.LITERAL), e.code);
            }
            if (e.stringIdAlias != null) {
                replacements.put(Pattern.compile(e.stringIdAlias, Pattern.LITERAL), e.code);
            }
        }
        emojiReplacement = replacements;
    }
    
    /**
     * Replace Emoji shortcodes in the given text with the corresponding Emoji
     * characters. Should be thread-safe.
     * 
     * @param input
     * @return 
     */
    public String emojiReplace(String input) {
        for (Pattern p : emojiReplacement.keySet()) {
            Matcher m = p.matcher(input);
            if (m.find()) {
                input = p.matcher(input).replaceAll(emojiReplacement.get(p));
            }
        }
        return input;
    }
    
    public void setCheerEmotes(Set<CheerEmoticon> newCheerEmotes) {
        cheers.add(newCheerEmotes);
        LOGGER.info("Found "+newCheerEmotes.size()+" cheer emotes");
    }
    
    public void setCheerState(String state) {
        cheers.setState(state);
    }
    
    public void setCheerBackground(Color color) {
        cheers.setBackgroundColor(color);
    }
    
    /**
     * Split up so global Twitch emotes can take precedence over
     * channel-specific emotes, while other global emotes do not.
     */
    private static class GlobalEmotes {
        
        private final Set<Emoticon> twitch = new HashSet<>();
        private final Set<Emoticon> other = new HashSet<>();
        
        public void add(Emoticon emote) {
            if (emote.type == Emoticon.Type.TWITCH
                    || emote.type == Emoticon.Type.CUSTOM2) {
                twitch.add(emote);
            }
            else {
                other.add(emote);
            }
        }
        
        public void remove(Emoticon emote) {
            twitch.remove(emote);
            other.remove(emote);
        }
        
        public Set<Emoticon> getTwitch() {
            return twitch;
        }
        
        public Set<Emoticon> getOther() {
            return other;
        }
        
        public Iterator<Emoticon> iterator() {
            return new CombinedIterator<>(twitch, other);
        }
        
    }
    
}
