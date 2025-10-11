
package chatty.util.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Not tested, don't use
 * 
 * @author tduva
 */
public class MacTTSProvider implements TTSProvider {

    private static final Logger LOGGER = Logger.getLogger(MacTTSProvider.class.getName());

    @Override
    public void speak(SpeakRequest request) throws Exception {
        // Use say command with rate and voice options
        List<String> command = new ArrayList<>();
        command.add("say");
        command.add("-v");
        command.add(request.voice);
        command.add("-r");
        command.add(String.valueOf(request.rate));
        command.add(request.text);

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        process.waitFor();

        if (process.exitValue() != 0) {
            throw new Exception("macOS TTS failed with exit code: " + process.exitValue());
        }
    }

    @Override
    public void stop() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pkill", "say");
            pb.start();
        } catch (Exception e) {
            LOGGER.warning("Failed to stop macOS TTS: " + e.getMessage());
        }
    }

    @Override
    public List<VoiceInfo> getAvailableVoices() {
        List<VoiceInfo> voices = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("say", "-v", "?");
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // macOS: VoiceName LanguageCode [# samples] [Description]
                // Example: "Alex                en_US    # Most people recognize me by my voice."
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    String name = parts[0];
                    String locale = parts[1];
                    // Gender is not available from 'say -v ?' output, so leave null
                    voices.add(new VoiceInfo(name, null, locale));
                }
            }
            process.waitFor();
        } catch (Exception e) {
            LOGGER.warning("Failed to get macOS voices: " + e.getMessage());
            voices.add(new VoiceInfo("Alex", null, "en_US"));
            voices.add(new VoiceInfo("Victoria", null, "en_US"));
            voices.add(new VoiceInfo("Daniel", null, "en_GB"));
        }
        return voices;
    }
    
    @Override
    public String getProviderId() {
        return "Mac";
    }

}
