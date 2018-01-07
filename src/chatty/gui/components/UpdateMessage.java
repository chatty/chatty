
package chatty.gui.components;

import chatty.Chatty;
import chatty.gui.UrlOpener;
import chatty.util.UrlRequest;
import chatty.util.MiscUtil;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * Can be shown to give more information on an available update. Loads the
 * current changelog from the server.
 * 
 * @author tduva
 */
public class UpdateMessage extends JDialog {
    
    private static final String CHANGELOG_URL = "http://chatty.github.io/changes.txt";
    //private static final String CHANGELOG_URL = "http://127.0.0.1/twitch/changes.txt";

    private static final String RELEASES_URL = "https://github.com/chatty/chatty/releases/download/v";
    
    private final JLabel version;
    private final JTextArea changelog;
    private boolean changelogLoaded;
    private String newVersion;

    
    public UpdateMessage(Window owner) {
        super(owner);
        
        changelog = new JTextArea();
        changelog.setFont(Font.decode(Font.MONOSPACED));
        changelog.setEditable(false);
        changelog.setColumns(87);
        changelog.setRows(30);
        changelog.setMargin(new Insets(5, 5, 5, 5));
        
        version = new JLabel();
        
        add(version, BorderLayout.NORTH);
        add(new JScrollPane(changelog), BorderLayout.CENTER);
        
        JPanel buttons = new JPanel();
        add(buttons, BorderLayout.SOUTH);
        
        final JButton openWebsite = new JButton("Open website");
        final JButton close = new JButton("Close");
        
        buttons.add(openWebsite);
        buttons.add(close);

        ActionListener buttonAction = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == openWebsite) {
                    UrlOpener.openUrlPrompt(UpdateMessage.this, Chatty.WEBSITE, true);
                } else if (e.getSource() == close) {
                    setVisible(false);
                } else {
                    //https://github.com/chatty/chatty/releases/download/v0.9/Chatty_0.9_hotkey_32bit.zip
                    String url = String.format(RELEASES_URL + "%1$s/Chatty_%1$s%2$s.zip", newVersion, ((JComponent) e.getSource()).getName());
                    UrlOpener.openUrlPrompt(UpdateMessage.this, url, true);
                }

            }
        };
        openWebsite.addActionListener(buttonAction);
        close.addActionListener(buttonAction);

        final JButton latest = new JButton("Download latest");
        latest.setName("");
        latest.addActionListener(buttonAction);

        if (MiscUtil.OS_WINDOWS) {
            final JLabel downloads = new JLabel("Downloads: ");
            latest.setText("Standard");
            final JButton hotkey32 = new JButton("Global hotkey (32-bit)");
            final JButton hotkey64 = new JButton("Global hotkey (64-bit)");
            final JButton standalone = new JButton("Standalone");

            hotkey32.setName("_hotkey_32bit");
            hotkey64.setName("_hotkey_64bit");
            standalone.setName("_windows_standalone");

            buttons.add(downloads);
            buttons.add(latest);
            buttons.add(hotkey32);
            buttons.add(hotkey64);
            buttons.add(standalone);

            hotkey32.addActionListener(buttonAction);
            hotkey64.addActionListener(buttonAction);
            standalone.addActionListener(buttonAction);
        } else {
            buttons.add(latest);
        }
        
        pack();
    }
    
    public void showDialog() {
        setTitle("Update available!");
        setVisible(true);
        loadChangelog();
    }
    
    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
        version.setText("<html><body style='padding: 6px;'>Your version: "+Chatty.VERSION+" | Latest: "+newVersion);
    }
    
    /**
     * Loads the changelog if not already successfully loaded.
     */
    private void loadChangelog() {
        if (changelogLoaded) {
            return;
        }
        changelog.setText("Loading..");
        UrlRequest request = new UrlRequest(CHANGELOG_URL);
        request.async((result, responseCode) -> {
            if (responseCode == 200) {
                changelog.setText(result);
                changelogLoaded = true;
            } else {
                changelog.setText("Error loading changelog.");
            }
        });
    }
    
}
