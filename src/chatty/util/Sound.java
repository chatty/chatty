
package chatty.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.sound.sampled.*;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;
import javax.sound.sampled.Line.Info;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

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
    
    //==========================
    // Instance
    //==========================
    private static Sound instance;
    
    public static Sound get() {
        if (instance == null) {
            instance = new Sound();
        }
        return instance;
    }
    
    /**
     * Should only be called from the EDT.
     * 
     * @param file
     * @param volume
     * @param id
     * @param delay
     * @throws Exception 
     */
    public static void play(Path file, float volume, String id, int delay) throws Exception {
        get().playInternal(file, volume, id, delay);
    }
    
    /**
     * Should only be called from the EDT.
     * 
     * @param name 
     */
    public static void setDeviceName(String name) {
        get().setDeviceNameInternal(name);
    }
    
    //==========================
    // Class
    //==========================
    private final Map<String, Long> lastPlayed = new HashMap<>();
    private final List<Pair<Clip, ElapsedTime>> clips = new LinkedList<>();
    
    private Mixer mixer;
    private String mixerName;
    
    Sound() {
        Timer timer = new Timer(10*1000, e -> {
            clearClips();
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    public void playInternal(Path file, float volume, String id, int delay) throws Exception {
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
            Clip clip = createClip(file, id);
            clips.add(new Pair<>(clip, new ElapsedTime(true)));
            String volumeInfo = setVolume(clip, volume);
            clip.start();
            LOGGER.info(String.format("Playing[%s]: %sms %s (%s) EDT: %s",
                    id, clip.getMicrosecondLength() / 1000, file, volumeInfo, SwingUtilities.isEventDispatchThread()));
        } catch (Exception ex) {
            LOGGER.warning("Couldn't play sound ("+id+"/"+file+"): "+ex);
            throw ex;
        }
    }
    
    private Clip createClip(Path file, String id) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        // getAudioInputStream() also accepts a File or InputStream
        AudioInputStream ais = AudioSystem.getAudioInputStream(file.toFile());
        ais = addConversion(ais, mixer != null ? mixer : AudioSystem.getMixer(null));
        DataLine.Info info = new DataLine.Info(Clip.class, ais.getFormat());
        final Clip clip;
        if (mixer != null) {
            clip = (Clip) mixer.getLine(info);
        }
        else {
            clip = (Clip) AudioSystem.getLine(info);
        }
        clip.open(ais);

        clip.addLineListener(event -> {
            boolean simulateIssue = Debugging.isEnabled("soundNoStop") && ThreadLocalRandom.current().nextBoolean();
            if (simulateIssue && event.getType() == LineEvent.Type.STOP) {
                return;
            }
            LOGGER.info("LineEvent["+id+"]: " + event.getType());
            if (event.getType() == LineEvent.Type.STOP) {
                clip.close();
            }
        });
        return clip;
    }
    
    private String setVolume(Clip clip, float volume) {
        // Volume, use what is available
        String volumeInfo;
        FloatControl gain = getFirstAvailableControl(clip,
                FloatControl.Type.MASTER_GAIN, FloatControl.Type.VOLUME);
        if (gain != null) {
            gain.setValue(calculateGain(volume,
                    gain.getMinimum(), gain.getMaximum()));
            volumeInfo = gain.toString();
        }
        else {
            volumeInfo = "no volume control";
        }
        return volumeInfo;
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
    
    /**
     * If the input format is not supported for the given mixer, try to find a
     * format that the mixer supports and try to create a conversion for the
     * input stream.
     * 
     * @param ais The original input stream
     * @param mixer The mixer
     * @return A new input stream with the conversion, or the original input
     * stream if a conversion is either not needed or not possible
     */
    private static AudioInputStream addConversion(AudioInputStream ais, Mixer mixer) {
        if (Debugging.isEnabled("audio")) {
            LOGGER.info(String.format("[Input Stream Format] %s\n[Mixer] %s\n%s",
                    ais.getFormat(),
                    mixer.getMixerInfo(),
                    StringUtil.join(mixer.getSourceLineInfo(),"\n")));
        }
        
        AudioFormat convertToFormat = null;
        for (Info info : mixer.getSourceLineInfo()) {
            if (info instanceof SourceDataLine.Info) {
                // Check whether sdi is for Clip or SourceDataLine?
                SourceDataLine.Info sdi = (SourceDataLine.Info) info;
                if (!sdi.isFormatSupported(ais.getFormat())) {
                    AudioFormat[] formats = sdi.getFormats();
                    sortFormats(formats);
                    for (AudioFormat format : formats) {
                        if (AudioSystem.isConversionSupported(format, ais.getFormat())) {
                            convertToFormat = new AudioFormat(
                                    format.getEncoding(),
                                    format.getSampleRate() == NOT_SPECIFIED
                                            ? ais.getFormat().getSampleRate()
                                            : format.getSampleRate(),
                                    format.getSampleSizeInBits() == NOT_SPECIFIED
                                            ? ais.getFormat().getSampleSizeInBits()
                                            : format.getSampleSizeInBits(),
                                    format.getChannels() == NOT_SPECIFIED
                                            ? ais.getFormat().getChannels()
                                            : format.getChannels(),
                                    format.getFrameSize() == NOT_SPECIFIED
                                            ? ais.getFormat().getFrameSize()
                                            : format.getFrameSize(),
                                    format.getFrameRate() == NOT_SPECIFIED
                                            ? ais.getFormat().getFrameRate()
                                            : format.getFrameRate(),
                                    format.isBigEndian());
                            break;
                        }
                    }
                    if (Debugging.isEnabled("audio")) {
                        LOGGER.info(String.format("[Available Formats] %s\n%s\n[Selected Format]\n%s",
                                sdi,
                                StringUtil.join(formats, "\n"),
                                convertToFormat));
                    }
                    if (convertToFormat != null) {
                        break;
                    }
                }
            }
        }
        if (convertToFormat != null) {
            ais = AudioSystem.getAudioInputStream(convertToFormat, ais);
        }
        return ais;
    }
    
    /**
     * Sort "better"(?) formats first.
     * 
     * @param formats 
     */
    private static void sortFormats(AudioFormat[] formats) {
        Arrays.sort(formats, new Comparator<AudioFormat>() {
            
            @Override
            public int compare(AudioFormat o1, AudioFormat o2) {
                if (o1.getChannels() != o2.getChannels()) {
                    return o2.getChannels() - o1.getChannels();
                }
                if (o1.getSampleSizeInBits() != o2.getSampleSizeInBits()) {
                    return o2.getSampleSizeInBits() - o1.getSampleSizeInBits();
                }
                return (int)(o2.getSampleRate() - o1.getSampleRate());
            }
            
        });
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
    
    public void setDeviceNameInternal(String name) {
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
    
    /**
     * Backup for closing clips if the Stop event does not get received for some
     * reason.
     */
    private void clearClips() {
        if (clips.isEmpty()) {
            return;
        }
        Iterator<Pair<Clip, ElapsedTime>> it = clips.iterator();
        int clipsClosed = 0;
        while (it.hasNext()) {
            Pair<Clip, ElapsedTime> entry = it.next();
            Clip clip = entry.key;
            if (!clip.isOpen()) {
                it.remove();
            }
            else {
                long clipLength = clip.getMicrosecondLength();
                boolean clipLengthPassed = clipLength != AudioSystem.NOT_SPECIFIED && entry.value.millisElapsed(clipLength / 1000 + 1000);
                if (clipLengthPassed || entry.value.secondsElapsed(120)) {
                    clipsClosed++;
                    clip.close();
                    it.remove();
                }
            }
        }
        if (clipsClosed > 0) {
            LOGGER.warning(String.format(Locale.ROOT, "%d clips closed which should already have been closed", clipsClosed));
        }
    }
    
}
