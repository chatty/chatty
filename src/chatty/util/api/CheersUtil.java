
package chatty.util.api;

import java.awt.Color;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stores CheerEmoticon objects for display in chat and manages setting their
 * special settings (background type, state).
 * 
 * @author tduva
 */
public class CheersUtil {

    /**
     * Stores CheerEmoticon objects and sort them by min_bits, so that higher
     * min_bits are checked first. That way the highest possible emoticon for
     * the spent bits is used.
     */
    private final Set<CheerEmoticon> cheerEmotes = new TreeSet<>(new Comparator<CheerEmoticon>() {

        @Override
        public int compare(CheerEmoticon s1, CheerEmoticon s2) {
            int cmp = Integer.compare(s2.min_bits, s1.min_bits);
            return cmp != 0 ? cmp : s1.code.compareTo(s2.code);
        }
    });
    
    private String currentBackground;
    private String currentState;
    
    /**
     * Stores the set of CheerEmoticon objects for showing them in chat.
     * Replaces any data already present.
     * 
     * @param data 
     */
    public void add(Set<CheerEmoticon> data) {
        cheerEmotes.addAll(data);
        //System.out.println(cheerEmotes.size()+" "+cheerEmotes);
        setSettings(currentBackground, currentState);
    }
    
    /**
     * Get the currently stored CheerEmoticon objects. The resulting Set should
     * not be modified.
     * 
     * @return 
     */
    public Set<CheerEmoticon> get() {
        return cheerEmotes;
    }
    
    /**
     * Sets some settings for the stored CheerEmoticon objects.
     * 
     * @param background A valid background type (light, dark)
     * @param state A valid state (static, animated)
     */
    public void setSettings(String background, String state) {
        for (CheerEmoticon emote : cheerEmotes) {
            emote.setSettings(background, state);
        }
        this.currentBackground = background;
        this.currentState = state;
    }
    
    /**
     * Changes the background type (light, dark) of the stored CheerEmoticon
     * objects based on the given background color. This should be the color the
     * cheers are displayed in front of, so the chat background color.
     *
     * @param color The Color object
     */
    public void setBackgroundColor(Color color) {
        String background = getBackground(color);
        if (!background.equals(currentBackground)) {
            setSettings(background, currentState);
        }
    }
    
    /**
     * Changes the state of the stored CheerEmoticon objects (static, animated).
     * 
     * @param state The state
     */
    public void setState(String state) {
        if (!state.equals(currentBackground)) {
            setSettings(currentBackground, state);
        }
    }

    /**
     * Gets a valid background type (light, dark) based on the given background
     * color.
     * 
     * @param color The Color object
     * @return A String containing the background type
     */
    private static String getBackground(Color color) {
        if (getLuma(color) < 100) {
            return "dark";
        }
        return "light";
    }
    
    /**
     * Calculate the Luma of the given color in some weird way, to be able to
     * determine whether it's more light or dark.
     * 
     * @param color
     * @return 
     */
    private static double getLuma(Color color) {
        return color.getRed() * 0.299 + color.getGreen() * 0.587 + color.getBlue() * 0.114;
    }
    
    public String getString(String stream) {
        Set<CheerEmoticon> blah = new TreeSet<>(new Comparator<CheerEmoticon>() {

            @Override
            public int compare(CheerEmoticon s1, CheerEmoticon s2) {
                int cmp = s1.code.compareTo(s2.code);
                return cmp != 0 ? cmp : Integer.compare(s1.min_bits, s2.min_bits);
            }
        });
        blah.addAll(cheerEmotes);
        StringBuilder b = new StringBuilder();
        for (CheerEmoticon emote : blah) {
            if (stream == null || emote.streamRestrictionContains(stream)) {
                b.append(emote.prefix).append(emote.min_bits).append(" ");
            }
        }
        return b.toString();
    }
    
    // For testing
    public static void main(String[] args) {
        System.out.println(getLuma(Color.DARK_GRAY));
    }
    
}
