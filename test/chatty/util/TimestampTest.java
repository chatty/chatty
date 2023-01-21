
package chatty.util;

import chatty.util.api.StreamInfo;
import chatty.util.api.StreamInfoHistoryItem;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author tduva
 */
public class TimestampTest {

    private static final int MINUTE = 60*1000;
    private static final int HOUR = 60*60*1000;
    
    @Test
    public void test() {
        // Using system timezone, for timestamp output without given timezone
        ZonedDateTime datetimeDefault = ZonedDateTime.of(2022, 3, 2, 18, 3, 20, 0, ZoneId.systemDefault());
        long time = datetimeDefault.toEpochSecond() * 1000;
        
        // Using UTC, for timestamp output with specific timezone
        LocalDateTime utcDatetime = LocalDateTime.of(2022, 3, 2, 18, 3, 20);
        long utcTime = utcDatetime.toEpochSecond(ZoneOffset.UTC) * 1000;
        
        t("", "", 0, "");
        t("off", "", 0, null);
        t("yyyy-MM-dd HH:mm:ss", "", time, "2022-03-02 18:03:20");
        t("yyyy-MM-dd HH:mm:ss", "local", time, "2022-03-02 18:03:20");
        
        t("yyyy-MM-dd HH:mm:ss", "UTC", utcTime, "2022-03-02 18:03:20");
        t("yyyy-MM-dd HH:mm:ss", "GMT+01:00", utcTime, "2022-03-02 19:03:20");
        t("yyyy-MM-dd HH:mm:ss", "GMT+02:00", utcTime, "2022-03-02 20:03:20");
        t("yyyy-MM-dd HH:mm:ss", "GMT-01:00", utcTime, "2022-03-02 17:03:20");
        t("yyyy-MM-dd HH:mm:ss", "GMT+01:30", utcTime, "2022-03-02 19:33:20");
        
        t("yyyy-MM-dd hh:mm:ss a'a:AM/PM'", "", time, "2022-03-02 06:03:20 PM");
        t("yyyy-MM-dd hh:mm:ss a'a:AM/afternoon'", "", time, "2022-03-02 06:03:20 afternoon");
        
        t("yyyy-MM-dd HH:mm:ss'{uptime}'", "", time, "2022-03-02 18:03:20");
        t("'{uptime}'yyyy-MM-dd HH:mm:ss", "", time, "2022-03-02 18:03:20");
        t("yyyy-MM-dd HH:mm:ss'{/,uptime}'", "", time, "2022-03-02 18:03:20");
        t("yyyy-MM-dd HH:mm:ss'{/,uptime}'", "", time, "2022-03-02 18:03:20");
        t("yyyy-MM-dd HH:mm:ss'{/,uptime}'", "", time, "2022-03-02 18:03:20/30m",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime}'yyyy-MM-dd HH:mm:ss", "", time, "30m2022-03-02 18:03:20",
                createStreamInfo(time, 30*MINUTE));
        t("yyyy-MM-dd'{uptime}'HH:mm:ss", "", time, "2022-03-0230m18:03:20",
                createStreamInfo(time, 30*MINUTE));
        t("yyyy-MM-dd' {uptime} 'HH:mm:ss", "", time, "2022-03-02 30m 18:03:20",
                createStreamInfo(time, 30*MINUTE));
        t("yyyy-MM-dd' {uptime} 'HH:mm:ss", "", time, "2022-03-02  18:03:20");
        t("yyyy-MM-dd HH:mm:ss'{/,uptime,/}'", "", time, "2022-03-02 18:03:20");
        t("yyyy-MM-dd HH:mm:ss'{/,uptime,/}'", "", time, "2022-03-02 18:03:20/30m/",
                createStreamInfo(time, 30*MINUTE));
        t("yyyy-MM-dd HH:mm:ss'{ - ,uptime}'", "", time, "2022-03-02 18:03:20 - 30m",
                createStreamInfo(time, 30*MINUTE));
        t("yyyy-MM-dd HH:mm:ss'{ - ,uptime, -}'", "", time, "2022-03-02 18:03:20 - 30m -",
                createStreamInfo(time, 30*MINUTE));
        t("yyyy-MM-dd HH:mm:ss'{ - ,uptime, -}'", "", time, "2022-03-02 18:03:20");
        
        t("'{uptime}'", "", time, "30m",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime}{uptime}'", "", time, "30m30m",
                createStreamInfo(time, 30*MINUTE));
        
        t("'{uptime}{uptime:p}'", "", time, "30m30m",
                createStreamInfo(time, 30*MINUTE, 30*MINUTE));
        t("'{uptime}{uptime:P}'", "", time, "30m",
                createStreamInfo(time, 30*MINUTE, 30*MINUTE));
        t("'{uptime}{uptime:p}'", "", time, "30m40m",
                createStreamInfo(time, 30*MINUTE, 40*MINUTE));
        t("'{uptime}{uptime:P}'", "", time, "30m40m",
                createStreamInfo(time, 30*MINUTE, 40*MINUTE));
        
        t("'{uptime}/'", "", time, "30m/",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime:c}'", "", time, "0:30",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime:c}'", "", time, "0:03",
                createStreamInfo(time, 3*MINUTE));
        t("'{uptime:c0}'", "", time, "0:03",
                createStreamInfo(time, 3*MINUTE));
        t("'{uptime:c00}'", "", time, "00:03",
                createStreamInfo(time, 3*MINUTE));
        t("'{uptime:t}'", "", time, "1h1m",
                createStreamInfo(time, 61*MINUTE));
        t("'{uptime:tH}'", "", time, "0h30m",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime:H}'", "", time, "0h 30m",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime:s}'", "", time, "30m 0s",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime:s0}'", "", time, "30m 00s",
                createStreamInfo(time, 30*MINUTE));
        t("'{uptime}'", "", time, "30h 0m",
                createStreamInfo(time, 30*HOUR));
        t("'{uptime}'", "", time, "30h 10m",
                createStreamInfo(time, 30*HOUR+10*MINUTE));
        t("'{uptime:c}'", "", time, "30:10",
                createStreamInfo(time, 30*HOUR+10*MINUTE));
    }
    
    private static void t(String input, String timezone, long time, String expected) {
        t(input, timezone, time, expected, null);
    }
    
    private static void t(String input, String timezone, long time, String expected, StreamInfo streamInfo) {
        Timestamp timestamp = new Timestamp(input, timezone);
        assertEquals(expected, timestamp.make2(time, streamInfo));
    }
    
    private static StreamInfo createStreamInfo(long time, long duration) {
        return createStreamInfo(time, duration, duration);
    }
    
    private static StreamInfo createStreamInfo(long time, long duration, long picnicDuration) {
        StreamInfo info = new StreamInfo("test", null);
        long testTime = time - 1;
        long startTime = testTime - duration;
        long startTimePicnic = testTime - picnicDuration;
        StreamInfoHistoryItem item = new StreamInfoHistoryItem(testTime, 0, null, null, StreamInfo.StreamType.LIVE, startTime, startTimePicnic);
        LinkedHashMap<Long, StreamInfoHistoryItem> history = new LinkedHashMap<>();
        history.put(testTime, item);
        info.setHistory(history);
        return info;
    }
    
}
