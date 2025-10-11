
package chatty.util.tts;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Untested, don't use yet
 * 
 * @author tduva
 */
public class LinuxTTSProvider implements TTSProvider {

    private static final Logger LOGGER = Logger.getLogger(LinuxTTSProvider.class.getName());

    @Override
    public void speak(SpeakRequest request) throws Exception {
        // Try espeak first, then festival, then fallback
        if (tryEspeak(request.text, request.voice, request.rate, request.volume, request.pitch)) {
            return;
        }
        if (tryFestival(request.text, request.rate, request.volume, request.pitch)) {
            return;
        }
        throw new Exception("No TTS engine available on Linux");
    }

    private boolean tryEspeak(String text, String voice, int rate, int volume, int pitch) {
        try {
            List<String> command = new ArrayList<>();
            command.add("espeak");
            command.add("-v");
            command.add(voice);
            command.add("-s");
            command.add(String.valueOf(rate));
            command.add("-a");
            command.add(String.valueOf(volume));
            command.add(text);

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean tryFestival(String text, int rate, int volume, int pitch) {
        try {
            String script = String.format("(SayText \"%s\")", text.replace("\"", "\\\""));
            ProcessBuilder pb = new ProcessBuilder("festival", "--tts");
            Process process = pb.start();

            try (java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(process.getOutputStream())) {
                writer.write(script);
            }

            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void stop() {
        try {
            ProcessBuilder pb = new ProcessBuilder("pkill", "espeak");
            pb.start();
            pb = new ProcessBuilder("pkill", "festival");
            pb.start();
        } catch (Exception e) {
            LOGGER.warning("Failed to stop Linux TTS: " + e.getMessage());
        }
    }

    @Override
    public List<VoiceInfo> getAvailableVoices() {
        List<VoiceInfo> voices = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("espeak", "--voices");
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // espeak output: "Pty Language Age/Gender VoiceName ..."
                // Example: " 5  en-us      M 1  default"
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 4) {
                    String locale = parts[1];
                    String gender = parts[2];
                    String name = parts[3];
                    voices.add(new VoiceInfo(name, gender, locale));
                }
            }
            process.waitFor();
        } catch (Exception e) {
            LOGGER.warning("Failed to get Linux voices: " + e.getMessage());
            voices.add(new VoiceInfo("en-us", "M", "en-us"));
            voices.add(new VoiceInfo("en-gb", "M", "en-gb"));
        }
        return voices;
    }
    
    @Override
    public String getProviderId() {
        return "Linux";
    }

}
