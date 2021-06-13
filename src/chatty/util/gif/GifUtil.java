
package chatty.util.gif;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
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
    
    /**
     * Loads a GIF from the given URL, fixing FPS, or just creates an ImageIcon
     * directly if it's not a valid GIF.
     * 
     * @param url The URL (local or internet) to get the image data from
     * @return The created ImageIcon, or null if an error occured creating the
     * image
     * @throws Exception When an error occured loading the image
     */
    public static ImageIcon getGifFromUrl(URL url) throws Exception {
        ImageIcon image = null;
        URLConnection c = url.openConnection();
        try (InputStream input = c.getInputStream()) {
            // Use readAllBytes() because GifDecoder doesn't handle streams well
            byte[] imageData = readAllBytes(input);

            try {
                //System.out.println(hash(imageData)+" "+url);
                image = fixGifFps(imageData);
            }
            catch (Exception ex) {
                /**
                 * If not a GIF, or another error occured, just create the image
                 * normally.
                 */
                image = new ImageIcon(imageData);
                if (image.getIconWidth() == -1) {
                    // new ImageIcon() breaks with some images (rare)
                    // Checking for MediaTracker.ERRORED seems to sometimes
                    // not work.
                    LOGGER.info("Using ImageIO for " + url);
                    Image loadedImage = ImageIO.read(new ByteArrayInputStream(imageData));
                    if (loadedImage != null) {
                        image.setImage(loadedImage);
                        image.setDescription("ImageIO");
                    }
                }
            }

            //System.out.println(url+" "+image.getImageLoadStatus()+" "+image.getIconHeight());
            if (image.getImageLoadStatus() == MediaTracker.ERRORED
                    || image.getIconWidth() == -1) {
                return null;
            }
        }
        return image;
    }

    /**
     * Decodes and re-writes the animated GIF to fix frame delays (according to
     * browser standard) and use better decoding than the built-in one.
     * 
     * @param imageData
     * @return
     * @throws IOException 
     */
    private static ImageIcon fixGifFps(byte[] imageData) throws IOException {
        GifDecoderFMS gif = new GifDecoderFMS();
        gif.read(new ByteArrayInputStream(imageData));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bos)) {
            BufferedImage firstImage = gif.getFrame(0);
            GifSequenceWriter w = GifSequenceWriter.create(output, firstImage);
            for (int i = 0; i < gif.getFrameCount(); i++) {
                w.writeToSequence(gif.getFrame(i), gif.getDelay(i));
            }
            w.close();
        }
        ImageIcon icon = new ImageIcon(bos.toByteArray());
        icon.setDescription("GIF");
        return icon;
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
