
package chatty.util.tts;

import chatty.User;
import chatty.util.Debugging;
import chatty.util.Sound;
import chatty.util.irc.MsgTags;
import chatty.util.settings.Setting;
import chatty.util.settings.Settings;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Handles Native OS Text-to-Speech.
 * 
 * Messages are added to a queue and then read one after the other. Methods to
 * skip through the queue or enable/disable speaking are provided.
 * 
 * @author tduva
 */
public class TextToSpeech {
    
    private static final Logger LOGGER = Logger.getLogger(TextToSpeech.class.getName());
    
    //==========================
    // Instance
    //==========================
    private static TextToSpeech instance;
    
    public static synchronized TextToSpeech get(Settings settings) {
        if (instance == null) {
            instance = new TextToSpeech(settings);
        }
        return instance;
    }
    
    public static synchronized void shutdownIfNecessary() {
        if (instance != null) {
            instance.shutdown();
        }
    }
    
    //==========================
    // Class
    //==========================
    
    private final LinkedList<SpeakRequest> queue;
    private final LinkedList<SpeakRequest> doneQueue;
    private SpeakRequest directRequest;
    private Thread thread;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition queueNotEmpty = lock.newCondition();
    private final Condition unpaused = lock.newCondition();
    private final AtomicBoolean isPaused;
    private final AtomicBoolean speakingInterrupted;
    private final AtomicBoolean currentlySpeaking;
    private final AtomicBoolean shutdown;
    
    private final Settings settings;
    
    // OS-specific TTS implementations
    private final TTSProvider ttsProvider;
    
    private TextToSpeech(Settings settings) {
        this.settings = settings;
        this.queue = new LinkedList<>();
        this.doneQueue = new LinkedList<>();
        this.isPaused = new AtomicBoolean(false);
        this.speakingInterrupted = new AtomicBoolean();
        this.currentlySpeaking = new AtomicBoolean();
        this.shutdown = new AtomicBoolean();
        
        // Initialize OS-specific TTS provider
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            this.ttsProvider = new WindowsTTSProvider();
        }
        // Untested, don't use yet
//        else if (os.contains("mac")) {
//            this.ttsProvider = new MacTTSProvider();
//        }
//        else if (os.contains("linux")) {
//            this.ttsProvider = new LinuxTTSProvider();
//        }
        else {
            this.ttsProvider = new FallbackTTSProvider();
        }
        
        LOGGER.info("TTS initialized with provider: " + ttsProvider.getClass().getSimpleName());
    }
    
    /**
     * Output the given text with the current settings.
     * 
     * @param text
     * @return 
     */
    public boolean speak(String text) {
        return speak(text,
                     (String) settings.mapGet("ttsVoice", ttsProvider.getProviderId()),
                     settings.getInt("ttsVolume"),
                     settings.getInt("ttsRate"),
                     settings.getInt("ttsPitch"),
                     SpeakRequest.Mode.QUEUE);
    }
    
    /**
     * Simple chat message output, if enabled.
     * 
     * @param user
     * @param text
     * @param tags
     * @param ownMessage 
     */
    public void message(User user, String text, MsgTags tags, boolean ownMessage) {
        if (ownMessage) {
            return;
        }
        if (!settings.getBoolean("ttsReadMessages")) {
            return;
        }
        if (settings.getBoolean("ttsIgnoreEmoteOnly") && (tags != null && tags.isValue("emote-only", "1"))) {
            return;
        }
        speak(String.format("%s says: %s", user.getName(), text));
    }
    
    public boolean hasProvider() {
        return !ttsProvider.getProviderId().isEmpty();
    }
    
    public boolean checkProvider() {
        if (!hasProvider()) {
            playSound(SOUND_ERROR);
            return false;
        }
        return true;
    }
    
    /**
     * Output the given text with specific settings.
     *
     * @param text The text to speak
     * @param voice
     * @param volume
     * @param rate
     * @param pitch
     * @param mode
     * @return 
     */
    public boolean speak(String text, String voice, int volume, int rate, int pitch, SpeakRequest.Mode mode) {
        if (settings == null
                || !settings.getBoolean("ttsEnabled")
                || !hasProvider()
                || text == null
                || text.trim().isEmpty()) {
            return false;
        }
        
        if (thread == null) {
            thread = new Thread(() -> {
                readQueue();
            });
            thread.setDaemon(true);
            thread.start();
        }
        
        // Sanitize text
        String sanitizedText = text;
        
        if (sanitizedText.trim().isEmpty()) {
            return false;
        }
        
        // Truncate if too long
        if (sanitizedText.length() > settings.getInt("ttsMaxLength")) {
            sanitizedText = sanitizedText.substring(0, settings.getInt("ttsMaxLength")) + " [text shortened]";
        }
        
        SpeakRequest request = new SpeakRequest(sanitizedText, voice, volume, rate, pitch, mode);
        lock.lock();
        try {
            if (mode == SpeakRequest.Mode.QUEUE) {
                queue.addLast(request);
            }
            else if (mode == SpeakRequest.Mode.STOP_SAY_DIRECTLY) {
                directRequest = request;
            }
            queueNotEmpty.signalAll();
        } finally {
            lock.unlock();
        }
        
        if (mode == SpeakRequest.Mode.STOP_SAY_DIRECTLY) {
            interruptSpeaking();
        }
        return true;
    }
    
    private void speakInternal(SpeakRequest request) {
        try {
            ttsProvider.speak(request);
//            LOGGER.info("TTS spoke: " + textToSpeak.substring(0, Math.min(50, textToSpeak.length())) + "...");
        } catch (Exception e) {
            LOGGER.warning("TTS error: " + Debugging.getStacktrace(e));
        } finally {
            
        }
    }
    
    private SpeakRequest peekRequest() {
        if (directRequest != null) {
            SpeakRequest result = directRequest;
            directRequest = null;
            return result;
        }
        return queue.peekFirst();
    }
    
    /**
     * Runs in thread.
     */
    private void readQueue() {
        while (true) {
            try {
                // Retrieve next message
                //--------------
                lock.lock();
                
                SpeakRequest message;
                try {
                    // Blocks if no element is available
                    Debugging.println("tts", "Waiting for message");
                    
                    while ((message = peekRequest()) == null) {
                        queueNotEmpty.await();
                    }
                    Debugging.println("tts", "Reading: %s", message);
                } finally {
                    lock.unlock();
                }
                //--------------
                
                // Output
                if (!isPaused.get()) {
                    currentlySpeaking.set(true);
                    // Blocks until it stops speaking (done or interrupted)
                    speakInternal(message);
                    currentlySpeaking.set(false);
                    if (!speakingInterrupted.get()) {
                        // Output done
                        //--------------
                        lock.lock();
                        try {
                            if (message.mode != SpeakRequest.Mode.STOP_SAY_DIRECTLY
                                    && !queue.isEmpty()) {
                                addDone(queue.removeFirst());
                                Debugging.println("tts", "Moved to done queue");
                            }
                        }
                        finally {
                            lock.unlock();
                        }
                        //--------------
                    }
                    else {
                        speakingInterrupted.set(false);
                    }
                }
                else {
                    // Paused, wait
                    //--------------
                    lock.lock();
                    try {
                        while (isPaused.get()) {
                            Debugging.println("tts", "Paused, waiting");
                            unpaused.await();
                        }
                        Debugging.println("tts", "Unpaused, continue");
                    }
                    finally {
                        lock.unlock();
                    }
                    //--------------
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void start() {
        if (!checkProvider()) {
            return;
        }
        if (!settings.getBoolean("ttsEnabled")) {
            playSound(SOUND_ERROR);
            return;
        }
        playSound(SOUND_UNMUTE);
        startInternal();
    }
    
    private void startInternal() {
        lock.lock();
        try {
            isPaused.set(false);
            unpaused.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
    public void pause() {
        if (!checkProvider()) {
            return;
        }
        pauseInternal();
        playSound(SOUND_MUTE);
    }
    
    private void pauseInternal() {
        isPaused.set(true);
        interruptSpeaking();
        LOGGER.info("TTS stopped");
    }
    
    private void interruptSpeaking() {
        if (currentlySpeaking.get()) {
            speakingInterrupted.set(true);
        }
        ttsProvider.stop();
    }
    
    public void enable() {
        if (!checkProvider()) {
            return;
        }
        playSound(SOUND_ON);
        settings.setBoolean("ttsEnabled", true);
        startInternal();
    }

    public void disable() {
        if (!checkProvider()) {
            return;
        }
        playSound(SOUND_OFF);
        settings.setBoolean("ttsEnabled", false);
        pauseInternal();
        clearQueueInternal();
    }
    
    public void clearQueue() {
        if (!checkProvider()) {
            return;
        }
        playSound(SOUND_CLEARQUEUE);
        clearQueueInternal();
    }
    
    public void clearQueueInternal() {
        lock.lock();
        try {
            queue.clear();
            doneQueue.clear();
        }
        finally {
            lock.unlock();
        }
        interruptSpeaking();
    }
    
    public void markQueueAsDone() {
        if (!checkProvider()) {
            return;
        }
        lock.lock();
        try {
            SpeakRequest request;
            while ((request = queue.pollFirst()) != null) {
                addDone(request);
            }
        }
        finally {
            lock.unlock();
        }
    }
    
    private static final int DONE_QUEUE_LIMIT = 100;
    
    // Must be called in a lock
    private void addDone(SpeakRequest request) {
        assert lock.isHeldByCurrentThread();
        doneQueue.addFirst(request);
        if (doneQueue.size() > DONE_QUEUE_LIMIT) {
            doneQueue.pollLast();
        }
    }
    
    private int getSize(LinkedList queue) {
        lock.lock();
        try {
            return queue.size();
        }
        finally {
            lock.unlock();
        }
    }
    
    public void skipForwards() {
        if (!checkProvider()) {
            return;
        }
        if (getSize(queue) < (isPaused.get() ? 1 : 2)) {
            playSound(SOUND_ERROR);
        }
        if (isPaused.get()) {
            start();
        }
        else {
            ttsProvider.stop();
        }
    }
    
    public void skipBackwards() {
        if (!checkProvider()) {
            return;
        }
        if (getSize(doneQueue) < 1) {
            playSound(SOUND_ERROR);
            return;
        }
        lock.lock();
        try {
            SpeakRequest prevMessage = doneQueue.pollFirst();
            if (prevMessage != null) {
                queue.addFirst(prevMessage);
                queueNotEmpty.signalAll();
            }
        }
        finally {
            lock.unlock();
        }
        if (isPaused.get()) {
            start();
        }
        else {
            interruptSpeaking();
        }
    }
    
    /**
     * Shutdown the TTS system.
     */
    public void shutdown() {
        shutdown.set(true);
        pauseInternal();
        clearQueueInternal();
        LOGGER.info("TTS shutdown complete");
    }
    
    /**
     * Get available voices for the current system.
     * 
     * @return List of available voice info objects
     */
    public List<VoiceInfo> getAvailableVoices() {
        return ttsProvider.getAvailableVoices();
    }
    
    public String getProviderId() {
        return ttsProvider.getProviderId();
    }
    
    /**
     * Get the current TTS settings as a string for display.
     * 
     * @return A string representation of current TTS settings
     */
//    public String getStatus() {
//        if (settings == null) {
//            return "TTS: Settings not loaded";
//        }
//        return String.format("TTS: %s, Rate: %d, Volume: %d, Pitch: %d, Voice: %s",
//                settings.getBoolean("ttsEnabled") ? "Enabled" : "Disabled",
//                settings.getInt("ttsRate"),
//                settings.getInt("ttsVolume"),
//                settings.getInt("ttsPitch"),
//                getCurrentVoice());
//    }
    
    public static void main(String[] args) throws ExecutionException {
        Settings settings = new Settings("", null);
        settings.addBoolean("ttsEnabled", true);
        settings.addLong("ttsMaxLength", 2000);
        settings.addLong("ttsRate", 100);
        settings.addLong("ttsVolume", 100);
        settings.addLong("ttsPitch", 0);
        settings.addMap("ttsVoice", new HashMap<>(), Setting.STRING);
        
        TextToSpeech speech = new TextToSpeech(settings);
        speech.getSize(new LinkedList());
        System.out.println("after");
//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                speech.stop();
//                speech.start();
//                speech.skipBackwards();
////                speech.skipBackwards();
//            }
//            
//        }, 5000);
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                speech.skipForwards();
//            }
//            
//        }, 8000);
////        timer.schedule(new TimerTask() {
////            @Override
////            public void run() {
////                speech.speak("test again");
////            }
////            
////        }, 12000);
////        
//        speech.speak("abc");
//        System.out.println(speech.speak("test 123 123 '1 2 3' ‛); $speak.Speak(‛blah"));
//        System.out.println(speech.speak("test a b c"));
    }
    
    
    private static final String SOUND_ERROR = "error.wav";
    private static final String SOUND_MUTE = "mute.wav";
    private static final String SOUND_UNMUTE = "unmute.wav";
    private static final String SOUND_ON = "on.wav";
    private static final String SOUND_OFF = "off.wav";
    private static final String SOUND_CLEARQUEUE = "clearqueue.wav";
    
    private void playSound(String filename) {
        if (shutdown.get()) {
            return;
        }
        try {
            Path file = Paths.get(TextToSpeech.class.getResource(filename).toURI());
            Sound.play(file, settings.getLong("ttsHotkeysVolume"), "TTS"+filename, 0);
        } catch (Exception ex) {
            
        }
    }
    
} 