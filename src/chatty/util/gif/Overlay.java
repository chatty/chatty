
package chatty.util.gif;

import chatty.gui.GuiUtil;
import chatty.util.Debugging;
import chatty.util.ImageCache.ImageRequest;
import chatty.util.seventv.WebPUtil;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;

/**
 *
 * @author tduva
 */
public class Overlay {
    
    /**
     * Create a new image that combines the given images, drawing them on top of
     * each other, with the first image appearing the furthest behind.
     * 
     * @param overlay
     * @return
     * @throws Exception 
     */
    public static ImageIcon overlayNew(LinkedHashMap<ImageIcon, Integer> overlay) throws Exception {
        if (overlay == null || overlay.isEmpty()) {
            return null;
        }
        if (overlay.size() == 1) {
            return overlay.entrySet().iterator().next().getKey();
        }
        ImageIcon base = null;
        int width = 0;
        int height = 0;
        int oh = 0;
        for (Map.Entry<ImageIcon, Integer> entry : overlay.entrySet()) {
            ImageIcon icon = entry.getKey();
            if (base == null) {
                base = entry.getKey();
            }
            width = Integer.max(width, icon.getIconWidth());
            height = Integer.max(height, icon.getIconHeight());
            int offset = Math.abs((int)(entry.getValue()/100.0*icon.getIconHeight()));
            int inclOffset = icon.getIconHeight()+offset;
            if (inclOffset > height) {
                oh += inclOffset - height;
                height = inclOffset;
            }
        }
        
        if (containsAnimatedImageSource(overlay)) {
            //-----------------
            // Animated result
            //-----------------
            // Contains at least one animated image
            List<ListAnimatedImage> images = new ArrayList<>();
            for (Map.Entry<ImageIcon, Integer> entry : overlay.entrySet()) {
                ImageIcon icon = entry.getKey();
                int offset = (int) (entry.getValue() / 100.0 * icon.getIconHeight());
                
                if (icon.getImage().getSource() instanceof AnimatedImageSource) {
                    //----------------
                    // Animated Image
                    //----------------
                    int[] pixels = new int[icon.getIconWidth() * icon.getIconHeight()];
                    AnimatedImage anim = ((AnimatedImageSource) icon.getImage().getSource()).getAnimatedImage();
                    List<ListAnimatedImageFrame> frames = new ArrayList<>();
                    for (int i = 0; i < anim.getFrameCount(); i++) {
                        BufferedImage bi = getImage(pixels, anim, i);
                        frames.add(createFrame(bi,
                                bi.getWidth(), bi.getHeight(),
                                width, height, offset, oh,
                                anim.getDelay(i)));
                    }
                    images.add(new ListAnimatedImage(frames, width, height, anim.getName()));
                }
                else {
                    //--------------
                    // Static Image
                    //--------------
                    List<ListAnimatedImageFrame> frames = new ArrayList<>();
                    frames.add(createFrame(icon.getImage(),
                            icon.getIconWidth(), icon.getIconHeight(),
                            width, height, offset, oh,
                            Integer.MAX_VALUE));
                    images.add(new ListAnimatedImage(frames, width, height, "Static Image"));
                }
            }
            return new ImageIcon(new OverlayListAnimatedImage(images, width, height, "Overlayed").createImage());
        }
        
        //---------------
        // Static result
        //---------------
        // Contains only static images
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        for (Map.Entry<ImageIcon, Integer> entry : overlay.entrySet()) {
            ImageIcon icon = entry.getKey();
            int offset = (int)(entry.getValue()/100.0*icon.getIconHeight());
            g.drawImage(icon.getImage(),
                    (width - icon.getIconWidth()) / 2,
                    (height - icon.getIconHeight()) / 2 + offset + oh,
                    null);
        }
        g.dispose();
        return new ImageIcon(img);
    }
    
    private static ListAnimatedImageFrame createFrame(Image image,
            int imgWidth, int imgHeight,
            int targetWidth, int targetHeight, int offset, int oh,
            int delay) throws IOException {
        BufferedImage result = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawImage(image,
                (targetWidth - imgWidth) / 2,
                (targetHeight - imgHeight) / 2 + offset + oh,
                null);
        g.dispose();
        return new ListAnimatedImageFrame(result, delay);
    }
    
    private static boolean containsAnimatedImageSource(LinkedHashMap<ImageIcon, Integer> overlay) {
        for (ImageIcon icon : overlay.keySet()) {
            if (icon.getImage().getSource() instanceof AnimatedImageSource) {
                return true;
            }
        }
        return false;
    }
    
    private static BufferedImage getImage(int[] pixels, AnimatedImage anim, int index) throws Exception {
        anim.getFrame(index, pixels);
        int width = anim.getSize().width;
        int height = anim.getSize().height;
        ColorModel colorModel = new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);
        SampleModel sampleModel = colorModel.createCompatibleSampleModel(width, height);
        DataBufferInt db = new DataBufferInt(pixels, width * height);
        WritableRaster raster = WritableRaster.createWritableRaster(sampleModel, db, null);
        return new BufferedImage(colorModel, raster, false, new Hashtable<Object, Object>());
    }
    
    // For testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            WebPUtil.runIfWebPAvailable(() -> {
                try {
                    JFrame dialog = new JFrame();
                    dialog.setSize(100, 100);
                    dialog.setLocationRelativeTo(null);
                    dialog.setVisible(true);
                    LinkedHashMap<ImageIcon, Integer> map = new LinkedHashMap<>();
                    
                    map.put(GifUtil.getGifFromUrl(new ImageRequest(new URL("<url>"))).icon, 0);
                    map.put(GifUtil.getGifFromUrl(new ImageRequest(new URL("<url>"))).icon, 0);
                    
                    Debugging.command("overlayframe");
                    dialog.add(new JLabel("text", overlayNew(map), 0), BorderLayout.CENTER);
                    dialog.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//                    AnimatedImage.setAnimationPause(0);
                    Timer timer = new Timer(100, e -> {
                        dialog.revalidate();
                    });
                    timer.start();
                } catch (Exception ex) {
                    Logger.getLogger(GuiUtil.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        });
        
    }
    
}
