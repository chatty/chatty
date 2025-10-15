
package chatty.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.swing.Timer;

/**
 * Save replies for a while for easier lookup for display and finding thread
 * ids.
 * 
 * @author tduva
 */
public class ReplyManager {
    
    private static final long DELETE_TIME = TimeUnit.HOURS.toMillis(12);
//    private static final long DELETE_TIME = TimeUnit.MINUTES.toMillis(2);
    
    private static final Map<String, List<Reply>> data = new HashMap<>();
    private static final Map<String, Long> lastAdded = new HashMap<>();
    
    static {
        Timer timer = new Timer((int)DELETE_TIME, e -> removeOld());
        timer.setRepeats(true);
        timer.start();
    }

    public synchronized static void addReply(String parentMsgId, String threadId, String msgId, String userMsg, String parentUserMsg) {
        if (threadId == null) {
            // If message that is replied to was already received as a reply recently, this should yield the thread id
            threadId = getThreadParentMsgId(parentMsgId);
        }
        if (threadId == null) {
            threadId = parentMsgId;
        }
        if (!data.containsKey(threadId)) {
            data.put(threadId, new ArrayList<>());
            if (parentUserMsg != null) {
                data.get(threadId).add(new Reply(parentMsgId, parentUserMsg));
            }
        }
        data.get(threadId).add(new Reply(msgId, userMsg));
        lastAdded.put(threadId, System.currentTimeMillis());
    }
    
    public synchronized static String getThreadParentMsgId(String msgId) {
        if (msgId == null) {
            return null;
        }
        for (Map.Entry<String, List<Reply>> entry : data.entrySet()) {
            for (Reply reply : entry.getValue()) {
                if (msgId.equals(reply.msgId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    public synchronized static List<Reply> getReplies(String threadId) {
        List<Reply> list = data.get(threadId);
        if (list == null) {
            threadId = getThreadParentMsgId(threadId);
            if (threadId != null) {
                list = data.get(threadId);
            }
        }
        if (list == null) {
            return null;
        }
        return new ArrayList<>(list);
    }
    
    public synchronized static String getFirstUserMsg(String parentMsgId) {
        List<Reply> list = data.get(parentMsgId);
        if (list != null && !list.isEmpty()) {
            return list.get(0).userMsg;
        }
        return null;
    }
    
    private synchronized static void removeOld() {
        Iterator<Map.Entry<String, Long>> it = lastAdded.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue() > DELETE_TIME) {
                it.remove();
                data.remove(entry.getKey());
            }
        }
    }
    
    public static class Reply {
        
        public final String msgId;
        public final String userMsg;
        
        private Reply(String msgId, String userMsg) {
            this.msgId = msgId;
            this.userMsg = userMsg;
        }
        
    }
    
}
