
package chatty.gui.defaults;

import chatty.lang.Language;
import chatty.util.IconManager;
import chatty.util.settings.Settings;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Allows the user to choose between some default settings.
 * 
 * @author tduva
 */
public class DefaultsDialog extends JFrame {

    public static void showAndWait(Settings settings) {
        try {
            CountDownLatch latch = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                DefaultsDialog dialog = new DefaultsDialog(settings, latch);
                dialog.setVisible(true);
            });
            latch.await();
        }
        catch (InterruptedException ex) {
            Logger.getLogger(DefaultsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private DefaultsDialog(Settings settings, CountDownLatch latch) {
        setTitle("Chatty - Initial Setup");
        setIconImages(IconManager.getMainIcons());
        
        DefaultsPanel panel = new DefaultsPanel(Language.getString("defaults.info"),
                new DefaultsPanel.DefaultsHelper() {
            @Override
            public void setString(String setting, String value) {
                settings.setString(setting, value);
            }

            @Override
            public void setLong(String setting, long value) {
                settings.setLong(setting, value);
            }

            @Override
            public void setBoolean(String setting, boolean value) {
                settings.setBoolean(setting, value);
            }

            @Override
            public String getStringDefault(String setting) {
                return settings.getStringDefault(setting);
            }

            @Override
            public boolean getBooleanDefault(String setting) {
                return settings.getBooleanDefault(setting);
            }

            @Override
            public boolean getEnabled(String option) {
                switch (option) {
                    case "notifications":
                        return true;
                    case "userlist":
                        return false;
                }
                return false;
            }

            @Override
            public void applied() {
                dispose();
            }
        });
        add(new JScrollPane(panel), BorderLayout.CENTER);
        
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosed(WindowEvent e) {
                latch.countDown();
            }

        });
        
        /**
         * With the JTextPane used as font preview it would add scrollbars when
         * not calling in invokeLater().
         */
        SwingUtilities.invokeLater(() -> {
            pack();
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        });
    }

    public static void main(String[] args) {
        Settings settings = new Settings("", null);
        showAndWait(settings);
        System.out.println("continue");
        System.exit(0);
    }
    
}
