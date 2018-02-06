
package chatty.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;

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
    
    private static final Map<String, Long> lastPlayed = new HashMap<>();
    
    private static Mixer mixer;
    private static String mixerName;
    
    public static void play(Path file, float volume, String id, int delay) throws Exception {
        
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
            AudioInputStream ais = AudioSystem.getAudioInputStream(file.toFile());
            
            DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
            final Clip clip;
            if (mixer != null) {
                clip = (Clip)mixer.getLine(info);
            } else {
                clip = (Clip)AudioSystem.getLine(info);
            }
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
            
            clip.addLineListener(new LineListener() {

                @Override
                public void update(LineEvent event) {
                    LOGGER.info("LineEvent: "+event);
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                }
            });
            
            clip.start();
            LOGGER.info("Playing sound "+id+"/"+file+" ("+volumeInfo+") EDT:"+SwingUtilities.isEventDispatchThread());
        } catch (Exception ex) {
            LOGGER.warning("Couldn't play sound ("+id+"/"+file+"): "+ex);
            throw ex;
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
    
    public static List<String> getDeviceNames() {
        List<String> result = new ArrayList<>();
        try {
            for (Mixer.Info info : AudioSystem.getMixerInfo()) {
                for (Line.Info info2 : AudioSystem.getMixer(info).getSourceLineInfo()) {
                    if (info2.getLineClass() == Clip.class) {
                        result.add(info.getName());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warning("Error getting list of sound devices: "+ex);
        }
        return result;
    }
    
    public static void setDeviceName(String name) {
        if (mixerName != null && mixerName.equals(name)) {
            return;
        }
        mixerName = name;
        if (name == null || name.isEmpty()) {
            mixer = null;
            LOGGER.info("Set to default sound device");
            return;
        }
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(name)) {
                mixer = AudioSystem.getMixer(info);
                LOGGER.info("Set sound device to "+name);
                return;
            }
        }
        mixer = null;
        LOGGER.info("Could not find sound device "+name);
    }
    
}
