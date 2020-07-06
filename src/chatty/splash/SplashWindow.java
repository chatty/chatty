
package chatty.splash;

import chatty.util.IconManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import static java.awt.GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JWindow;
import java.util.ArrayList;

/**
 * Backup splashscreen, when the "native" one doesn't work (for example for the
 * javapackager version). This will be displayed a bit later, but still worth it
 * probably.
 * 
 * @author tduva
 */
public class SplashWindow extends JFrame {
    
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
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Chatty starting..");
        setWindowIcons();
        
        if (translucency) {
            setBackground(new Color(0,0,0,0));
        }
    }

    /**
     * Sets different sizes of a splash window icon.
     */
    private void setWindowIcons() {
        setIconImages(IconManager.getMainIcons());
    }
    
    /**
     * Create and show a splash window, with the given JComponent overlayed.
     * 
     * Should be run in the EDT.
     * 
     * @param custom Override paintComponent() for this component to draw on
     * the splash screen
     * @param location
     */
    public static void createSplashWindow(JComponent custom, Point location) {
        closeSplashWindow();
        window = new SplashWindow(custom);
        if (location != null) {
            window.setLocation(location);
        }
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
                
            }, null);
            Thread.sleep(3000);
            System.exit(0);
        } catch (InterruptedException ex) {
        }
    }
    
}
