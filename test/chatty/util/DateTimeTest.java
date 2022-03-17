
package chatty.util;

import static chatty.util.DateTime.D;
import static chatty.util.DateTime.DAY;
import chatty.util.DateTime.Formatting;
import static chatty.util.DateTime.H;
import static chatty.util.DateTime.HOUR;
import static chatty.util.DateTime.M;
import static chatty.util.DateTime.MINUTE;
import static chatty.util.DateTime.N;
import static chatty.util.DateTime.S;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tduva
 */
public class DateTimeTest {

    /**
     * 
     */
    @Test
    public void testDuration() {
        assertEquals(DateTime.duration(0, 0, 0), "0s");
        assertEquals(DateTime.duration(1000*MINUTE, 0, 0), "1m 0s");
        assertEquals(DateTime.duration(1000*61, 0, 0), "1m 1s");
        assertEquals(DateTime.duration(1000*MINUTE, 1, 0), "1m");
        assertEquals(DateTime.duration(1000*MINUTE, 0, 1), "1m");
        assertEquals(DateTime.duration((long)1000*60*300+10000, 6, 0), "5h 0m 10s");
        assertEquals(DateTime.duration(1000*MINUTE*30, 0, 1), "30m");
        assertEquals(DateTime.duration(1000*(HOUR*25+300), 0, 0), "1d 1h 5m 0s");
        assertEquals(DateTime.duration(1000*(HOUR*25+300), 2, 0), "1d 1h");
        assertEquals(DateTime.duration(1000*(HOUR*25+300), 0, 1), "1d 1h 5m");
        assertEquals(DateTime.duration(1000*(HOUR*25+300), 3, 0), "1d 1h 5m");
        assertEquals(DateTime.duration(1000*(HOUR*1+300), 3, 0), "1h 5m 0s");
        assertEquals(DateTime.duration(0, 3, 2, 1), "0m");
        assertEquals(DateTime.duration(0, 4, 0, M), "0h");
        assertEquals(DateTime.duration(0, M, 0, M), "0m");
        assertEquals(DateTime.duration(1000*MINUTE, M, 0, M), "1m");
        assertEquals(DateTime.duration(1000*DAY, M, 0, M), "1440m");
        assertEquals(DateTime.duration(1000*DAY, M, 0, S), "1440m");
        assertEquals(DateTime.duration(1000*(DAY+1), M, 0, S), "1440m");
        assertEquals(DateTime.duration(1000*(DAY+1), M, 0, 0), "1440m 1s");
        assertEquals(DateTime.duration(1000*(DAY+1), S, 0, 0), (DAY+1)+"s");
        assertEquals(DateTime.duration(1000*(DAY+1), S, 0, D), (DAY+1)+"s");
        assertEquals(DateTime.duration(1000*(DAY+1), H, 1, S), "24h");
        assertEquals(DateTime.duration(1000*(DAY+1), H, 0, S), "24h 0m");
        assertEquals(DateTime.duration(1000*(DAY+MINUTE), H, 0, S), "24h 1m");
        assertEquals(DateTime.duration(1000*(2*DAY), H, 0, S), "48h 0m");
        assertEquals(DateTime.duration(1000*10, H, 0, S), "0m");
        assertEquals(DateTime.duration(1000*10, H, 0, N), "10s");
        assertEquals(DateTime.duration(0, 0, 0, 0, 0), "");
        assertEquals(DateTime.duration(0, 0, 0, 0, 0, Formatting.LEADING_ZERO_VALUES), "0y 0d 0h 0m 0s");
        assertEquals(DateTime.duration(0, 0, 0, S, 1), "0m");
        assertEquals(DateTime.duration(0, S, 0, S, 1), "0s");
        assertEquals(DateTime.duration(0, H, 0, N, 3), "0h 0m 0s");
        assertEquals(DateTime.duration(1000*MINUTE, N, 0, N, 0), "1m 0s");
        assertEquals(DateTime.duration(1000*MINUTE, N, 0, N, 0, Formatting.NO_ZERO_VALUES), "1m");
        assertEquals(DateTime.duration((long)123456789*1000, 1, 0, Formatting.VERBOSE), "3 years");
//        for (long i=0;i<(1000*DAY*2);i += 33) {
//            assertEquals(DateTime.duration(i, H, 3, 0, LD_ZERO), DateTime.duration3(i/1000, true));
//        }
        for (int i=-5;i<10;i++) {
            for (int y=-5;y<10;y++) {
                for (int k=-5;k<10;k++) {
                    for (int j=-5;j<10;j++) {
                        DateTime.duration(0, i, y, k, j);
                        DateTime.duration(1000*DAY, i, y, k, j);
                        for (int t=-5;t<1000*(DAY+10);t+=1234567) {
                            DateTime.duration(t, i, y, k, j);
                        }
                    }
                }
            }
        }
        
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0), "1h 2m 10s");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.DOUBLE_DIGITS), "01h 02m 10s");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.DOUBLE_DIGITS_EXCEPT_FIRST), "1h 02m 10s");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.CLOCK_STYLE), "1:2:10");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.CLOCK_STYLE, Formatting.DOUBLE_DIGITS_EXCEPT_FIRST), "1:02:10");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.CLOCK_STYLE, Formatting.DOUBLE_DIGITS), "01:02:10");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.NO_SPACES), "1h2m10s");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, 0, 0, Formatting.DOUBLE_DIGITS, Formatting.LAST_ONE_EXACT, Formatting.DOUBLE_DIGITS_EXCEPT_FIRST), "1h 02m 10.0s");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, S, 0, Formatting.DOUBLE_DIGITS, Formatting.LAST_ONE_EXACT, Formatting.DOUBLE_DIGITS_EXCEPT_FIRST), "1h 02.1m");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, S, 0, Formatting.LAST_ONE_EXACT, Formatting.DOUBLE_DIGITS_EXCEPT_FIRST), "1h 02.1m");
        assertEquals(DateTime.duration(d(0, 1, 2, 10), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.1m"); // 10 / 60 = 0.16
        assertEquals(DateTime.duration(d(0, 1, 2, 11), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.1m"); // 11 / 60 = 0.18
        assertEquals(DateTime.duration(d(0, 1, 2, 12), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.2m"); // 12 / 60 = 0.2
        assertEquals(DateTime.duration(d(0, 1, 2, 15), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.2m"); // 15 / 60 = 0.25
        assertEquals(DateTime.duration(d(0, 1, 2, 30), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.5m"); // 30 / 60 = 0.5
        assertEquals(DateTime.duration(d(0, 1, 2, 33), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.5m"); // 33 / 60 = 0.55
        assertEquals(DateTime.duration(d(0, 1, 2, 35), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.5m"); // 35 / 60 = 0.58
        assertEquals(DateTime.duration(d(0, 1, 2, 36), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.6m"); // 36 / 60 = 0.6
        assertEquals(DateTime.duration(d(0, 1, 2, 59), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 2.9m"); // 59 / 60 = 0.98
        
        assertEquals(DateTime.duration(d(0, 1, 9, 59), 0, 0, S, 0, Formatting.LAST_ONE_EXACT), "1h 9.9m");
        assertEquals(DateTime.duration(d(0, 1, 9, 59), 0, 0, S, 0, Formatting.LAST_ONE_EXACT, Formatting.DOUBLE_DIGITS), "01h 09.9m");
    }
    
    private static final long d(int days, int hours, int minutes, int seconds) {
        return (days*DAY + hours*HOUR + minutes*MINUTE + seconds) * 1000;
    }
    
    @Test
    public void testParseDatetime() {
        assertEquals(DateTime.parseDatetime("2015-05-15T22:16:57+02:00"), 1431721017000L);
        assertEquals(DateTime.parseDatetime("2015-05-15T17:27:16Z"), 1431710836000L);
        assertEquals(DateTime.parseDatetime("2014-01-10T17:44:50.027732Z"), 1389375890027L);
    }
    
}
