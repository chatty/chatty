
package chatty.util.api;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tduva
 */
public class ChatGif {

    private CachedImageManager<ChatGif> images;
    public final String id;
    public final String url;
    public final String msgText;
    
    /**
     * Start/end index in the message, not necessarily correct when using cached
     * instance of the same GIF (if the message actually differs).
     */
    public final int start;
    public final int end;
    
    public ChatGif(String id, String url, int start, int end, String msgText) {
        this.id = id;
        this.url = url;
        this.msgText = msgText;
        this.start = start;
        this.end = end;
    }
    
    public CachedImage<ChatGif> getIcon(CachedImage.CachedImageUser user, int maxHeight) {
        if (images == null) {
            images = new CachedImageManager<>(this, new CachedImage.CachedImageRequester() {
                @Override
                public String getImageUrl(int scale, CachedImage.ImageType type) {
                    if (scale == 1) {
                        return url;
                    }
                    return null;
                }

                @Override
                public Dimension getBaseSize() {
                    return new Dimension(100, 100);
                }

                @Override
                public boolean forceBaseSize() {
                    return false;
                }
            }, "chatgif");
        }
        return images.getIcon(-1, maxHeight, null, CachedImage.ImageType.ANIMATED_DARK, null, user);
    }
    
    public int clearOldImages(int imageExpireMinutes) {
        return images.clearOldImages(imageExpireMinutes);
    }
    
    @Override
    public String toString() {
        return msgText;
    }
    
    private static final List<ChatGif> EMPTY = new ArrayList<>();
    
    public static List<ChatGif> parse(String data, String msgText) {
        if (data == null || data.isEmpty()) {
            return EMPTY;
        }

        List<ChatGif> result = new ArrayList<>();
        String[] gifs = data.split(",");
        for (String gif : gifs) {
            String[] split = gif.split("\\|", 3);
            try {
                String[] rangeSplit = split[0].split("-", 2);
                int start = Integer.parseInt(rangeSplit[0]);
                int end = Integer.parseInt(rangeSplit[1]);

                String id = split[1];
                String url = split[2];

                result.add(new ChatGif(id, url, start, end, msgText.substring(start+1, end)));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                //System.out.println(ex);
            }
        }
        return result;
    }
    
    public static void main(String[] args) {
        System.out.println(parse("0-43|l1AsJYjSxCcslp3Vu|https://media4.giphy.com/media/l1AsJYjSxCcslp3Vu/giphy.gif?cid=095d7a5d2hfstidos4gdmzftw987c5bxkd27qy6ayechjslo&ep=v1_gifs_search&rid=giphy.gif&ct=g", "[Season 1 Test GIF by SpongeBob SquarePants]"));
    }
    
}
