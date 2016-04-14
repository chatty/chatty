
package chatty.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Timer;

/**
 * Counts ignored messges and outputs that number in regular intervals (if > 0).
 * Also provides some constants for ignored messges that don't really fit
 * anywhere else.
 * 
 * @author tduva
 */
public class IgnoredMessages {
    
    public static final int MODE_HIDE = 0;
    public static final int MODE_COUNT = 1;
    public static final int MODE_COMPACT = 2;
    public static final int MODE_SHOW = 3;
    
    /**
     * How often to output ignored message count (seconds).
     */
    private final static int DELAY = 30;
    
    private final MainGui g;
    private final Map<String, Integer> countPerChannel = new HashMap<>();
    
    public IgnoredMessages(MainGui g) {
        this.g = g;
        
        Timer timer = new Timer(DELAY*1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                output();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }
    
    /**
     * Count ignored message for the given channel.
     * 
     * @param channel 
     */
    public void ignoredMessage(String channel) {
        if (!countPerChannel.containsKey(channel)) {
            countPerChannel.put(channel, 1);
        } else {
            countPerChannel.put(channel, countPerChannel.get(channel)+1);
        }
    }
    
    /**
     * Output ignored messages for all channels that have ignored messages and
     * reset count.
     */
    private void output() {
        for (String channel : countPerChannel.keySet()) {
            int count = countPerChannel.get(channel);
            if (count > 0) {
                g.ignoredMessagesCount(channel, "Ignored "+count+" message"+(count == 1 ? "" : "s")
                        +" in the last "+DELAY+"s");
            }
        }
        countPerChannel.clear();
    }
    
}
