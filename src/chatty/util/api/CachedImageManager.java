
package chatty.util.api;

import chatty.util.HalfWeakSet;
import chatty.util.HalfWeakSet2;
import chatty.util.api.CachedImage.CachedImageRequester;
import chatty.util.api.CachedImage.ImageType;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Objects;
import chatty.util.api.CachedImage.CachedImageUser;

/**
 * Provides a facility to manage loading and caching of images.
 * 
 * @author tduva
 */
public class CachedImageManager<T> {

    private HalfWeakSet2<CachedImage<T>> images;
    private final T object;
    private final CachedImageRequester requester;
    private final String prefix;
    
    public CachedImageManager(T object, CachedImageRequester requester, String prefix) {
        this.object = object;
        this.requester = requester;
        this.prefix = prefix;
    }
    
    /**
     * Get a cached image or create a new one if necessary. All the parameters
     * (except for the CachedImageUser) have to match in order for a cached image
     * to be selected.
     * 
     * @param scaleFactor How to scale the image (values equal or smaller 0
     * don't scale)
     * @param maxHeight The resulting scaled image won't be higher than this
     * (values equal or small 0 ignore this)
     * @param customKey Doesn't affect the image, but provides an additional way
     * to differentiate between cached images
     * @param imageType Static, animated etc.
     * @param user Is informed when the image changes
     * @return An CachedImage, either containing a default placeholder image or
 the final cached image
     */
    public CachedImage<T> getIcon(float scaleFactor, int maxHeight, Object customKey, ImageType imageType, CachedImageUser user) {
        if (images == null) {
            images = new HalfWeakSet2<>();
        }
        CachedImage<T> resultImage = null;
        for (CachedImage<T> image : images) {
            if (image != null
                    && image.scaleFactor == scaleFactor
                    && image.maxHeight == maxHeight
                    && Objects.equals(image.customKey, customKey)
                    && image.imageType == imageType) {
                resultImage = image;
            }
        }
        if (resultImage == null) {
            resultImage = new CachedImage<>(object, requester, prefix, scaleFactor, maxHeight, customKey, imageType);
            images.add(resultImage);
        } else {
            images.markStrong(resultImage);
        }
        if (user != null) {
            resultImage.addUser(user);
        }
        return resultImage;
    }
    
    /**
     * Removes all currently cached images. Should probably be called from the
     * EDT.
     */
    public void clearImages() {
        if (images != null) {
            images.clear();
        }
    }
    
    /**
     * Set unused image objects to be garbage collected. Should only be called
     * from the EDT.
     *
     * @param imageExpireMinutes
     * @return
     */
    public int clearOldImages(int imageExpireMinutes) {
        if (images != null) {
            images.cleanUp();
            Set<CachedImage<T>> toRemove = new HashSet<>();
            Iterator<CachedImage<T>> it = images.strongIterator();
            while (it.hasNext()) {
                CachedImage<T> image = it.next();
                if (image.getLastUsedAge() > imageExpireMinutes*60*1000) {
                    toRemove.add(image);
                }
            }
            for (CachedImage<T> image : toRemove) {
                images.markWeak(image);
            }
            return toRemove.size();
        }
        return 0;
    }
    
}
