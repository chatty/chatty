
package chatty;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class TestTimer implements Runnable {

    TwitchClient client;
    int max;
    public TestTimer(TwitchClient client, int max) {
        this.client = client;
        this.max = max;
    }
    
    @Override
    public void run() {

        SecureRandom random = new SecureRandom();
        long start = System.currentTimeMillis();
        User user = new User("tduvatest", Room.createRegular(""));
        for (int i=0;i<max;i++) {
            
//            user.setUsercolorManager(client.usercolorManager);
            String[] lines = new String[]{":)", "Kappa", "Longer line and whatnot",
                "Longer line with emotes FrankerZ Kappa :) 😉 😊 😋 😎 😍",
                "SSSsss",
                ":D",
                "lol :D",
                "well..",
                "Kappa FrankerZ abc mah a b c d ef gh ij klm nop qrstu vw"};
            
            String line = lines[random.nextInt(lines.length)];
            client.g.printMessage(user, line, false, null, 0);
            //client.userJoined("#test","user"+ new BigInteger(20,random).toString());
//            for (int y=0;y<10;y++) {
//                client.g.printMessage("test"+y, user, line, false);
//            }
            //client.g.printDebug(line);
            //client.api.requestFollowers("whatever");
//            try {
//                Thread.sleep(10);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(TestTimer.class.getName()).log(Level.SEVERE, null, ex);
//            }
        }
        System.out.println("Total: "+((System.currentTimeMillis() - start)));
        
    }
    
}
