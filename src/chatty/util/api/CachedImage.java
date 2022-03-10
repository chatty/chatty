
package chatty.util.api;

import chatty.util.ImageCache;
import chatty.util.ImageCache.ImageRequest;
import chatty.util.ImageCache.ImageResult;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

/**
 * Used for loading an image concurrently for Emoticon and Usericon.
 * 
 * @author tduva
 */
public class CachedImage<T> {
    
    private static final Logger LOGGER = Logger.getLogger(CachedImage.class.getName());

    /**
     * Try loading the image these many times, which will be tried if an error
     * occurs.
     */
    private static final int MAX_LOADING_ATTEMPTS = 3;

    /**
     * How much time (milliseconds) has to pass in between loading attempts,
     * which will only happen if an error occured.
     */
    private static final int LOADING_ATTEMPT_DELAY = 30 * 1000;

    /**
     * Number of seconds emote images are supposed to be cached for before they
     * are refreshed.
     */
    private static final int CACHE_TIME = 60 * 60 * 24 * 14;

    public static interface CachedImageUser {

        void iconLoaded(Image oldImage, Image newImage, boolean sizeChanged);
    }

    /**
     * This needs to be provided when requesting an image and supplies necessary
     * information.
     */
    public static abstract class CachedImageRequester {

        /**
         * Get the URL. Providing an URL for x2 scale is optional and it will
         * fall back to x1 scale. The URL must match the actual scale of the
         * image, so don't return a x2 URL that actually points to an x1 image.
         * 
         * @param scale The image scale (1x, 2x) of the image
         * @param type The image type (static, animated)
         * @return The URL or null if none is available for that size
         */
        public abstract String getImageUrl(int scale, ImageType type);

        /**
         * Get the width and height of the image unscaled.
         *
         * @return The Dimension (must not be null)
         */
        public abstract Dimension getBaseSize();
        
        /**
         * Use the size returned by {@link getBaseSize()}, even if the actual
         * image has a different size. This can be useful if only one version of
         * an image is available that should by default by downsized.
         * 
         * @return 
         */
        public abstract boolean forceBaseSize();

        /**
         * This is run in a background thread, so if it needs to be in the EDT,
         * make sure to handle it as such.
         *
         * @param result
         */
        public void imageLoaded(ImageResult result) { }

        /**
         * Whether to load the actual image. In some cases it might make sense
         * to keep the default placeholder image (e.g. if it is known that the
         * correct image can not be retrieved yet).
         * 
         * @return 
         */
        public boolean loadImage() {
            return true;
        }
        
        /**
         * Provides the ability to modify the loaded image before it is used.
         * 
         * @param image
         * @return The changed image, or null if it shouldn't be changed
         */
        public Image modifyImage(ImageIcon image) {
            return null;
        }

    }

    public static enum ImageType {
        STATIC, ANIMATED_DARK, ANIMATED_LIGHT
    }
    
    public final float scaleFactor;
    public final int maxHeight;
    public final Object customKey;
    public final ImageType imageType;
    private final T object;

    private Set<CachedImageUser> users;
    private final CachedImageRequester requester;
    
    private ImageIcon icon;

    /**
     * The source the image was loaded from. This may not be the same for all
     * images, because image providers may have different URLs for different
     * image sizes.
     */
    private String loadedFrom;

    private boolean loading = false;
    private boolean loadingError = false;
    private boolean isLoaded = false;
    private volatile int loadingAttempts = 0;
    private long lastLoadingAttempt;
    private long lastUsed;
    private final String prefix;

    public CachedImage(T object, CachedImageRequester requester, String prefix, float scaleFactor, int maxHeight, Object customKey, ImageType imageType) {
        this.object = object;
        this.scaleFactor = scaleFactor;
        this.maxHeight = maxHeight;
        this.customKey = customKey;
        this.imageType = imageType;
        this.requester = requester;
        this.prefix = prefix;
    }

    /**
     * Gets the ImageIcon for this CachedImage. When this is called for the
     * first time, the image will be loaded asynchronously and a temporary
     * default image of the same size is returned (based on whatever base size
     * is known).
     *
     * @return
     */
    public ImageIcon getImageIcon() {
        lastUsed = System.currentTimeMillis();
        if (icon == null) {
            /**
             * Note: The temporary image (as well as the actual image) are used
             * as a key for GIF handling in ChannelTextPane, so it is important
             * not to reuse the same temporary image across different emotes.
             */
            icon = getDefaultIcon(false);
            if (requester.loadImage()) {
                loadImage();
            }
        }
        else if (loadingError) {
            if (loadImage()) {
                LOGGER.warning("Trying to load " + object + " again (" + loadedFrom + ")");
            }
        }
        return icon;
    }

    public long getLastUsedAge() {
        return System.currentTimeMillis() - lastUsed;
    }

    /**
     * Get the object this CachedImage is a part of.
     *
     * @return
     */
    public T getObject() {
        return object;
    }

    /**
     * Gets the URL in String form where the image was loaded from.
     *
     * @return The URL or null if the image hasn't been loaded yet
     */
    public String getLoadedFrom() {
        return loadedFrom;
    }

    /**
     * Try to load the image, if it's not already loading and if the max loading
     * attempts are not exceeded.
     *
     * @return true if the image will be attempted to be loaded, false otherwise
     */
    private boolean loadImage() {
        if (!loading && loadingAttempts < MAX_LOADING_ATTEMPTS
                && System.currentTimeMillis() - lastLoadingAttempt > LOADING_ATTEMPT_DELAY) {
            loading = true;
            loadingError = false;
            loadingAttempts++;
            lastLoadingAttempt = System.currentTimeMillis();
            (new IconLoader(this)).execute();
            return true;
        }
        return false;
    }

    private void setLoadingError() {
        setImageIcon(getDefaultIcon(true), false);
        loadingError = true;
    }

    public void setImageIcon(ImageIcon newIcon, boolean success) {
        if (icon == null) {
            setDefaultIcon();
        }
        boolean sizeChanged = icon.getIconWidth() != newIcon.getIconWidth()
                || icon.getIconHeight() != newIcon.getIconHeight();
        Image oldImage = icon.getImage();
        icon.setImage(newIcon.getImage());
        icon.setDescription(newIcon.getDescription());
        if (success) {
            setLoaded();
        }
        informUsers(oldImage, newIcon.getImage(), sizeChanged);
    }

    private void setLoadedFrom(String url) {
        this.loadedFrom = url;
    }

    public boolean isAnimated() {
        boolean emoteAnimated = object instanceof Emoticon ? ((Emoticon) object).isAnimated() : false;
        boolean imageLoadedAsGif = icon != null && icon.getDescription() != null && icon.getDescription().startsWith("GIF");
        return emoteAnimated || imageLoadedAsGif;
    }

    /**
     * Either error or successfully loaded.
     */
    private void setLoadingDone() {
        loading = false;
    }

    private void setLoaded() {
        isLoaded = true;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void addUser(CachedImageUser user) {
        if (users == null) {
            users = Collections.newSetFromMap(new WeakHashMap<CachedImageUser, Boolean>());
        }
        users.add(user);
    }

    private void informUsers(Image oldImage, Image newImage, boolean sizeChanged) {
        for (CachedImageUser user : users) {
            user.iconLoaded(oldImage, newImage, sizeChanged);
        }
    }

    /**
     * Construct a default icon based on the size of this emoticon.
     *
     * @return
     */
    private ImageIcon getDefaultIcon(boolean error) {
        ImageIcon icon = new ImageIcon(getDefaultImage(error));
        Image modified = requester.modifyImage(icon);
        if (modified != null) {
            icon.setImage(modified);
        }
        return icon;
    }

    public void setDefaultIcon() {
        icon = getDefaultIcon(false);
    }

    /**
     * Construct a default image based on the size of this emoticon.
     *
     * @param error If true, uses red color to indicate an error.
     * @return
     */
    private Image getDefaultImage(boolean error) {
        // Determine the assumed size of the emote, for the placerholder image
        Dimension d = ImageRequest.getScaledSize(requester.getBaseSize(), scaleFactor, maxHeight);
        int width = d.width;
        int height = d.height;

        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics g = res.getGraphics();
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.LIGHT_GRAY);
        if (error) {
            g.setColor(Color.red);
        }
        int sWidth = g.getFontMetrics().stringWidth("[x]");
        g.drawString("[x]", width / 2 - sWidth / 2, height / 2);

        g.dispose();
        return res;
    }

    @Override
    public String toString() {
        return String.format("%s/%s/%s/%s|%s",
                scaleFactor, maxHeight, imageType, customKey, icon);
    }

    /**
     * Builds a String of the size of this image, including the original size if
     * there is a scaled size, meant for display.
     *
     * @return The String containing the width and height
     */
    public String getSizeString() {
        Dimension baseSize = requester.getBaseSize();
        int scaledWidth = icon.getIconWidth();
        int scaledHeight = icon.getIconHeight();
        if ((scaledWidth != baseSize.width || scaledHeight != baseSize.height)
                && requester.forceBaseSize()) {
            return scaledWidth + "x" + scaledHeight + " (" + baseSize.width + "x" + baseSize.height + ")";
        }
        return scaledWidth + "x" + scaledHeight;
    }
    
    //==========================
    // Load Image
    //==========================
    /**
     * A Worker class to load the Icon. Not doing this in it's own thread can
     * lead to lag when a lot of new icons are being loaded.
     */
    private class IconLoader extends SwingWorker<ImageIcon, Object> {

        private final CachedImage<T> image;

        public IconLoader(CachedImage<T> image) {
            this.image = image;
        }

        @Override
        protected ImageIcon doInBackground() throws Exception {

            // Get the assumed size or size loaded from the size cache
            Dimension defaultSize = requester.getBaseSize();

            /**
             * Especially Emoji need this, since their emote images aren't the
             * intended size, this forces the set width/height to be used.
             */
            boolean forceBaseSize = requester.forceBaseSize();

            ImageRequest request = new ImageCache.ImageRequest(
                    scale -> requester.getImageUrl(scale, image.imageType),
                    image.scaleFactor,
                    image.maxHeight,
                    defaultSize,
                    forceBaseSize);

            if (!request.valid) {
                return null;
            }

            ImageResult result = ImageCache.getImage(request, prefix, CACHE_TIME);
            image.setLoadedFrom(request.getRequestedURL().toString());

            /**
             * If an error occured loading the image, return null.
             */
            if (result == null || !result.isValidImage()) {
                return null;
            }

            /**
             * Max size fallback, just in case.
             */
            if (result.icon.getIconWidth() > ImageRequest.MAX_SCALED_WIDTH
                    || result.icon.getIconHeight() > ImageRequest.MAX_SCALED_HEIGHT) {
                return null;
            }

            requester.imageLoaded(result);
            
            Image modifiedImage = requester.modifyImage(result.icon);
            if (modifiedImage != null) {
                result.icon.setImage(modifiedImage);
            }
            return result.icon;
        }

        /**
         * The image should be done loading, replace the defaulticon with the
         * actual loaded icon and tell the user that it's loaded.
         */
        @Override
        protected void done() {
            try {
                // This is null if an error occured
                ImageIcon loadedIcon = get();
                if (loadedIcon == null) {
                    image.setLoadingError();
                }
                else {
                    image.setImageIcon(loadedIcon, true);
                }
                image.setLoadingDone();
            }
            catch (InterruptedException | ExecutionException ex) {
                LOGGER.warning("Unexpected error when loading emoticon: " + ex);
            }
        }
    }
}
