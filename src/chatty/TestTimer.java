
package chatty;

import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tduva
 */
public class TestTimer implements Runnable {

    private final int max;
    private final Consumer<Integer> action;
    private final int delay;
    
    public TestTimer(Consumer<Integer> action, int max, int delay) {
        this.max = max;
        this.action = action;
        this.delay = delay;
    }
    
    @Override
    public void run() {
        for (int i=0;i<max;i++) {
            action.accept(i);
            
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                Logger.getLogger(TestTimer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public static void testTimer(TwitchClient client, Room room, String command, int repeats, int delay) {
        CustomCommand cc = CustomCommand.parse(command);
        new Thread(new TestTimer(i -> {
            client.anonCustomCommand(room, cc, Parameters.create(String.valueOf(i)));
        }, repeats, delay)).start();
    }
    
}
