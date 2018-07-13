
package chatty.gui.components.updating;

import chatty.lang.Language;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class FileDownloaderDialog extends JDialog {
    
    private final JProgressBar progressBar;
    private final JButton cancelButton;
    private final JLabel error;
    
    private final FileDownloader downloader;
    private boolean complete;
    
    public static boolean downloadFile(Window owner, URL from, Path to, String title) {
        FileDownloaderDialog d = new FileDownloaderDialog(owner, from, to, title);
        return d.showDialog();
    }
    
    public FileDownloaderDialog(Window owner, URL from, Path to, String title) {
        super(owner);
        
        setTitle(title);
        setModal(true);
        
        addWindowListener(new WindowAdapter() {
            
            @Override
            public void windowClosed(WindowEvent e) {
                downloader.cancel();
            }
            
        });
        
        downloader = new FileDownloader(from, to, new FileDownloader.FileDownloaderListener() {

            @Override
            public void completed(long totalBytes, long contentLength) {
                SwingUtilities.invokeLater(() -> {
                    complete = true;
                    setVisible(false);
                });
            }

            @Override
            public void error(IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    if (ex instanceof FileNotFoundException) {
                        setError("Not found (" + ex.getLocalizedMessage() + ")");
                    } else {
                        setError(ex.toString());
                    }
                });
            }

            @Override
            public void progress(long totalBytes, long contentLength) {
                SwingUtilities.invokeLater(() -> {
                    if (contentLength == -1) {
                        progressBar.setIndeterminate(true);
                    } else {
                        int percentage = (int) ((totalBytes / (double) contentLength) * 100);
                        progressBar.setValue(percentage);
                        progressBar.setString(String.format("%.2f MB / %.2f MB",
                                (totalBytes / 1024.0 / 1024),
                                (contentLength / 1024.0 / 1024)));
                    }
                });
            }

            @Override
            public void cancelled(long totalBytes, long contentLength) {
                SwingUtilities.invokeLater(() -> {
                    setVisible(false);
                });
            }
        });
        
        error = new JLabel();
        add(error, BorderLayout.NORTH);
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        add(progressBar, BorderLayout.CENTER);
        
        cancelButton = new JButton(Language.getString("dialog.button.cancel"));
        add(cancelButton, BorderLayout.SOUTH);
        
        cancelButton.addActionListener(e -> {
            downloader.cancel();
            setVisible(false);
        });
        
        setMinimumSize(new Dimension(250, 10));
        
        pack();
    }
    
    public boolean showDialog() {
        downloader.startAsyncDaemon();
        //setError("FileNotFoundException: https://github.com/chatty/chatty/releases/download/v0.9.1/Chatty_0.9.1.zip");
        setLocationRelativeTo(getOwner());
        setVisible(true);
        return complete;
    }
    
    private void setError(String message) {
        error.setText("<html><body style='padding:5px;width:250px;'>Download failed: "+message);
        pack();
        setLocationRelativeTo(getOwner());
    }
    
    public static void main(String[] args) throws MalformedURLException {
        System.out.println(downloadFile(null,
                new URL("https://github.com/chatty/chatty/releases/download/v0.9.1/Chatty_0.9.1.zip"),
                Paths.get("G:\\test2.zip"), "Downloading update"));
        System.exit(0);
    }
    
}
