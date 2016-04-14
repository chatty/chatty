
package chatty.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * General URL Request that just reads the whole file into a String.
 * 
 * @author tduva
 */
public abstract class UrlRequest implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(UrlRequest.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    
    private String url;
    private int responseCode;
    private String result;
    private String encoding;
    
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
        this.label = label+" ";
    }

    @Override
    public void run() {
        LOGGER.info(label+"Request: "+url);
        getUrl(url);
        if (result != null) {
            LOGGER.info(label+"Response (" + responseCode + ", " + result.length()
                    + (encoding != null ? ", " + encoding : "") + "): " + url);
        }
        requestResult(result, responseCode);
    }
    
    /**
     * Override this to get the result of the request. The {@code result}
     * contains all the text returned by the request (may be null if the request
     * failed), the {@code responseCode} is the HTTP Response Code (which may be
     * 0 depending on whether/how the request failed).
     * 
     * @param result The text returned by the request, with linebreaks preserved
     * @param responseCode The HTTP Response Code
     */
    abstract public void requestResult(String result, int responseCode);

    /**
     * Gets the given URL and reads the result line by line.
     *
     * @param targetUrl
     * @return
     */
    private void getUrl(String targetUrl) {
        HttpURLConnection connection = null;
        Exception exception = null;
        try {
            URL url = new URL(targetUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            encoding = connection.getContentEncoding();

            // Read response
            InputStream input = connection.getInputStream();
            if ("gzip".equals(connection.getContentEncoding())) {
                input = new GZIPInputStream(input);
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, CHARSET))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                    response.append("\n");
                }
            }
            result = response.toString();
        } catch (IOException ex) {
            exception = ex;
        } finally {
            if (connection != null) {
                try {
                    responseCode = connection.getResponseCode();
                    connection.disconnect();
                    if (responseCode != 200) {
                        LOGGER.warning(label+"Request Error [" + url + "] ("+responseCode+", " + exception + ")");
                    }
                } catch (IOException ex) {
                    LOGGER.warning(label+"Request Error [" + url + "] (" + exception + ")");
                }
            } else {
                LOGGER.warning(label+"Request Error [" + url + "] (" + exception + ")");
            }
        }
    }
}
