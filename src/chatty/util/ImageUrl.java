
package chatty.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 *
 * @author tduva
 */
public abstract class ImageUrl {

    private static final Map<TemplateImageUrl, TemplateImageUrl> cache = new WeakHashMap<>();
    
    private static ImageUrl cache(TemplateImageUrl object) {
        synchronized (cache) {
            ImageUrl cached = cache.get(object);
            if (cached != null) {
                return cached;
            }
            cache.put(object, object);
            return object;
        }
    }
    
    public static int cacheSize() {
        return cache.size();
    }
    
    public static class Builder {
        
        private final Map<Integer, String> urls;
        private final String id;
        
        public Builder(String id) {
            this.id = id;
            this.urls = new HashMap<>();
        }
        
        public Builder(String id, Map<Integer, String> urls) {
            this.id = id;
            this.urls = urls;
        }
        
        public ImageUrl.Builder addUrl(int size, String url) {
            urls.put(size, url);
            return this;
        }
        
        public ImageUrl build() {
            if (urls.size() == 1 && urls.containsKey(1)) {
                return new SingleImageUrl(urls.get(1));
            }
            if (id == null) {
                return new MapImageUrl(urls);
            }
            String template = null;
            for (Map.Entry<Integer, String> entry : urls.entrySet()) {
                int size = entry.getKey();
                String url = entry.getValue();
                String temp = StringUtil.replaceLast(url, id, "{id}");
                temp = StringUtil.replaceLast(temp, String.valueOf(entry.getKey()), "{x}");
                if (!buildUrl(temp, id, size).equals(url)
                        || (template != null && !template.equals(temp))) {
                    return new MapImageUrl(urls);
                }
                template = temp;
            }
            ImageUrl result = cache(new TemplateImageUrl(template, urls.keySet()));
//            System.out.println(result+" "+System.identityHashCode(result)+" "+urls);
//            System.out.println(cacheSize());
            return result;
        }
        
    }
    
    public static final void main(String[] args) {
        ImageUrl url = ImageUrl.builder("382581").addUrl(1, "https://cdn.frankerfacez.com/emote/382581/1").addUrl(2, "https://cdn.frankerfacez.com/emote/382581/2").build();
        ImageUrl url2 = ImageUrl.builder("382582").addUrl(1, "https://cdn.frankerfacez.com/emote/382582/1").addUrl(2, "https://cdn.frankerfacez.com/emote/382582/2").build();
        System.out.println(url == url2);
        
        System.out.println(ImageUrl.builder("382581").addUrl(1, "https://cdn.frankerfacez.com/emote/382581/2").addUrl(2, "https://cdn.frankerfacez.com/emote/382581/2").build().getClass());
    }
    
    public static ImageUrl.Builder builder(String id) {
        return new ImageUrl.Builder(id);
    }
    
    private static String buildUrl(String template, String id, int size) {
        return template.replace("{id}", id).replace("{x}", String.valueOf(size));
    }
    
    public abstract String getUrl(String id, int size);
    
    public static class TemplateImageUrl extends ImageUrl {
        
        private final String template;
        private final Set<Integer> sizes;
        
        private TemplateImageUrl(String template, Set<Integer> sizes) {
            this.template = template;
            this.sizes = sizes;
        }
        
        @Override
        public String getUrl(String id, int size) {
            if (sizes.contains(size)) {
                return buildUrl(template, id, size);
            }
            return null;
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s]", template, sizes);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TemplateImageUrl other = (TemplateImageUrl) obj;
            if (!Objects.equals(this.template, other.template)) {
                return false;
            }
            return Objects.equals(this.sizes, other.sizes);
        }
        
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.template);
            hash = 53 * hash + Objects.hashCode(this.sizes);
            return hash;
        }
        
    }
    
    public static class MapImageUrl extends ImageUrl {

        private final Map<Integer, String> urls;
        
        private MapImageUrl(Map<Integer, String> urls) {
            this.urls = urls;
        }
        
        @Override
        public String getUrl(String id, int size) {
            return urls.get(size);
        }
        
    }
    
    public static class SingleImageUrl extends ImageUrl {
        
        private final String url;
        
        private SingleImageUrl(String url) {
            this.url = url;
        }
        
        @Override
        public String getUrl(String id, int size) {
            if (size == 1) {
                return url;
            }
            return null;
        }
        
    }
    
}
