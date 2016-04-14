
package chatty.util.api;

import chatty.Chatty;
import chatty.util.api.TwitchApi.RequestType;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * A request to the Twitch API that is running in a seperate Thread. Does the
 * request and reads the answer, which is then send back to the TwitchApi
 * object.
 * 
 * @author tduva
 */
public class TwitchApiRequest implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(TwitchApiRequest.class.getName());
    /**
     * Timeout for connecting in milliseconds.
     */
    private static final int CONNECT_TIMEOUT = 30*1000;
    /**
     * Timeout for reading from the connection in milliseconds.
     */
    private static final int READ_TIMEOUT = 60*1000;
    
    private static final String CLIENT_ID = Chatty.CLIENT_ID;
    
    private String url;
    private TwitchApi origin;
    private RequestType type;
    private String token;
    private String data = null;
    private String encoding;
    private int responseCode = -1;
    private String requestMethod = "GET";
    private String contentType = "application/json";
    private String apiVersion = "v3";
    private String error;
    
    /**
     * Construct a new request without a token.
     * 
     * @param origin
     * @param type
     * @param url 
     */
    public TwitchApiRequest(TwitchApi origin, RequestType type, String url) {
        this(origin, type, url, null);
    }
    
    /**
     * Construct a new request with the given type, url and token. The token
     * can be null if no token should be used.
     * 
     * @param origin
     * @param type
     * @param url
     * @param token 
     */
    public TwitchApiRequest(TwitchApi origin, RequestType type, String url, String token) {
        this.url = url;
        this.origin = origin;
        this.type = type;
        this.token = token;
    }
    
    /**
     * Set the data, if data is to be send.
     * 
     * @param data
     * @param requestMethod 
     */
    public void setData(String data, String requestMethod) {
        this.data = data;
        this.requestMethod = requestMethod;
    }
    
    public void setApiVersion(String version) {
        this.apiVersion = version;
    }
    
    /**
     * Set the request type (like GET, POST, ..)
     * 
     * @param requestMethod 
     */
    public void setRequestType(String requestMethod) {
        this.requestMethod = requestMethod;
    }
    
    /**
     * Sets the content type of the send data.
     * 
     * @param contentType 
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void run() {
        String result = getUrl(url);
        if (token != null) {
            origin.requestResult(type, url, result, responseCode, error, encoding, token);
        }
        else {
            origin.requestResult(type, url, result, responseCode, error, encoding);
        }
    }
    
    /**
     * Request the given URL, with the properties (request type, token, data)
     * as definied in the creation of this object.
     * 
     * @param targetUrl
     * @return 
     */
    private String getUrl(String targetUrl) {
        Charset charset = Charset.forName("UTF-8");
        URL url;
        HttpURLConnection connection = null;

        try {
            url = new URL(targetUrl);
            connection = (HttpURLConnection)url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
        
            // Request properties
            connection.setRequestProperty("Accept", "application/vnd.twitchtv."+apiVersion+"+json");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("Client-ID", CLIENT_ID);
            // Add token if necessary
            if (token != null) {
                connection.setRequestProperty("Authorization", "OAuth "+token);
            }
            
            connection.setRequestMethod(requestMethod);
            connection.setRequestProperty("Content-Type", contentType);
            if (data != null) {
                // Send data if necessary
                connection.setDoOutput(true);
                try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), charset)) {
                    out.write(data);
                }
                LOGGER.info("Sending data: "+data);
            }

            // Whether token was used or not
            if (token != null) {
                LOGGER.info(connection.getRequestMethod()+" ("+apiVersion+"): "+targetUrl+" "
                        + "(using authorization)");
            } else {
                LOGGER.info(connection.getRequestMethod()+" ("+apiVersion+"): "+targetUrl);
            }
            
            encoding = connection.getContentEncoding();
            //System.out.println(connection.getHeaderFields());
            
            // Read response
            InputStream input = connection.getInputStream();
            if ("gzip".equals(connection.getContentEncoding())) {
                input = new GZIPInputStream(input);
            }

            StringBuilder response;
            try (BufferedReader reader
                    = new BufferedReader(new InputStreamReader(input, charset))) {
                String line;
                response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            
            return response.toString();
        } catch (SocketTimeoutException ex) {
            error = ex.toString();
            return null;
        } catch (IOException ex) {
            error = ex.toString();
            return null;
        } finally {
            if (connection != null) {
                try {
                    responseCode = connection.getResponseCode();
                } catch (IOException ex) {
//                    LOGGER.warning("IOException2: " + ex);
                }
                connection.disconnect();
            }
        }
    }
    
}
