
package chatty.util.gif;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.util.List;

/**
 * An implementation of AnimatedImage that stores the frames in a list, with
 * the pixels compressed as a PNG to save space.
 * 
 * @author tduva
 */
public class ListAnimatedImage implements AnimatedImage {

    private final List<ListAnimatedImageFrame> frames;
    private final Dimension size;
    private final String name;
    private final int preferredPauseFrame;
    
    public ListAnimatedImage(List<ListAnimatedImageFrame> frames, int width, int height, String name) {
        this.frames = frames;
        this.size = new Dimension(width, height);
        this.name = name;
        
        // Determin preferred frame for pausing this image
        int highestVisiblePixelCount = 0;
        int mostVisibleFrame = 0;
        for (int i = 0; i < frames.size(); i++) {
            int visiblePixelCount = frames.get(i).getVisiblePixelCount();
            if (visiblePixelCount > highestVisiblePixelCount) {
                mostVisibleFrame = i;
                highestVisiblePixelCount = visiblePixelCount;
            }
        }
        if (highestVisiblePixelCount == 0
                || (frames.get(0).getVisiblePixelCount() * 10) / highestVisiblePixelCount > 7) {
            // Prefer first frame if it's visibility isn't much lower
            this.preferredPauseFrame = 0;
        }
        else {
            this.preferredPauseFrame = mostVisibleFrame;
        }
    }
    
    /**
     * Update the pixels in the given array with that frame. Not thread safe.
     * 
     * @param frame
     * @param pixels
     * @throws Exception 
     */
    @Override
    public void getFrame(int frame, int[] pixels) throws Exception {
        frames.get(frame).getImage(pixels);
    }

    @Override
    public int getFrameCount() {
        return frames.size();
    }

    @Override
    public int getDelay(int frame) {
        return frames.get(frame).getDelay();
    }

    @Override
    public Dimension getSize() {
        return size;
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public int getPreferredPauseFrame() {
        return preferredPauseFrame;
    }
    
    public Image createImage() {
        return Toolkit.getDefaultToolkit().createImage(new AnimatedImageSource(this));
    }
    
}
