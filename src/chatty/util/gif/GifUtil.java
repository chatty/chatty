
package chatty.util.gif;

import chatty.util.gif.GifDecoder.GifImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

/**
 *
 * @author tduva
 */
public class GifUtil {
    
    private static final Logger LOGGER = Logger.getLogger(GifUtil.class.getName());
    
    public static ImageIcon getGifFromUrl(URL url) throws Exception {
        try (InputStream input = url.openStream()) {
            // Use readAllBytes() because GifDecoder doesn't handle streams well
            return fixGifFps(readAllBytes(input));
        }
    }
    
    /**
     * Decodes and re-writes the animated GIF with modified FPS. Any delay
     * smaller than 10ms is set to 100ms. Yes, this is kind of ugly, but it
     * worked best with the GIF emotes I tested.
     * 
     * @param imageData
     * @return
     * @throws IOException 
     */
    private static ImageIcon fixGifFps(byte[] imageData) throws IOException {
        final GifImage gif = GifDecoder.read(imageData);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bos)) {
            int delay = gif.getDelay(0) * 10;
            if (delay <= 10) {
                delay = 100;
            }
            GifSequenceWriter w = new GifSequenceWriter(output, gif.getFrame(0).getType(), delay, true);
            for (int i = 0; i < gif.getFrameCount(); i++) {
                //System.out.println(gif.getDelay(i));
                w.writeToSequence(gif.getFrame(i));
            }
            w.close();
        }
        return new ImageIcon(bos.toByteArray());
    }

    /**
     * Read all bytes from the stream into a byte array.
     * 
     * @param input
     * @return
     * @throws IOException 
     */
    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer, 0, buffer.length)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toByteArray();
    }
    
}
