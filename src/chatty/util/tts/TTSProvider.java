
package chatty.util.tts;

import java.util.List;

/**
 *
 * @author tduva
 */

public interface TTSProvider {

    /**
     * Read the given message following the settings if possible. This method
     * must block until reading the message has been finished.
     * 
     * @param speakRequest
     * @throws Exception 
     */
    void speak(SpeakRequest speakRequest) throws Exception;

    /**
     * Interrupt reading the message immediately (e.g. killing the process).
     */
    void stop();

    /**
     * The list of voices available.
     * 
     * @return 
     */
    List<VoiceInfo> getAvailableVoices();

    /**
     * Should be unique between available providers.
     * 
     * @return 
     */
    String getProviderId();
    
}
