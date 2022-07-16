
package chatty.util.gif;

import com.pngencoder.PngEncoder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A single frame of a ListAnimatedImage. The pixel data is compressed in the
 * PNG format. Decoding the pixel data on each frame may cause higher CPU usage
 * than just storing the entire frame uncompressed, however the difference seems
 * fairly small. It might make sense to make compression optional though.
 * 
 * @author tduva
 */
public class ListAnimatedImageFrame {
    
    private final byte[] compressed;
    private final int delay;
    private final int width;
    private final int height;
    private final int visiblePixelCount;
    
    public ListAnimatedImageFrame(BufferedImage image, int delay) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            new PngEncoder().withBufferedImage(image).withCompressionLevel(1).toStream(baos);
            compressed = baos.toByteArray();
        }
        this.delay = delay;
        this.width = image.getWidth();
        this.height = image.getHeight();
        
        // Determine frame "visibility"
        int transparentPixelCount = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int pixel = image.getRGB(x, y);
                if ((pixel & 0xff000000) == 0) {
                    transparentPixelCount++;
                }
            }
        }
        this.visiblePixelCount = width * height - transparentPixelCount;
    }
    
    /**
     * Fill the given pixels array with the decoded pixels.
     * 
     * @param pixels
     * @throws IOException 
     */
    public void getImage(int[] pixels) throws IOException {
        PNGDecoder decoder = new PNGDecoder(new ByteArrayInputStream(compressed));
        decoder.decode(new ARGBBuffer(pixels), width * 4, PNGDecoder.Format.RGBA);
    }
    
    public int getDelay() {
        return delay;
    }
    
    /**
     * Returns the number of pixels with higher than 0 alpha value.
     * 
     * @return 
     */
    public int getVisiblePixelCount() {
        return visiblePixelCount;
    }
    
}
