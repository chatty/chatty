
package chatty.gui.components.updating;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class FileDownloader implements Runnable {
    
    private static final Logger LOGGER = Logger.getLogger(FileDownloader.class.getName());
    
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 10000;
    
    private final URL from;
    private final Path to;
    private final FileDownloaderListener listener;
    
    private volatile boolean cancelled;
    
    public FileDownloader(URL from, Path to, FileDownloaderListener listener) {
        this.from = from;
        this.to = to;
        this.listener = listener;
    }
    
    public void startAsync() {
        Thread worker = new Thread(this);
        worker.start();
    }
    
    public void startAsyncDaemon() {
        Thread worker = new Thread(this);
        worker.setDaemon(true);
        worker.start();
    }
    
    public void cancel() {
        System.out.println("Cancel");
        this.cancelled = true;
    }
    
    @Override
    public void run() {
        URLConnection connection = null;
        try {
            connection = from.openConnection();
            //connection.setRequestProperty("User-Agent", VERSION);
            //connection.setRequestProperty("Accept-Encoding", "gzip");
            
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            int responseCode = -1;
            if (connection instanceof HttpURLConnection) {
                responseCode = ((HttpURLConnection)connection).getResponseCode();
            }
            long contentLength = connection.getContentLengthLong();
            
            LOGGER.info("Download started ("+responseCode+"): "+from+" -> "+to);
            
            long totalBytes = 0;
            try (BufferedInputStream reader = new BufferedInputStream(connection.getInputStream());
                    FileOutputStream output = new FileOutputStream(to.toFile())) {
                final byte[] buffer = new byte[512];
                int count;
                while ((count = reader.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                    totalBytes += count;
                    listener.progress(totalBytes, contentLength);
                    if (cancelled) {
                        listener.cancelled(totalBytes, contentLength);
                        LOGGER.info("Download cancelled");
                        return;
                    }
                }
            }
            LOGGER.info(String.format(Locale.ROOT, "Download completed (%d/%d)", totalBytes, contentLength));
            listener.completed(totalBytes, contentLength);
        } catch (IOException ex) {
            LOGGER.warning("Request Error [" + from + "] (" + ex + ")");
            listener.error(ex);
        }
    }
    
    public interface FileDownloaderListener {
        
        public void completed(long totalBytes, long contentLength);
        public void error(IOException ex);
        public void progress(long totalBytes, long contentLength);
        public void cancelled(long totalBytes, long contentLength);
        
    }
    
    public static void main(String[] args) throws IOException {
        URL from = new URL("https://github.com/chatty/chatty/releases/download/v0.9.1/Chatty_0.9.1.zip");
        Path to = Paths.get("G:\\testi.zip");
        FileDownloader downloader = new FileDownloader(from, to, new FileDownloaderListener() {

            @Override
            public void completed(long totalBytes, long contentLenght) {
                System.out.println("completed "+totalBytes+" "+contentLenght);
            }

            @Override
            public void error(IOException ex) {
                System.out.println("error "+ex);
            }

            @Override
            public void progress(long totalBytes, long contentLenght) {
                //System.out.println("progress "+totalBytes+" "+contentLenght+" "+(totalBytes/(double)contentLenght));
            }

            @Override
            public void cancelled(long totalBytes, long contentLength) {
                System.out.println("cancelled "+totalBytes+" "+contentLength);
            }
        });
        long start = System.currentTimeMillis();
        downloader.startAsync();
        try {
            System.out.println("abc");
            Thread.sleep(3000);
            downloader.cancel();
        } catch (InterruptedException ex) {
            Logger.getLogger(FileDownloader.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(System.currentTimeMillis() - start);
    }
    
}
