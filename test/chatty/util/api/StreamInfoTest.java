
package chatty.util.api;

import chatty.util.DateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class StreamInfoTest {

    private static final long MINUTE = 60*1000;
    
    @Test
    public void test() {
        TestHistory testHistory = new TestHistory(0);
        testHistory.addItems(10, 2);
        testHistory.setLive(true);
        testHistory.setStreamStart();
        testHistory.setPicnicStreamStart();
        testHistory.addItems(30, 2);
        testHistory.setLive(false);
        testHistory.addItems(2, 2);
        testHistory.setLive(true);
        testHistory.setStreamStart();
        testHistory.addItems(30, 2);
        
        StreamInfo info = new StreamInfo("test", null);
        info.setHistory(testHistory.history);
        
//        testHistory.output();
        
        assertEquals(-1, info.getHistoryStreamStart(1*MINUTE, false));
        assertEquals(-1, info.getHistoryStreamStart(1*MINUTE, true));
        assertEquals(-1, info.getHistoryStreamStart(4*MINUTE, false));
        assertEquals(-1, info.getHistoryStreamStart(22*MINUTE-1, false));
        assertEquals(1200000, info.getHistoryStreamStart(22*MINUTE, false));
        assertEquals(1200000, info.getHistoryStreamStart(25*MINUTE, false));
        assertEquals(1200000, info.getHistoryStreamStart(81*MINUTE, false));
        assertEquals(1200000, info.getHistoryStreamStart(81*MINUTE, true));
        assertEquals(5040000, info.getHistoryStreamStart(120*MINUTE, false));
        assertEquals(1200000, info.getHistoryStreamStart(120*MINUTE, true));
        assertEquals(-1, info.getHistoryStreamStart(500*MINUTE, false));
        assertEquals(-1, info.getHistoryStreamStart(500*MINUTE, true));
    }
    
    private static class TestHistory {
        
        private long currentTime = 1;
        public LinkedHashMap<Long, StreamInfoHistoryItem> history = new LinkedHashMap<>();
        public long startTime;
        public long picnicStartTime;
        public boolean live;
        
        public TestHistory(long initialTime) {
            this.currentTime = initialTime;
        }
        
        public void setStreamStart() {
            this.startTime = currentTime;
        }
        
        public void setPicnicStreamStart() {
            this.picnicStartTime = currentTime;
        }
        
        public void setLive(boolean live) {
            this.live = live;
        }
        
        public void addTime(int minutes) {
            this.currentTime += minutes*60*1000;
        }
        
        public void addItems(int count, int minutes) {
            for (int i = 0; i < count; i++) {
                addTime(minutes);
                add();
            }
        }
        
        public void add() {
            if (live) {
                history.put(currentTime, new StreamInfoHistoryItem(currentTime, 0, "test", "game", StreamInfo.StreamType.LIVE, null, startTime, picnicStartTime));
            }
            else {
                history.put(currentTime, new StreamInfoHistoryItem(currentTime));
            }
        }
        
        public void output() {
            for (StreamInfoHistoryItem item : history.values()) {
                if (item.isOnline()) {
                    System.out.println(String.format("%s / %s / %s (%s / %s)",
                            DateTime.duration(item.getTime()),
                            DateTime.duration(item.getTime() - item.getStreamStartTime()),
                            DateTime.duration(item.getTime() - item.getStreamStartTimeWithPicnic()),
                            item.getStreamStartTime(),
                            item.getStreamStartTimeWithPicnic()
                    ));
                }
                else {
                    System.out.println(DateTime.duration(item.getTime()));
                }
            }
        }
        
    }
    
}
