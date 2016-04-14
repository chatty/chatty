
package chatty.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.sampled.*;

/**
 * Static methods to play sounds.
 * 
 * @author tduva
 */
public class Sound {
    
    private static final int MAX_VOLUME = 100;
    private static final int MIN_VOLUME = 0;
    
    private static final int MIN_GAIN = -40;
    
    private static final Logger LOGGER = Logger.getLogger(Sound.class.getName());
    private static final String PATH = System.getProperty("user.dir")
            +File.separator+"sounds"+File.separator;
    
    private static final Map<String, Long> lastPlayed = new HashMap<>();
    
    public static void play(String fileName, float volume, String id, int delay) {
        
        if (lastPlayed.containsKey(id)) {
            long timePassed = (System.currentTimeMillis() - lastPlayed.get(id)) / 1000;
            if (timePassed < delay) {
                //LOGGER.info("Not playing sound "+id+" ("+timePassed+"/"+delay+")");
                return;
            }
        }
        if (delay >= 0) {
            lastPlayed.put(id, System.currentTimeMillis());
        }
        
        try {
            // getAudioInputStream() also accepts a File or InputStream
            File file = new File(PATH+fileName);
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            
            DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
            Clip clip = (Clip)AudioSystem.getLine(info);
            clip.open(ais);
            
            // Volume, use what is available
            String volumeInfo;
            FloatControl gain = getFirstAvailableControl(clip,
                    FloatControl.Type.MASTER_GAIN, FloatControl.Type.VOLUME);
            if (gain != null) {
                gain.setValue(calculateGain(volume,
                        gain.getMinimum(), gain.getMaximum()));
                volumeInfo = gain.toString();
            } else {
                volumeInfo = "no volume control";
            }
            
            clip.start();
            LOGGER.info("Playing sound "+id+"/"+fileName+" ("+volumeInfo+")");
        } catch (NullPointerException | LineUnavailableException | IOException | UnsupportedAudioFileException ex) {
            LOGGER.warning("Couldn't play sound ("+id+"/"+fileName+"): "+ex.getLocalizedMessage());
        }
    }
    
    private static FloatControl getFirstAvailableControl(Clip clip,
            FloatControl.Type... types) {
        for (FloatControl.Type type : types) {
            if (clip.isControlSupported(type)) {
                return (FloatControl)clip.getControl(type);
            }
        }
        return null;
    }
    
    private static float calculateGain(float volume, float min, float max) {
        // Restrict to min/max
        if (volume > MAX_VOLUME) {
            volume = MAX_VOLUME;
        }
        if (volume < MIN_VOLUME) {
            volume = MIN_VOLUME;
        }
        volume = (float)(MAX_VOLUME - MAX_VOLUME*Math.pow(1.25, -0.25*volume));
        
        if (min < MIN_GAIN) {
            min = MIN_GAIN;
        }
        float range = max - min;
        float gain = ((range * volume / (MAX_VOLUME - MIN_VOLUME)) + min);
        return gain;
    }
    
}
