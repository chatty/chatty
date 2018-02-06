
package chatty.gui.components;

import chatty.Chatty;
import chatty.gui.UrlOpener;
import chatty.util.UrlRequest;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    
    private final JLabel version;
    private final JTextArea changelog;
    private boolean changelogLoaded;
    
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
                }
            }
        };
        openWebsite.addActionListener(buttonAction);
        close.addActionListener(buttonAction);
        
        pack();
    }
    
    public void showDialog() {
        setTitle("Update available!");
        setVisible(true);
        loadChangelog();
    }
    
    public void setNewVersion(String newVersion) {
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
