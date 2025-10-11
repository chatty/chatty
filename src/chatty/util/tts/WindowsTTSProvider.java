
package chatty.util.tts;

import chatty.util.Debugging;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class WindowsTTSProvider implements TTSProvider {
    
    private static final Logger LOGGER = Logger.getLogger(WindowsTTSProvider.class.getName());
    
    private volatile Process process;
    private volatile boolean forceStopped;

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    @Override
    public void speak(SpeakRequest request) throws Exception {
        // Convert parameters for SSML
        int ssmlRate = (int) request.rate;
        String ssmlPitch = String.format("%+d%%", request.pitch);
        String voice = request.voice;

        if (voice == null || voice.isEmpty()) {
            voice = "default";
        }
        
        // Escape text for XML
        String escapedText = escapeXml(request.text);

        // Create SSML string
        String ssml = String.format(
                "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>"
                + "<voice name='%s'>"
                + "<prosody rate='%d%%' volume='%d' pitch='%s'>"
                + "%s"
                + "</prosody>"
                + "</voice>"
                + "</speak>",
                voice, ssmlRate, request.volume, ssmlPitch, escapedText);
        
        Debugging.println("tts", ssml);

        String script = "Add-Type -AssemblyName System.Speech; "
                + "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$speak.SpeakSsml($env:tts_ssml); "
                + "$speak.Dispose();";

        ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", script);
        pb.environment().put("tts_ssml", ssml);

        forceStopped = false;
        
        process = pb.start();
        process.waitFor();

        if (process.exitValue() != 0 && !forceStopped) {
            java.io.InputStream errorStream = process.getErrorStream();
            String error = new java.util.Scanner(errorStream).useDelimiter("\\\\A").next();
            LOGGER.warning("PowerShell TTS error: " + error);
            throw new Exception("PowerShell TTS failed with exit code: " + process.exitValue() + ", error: " + error);
        }
    }

    @Override
    public void stop() {
        if (process != null) {
            forceStopped = true;
            process.destroy();
        }
    }

    @Override
    public String getProviderId() {
        return "WinSpeech";
    }
    
    @Override
    public List<VoiceInfo> getAvailableVoices() {
        List<VoiceInfo> voices = new ArrayList<>();
        voices.add(new VoiceInfo("Default", null, null));
        try {
            String script = "Add-Type -AssemblyName System.Speech; "
                    + "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                    + "$speak.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo.Name + '|' + $_.VoiceInfo.Gender + '|' + $_.VoiceInfo.Culture.Name }; "
                    + "$speak.Dispose();";

            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", script);
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    String[] parts = line.trim().split("\\|");
                    String name = parts.length > 0 ? parts[0] : null;
                    String gender = parts.length > 1 ? parts[1] : null;
                    String locale = parts.length > 2 ? parts[2] : null;
                    voices.add(new VoiceInfo(name, gender, locale));
                }
            }
            process.waitFor();
        } catch (Exception e) {
            LOGGER.warning("Failed to get Windows voices: " + e.getMessage());
        }
        return voices;
    }

}
