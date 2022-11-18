
package chatty.util.api.queue;

import chatty.Chatty;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.json.simple.JSONObject;

/**
 *
 * @author tduva
 */
public class Request implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Request.class.getName());
    
    private static final Charset CHARSET = Charset.forName("UTF-8");
    
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
    private RequestResultListener listener;
    private String token;
    private String data = null;
    private String requestMethod = "GET";
    private String contentType = "application/json";
    
    /**
     * Construct a new request with the given type, url and token. The token
     * can be null if no token should be used.
     * 
     * @param url
     * @param version
     */
    public Request(String url) {
        this.url = url;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public void setJSONData(String requestMethod, String data) {
        this.requestMethod = requestMethod;
        this.data = data;
        this.contentType = "application/json";
    }
    
    public void setJSONData(String requestMethod, Map<String, String> data) {
        setJSONData(requestMethod, new JSONObject(data).toJSONString());
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
     * Set the listener for this request. Should probably not be set when this
     * request is supposed to be performed by QueuedApi, since it will overwrite
     * it (set to Entry instead).
     * 
     * @param origin 
     */
    public void setResultListener(RequestResultListener origin) {
        this.listener = origin;
    }

    @Override
    public void run() {
        if (requestMethod.equals("PATCH")) {
            apache();
        }
        else {
            regular();
        }
    }
    
    private void apache() {
        if (listener == null) {
            return;
        }
        String responseText = null;
        int responseCode = -1;
        int ratelimitRemaining = -1;
        String responseEncoding = null;
        String requestError = null;
        String errorText = null;
        
        LOGGER.info(String.format("%s*: %s%s",
                requestMethod,
                url,
                data != null ? " ("+data+")" : ""));
        
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT))
                .setResponseTimeout(30, TimeUnit.SECONDS)
                .build();
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            ClassicHttpRequest request = new HttpUriRequestBase(requestMethod, new URI(url));
            request.addHeader("Client-ID", CLIENT_ID);
            if (token != null) {
                request.addHeader("Authorization", "Bearer "+token);
            }
            if (data != null) {
                StringEntity stringEntity;
                if (contentType.equals("application/json")) {
                    stringEntity = new StringEntity(data, ContentType.APPLICATION_JSON, "UTF-8", false);
                }
                else {
                    stringEntity = new StringEntity(data, CHARSET);
                }
                request.setEntity(stringEntity);
            }
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                responseCode = response.getCode();
                responseEncoding = getStringHeader(response.getFirstHeader("Content-Encoding"), null);
                ratelimitRemaining = getIntHeader(response.getFirstHeader("Ratelimit-Remaining"), -1);
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    responseText = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
                    EntityUtils.consume(responseEntity);
                    if (!String.valueOf(responseCode).startsWith("2")) {
                        errorText = responseText;
                        responseText = null;
                    }
                }
            }
        }
        catch (IOException | URISyntaxException | ParseException ex) {
            requestError = ex.toString();
        }
        
        //-----------------------
        // Debug output / Output
        //-----------------------
        LOGGER.info(String.format(Locale.ROOT, "GOT (%d/%d, %d%s): %s%s",
                responseCode,
                ratelimitRemaining,
                responseText != null ? responseText.length() : -1,
                responseEncoding != null ? ", " + responseEncoding : "",
                url,
                requestError != null ? " ["+requestError+"]" : ""));
        
        
        listener.requestResult(responseText, responseCode, errorText, ratelimitRemaining);
    }
    
    private static int getIntHeader(Header header, int defaultValue) {
        if (header != null) {
            try {
                return Integer.parseInt(header.getValue());
            }
            catch (NumberFormatException ex) {
                // Do nothing
            }
        }
        return defaultValue;
    }
    
    private static String getStringHeader(Header header, String defaultValue) {
        if (header != null) {
            return header.getValue();
        }
        return defaultValue;
    }
    
    private void regular() {
        if (listener == null) {
            return;
        }
        String responseText = null;
        String errorText = null;
        int responseCode = -1;
        int ratelimitRemaining = -1;
        String responseEncoding = null;
        String requestError = null;

        LOGGER.info(String.format("%s: %s%s",
                requestMethod,
                url,
                data != null ? " ("+data+")" : ""));
        
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
        
            //---------------------------
            // Request properties / Data
            //---------------------------
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestProperty("Client-ID", CLIENT_ID);
            if (token != null) {
                connection.setRequestProperty("Authorization", "Bearer "+token);
            }
            connection.setRequestMethod(requestMethod);

            if (data != null) {
                connection.setRequestProperty("Content-Type", contentType);
                connection.setDoOutput(true);
                try (OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream(), CHARSET)) {
                    out.write(data);
                }
            }

            //------------------
            // Response Headers
            //------------------
            responseEncoding = connection.getContentEncoding();
            ratelimitRemaining = connection.getHeaderFieldInt("Ratelimit-Remaining", -1);

            //--------------------
            // Read response text
            //--------------------
            InputStream input = checkGZIP(connection.getInputStream(), connection);
            responseText = readText(input);
        } catch (SocketTimeoutException ex) {
            requestError = ex.toString();
        } catch (IOException ex) {
            requestError = ex.toString();
        } finally {
            if (connection != null) {
                try {
                    InputStream errorInput = checkGZIP(connection.getErrorStream(), connection);
                    if (errorInput != null) {
                        errorText = readText(errorInput);
                    }
                    responseCode = connection.getResponseCode();
                } catch (IOException ex) {
                    // Do nothing, responseCode will simply be -1
                }
                connection.disconnect();
            }
        }
        
        //-----------------------
        // Debug output / Output
        //-----------------------
        LOGGER.info(String.format(Locale.ROOT, "GOT (%d/%d, %d%s): %s%s",
                responseCode,
                ratelimitRemaining,
                responseText != null ? responseText.length() : -1,
                responseEncoding != null ? ", " + responseEncoding : "",
                url,
                requestError != null ? " ["+requestError+"]" : ""));
        
        listener.requestResult(responseText, responseCode, errorText, ratelimitRemaining);
    }
    
    private static InputStream checkGZIP(InputStream input, HttpURLConnection connection) throws IOException {
        if (input != null && "gzip".equals(connection.getContentEncoding())) {
            return new GZIPInputStream(input);
        }
        return input;
    }
    
    private static String readText(InputStream input) throws IOException {
        StringBuilder response;
        try ( BufferedReader reader
                = new BufferedReader(new InputStreamReader(input, CHARSET))) {
            String line;
            response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Request other = (Request) obj;
        if (!Objects.equals(this.url, other.url)) {
            return false;
        }
        if (!Objects.equals(this.token, other.token)) {
            return false;
        }
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        if (!Objects.equals(this.requestMethod, other.requestMethod)) {
            return false;
        }
        if (!Objects.equals(this.contentType, other.contentType)) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.url);
        hash = 37 * hash + Objects.hashCode(this.token);
        hash = 37 * hash + Objects.hashCode(this.data);
        hash = 37 * hash + Objects.hashCode(this.requestMethod);
        hash = 37 * hash + Objects.hashCode(this.contentType);
        return hash;
    }
    
    @Override
    public String toString() {
        return url;
    }
    
}
