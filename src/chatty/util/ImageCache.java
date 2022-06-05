
package chatty.util;

import chatty.util.gif.GifUtil;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 * Allows the use of getImage() methods that get an image from an URL, while
 * automatically caching it on file for the next request. Each request can have
 * a prefix, which is added to the cache filename and makes it possible to only
 * delete some of the cache files with the also contained clear cache functions.
 * 
 * A global path for the cache can be set with {@link setDefaultPath(Path)}, but
 * a different path can also be specified for each method. It is also possible
 * to globally enable/disable the cache.
 * 
 * @author tduva
 */
public class ImageCache {
    
    private static final Logger LOGGER = Logger.getLogger(ImageCache.class.getName());
    
    /**
     * Prefix for all image cache files (in front of the prefix defined in the
     * individual method calls).
     */
    private static final String GLOBAL_PREFIX = "imgcache-";
    
    /**
     * Used as expire time for {@link #deleteExpiredFiles()} and
     * {@link #deleteExpiredFiles(Path)}.
     */
    private static final int DELETE_FILES_OLDER_THAN = 60*60*24*30;
    
    /**
     * Number of files checked that will stop it going to the next directory.
     */
    private static final int EXPIRED_FILES_CHECK_CAP = 100;
    
    private static volatile Path defaultPath = Paths.get("");
    private static volatile boolean cachingEnabled = true;
    
    /**
     * Sets the default image cache Path, used by some functions.
     * 
     * @see #deleteExpiredFiles()
     * @see getImage(URL, String, int)
     * @see clearCache(String)
     * 
     * @param path The default Path for the image cache
     */
    public static void setDefaultPath(Path path) {
        defaultPath = path;
    }
    
    /**
     * Globally enable/disable image caching. If this is off, then the
     * getImage() functions will simply request the image directly.
     * 
     * @param enabled Whether to enable the image cache
     */
    public static void setCachingEnabled(boolean enabled) {
        cachingEnabled = enabled;
    }
    
    /**
     * Some testing stuff.
     * 
     * @param args 
     */
    public static void main(String[] args) {
        try {
            //clearCache("test");
            //saveFile("http://static-cdn.jtvnw.net/jtv_user_pictures/chansub-global-emoticon-7ba1fb012fce74a9-30x30.png");
            setDefaultPath(Paths.get("cache"));
            URL testUrl = new URL("http://127.0.0.1");
            //testUrl = new URL("http://static-cdn.jtvnw.net/jtv_user_pictures/chansub-global-emoticon-7ba1fb012fce74a9-30x30.png");
//            System.out.println(getImage(testUrl, "test", 30));
        } catch (MalformedURLException ex) {
            Logger.getLogger(ImageCache.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Deletes all image cache files with the given prefix, or all image cache
     * files if the prefix is null. Uses the default path.
     * 
     * @param prefix The prefix, or null to delete all image cache files
     * @return The number of deleted files, or -1 if failed
     */
    public static int clearCache(String prefix) {
        return clearCache(defaultPath, prefix);
    }
    
    /**
     * Deletes all image cache files with the given prefix, or all image cache
     * files if the prefix is null. Deletes folders as possible.
     * 
     * @param path The path to delete the files from
     * @param prefix The prefix, or null to delete all image cache files
     * @return The number of deleted files, or -1 if failed
     */
    private static int clearCache(Path path, String prefix) {
        try {
            File dir = path.toRealPath().toFile();
            String fullPrefix;
            if (prefix == null) {
                fullPrefix = GLOBAL_PREFIX;
            } else {
                fullPrefix = GLOBAL_PREFIX+prefix+"__";
            }
            int deletedFilesCount = MiscUtil.deleteInDir(dir, fullPrefix, false);
            LOGGER.info(String.format(Locale.ROOT, "ImageCache: Deleted %d files in %s",
                    deletedFilesCount, dir));
            return deletedFilesCount;
        } catch (IOException ex) {
            LOGGER.warning("ImageCache: Failed to resolve path ["+ex+"]");
            return -1;
        }
    }
    
    /**
     * Deletes any cache files in the old format, where all files were in one
     * directory.
     */
    private static void removeOldCache() {
        try {
            int deletedCount = 0;
            File dir = defaultPath.toRealPath().toFile();
            File[] files = dir.listFiles(file -> {
                return file.isFile() && file.getName().startsWith(GLOBAL_PREFIX);
            });
            if (files != null) {
                for (File file : files) {
                    if (file.delete()) {
                        deletedCount++;
                    }
                }
            }
            if (deletedCount > 0) {
                LOGGER.info(String.format(Locale.ROOT, "Deleted %d files from old cache",
                        deletedCount));
            }
        } catch (IOException ex) {
            LOGGER.warning("ImageCache: Error deleting old cache ("+ex+")");
        }
    }
    
    /**
     * Remove all the image cache files (that are starting with the global
     * prefix) from the path set with setDefaultPath() that have
     * expired according to the default expire time (roughly 2 months).
     * 
     * @see setDefaultPath(Path path)
     * @see #deleteExpiredFiles(Path)
     * @see clearOldFiles(Path path, int expireTime)
     */
    public static void deleteExpiredFiles() {
        deleteExpiredFiles(defaultPath);
    }
    
    /**
     * Remove all the image cache files (that are starting with the global
     * prefix) from the given Path that have expired according to the default
     * expire time.
     * 
     * @param imgCachePath The path to delete the files from
     */
    public static void deleteExpiredFiles(Path imgCachePath) {
        removeOldCache();
        LOGGER.info("ImageCache: Checking for old files in random directory..");
        try {
            File[] dirs = imgCachePath.toRealPath().toFile().listFiles(file -> {
                return file.isDirectory() && file.getName().startsWith(GLOBAL_PREFIX);
            });
            if (dirs != null && dirs.length > 0) {
                File random = dirs[ThreadLocalRandom.current().nextInt(dirs.length)];
                File[] subdirs = random.listFiles(file -> {
                    return file.isDirectory();
                });
                deleteExpiredFilesInSubDirs(subdirs);
            }
        } catch (IOException ex) {
            LOGGER.warning("ImageCache: Failed clearing old files ["+ex+"]");
        }
    }
    
    /**
     * Clears old cache files in one or more of the given directories, in random
     * order, with a cap on how many files are checked.
     *
     * @param dirsArray
     * @throws IOException 
     */
    private static void deleteExpiredFilesInSubDirs(File[] dirsArray) throws IOException {
        if (dirsArray == null || dirsArray.length == 0) {
            return;
        }
        // Take dir from this list and remove when checked
        List<File> dirs = new ArrayList<>(Arrays.asList(dirsArray));
        int fileCount = 0;
        while (!dirs.isEmpty() && fileCount < EXPIRED_FILES_CHECK_CAP) {
            File random = dirs.get(ThreadLocalRandom.current().nextInt(dirs.size()));
            fileCount += deleteExpiredFilesInDir(random.getCanonicalFile(), DELETE_FILES_OLDER_THAN);
            dirs.remove(random);
        }
        LOGGER.info(String.format(Locale.ROOT, "ImageCache: Checked %d files in %d subdirs (%s)",
                fileCount,
                dirsArray.length - dirs.size(),
                dirsArray[0].getParent()));
    }
    
    /**
     * Remove all image cache files (that are starting with the global prefix)
     * from the given Path that have expired according to the given number of
     * seconds.
     * 
     * @param path The path to delete the files from
     * @param expireTime The time in seconds that needs to have passed since the
     * files last modification date for it to be considered expired
     * @return The number of files checked
     */
    private static int deleteExpiredFilesInDir(File dir, int expireTime) {
        File[] files = dir.listFiles();
        if (files != null) {
            int deleted = 0;
            int toDelete = 0;
            for (File file : files) {
                // If not a image cache file according to prefix, go to the next
                if (!file.getName().startsWith(GLOBAL_PREFIX)) {
                    continue;
                }
                // Check last modified date and delete if appropriate
                long lastModified = file.lastModified();
                long ago = (System.currentTimeMillis() - lastModified) / 1000;
                if (ago > expireTime) {
                    toDelete++;
                    if (file.delete()) {
                        deleted++;
                    }
                }
            }
            if (toDelete > 0) {
                LOGGER.info(String.format(Locale.ROOT, "ImageCache: Deleted %d/%d files (checked %d in %s)",
                        deleted, toDelete, files.length, dir));
            }
            return files.length;
        }
        return 0;
    }
    
    /**
     * Gets the image from the given URL, with caching on the default path set
     * with {@link setDefaultPath(Path)}.
     *
     * @see getImage(URL, Path, String, int)
     *
     * @param request
     * @param prefix
     * @param expireTime
     * @return 
     */
    public static ImageResult getImage(ImageRequest request, String prefix, int expireTime) {
        return getImage(request, defaultPath, prefix, expireTime);
    }
    
    /**
     * Gets the image from the given URL, with caching on the given path.
     * 
     * <p>
     * Images are cached in files on the given path, with the given prefix.
     * </p>
     * 
     * <p>
     * If the requested image is already cached and not expired, the cached
     * image will be used. If the requested image is cached, but expired, it
     * will be requested from the URL and the new image used, unless the request
     * from the URL failed, then the cached image will be used. If the requested
     * image is not in the cache, it will be requested from the URL and if that
     * requests fails, null is returned.
     * </p>
     * 
     * <p>
     * If caching fails altogether (e.g. if no read/write access is available)
     * then it will fallback to requesting the image directly from the URL
     * without caching. If that fails as well, null is returned.
     * </p>
     * 
     * <p>
     * Files that are considered local (protocol of "file" or "jar") are not
     * cached.
     * </p>
     * 
     * <p>
     * Expired files will not be deleted, they may even still be used (see
     * above), it simply means a new image requested from the URL is preferred.
     * You can actually delete the cache with {@see clearOldFiles(Path, int)}.
     * </p>
     *
     * @param request 
     * @param path The path to cache the image in
     * @param prefix The prefix to use for the cache file
     * @param expireTime The expire time of the cache in seconds
     * @return The ImageIcon or null if an error occured
     */
    public static ImageResult getImage(ImageRequest request, Path path, String prefix, int expireTime) {
        if (cachingEnabled && !isLocalURL(request.requestedURL)) {
            ImageResult image = getCachedImage(request, path, prefix, expireTime);
            if (image != null) {
                return image;
            }
        }
        return getImageDirectly(request);
    }
    
    /**
     * Requests the image from the given URL directly, without caching.
     * 
     * @param request 
     * @return The ImageIcon or null if an error occured
     */
    public static ImageResult getImageDirectly(ImageRequest request) {
        try {
            return GifUtil.getGifFromUrl(request);
        } catch (Exception ex) {
            LOGGER.warning("Error loading image: "+ex);
        }
        return null;
    }
    
    /**
     * Gets the image from the given URL, with caching on the given path.
     * 
     * <p>
     * Images are cached in files on the given path, with the given prefix.
     * </p>
     * 
     * <p>
     * If the requested image is already cached and not expired, the cached
     * image will be used. If the requested image is cached, but expired, it
     * will be requested from the URL and the new image used, unless the request
     * from the URL failed, then the cached image will be used. If the requested
     * image is not in the cache, it will be requested from the URL and if that
     * requests fails, null is returned.
     * </p>
     * 
     * <p>
     * If writing/reading the files doesn't work at all (e.g. access denied),
     * then null will always be returned since this requires the files to be
     * written to the cache.
     * </p>
     * 
     * @param url The URL to get the image from (also used to determine the
     * cache filename
     * @param path The Path to use as cache directory
     * @param prefix The cache filename prefix
     * @param expireTime How many seconds ago the cache file was last modified
     * for it to be considered expired and trying to request again
     * @return The ImageIcon or null if an error occured
     */
    private static ImageResult getCachedImage(ImageRequest request, Path path, String prefix,
            int expireTime) {
        
        String id = sha1(request.requestedURL.toString());
        
        path = path.resolve(GLOBAL_PREFIX+prefix).resolve(id.substring(0, 1));
        path.toFile().mkdirs();
        Path file = path.resolve(getFilename(prefix, id));
        ImageResult result = null;
        
        Object o = getLockObject(id);
        synchronized(o) {
            result = getCachedImage2(request, file, expireTime);
        }
        removeLockObject(id);
        return result;
    }
    
    private static ImageResult getCachedImage2(ImageRequest request, Path file, int expireTime) {
        ImageResult fromFile = getImageFromFile(file, request);
        if (fromFile == null) {
            // The image was NOT read from file successfully
            //System.out.println("Loading image from server (cache not found)"+url);
            if (saveFile(request.requestedURL, file)) {
                fromFile = getImageFromFile(file, request);
            }
        } else {
            // The image was read from file successfully
            if (hasExpired(expireTime, file)) {
                //System.out.println("Loading image from server (expired)"+url);
                if (saveFile(request.requestedURL, file)) {
                    // Only use new image from file if it was saved successfully
                    fromFile = getImageFromFile(file, request);
                }
            }
        }
        return fromFile;
    }
    
    private static boolean hasExpired(int expireTime, Path file) {
        long lastModified = file.toFile().lastModified();
        long ago = (System.currentTimeMillis() - lastModified) / 1000;
        if (lastModified == 0 || (expireTime > 0 && ago > expireTime)) {
            return true;
        }
        return false;
    }
    
    private static String getFilename(String prefix, String id) {
        return GLOBAL_PREFIX+prefix+"__"+id;
    }
    
    private static boolean saveFile(URL url, Path file) {
        try {
            URLConnection c = url.openConnection();
            try (InputStream is = c.getInputStream()) {
                long written = Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
                if (written > 0) {
                    return true;
                }
            }
        } catch (IOException ex) {
            LOGGER.warning("Error saving " + url + " to " + file + ": " + ex);
        }
        return false;
    }
    
    private static ImageResult getImageFromFile(Path file, ImageRequest request) {
        try {
            request.setCacheFile(file);
            return GifUtil.getGifFromUrl(request);
        } catch (FileNotFoundException ex) {
            // Fail silently, images are expected to often be not cached yet
        } catch (Exception ex) {
            LOGGER.warning("Error loading image from file: "+ex+" "+Debugging.getStacktrace(ex));
        }
        return null;
    }
    
    public static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return byteArrayToHexString(md.digest(input.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            Logger.getLogger(ImageCache.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
    
    /**
     * URL is currently assumed local if the protocol is either "file" or "jar".
     * 
     * @param url The URL to check
     * @return true if the URL is assumed local, false otherwise
     */
    public static boolean isLocalURL(URL url) {
        return "file".equalsIgnoreCase(url.getProtocol())
                || "jar".equalsIgnoreCase(url.getProtocol());
    }
    
    private static final Map<String, Object> lockObjects = new HashMap<>();
    
    private static Object getLockObject(String file) {
        synchronized(lockObjects) {
            Object o = lockObjects.get(file);
            if (o == null) {
                o = new Object();
                lockObjects.put(file, o);
            }
            return o;
        }
    }
    
    private static void removeLockObject(String file) {
        synchronized(lockObjects) {
            lockObjects.remove(file);
        }
    }
    
    /**
     * Handles determining the scaled size and fitting URL for loading an image.
     */
    public static class ImageRequest {
        
        public static final int MAX_SCALED_WIDTH = 250;
        public static final int MAX_SCALED_HEIGHT = 150;
        
        public final int urlFactor;
        public final int maxHeight;
        public final float scaleFactor;
        public final boolean valid;
        public final boolean resize;
        public final Dimension defaultSize;
        
        private final URL requestedURL;
        private URL cacheURL;

        public ImageRequest(URL url) {
            this.requestedURL = url;
            this.urlFactor = 1;
            this.maxHeight = -1;
            this.scaleFactor = -1;
            this.valid = true;
            this.resize = false;
            this.defaultSize = null;
        }
        
        /**
         * An image request with resizing.
         * 
         * @param urlRequester Must return an URL for an URL factor (1 or 2) or
         * null if the factor can't be served
         * @param scaleFactor The image scale for resizing
         * @param maxHeight The max height of the image
         * @param defaultSize The default base size
         * @param forceDefaultAsBase Use the defaultSize for resizing, even if
         * the actualBaseSize is different
         */
        public ImageRequest(Function<Integer, String> urlRequester, float scaleFactor, int maxHeight, Dimension defaultSize, boolean forceDefaultAsBase) {
            // Determine URL
            Dimension expectedSize = getScaledSize(defaultSize, scaleFactor, maxHeight);
            int actualUrlFactor = 1;
            String preferredUrl = null;
            if (expectedSize.width > defaultSize.width) {
                preferredUrl = urlRequester.apply(2);
                if (preferredUrl != null) {
                    actualUrlFactor = 2;
                }
            }
            if (preferredUrl == null) {
                preferredUrl = urlRequester.apply(1);
            }
            URL actualUrl = null;
            try {
                actualUrl = new URL(preferredUrl);
            }
            catch (MalformedURLException ex) {
                LOGGER.warning("Invalid image URL: "+preferredUrl);
            }
            
            // Init
            this.maxHeight = maxHeight;
            this.scaleFactor = scaleFactor;
            this.requestedURL = actualUrl;
            this.urlFactor = actualUrlFactor;
            this.valid = requestedURL != null;
            this.resize = true;
            this.defaultSize = forceDefaultAsBase ? defaultSize : null;
        }
        
        public void setCacheFile(Path file) throws MalformedURLException {
            this.cacheURL = file.toUri().toURL();
        }
        
        /**
         * This may be a cache file or the originally requested URL.
         * 
         * @return 
         */
        public URL getLoadFromURL() {
            return cacheURL != null ? cacheURL : requestedURL;
        }
        
        /**
         * This is the original URL that was requested.
         * 
         * @return 
         */
        public URL getRequestedURL() {
            return requestedURL;
        }
        
        /**
         * Resize image if enabled and necessary and return created result.
         * 
         * @param icon
         * @param isGif
         * @return 
         */
        public ImageResult finishIcon(ImageIcon icon, boolean isGif) {
            Dimension actualBaseSize = getSizeFromImage(icon);
            Dimension baseSize = defaultSize != null ? defaultSize : actualBaseSize;
            Dimension scaledSize = getScaledSize(baseSize, scaleFactor, maxHeight);
            if (resize && !actualBaseSize.equals(scaledSize)) {
                Image image = getScaledImage(icon.getImage(), scaledSize.width, scaledSize.height);
                icon.setImage(image);
            }
            return new ImageResult(icon, actualBaseSize, isGif);
        }
        
        /**
         * Get the scaled size, taking into account the actualBaseSize only if
         * this request isn't forced to use the defaultSize.
         * 
         * @param actualBaseSize The urlFactor corrected actual size
         * @return The scaled size, or null if no resizing should be performed
         */
        public Dimension getScaledSizeIfNecessary(Dimension actualBaseSize) {
            Dimension baseSize = defaultSize != null ? defaultSize : actualBaseSize;
            Dimension scaledSize = getScaledSize(baseSize, scaleFactor, maxHeight);
            if (resize && !actualBaseSize.equals(scaledSize)) {
                return scaledSize;
            }
            return null;
        }
        
        /**
         * Scale the given Dimension based on the given settings. Also checks if
         * the resulting size is within reasonable boundaries.
         *
         * @param d The dimension to modify
         * @param scaleFactor The scale factor, values smaller or equal to 0 are
         * ignored
         * @param maxHeight The maximum height, values smaller or equal to 0 are
         * ignored
         * @return A new Dimension that has been scaled accordingly
         */
        public static Dimension getScaledSize(Dimension d, float scaleFactor, int maxHeight) {
            float scaledWidth = d.width;
            float scaledHeight = d.height;

            if (scaleFactor > 0) {
                scaledWidth *= scaleFactor;
                scaledHeight *= scaleFactor;
            }

            if (maxHeight > 0 && scaledHeight > maxHeight) {
                scaledWidth = scaledWidth / (scaledHeight / maxHeight);
                scaledHeight = maxHeight;
            }

            /**
             * Convert into int before checking
             */
            int resultWidth = (int) scaledWidth;
            int resultHeight = (int) scaledHeight;
            if (resultWidth < 1) {
                resultWidth = 1;
            }
            if (resultHeight < 1) {
                resultHeight = 1;
            }

            /**
             * This shouldn't really happen, but just in case, so no ridicously
             * huge (default) image is created.
             */
            if (resultWidth > MAX_SCALED_WIDTH) {
                resultWidth = MAX_SCALED_WIDTH;
            }
            if (resultHeight > MAX_SCALED_HEIGHT) {
                resultHeight = MAX_SCALED_HEIGHT;
            }
            return new Dimension(resultWidth, resultHeight);
        }

        /**
         * Gets the URL factor corrected size from the image.
         *
         * @param icon The ImageIcon to get the size from
         * @return The size of the given image
         */
        public Dimension getSizeFromImage(ImageIcon icon) {
            return getUrlFactorCorrectedSize(icon.getIconWidth(), icon.getIconHeight());
        }
        
        /**
         * Gets the URL factor corrected size from the image.
         *
         * @param image The image to get the size from
         * @return The size of the given image
         */
        public Dimension getSizeFromImage(BufferedImage image) {
            return getUrlFactorCorrectedSize(image.getWidth(), image.getHeight());
        }
        
        /**
         * Corrects the given size based on the stored URL factor, resulting in
         * the base size (so if a 2x URL was used it would divide by 2).
         * 
         * @param width
         * @param height
         * @return 
         */
        private Dimension getUrlFactorCorrectedSize(int width, int height) {
            if (urlFactor > 1) {
                width /= urlFactor;
                height /= urlFactor;
            }
            return new Dimension(width, height);
        }

        private Image getScaledImage(Image img, int width, int height) {
            return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        }
        
    }
    
    /**
     * Holds some additional information related to the loaded image.
     */
    public static class ImageResult {
        
        /**
         * The loaded icon, possibly resized.
         */
        public final ImageIcon icon;
        
        /**
         * The URL factor corrected base size of the actual loaded image.
         */
        public final Dimension actualBaseSize;
        
        /**
         * Whether the image was loaded through the special GIF loader. It may
         * still be a GIF even if this is false, although only if an error
         * occured with the special GIF loader.
         */
        public final boolean loadedAsGif;
        
        public ImageResult(ImageIcon icon, Dimension actualBaseSize, boolean loadedAsGif) {
            this.icon = icon;
            this.actualBaseSize = actualBaseSize;
            this.loadedAsGif = loadedAsGif;
        }
        
        public boolean isValidImage() {
            return icon != null
                    && icon.getImageLoadStatus() != MediaTracker.ERRORED
                    && icon.getIconWidth() != -1
                    && icon.getIconHeight() != -1;
        }
        
    }
    
}
