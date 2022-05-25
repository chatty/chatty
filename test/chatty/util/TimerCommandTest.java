
package chatty.util;

import chatty.Room;
import chatty.util.TimerCommand.TimerEntry;
import chatty.util.commands.Parameters;
import chatty.util.settings.Settings;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * These don't test all the features and date/time formats, but better than
 * nothing.
 *
 * @author tduva
 */
public class TimerCommandTest {

    private static final Parameters EMPTY = Parameters.create("");
    
    @Test
    public void testResult() {
        TimerCommand timerCommand = new TimerCommand(new Settings(null, null), new TimerCommand.TimerAction() {
            @Override
            public void performAction(String command, String channel, Parameters parameters, Set<TimerCommand.Option> options) {
                // Not needed
            }

            @Override
            public void log(String line) {
                // Not needed
            }
        });
        
        check(timerCommand, "1h10m abc", makeTargetTime(0, 1, 10, 0, 0), "1", "abc");
        check(timerCommand, "1d10m abc", makeTargetTime(1, 0, 10, 0, 0), "2", "abc");
        check(timerCommand, "5000ms1d10m abc", makeTargetTime(1, 0, 10, 0, 5000), "3", "abc");
        
        check(timerCommand, ":id 1h10m abc", makeTargetTime(0, 1, 10, 0, 0), "id", "abc");
        check(timerCommand, "1d10m abc", makeTargetTime(1, 0, 10, 0, 0), "4", "abc");
        check(timerCommand, "5000ms1d10m abc", makeTargetTime(1, 0, 10, 0, 5000), "5", "abc");
        check(timerCommand, "10", -1, null, null);
        check(timerCommand, "10m /echo abc", makeTargetTime(0, 0, 10, 0, 0), null, "/echo abc");
        check(timerCommand, "10m  ", makeTargetTime(0, 0, 10, 0, 0), null, " ");
        check(timerCommand, "10m5 c", makeTargetTime(0, 0, 10, 5, 0), null, null);
        check(timerCommand, "10m5s c", makeTargetTime(0, 0, 10, 5, 0), null, null);
        check(timerCommand, "5s10m c", makeTargetTime(0, 0, 10, 5, 0), null, null);
        
        check(timerCommand, ":a 50 c", makeTargetTime(0, 0, 0, 50, 0), null, null);
        check(timerCommand, ":a 50 c", -1, null, null);
        check(timerCommand, "-o :a 50 c", makeTargetTime(0, 0, 0, 50, 0), null, null);
        
        // Make sure the date is in the future
        int year = ZonedDateTime.now().getYear() + 1;
        checkDatetime(timerCommand, "["+year+"-01-02 2:50] /echo abc", year, 1, 2, 2, 50, 0, 0);
        checkDatetime(timerCommand, "["+year+"-01-02 02:50] /echo abc", year, 1, 2, 2, 50, 0, 0);
        checkDatetime(timerCommand, "["+year+"-01-03 2:50:30] /echo abc", year, 1, 3, 2, 50, 30, 0);
        checkDatetime(timerCommand, "["+year+"-01-03 02:50:30] /echo abc", year, 1, 3, 2, 50, 30, 0);
        
        // Make sure the time is in the future
        ZonedDateTime target = ZonedDateTime.now().plusHours(1);
        year = target.getYear();
        int month = target.getMonthValue();
        int day = target.getDayOfMonth();
        int hour = target.getHour();
        checkDatetime(timerCommand, hour+":30 /echo abc", year, month, day, hour, 30, 0, 0);
        checkDatetime(timerCommand, hour+":30:10 /echo abc", year, month, day, hour, 30, 10, 0);
        checkDatetime(timerCommand, hour+":30:02 /echo abc", year, month, day, hour, 30, 2, 0);
    }
    
    /**
     * For durations the target time is calculated by adding to the current
     * time, which won't be exactly the same for the expected value and the one
     * in the timer, so allow for some leeway (still makes sure that the time
     * isn't completely wrong).
     *
     * @param a
     * @param b
     */
    private void checkTargetTime(long a, long b) {
        assertTrue(Math.abs(a - b) < 1000);
    }
    
    private long makeTargetTime(int days, int hours, int minutes, int seconds, int milliseconds) {
        return System.currentTimeMillis()
                + DAYS.toMillis(days)
                + HOURS.toMillis(hours)
                + MINUTES.toMillis(minutes)
                + SECONDS.toMillis(seconds)
                + milliseconds;
    }
    
    private void check(TimerCommand timerCommand, String input, long targetTime, String id, String command) {
        TimerEntry entry = timerCommand.command(input, Room.EMPTY, Parameters.create("")).entry;
        if (entry != null) {
            checkTargetTime(targetTime, entry.targetTime);
            if (id != null) {
                assertEquals(entry.id, id);
            }
            if (command != null) {
                assertEquals(entry.command, command);
            }
        }
        else if (targetTime > 0) {
            throw new AssertionError("No entry created");
        }
    }
    
    private void checkDatetime(TimerCommand timerCommand, String input, int year, int month, int day, int hour, int minute, int second, int milliseconds) {
        TimerEntry entry = timerCommand.command(input, Room.EMPTY, Parameters.create("")).entry;
        long expected = ZonedDateTime.of(
                year, month, day, hour, minute, second,
                (int) MILLISECONDS.toNanos(milliseconds),
                ZoneId.systemDefault()
        ).toInstant().toEpochMilli();
        checkTargetTime(expected, entry.targetTime);
    }
    
    @Test
    public void testIdAndStop() {
        TimerCommand timerCommand = new TimerCommand(new Settings(null, null), new TimerCommand.TimerAction() {
            @Override
            public void performAction(String command, String channel, Parameters parameters, Set<TimerCommand.Option> options) {
                // Not needed
            }

            @Override
            public void log(String line) {
                // Not needed
            }
        });
        timerCommand.command(":abc 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(1, timerCommand.getNumTimers());
        timerCommand.command("stop abc", Room.EMPTY, EMPTY);
        assertEquals(0, timerCommand.getNumTimers());
        
        // Create several with the same id prefix
        timerCommand.command(":a* 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(1, timerCommand.getNumTimers());
        timerCommand.command(":a* 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(2, timerCommand.getNumTimers());
        timerCommand.command(":b* 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(3, timerCommand.getNumTimers());
        timerCommand.command(":b* 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(4, timerCommand.getNumTimers());
        timerCommand.command("stop a", Room.EMPTY, EMPTY);
        assertEquals(4, timerCommand.getNumTimers());
        timerCommand.command("stop a*", Room.EMPTY, EMPTY);
        assertEquals(2, timerCommand.getNumTimers());
        timerCommand.command("stop b1", Room.EMPTY, EMPTY);
        assertEquals(1, timerCommand.getNumTimers());
        timerCommand.command(":b* 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(2, timerCommand.getNumTimers());
        timerCommand.command("stop *", Room.EMPTY, EMPTY);
        assertEquals(0, timerCommand.getNumTimers());
        
        timerCommand.command(":abc 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(1, timerCommand.getNumTimers());
        timerCommand.command(":a* 1h /echo abc", Room.EMPTY, EMPTY);
        assertEquals(2, timerCommand.getNumTimers());
        timerCommand.command(":a* stop", Room.EMPTY, EMPTY);
        assertEquals(0, timerCommand.getNumTimers());
    }
    
}
