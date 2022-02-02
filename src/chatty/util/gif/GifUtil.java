
package chatty.util.gif;

import chatty.util.ImageCache.ImageRequest;
import chatty.util.ImageCache.ImageResult;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
     * @param request Contains the URL (local or internet) to get the image data
     * from and other information
     * @return The created ImageIcon, or null if an error occured creating the
     * image
     * @throws Exception When an error occured loading the image
     */
    public static ImageResult getGifFromUrl(ImageRequest request) throws Exception {
        ImageResult result = null;
        URLConnection c = request.getLoadFromURL().openConnection();
        try (InputStream input = c.getInputStream()) {
            // Use readAllBytes() because GifDecoder doesn't handle streams well
            byte[] imageData = readAllBytes(input);

            try {
                //System.out.println(hash(imageData)+" "+url);
                result = fixGifFps(imageData, request);
            }
            catch (Exception ex) {
                /**
                 * If not a GIF, or another error occured, just create the image
                 * normally.
                 */
                ImageIcon icon = new ImageIcon(imageData);
                if (icon.getIconWidth() == -1) {
                    // new ImageIcon() breaks with some images (rare)
                    // Checking for MediaTracker.ERRORED seems to sometimes
                    // not work.
                    LOGGER.info("Using ImageIO for " + request.getRequestedURL()+" / "+request.getLoadFromURL());
                    Image loadedImage = ImageIO.read(new ByteArrayInputStream(imageData));
                    if (loadedImage != null) {
                        icon.setImage(loadedImage);
                        icon.setDescription("ImageIO");
                    }
                }
                boolean iconValid = icon.getImageLoadStatus() != MediaTracker.ERRORED
                        && icon.getIconWidth() != -1;
                if (iconValid) {
                    result = request.finishIcon(icon, false);
                }
            }
            
            if (result != null && !result.isValidImage()) {
                result.icon.getImage().flush();
                return null;
            }
        }
        return result;
    }

    /**
     * Decodes and re-writes the animated GIF to fix frame delays (according to
     * browser standard) and use better decoding than the built-in one.
     * 
     * @param imageData
     * @return
     * @throws IOException 
     */
    private static ImageResult fixGifFps(byte[] imageData, ImageRequest request) throws IOException {
        GifDecoderFMS gif = new GifDecoderFMS();
        gif.read(new ByteArrayInputStream(imageData));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Dimension actualBaseSize;
        try (ImageOutputStream output = ImageIO.createImageOutputStream(bos)) {
            // Determine sizes
            BufferedImage firstImage = gif.getFrame(0);
            actualBaseSize = request.getSizeFromImage(firstImage);
            Dimension scaledSize = request.getScaledSizeIfNecessary(actualBaseSize);
            // Write frames
            GifSequenceWriter w = GifSequenceWriter.create(output, firstImage);
            for (int i = 0; i < gif.getFrameCount(); i++) {
                BufferedImage frame = gif.getFrame(i);
                if (scaledSize != null) {
                    frame = resize(frame, scaledSize.width, scaledSize.height);
                }
                w.writeToSequence(frame, gif.getDelay(i));
            }
            w.close();
        }
        ImageIcon icon = new ImageIcon(bos.toByteArray());
        icon.setDescription("GIF");
        return new ImageResult(icon, actualBaseSize, true);
    }
    
    private static BufferedImage resize(BufferedImage image, int width, int height) {
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage result = new BufferedImage(width, height, image.getType());
        result.getGraphics().drawImage(scaledImage, 0, 0, null);
        result.getGraphics().dispose();
        return result;
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
