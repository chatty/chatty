
package chatty.util.gif;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.List;

/**
 * An AnimatedImage that combines several static or animated images of the same
 * size. The first image is considered the main image, with the others drawn on
 * top of it. The main image is mainly relevant for the pause frame, although
 * sometimes the pause frame may not be correctly set.
 *
 * @author tduva
 */
public class OverlayListAnimatedImage implements AnimatedImage {
    
    private final List<ListAnimatedImage> images;
    private final Dimension size;
    private final String name;
    private final ListAnimatedImage mainImage;
    private final int numFrames;
    
    private final int[] atFrame;
    private final int[] delays;
    private final int[][] buffer;
    
    private int lastRequestedFrame = -1;
    
    public OverlayListAnimatedImage(List<ListAnimatedImage> images, int width, int height, String name) {
        this.images = images;
        this.size = new Dimension(width, height);
        this.name = name;
        mainImage = images.get(0);
        
        /**
         * Use the main image frame count, which might be useful for the
         * preferred pause frame. However the main image may also be a static
         * image, so do at least two frames so it can animated at all.
         */
        numFrames = Math.max(mainImage.getFrameCount(), 2);
        
        atFrame = new int[images.size()];
        
        delays = new int[images.size()];
        
        buffer = new int[images.size()][width*height];
    }

    /**
     * Loads a composite of all the images into {@code pixels}. The
     * {@code requestedFrame} does not necessarily correlate to the frames of
     * any one animated image:
     *
     * <ul>
     * <li>
     * If {@code requestedFrame} has increased by one compared to the previous
     * request it will load the next composited frame (which will actually
     * consist of the next frame of one or several of the invididual images, and
     * the already cached frame for others, depending on frame timing).
     * </li>
     * <li>
     * Otherwise the {@code requestedFrame} will set the main image to the
     * requested frame and the others to the first frame.
     * </li>
     * </ul>
     *
     * @param requestedFrame
     * @param pixels
     * @throws Exception 
     */
    @Override
    public void getFrame(int requestedFrame, int[] pixels) throws Exception {
        if (requestedFrame != (lastRequestedFrame + 1) % numFrames) {
            atFrame[0] = requestedFrame;
            for (int i = 1; i < atFrame.length; i++) {
                atFrame[i] = 0;
            }
            for (int i = 0; i < delays.length; i++) {
                delays[i] = 0;
            }
        }
        /**
         * The lowest delay out of all images are the frames that the requester
         * will have waited for (as reported by getDelay()), so load those.
         */
        int lowestDelay = getLowestDelay();
        for (int i = 0; i < delays.length; i++) {
            if (delays[i] == lowestDelay) {
                ListAnimatedImage image = images.get(i);
                int imageFrameIndex = atFrame[i];
                image.getFrame(imageFrameIndex, buffer[i]);
                delays[i] = image.getDelay(imageFrameIndex);
                atFrame[i] = (imageFrameIndex + 1) % image.getFrameCount();
            }
            else {
                delays[i] -= lowestDelay;
            }
        }
        lastRequestedFrame = requestedFrame;
        
        // Composite
        System.arraycopy(buffer[0], 0, pixels, 0, pixels.length);
        for (int i = 1; i < buffer.length; i++) {
            composite(buffer[i], pixels, pixels);
        }
    }
    
    private int getLowestDelay() {
        int lowest = Integer.MAX_VALUE;
        for (int delay : delays) {
            if (delay < lowest) {
                lowest = delay;
            }
        }
        return lowest;
    }

    @Override
    public int getFrameCount() {
        return numFrames;
    }

    @Override
    public int getDelay(int frame) {
        return getLowestDelay();
    }

    @Override
    public Dimension getSize() {
        return mainImage.getSize();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPreferredPauseFrame() {
        return mainImage.getPreferredPauseFrame();
    }
    
    public Image createImage() {
        return Toolkit.getDefaultToolkit().createImage(new AnimatedImageSource(this));
    }
    
    /**
     * Combine the images of {@code source1} and {@code source2} with
     * transparency, whereas {@code source1} appears as being on top.
     *
     * See: https://stackoverflow.com/a/2212074
     *
     * @param source1
     * @param source2
     * @param target Where the combined pixels are written to, may be the same
     * array as {@code source1} or {@code source2}, the changes are applied
     * sequentially for each pixel after reading the value, so there shouldn't
     * be any conflicts
     */
    private static void composite(int[] source1, int[] source2, int[] target) {
        for (int i = 0; i < source1.length; i++) {
            int a1 = (source1[i] >> 24) & 0xff;
            int r1 = (source1[i] & 0x00ff0000) >> 16;
            int g1 = (source1[i] & 0x0000ff00) >> 8;
            int b1 = (source1[i] & 0x000000ff);
            
            int a2 = (source2[i] >> 24) & 0xff;
            int r2 = (source2[i] & 0x00ff0000) >> 16;
            int g2 = (source2[i] & 0x0000ff00) >> 8;
            int b2 = (source2[i] & 0x000000ff);
            
            int a = a1 + (255 - a1) * a2 / 255;
            int r = r1 * a1 / 255 + r2 * (255 - a1) * a2 / 65025;
            int g = g1 * a1 / 255 + g2 * (255 - a1) * a2 / 65025;
            int b = b1 * a1 / 255 + b2 * (255 - a1) * a2 / 65025;
            
            target[i] = (((a      ) << 24) |
                        ((r & 0xff) << 16) |
                        ((g & 0xff) <<  8) |
                        ((b & 0xff)      ));
        }
    }

}
