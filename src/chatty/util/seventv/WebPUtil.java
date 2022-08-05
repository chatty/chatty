
package chatty.util.seventv;

import chatty.Helper;
import chatty.util.ImageCache.ImageRequest;
import chatty.util.ImageCache.ImageResult;
import chatty.util.MiscUtil;
import chatty.util.gif.GifUtil;
import chatty.util.gif.ListAnimatedImage;
import chatty.util.gif.ListAnimatedImageFrame;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import webpdecoderjn.WebPDecoder;
import webpdecoderjn.WebPDecoder.WebPImage;
import webpdecoderjn.WebPDecoder.WebPImageFrame;

/**
 * Handles executing code only if WebP Decoding is available, e.g. performing an
 * API request that is expected to result in having to decode WebP images.
 *
 * @author tduva
 */
public class WebPUtil {
    
    private static final Logger LOGGER = Logger.getLogger(WebPUtil.class.getName());

    private static final Object LOCK = new Object();
    
    private static final List<Runnable> waiting = new ArrayList<>();
    
    private static boolean checking;
    private static boolean checked;
    private static boolean available;
    
    public static void runIfWebPAvailable(Runnable runnable) {
        if (!MiscUtil.OS_WINDOWS && !MiscUtil.OS_LINUX) {
            return;
        }
        if (isAvailable()) {
            runnable.run();
            return;
        }
        synchronized (LOCK) {
            if (!checked) {
                // Not done checking yet, so add to waiting list
                waiting.add(runnable);
                
                if (!checking) {
                    // Not checking yet, so check now
                    checking = true;
                    new Thread(() -> {
                        boolean success = check();
                        
                        // After check, see if and what to run
                        List<Runnable> runNow = new ArrayList<>();
                        synchronized (LOCK) {
                            available = success;
                            checking = false;
                            checked = true;
                            if (available) {
                                // Only run if actually available
                                runNow.addAll(waiting);
                            }
                            // Always clear though
                            waiting.clear();
                        }
                        // Run outside of lock
                        for (Runnable waitingRunnable : runNow) {
                            // Add some time in between
                            try {
                                Thread.sleep(100);
                            }
                            catch (InterruptedException ex) {
                                break;
                            }
                            waitingRunnable.run();
                        }
                    }, "WebP Check").start();
                }
            }
        }
    }
    
    public static boolean isAvailable() {
        synchronized(LOCK) {
            return available;
        }
    }
    
    /**
     * Initialize the decoding library and check if it works
     * 
     * @return true if the library works
     */
    private static boolean check() {
        try {
            WebPDecoder.init();
            boolean success = WebPDecoder.test();
            LOGGER.info("WebP Decoding available: "+success);
            return success;
        }
        catch (IOException ex) {
            LOGGER.warning("Error checking WebP: "+ex);
        }
        return false;
    }
    
    public static ImageResult decode(byte[] data, ImageRequest request) {
        if (!available) {
            return null;
        }
        try {
            WebPImage img = WebPDecoder.decode(data);
            Dimension size = new Dimension();
            Dimension actualBaseSize = new Dimension(img.canvasWidth, img.canvasHeight);
            Dimension scaledSize = request.getScaledSizeIfNecessary(actualBaseSize);
            if (img.frameCount == 1) {
                ImageIcon icon = new ImageIcon(img.frames.get(0).img);
                icon.setDescription("WebP");
                return request.finishIcon(icon, false);
            }
            else {
                List<ListAnimatedImageFrame> frames = new ArrayList<>();
                for (WebPImageFrame frame : img.frames) {
                    BufferedImage image = frame.img;
                    if (scaledSize != null) {
                        image = GifUtil.resize(image, scaledSize.width, scaledSize.height);
                    }
                    size = new Dimension(image.getWidth(), image.getHeight());
                    frames.add(new ListAnimatedImageFrame(image, frame.delay));
                }
                ImageIcon icon = new ImageIcon(
                        new ListAnimatedImage(
                                frames,
                                size.width,
                                size.height,
                                request.getRequestedURL().toString()
                        ).createImage()
                );
                icon.setDescription("WebP (animated)");
                return new ImageResult(icon, actualBaseSize, false);
            }
        }
        catch (UnsatisfiedLinkError | IOException ex) {
            LOGGER.warning(String.format("Error decoding %s: %s",
                    request.getRequestedURL().toString(),
                    ex));
        }
        return null;
    }
    
}
