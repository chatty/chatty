
package chatty.util;

import chatty.Chatty;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * General URL Request that just reads the whole file into a String.
 * 
 * @author tduva
 */
public class UrlRequest {
    
    private static final Logger LOGGER = Logger.getLogger(UrlRequest.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    
    private static final String VERSION = "Chatty "+Chatty.VERSION;
    
    private String url;
    
    /**
     * Label used for debug messages, which can be set by the user.
     */
    private String label = "";
    
    /**
     * Construct without URL. The URL should be set via {@link setUrl(String)}.
     */
    public UrlRequest() {
    }
        
    public UrlRequest(String url) {
        this.url = url;
    }
    
    /**
     * Sets the URL. Should be done before running the thread.
     * 
     * @param url The URL to request as a String
     */
    public final void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Sets the label to be used for debug messages, for easier identification
     * of what the request is used for.
     * 
     * @param label The label to be put in front of the debug messages (a space
     * is automatically added after it)
     */
    public final void setLabel(String label) {
        this.label = "["+label+"]";
    }

    public void async(ResultListener listener) {
        new Thread(() -> {
            FullResult result = new FullResult();
            performRequest(result);
            listener.result(result.getResult(), result.getResponseCode());
        }).start();
    }
    
    public void asyncLines(ResultLinesListener listener) {
        new Thread(() -> {
            LinesResult result = new LinesResult();
            performRequest(result);
            listener.result(result.getResult(), result.getResponseCode());
        }).start();
    }
    
    public FullResult sync() {
        FullResult result = new FullResult();
        performRequest(result);
        return result;
    }
    
    public LinesResult syncLines() {
        LinesResult result = new LinesResult();
        performRequest(result);
        return result;
    }

    /**
     * Gets the given URL and reads the result.
     *
     * @param targetUrl
     * @return
     */
    private void performRequest(Result result) {
        LOGGER.info("<"+label+" "+url);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent", VERSION);
            connection.setRequestProperty("Accept-Encoding", "gzip");
            
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            String encoding = connection.getContentEncoding();
            
            // Read response
            InputStream input = connection.getInputStream();
            if ("gzip".equals(connection.getContentEncoding())) {
                input = new GZIPInputStream(input);
            }

            int responseCode = connection.getResponseCode();
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, CHARSET))) {
                result.fill(reader, responseCode);
            }
            LOGGER.info(String.format(">%s (%s, %,d%s): %s",
                    label,
                    responseCode,
                    result.getLength(),
                    (encoding != null ? ", " + encoding : ""),
                    url));
        } catch (IOException ex) {
            if (ex instanceof FileNotFoundException) {
                result.responseCode = 404;
            }
            LOGGER.warning(String.format("!%s (%s): %s",
                    label, ex, url));
            result.error = ex.getClass().getSimpleName()+" ("+ex.getLocalizedMessage()+")";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    
    public interface ResultListener {
        public void result(String result, int responseCode);
    }
    
    public interface ResultLinesListener {
        public void result(List<String> lines, int responseCode);
    }
    
    
    public static abstract class Result {
        
        protected int responseCode;
        protected int length;
        protected String error;
        
        public abstract void fill(BufferedReader reader, int responseCode) throws IOException;
        
        public int getResponseCode() {
            return responseCode;
        }
        
        public int getLength() {
            return length;
        }
        
        public String getError() {
            return error;
        }
    }
    
    public static class FullResult extends Result {
        
        private String result;
        
        @Override
        public void fill(BufferedReader reader, int responseCode) throws IOException {
            this.responseCode = responseCode;
            
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                b.append(line).append("\n");
                length += line.length() + 1;
            }
            result = b.toString();
        }
        
        /**
         * The result of the request as text.
         * 
         * @return the result, or null if an error occured
         */
        public String getResult() {
            return result;
        }
    }
    
    public static class LinesResult extends Result {
        
        private List<String> result;

        @Override
        public void fill(BufferedReader reader, int responseCode) throws IOException {
            this.responseCode = responseCode;
            
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
                length += line.length();
            }
            result = lines;
        }
        
        /**
         * The result of the request as lines of text.
         * 
         * @return the result, or null if an error occured
         */
        public List<String> getResult() {
            return result;
        }
        
    }
    
    
    public static void main(String[] args) {
        //UrlRequest request = new UrlRequest("http://tduva.com/res/emotesetinfo.txt");
        //UrlRequest request = new UrlRequest("https://google.de");
        
        // Testing a http URL that will try to redirect to a https URL
        UrlRequest request = new UrlRequest("http://bit.ly/1II1E2o");
        request.setLabel("Test");
        
        FullResult result = request.sync();
        //System.out.println(result.getResult());
        
        //FullResult result2 = request.sync();
        //System.out.println(result2.getResult());
    }
    
}
