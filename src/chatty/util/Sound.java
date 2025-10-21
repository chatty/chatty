
package chatty.util;

import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.io.IOException;
import java.net.URL;
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
    
    public static final Object SOUND_COMMAND_UNIQUE = new Object();
    
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
     * Should only be called from the EDT. Play a sound from an external file.
     * 
     * @param file The file
     * @param volume The volume 0-100
     * @param id An id, mainly used for the delay
     * @param delay Delay in seconds
     * @throws java.lang.Exception If an error occurs an exception may be thrown
     * in addition to logging
     */
    public static void play(Path file, float volume, String id, int delay) throws Exception {
        get().playInternal(file, volume, id, delay);
    }
    
    /**
     * Should only be called from the EDT. This variant with URL is for sounds
     * inside the JAR and won't work with a sound command.
     */
    public static void play(URL file, float volume, String id, int delay) throws Exception {
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
    
    public static void setCommand(boolean enabled, String command) {
        get().setCommandInternal(enabled, command);
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
    
    private boolean checkDelay(String id, int delay) {
        if (lastPlayed.containsKey(id)) {
            long timePassed = (System.currentTimeMillis() - lastPlayed.get(id)) / 1000;
            if (timePassed < delay) {
                //LOGGER.info("Not playing sound "+id+" ("+timePassed+"/"+delay+")");
                return false;
            }
        }
        if (delay >= 0) {
            lastPlayed.put(id, System.currentTimeMillis());
        }
        return true;
    }
    
    private void playInternal(Path file, float volume, String id, int delay) throws Exception {
        if (checkDelay(id, delay)) {
            if (command != null) {
                runCommand(command, file, volume);
            }
            else {
                playInternal2(file.toUri().toURL(), volume, id);
            }
        }
    }
    
    private void playInternal(URL file, float volume, String id, int delay) throws Exception {
        if (checkDelay(id, delay)) {
            playInternal2(file, volume, id);
        }
    }
    
    private void playInternal2(URL file, float volume, String id) throws Exception {
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
    
    private Clip createClip(URL file, String id) throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        // getAudioInputStream() also accepts a File or InputStream
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
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
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                for (Line.Info lineInfo : AudioSystem.getMixer(mixerInfo).getSourceLineInfo()) {
                    if (lineInfo.getLineClass() == Clip.class) {
                        result.add(mixerInfo.getName());
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
    
    private CustomCommand command;
    
    private void setCommandInternal(boolean enabled, String commandRaw) {
        if (enabled) {
            command = CustomCommand.parse(commandRaw);
        }
        else {
            command = null;
        }
    }
    
    public String runCommand(CustomCommand command, Path file, float volume) {
        Parameters param = Parameters.create("");
        param.put("file", file.toAbsolutePath().toString().replace("\"", "\\\""));
        param.put("volume", String.valueOf(volume));
        
        if (command.hasError()) {
            LOGGER.warning("Sound command error: "+command.getSingleLineError());
            return "Error: "+command.getSingleLineError();
        }
        else {
            String resultCommand = command.replace(param);
            ProcessManager.execute(resultCommand, "SoundCommand", null);
            return "Running: "+resultCommand;
        }
    }
    
    /**
     * Output info on all mixers for debugging.
     */
    public static void logAudioSystemInfo() {
        StringBuilder b = new StringBuilder();
        b.append("\n=== Audio System Debug Information ===\n");
        
        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            b.append("Total mixers: ").append(mixerInfos.length).append("\n");
            
            Mixer.Info defaultMixerInfo = AudioSystem.getMixer(null).getMixerInfo();
            
            for (int i = 0; i < mixerInfos.length; i++) {
                Mixer.Info info = mixerInfos[i];
                b.append("Mixer ").append(i+1);
                if (defaultMixerInfo.getName().equals(info.getName())) {
                    b.append(" (default)");
                }
                b.append(":").append("\n");
                b.append("  Name: ").append(info.getName()).append("\n");
                b.append("  Description: ").append(info.getDescription()).append("\n");
                b.append("  Vendor: ").append(info.getVendor()).append("\n");
                b.append("  Version: ").append(info.getVersion()).append("\n");
                
                try {
                    Mixer mixer = AudioSystem.getMixer(info);
                    Line.Info[] sourceLines = mixer.getSourceLineInfo();
                    Line.Info[] targetLines = mixer.getTargetLineInfo();
                    
                    b.append("  Source lines: ").append(sourceLines.length).append("\n");
                    for (Line.Info lineInfo : sourceLines) {
                        b.append("    - ").append(lineInfo.getLineClass().getSimpleName());
                        b.append(" (").append(lineInfo.toString()).append(")").append("\n");
                    }
                    
                    b.append("  Target lines: ").append(targetLines.length).append("\n");
                    for (Line.Info lineInfo : targetLines) {
                        b.append("    - ").append(lineInfo.getLineClass().getSimpleName());
                        b.append(" (").append(lineInfo.toString()).append(")").append("\n");
                    }
                }
                catch (Exception ex) {
                    b.append("  Error accessing mixer: ").append(ex).append("\n");
                }
            }
        }
        catch (Exception ex) {
            b.append("Error getting audio system info: ").append(ex).append("\n");
        }
        b.append("=== End Audio System Debug Information ===").append("\n");
        
        LOGGER.info(b.toString());
    }
    
}
