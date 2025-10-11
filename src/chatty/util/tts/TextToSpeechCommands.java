
package chatty.util.tts;

import chatty.Commands;
import chatty.util.settings.Settings;

/**
 *
 * @author tduva
 */
public class TextToSpeechCommands {
    
    public static Settings settings;

    public static String command(Commands.CommandParameters p) {
        if (!TextToSpeech.get(settings).hasProvider()) {
            return "TTS not available on this system.";
        }
        switch (p.getArgs()) {
            case "start":
                TextToSpeech.get(settings).start();
                return "TTS started.";
            case "pause":
                TextToSpeech.get(settings).pause();
                return "TTS paused (messages still being queued).";
            case "clear":
                TextToSpeech.get(settings).clearQueue();
                return "TTS Queue cleared.";
            case "on":
                TextToSpeech.get(settings).enable();
                return "TTS turned on.";
            case "off":
                TextToSpeech.get(settings).disable();
                return "TTS turned off (no more messages being queued).";
        }
        
        Commands.CommandParsedArgs parsed = p.parsedArgs(2);
        if (parsed != null) {
            switch (parsed.get(0)) {
                case "say":
                    return commandSay(parsed.get(1));
            }
        }
        return "Usage: /tts <say|start|pause|clear|on|off>";
    }
    
    private static String commandSay(String text) {
        TextToSpeech.get(settings).speak(text);
        return "Blah";
    }
    
}
