
package chatty;

import chatty.gui.MainGui;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A Timer that runs throughout the execution of the program, mainly to update
 * the title and such, which however in turn also gets the StreamInfo.
 * 
 * @author tduva
 */
public class UpdateTimer extends Timer {

    private final TimerTask task;
    /**
     * Delay between executions in seconds
     */
    private static final int DELAY = 5;

    public UpdateTimer(final MainGui g) {
        task = new TimerTask() {

            @Override
            public void run() {
                g.updateState();
            }
        };
        schedule(task, DELAY*1000, DELAY*1000); 
   }
    
    
}
