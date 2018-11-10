
package chatty.splash;

import static chatty.Chatty.VERSION;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.SplashScreen;
import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 *
 * @author tduva
 */
public class Splash {
    
    private static String[] getThings() {
        return new String[]{
            "Happy Birthday, Broughy!",
            "When's True 100%+?",
            "Jaffa Kree!",
            "Java is an island as well!",
            "And we're walking..",
            "Let's make life take the lemons back!",
            "CatBag",
            "Never eat the antennae.",
            "80 Million Kilograms is a lot of Kilograms",
            "Color or Colour?",
            "The hypnotic tick-tack of stilettos on virtual cobble",
            "Do you get to the cloud district often?",
            "Getting into the swing of things..",
            "I've heard if both ways",
            "🐣",
            "Igniting the midnight petroleum!",
            "70k lines of code!",
            "Free and open-source!",
            "Batteries not included",
            "Made with your support!",
            "Guten Morgen!",
            "Wind's howling.",
            "Rac + <Shift-Tab>",
            "Alternating backgrounds!",
            "Right-click on all the things!",
            "Splash screen!",
            "Pop goes the weasel!",
            ":)",
            "Donaudampfschiffahrtsgesellschaftskapitän",
            "Schleyfsteyn!",
            "Never dig below your feet!"
        };
    }
    
    private static String getThing() {
        if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) == 1) {
            return "Happy new year!";
        }
        String[] things = getThings();
        return things[ThreadLocalRandom.current().nextInt(things.length)];
    }

    public static void drawOnSplashscreen() {
        final SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            Graphics2D g = splash.createGraphics();
            if (g != null) {
                Rectangle r = splash.getBounds();
                
                g.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Font boldFont = new Font(Font.DIALOG, Font.BOLD, 12);
                Font regularFont = new Font(Font.DIALOG, Font.PLAIN, 12);
                
                // Extra text
                g.setColor(Color.BLACK);
                g.setFont(boldFont);
                String text = getThing();
                int width = g.getFontMetrics().stringWidth(text);
                g.drawString(text, r.width / 2 - width / 2, 160);
                
                // Version String
                g.setColor(Color.DARK_GRAY);
                g.setFont(regularFont);
                g.drawString(VERSION, 10, 16);
                
                splash.update();
            }
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        SwingUtilities.invokeLater(() -> {
            drawOnSplashscreen();
        });
        Thread.sleep(2000);
    }
    
}
