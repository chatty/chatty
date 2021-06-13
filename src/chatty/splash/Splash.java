
package chatty.splash;

import chatty.Chatty;
import static chatty.Chatty.VERSION;
import chatty.Helper;
import chatty.Helper.IntegerPair;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class Splash {
    
    private static final int SPLASH_WIDTH = 300;
    private static final int SPLASH_HEIGHT = 176;
    
    private static String[] getThings() {
        return new String[]{
            "Happy Birthday, Broughy!",
            "Top 5 is always possible!",
            "When's True 100%+?",
            "Oh hey, thanks!",
            "Carl Carl, dude dude, man!",
            "Old Reece still got it cracking.",
            "Jaffa Kree!",
            "What fate Omoroca?",
            "Java is an island as well!",
            "And we're walking..",
            "Let's make life take the lemons back!",
            "CatBag",
            "Never eat the antennae.",
            "80 Million Kilograms is a lot of Kilograms",
            "Color or Colour?",
            "The hypnotic tick-tack of stilettos on virtual cobble",
            "Do you get to the cloud district often?",
            "Getting into the Swing of things..",
            "I've heard it both ways",
            "🐣",
            "Igniting the midnight petroleum!",
            "Tea, Earl Grey, Hot",
            "There's coffee in that nebula!",
            "70k lines of code!",
            "Free and open-source!",
            "Batteries not included",
            "Made with your support!",
            "Guten Morgen!",
            "Have a nice day!",
            "Wind's howling.",
            "Rac + <Shift-Tab>",
            "Alternating backgrounds!",
            "Right-click on all the things!",
            "Click on username to moderate",
            "'View - Channel Admin' to set your stream title",
            "Splash screen!",
            "Pop goes the weasel!",
            ":)",
            "Donaudampfschiffahrtsgesellschaftskapitän",
            "Schleyfsteyn!",
            "Never dig below your feet!",
            "Kohle, Kohle, Kohle 🎵",
            "Remember the Cant!",
            "Global Hotkeys!"
        };
    }
    
    private final static String thing = getThing();
    
    private static String getThing() {
        if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == 1) {
            return "Happy new year!";
        }
        String[] things = getThings();
        return things[ThreadLocalRandom.current().nextInt(things.length)];
    }

    public static void initSplashScreen(final Point location) {
        if (SwingUtilities.isEventDispatchThread()) {
            drawOnSplashscreen(location);
        } else {
            SwingUtilities.invokeLater(() -> {
                drawOnSplashscreen(location);
            });
        }
    }
    
    public static void closeSplashScreen() {
        if (SwingUtilities.isEventDispatchThread()) {
            SplashWindow.closeSplashWindow();
        } else {
            SwingUtilities.invokeLater(() -> {
                SplashWindow.closeSplashWindow();
            });
        }
    }
    
    private static void drawOnSplashscreen(final Point location) {
        System.out.println(Chatty.uptimeMillis());
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            // Native
            Graphics2D g = splash.createGraphics();
            if (g != null) {
                Rectangle bounds = splash.getBounds();
                draw(g, bounds.width, bounds.height);
                splash.update();
            }
        } else {
            // Backup
            SplashWindow.createSplashWindow(new JComponent() {

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    draw((Graphics2D)g, getWidth(), getHeight());
                }
            
            }, location);
        }
    }

    private static void draw(Graphics2D g, int w, int h) {
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        Font boldFont = new Font(Font.DIALOG, Font.BOLD, 12);
        Font regularFont = new Font(Font.DIALOG, Font.PLAIN, 12);

        // Extra text
        g.setColor(Color.BLACK);
        g.setFont(boldFont);
        
        int width = g.getFontMetrics().stringWidth(thing);
        g.drawString(thing, w / 2 - width / 2, 160);

        // Version String
        g.setColor(Color.DARK_GRAY);
        g.setFont(regularFont);
        g.drawString("v"+VERSION, 10, 16);
    }
    
    public static Point getLocation(String setting) {
        if (setting == null) {
            return null;
        }
        String[] split = setting.split(";");
        if (split.length < 2) {
            return null;
        }
        IntegerPair coords = Helper.getNumbersFromString(split[0]);
        IntegerPair size = Helper.getNumbersFromString(split[1]);
        Rectangle r = new Rectangle(coords.a, coords.b, size.a, size.b);
        return new Point((int)r.getCenterX() - SPLASH_WIDTH/2, (int)r.getCenterY() - SPLASH_HEIGHT/2);
    }
    
    public static void main(String[] args) throws InterruptedException {
        initSplashScreen(null);
        Thread.sleep(10*1000);
        closeSplashScreen();
    }
    
}
