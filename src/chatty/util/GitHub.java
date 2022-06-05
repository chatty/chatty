
package chatty.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author tduva
 */
public class GitHub {
    
    private static final Logger LOGGER = Logger.getLogger(GitHub.class.getName());
    
    private static final String URL = "https://api.github.com/repos/chatty/chatty/releases";
    
    public static Releases getReleases() {
        UrlRequest request = new UrlRequest(URL);
        UrlRequest.FullResult result = request.sync();
        if (result.responseCode == 200) {
            try {
                List<Release> parsed = parseReleases(result.getResult());
                return new Releases(parsed);
            } catch (Exception ex) {
                LOGGER.warning("Error parsing releases: "+ex);
            }
        }
        return null;
    }
    
    private static final List<Release> parseReleases(String data) throws Exception {
        JSONArray array = (JSONArray)new JSONParser().parse(data);
        List<Release> result = new ArrayList<>();
        for (Object obj : array) {
            result.add(parseRelease((JSONObject)obj));
        }
        return result;
    }
    
    private static final Release parseRelease(JSONObject data) {
        String tag = (String)data.get("tag_name");
        String name = (String)data.get("name");
        String description = (String)data.get("body");
        boolean isBeta = (Boolean)data.get("prerelease");
        long published = DateTime.parseDatetime((String)data.get("published_at"));
        List<Asset> assets = new ArrayList<>();
        JSONArray assetData = (JSONArray)data.get("assets");
        for (Object obj : assetData) {
            JSONObject asset = (JSONObject)obj;
            String download_url = (String) asset.get("browser_download_url");
            String asset_name = (String) asset.get("name");
            assets.add(new Asset(asset_name, download_url));
        }
        return new Release(tag, name, description, isBeta, assets, published);
    }
    
    public static class Releases {
        private final List<Release> releases;
        private final Release latest;
        private final Release latestBeta;
        
        public Releases(List<Release> releases) {
            this.releases = releases;
            Release latestTemp = null;
            Release latestBetaTemp = null;
            for (Release r : releases) {
                if (latestTemp == null && !r.isBeta()) {
                    latestTemp = r;
                }
                if (latestBetaTemp == null && latestTemp == null && r.isBeta()) {
                    latestBetaTemp = r;
                }
            }
            this.latest = latestTemp;
            this.latestBeta = latestBetaTemp;
        }
        
        public Release getLatest() {
            return latest;
        }
        
        public Release getLatestBeta() {
            return latestBeta;
        }
        
        public List<Release> getReleases() {
            return releases;
        }
        
        public Release getByVersion(String version) {
            for (Release r : releases) {
                if (r.getVersion().equals(version)) {
                    return r;
                }
            }
            return null;
        }
        
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "total: %d latest: %s latestBeta: %s",
                    releases.size(), getLatest(), getLatestBeta());
        }
    }

    public static class Release {
        
        private static final long DAY = 1000*60*60*24;

        private final String name;
        private final String description;
        private final String tag;
        private final boolean isBeta;
        private final List<Asset> assets;
        private final String version;
        private final long publishedAt;
        
        public Release(String tag, String name, String description, boolean beta, List<Asset> assets, long publishedAt) {
            this.tag = tag;
            this.isBeta = beta;
            this.assets = assets;
            this.name = name;
            this.description = description;
            if (tag.startsWith("v")) {
                this.version = tag.substring(1);
            } else {
                this.version = tag;
            }
            this.publishedAt = publishedAt;
        }
        
        public boolean isBeta() {
            return isBeta;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getTag() {
            return tag;
        }
        
        public String getVersion() {
            return version;
        }
        
        public long getPublishedAt() {
            return publishedAt;
        }
        
        public String getPublishedAgo() {
            return DateTime.agoText(publishedAt);
        }
        
        public int monthsOld() {
            return (int)((System.currentTimeMillis() - publishedAt) / (DAY*30));
        }
        
        public int daysOld() {
            return (int)((System.currentTimeMillis() - publishedAt) / (DAY));
        }
        
        public boolean isOld() {
            return (System.currentTimeMillis() - publishedAt) > DAY*100;
        }
        
        public List<Asset> getAssets() {
            return assets;
        }
        
        public Asset getAsset(String nameSuffix) {
            for (Asset a : assets) {
                if (a.getName().endsWith(nameSuffix)) {
                    return a;
                }
            }
            return null;
        }
        
        @Override
        public String toString() {
            return String.format("[%s] %s %s %s", tag, name, isBeta, assets);
        }
    }
    
    public static class Asset {
        
        private final String name;
        private final String download_url;
        
        public Asset(String name, String download_url) {
            this.name = name;
            this.download_url = download_url;
        }
        
        public String getName() {
            return name;
        }
        
        public String getUrl() {
            return download_url;
        }
        
        @Override
        public String toString() {
            return name+"/"+download_url;
        }
    }
    
    public static void main(String[] args) {
        Releases result = getReleases();
        System.out.println(result);
        for (Release r : result.releases) {
            System.out.println(r);
        }
    }
    
}
