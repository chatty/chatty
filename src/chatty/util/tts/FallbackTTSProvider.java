
package chatty.util.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Maybe add MaryTTS or similar.
 * 
 * @author tduva
 */
public class FallbackTTSProvider implements TTSProvider {

    private static final Logger LOGGER = Logger.getLogger(FallbackTTSProvider.class.getName());

    private String currentVoice = "default";

    @Override
    public void speak(SpeakRequest request) throws Exception {
        LOGGER.warning("No TTS engine available for this platform");
    }

    @Override
    public void stop() {
        // Nothing to do
    }

    @Override
    public List<VoiceInfo> getAvailableVoices() {
        List<VoiceInfo> voices = new ArrayList<>();
        voices.add(new VoiceInfo("default", null, null));
        return voices;
    }
    
    @Override
    public String getProviderId() {
        return "";
    }
    
}
