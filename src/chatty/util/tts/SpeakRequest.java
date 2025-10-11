
package chatty.util.tts;

/**
 *
 * @author tduva
 */
public class SpeakRequest {
    
    public enum Mode {
        STOP_SAY_DIRECTLY, QUEUE
    }

    public final String text;
    public final String voice;
    public final int volume;
    public final int rate;
    public final int pitch;
    public final Mode mode;

    public SpeakRequest(String text, String voice, int volume, int rate, int pitch, Mode mode) {
        this.text = text;
        this.voice = voice;
        this.volume = volume;
        this.rate = rate;
        this.pitch = pitch;
        this.mode = mode;
    }

}
