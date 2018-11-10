
package chatty.splash;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;
import java.awt.GraphicsEnvironment;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JWindow;

/**
 * Backup splashscreen, when the "native" one doesn't work (for example for the
 * javapackager version). This will be displayed a bit later, but still worth it
 * probably.
 * 
 * @author tduva
 */
public class SplashWindow extends JWindow {
    
    private static SplashWindow window = null;
    
    private SplashWindow(JComponent custom) {
        boolean translucency = isTranslucencySupported();
        
        // Rounded corners only worth with translucency
        ImageIcon image = new ImageIcon(getClass().getResource(
                translucency ? "splash.png" : "splash_rectangle.png"));
        setSize(image.getIconWidth(), image.getIconHeight());
        add(new JLabel(image), BorderLayout.CENTER);
        setGlassPane(custom);
        getGlassPane().setVisible(true);
        setLocationRelativeTo(null);
        
        if (translucency) {
            setBackground(new Color(0,0,0,0));
        }
    }
    
    /**
     * Create and show a splash window, with the given JComponent overlayed.
     * 
     * Should be run in the EDT.
     * 
     * @param custom Override paintComponent() for this component to draw on
     * the splash screen
     */
    public static void createSplashWindow(JComponent custom) {
        closeSplashWindow();
        window = new SplashWindow(custom);
        window.setVisible(true);
    }
    
    /**
     * Close the splash window (if present). Should be run in the EDT.
     */
    public static void closeSplashWindow() {
        if (window != null) {
            window.setVisible(false);
            window.dispose();
            window = null;
        }
    }
    
    private static boolean isTranslucencySupported() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        return gd.isWindowTranslucencySupported(PERPIXEL_TRANSLUCENT);
    }
    
    public static void main(String[] args) {
        try {
            createSplashWindow(new JComponent() {
                
            });
            Thread.sleep(3000);
            System.exit(0);
        } catch (InterruptedException ex) {
        }
    }
    
}
