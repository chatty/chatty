
package chatty.util;

import chatty.Room;
import chatty.util.commands.Parameters;
import chatty.util.settings.Settings;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.NANO_OF_SECOND;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the /timer command.
 * 
 * @author tduva
 */
public class TimerCommand {
    
    public static final String TIMER_PARAMETERS_KEY = "timercommand";
    public static final String TIMER_PARAMETERS_KEY_CHANGED_CHANNEL = "timercommand-changed-channel";
    
    private static final String SETTING_NAME = "timers";
    
    private final List<TimerEntry> entries = new ArrayList<>();
    private final Timer timer = new Timer("TimerCommand");
    private final TimerAction action;
    
    private final Map<String, Integer> idCounter = new HashMap<>();
    
    public static interface TimerAction {
        public void performAction(String command, String channel, Parameters parameters, Set<Option> options);
        public void log(String line);
    }
    
    public enum Option {
        CHANNEL_LENIENT("a"), KEEP_THROUGH_RESTART("r"), SILENT("s"),
        OVERWRITE("o"), ADDED_ORDER("q");
        
        private final String id;

        Option(String id) {
            this.id = id;
        }
        
        public static Option fromId(String id) {
            for (Option s : Option.values()) {
                if (s.id.equals(id)) {
                    return s;
                }
            }
            return null;
        }
    }
    
    public TimerCommand(Settings settings, TimerAction action) {
        this.action = action;
        settings.addSettingsListener((s) -> {
            saveSettings(s);
        });
    }
    
    private static final Pattern PARSER = Pattern.compile("(?:-(?<options>[a-z]+) )?(?::(?<id>[^ ]+) )?(?<command>stop|list|(?<simple>[0-9mshd:]+) |\\[(?<datetime>[0-9: -]+)\\] )(?<task>.*)");
    
    private static final String USAGE = "Usage: /timer [-options] [:id] <duration|time|'list'|'stop'> <timed command>";
    
    public TimerResult command(String input, Room room, Parameters parameters) {
        if (input == null) {
            return new TimerResult(USAGE, null);
        }
        if (parameters.hasKey(TIMER_PARAMETERS_KEY)) {
            /**
             * If this were to be allowed would also have to include the nested
             * commands parameter when running the command later, to prevent
             * Custom Commands from calling eachother endlessly. Currently this
             * should be prevented by this and other commands should not remove
             * parameters in general.
             */
            return new TimerResult("Can't call /timer from a timed command", null);
        }
        Matcher m = PARSER.matcher(input);
        if (m.matches()) {
            String id = m.group("id");
            String command = m.group("command");
            String task = m.group("task");
            Set<Option> options = getOptions(m.group("options"));
            if (command.equals("list")) {
                // List timers
                if (id == null && !StringUtil.isNullOrEmpty(task.trim())) {
                    id = task.trim();
                }
                return new TimerResult(makeList(id, !options.contains(Option.ADDED_ORDER)), null);
            }
            else if (command.equals("stop")) {
                // Stop timers
                if (id == null) {
                    if (!StringUtil.isNullOrEmpty(task.trim())) {
                        id = task.trim();
                    }
                    else {
                        return new TimerResult("Timer id needs to be specified: /timer stop <id>", null);
                    }
                }
                List<TimerEntry> filtered = getEntriesById(id);
                if (!filtered.isEmpty()) {
                    for (TimerEntry entry : filtered) {
                        stopTimer(entry.id);
                    }
                    return new TimerResult(String.format("Timer stopped: %s", StringUtil.join(filtered, ", ", entry -> ((TimerEntry)entry).id)), null);
                }
                return new TimerResult(String.format("No matching timer found for '%s'", id), null);
            }
            else {
                // Start timer
                if (StringUtil.isNullOrEmpty(task)) {
                    return new TimerResult(USAGE, null);
                }
                long targetTime;
                String simpleTarget = m.group("simple");
                if (simpleTarget != null) {
                    if (simpleTarget.contains(":")) {
                        targetTime = parseDatetime(simpleTarget);
                    }
                    else {
                        targetTime = parseDuration(simpleTarget);
                    }
                }
                else {
                    targetTime = parseDatetime(m.group("datetime"));
                }
                if (isTargetInFuture(targetTime)) {
                    id = makeId(id);
                    if (getEntryById(id) != null) {
                        if (options.contains(Option.OVERWRITE)) {
                            stopTimer(id);
                        }
                        else {
                            return new TimerResult("Timer with id "+id+" already exists", null);
                        }
                    }
                    TimerEntry entry = addEntry(id, targetTime, task, room.getChannel(), options);
                    if (entry == null) {
                        return new TimerResult("Invalid time, no timer added", null);
                    }
                    if (entry.options.contains(Option.SILENT)) {
                        action.log("Silenced: "+entry);
                        return new TimerResult(null, entry);
                    }
                    return new TimerResult(entry.toString(), entry);
                }
                return new TimerResult("Invalid time, no timer added", null);
            }
        }
        return new TimerResult(USAGE, null);
    }
    
    /**
     * Uses an auto-generated number if the id is not given or it ends in "*",
     * in which case it also ensures that the id isn't already in use (which
     * could happen with a manually entered id or from a restored timer).
     * 
     * @param id
     * @return 
     */
    private synchronized String makeId(String id) {
        String numKey = null;
        if (StringUtil.isNullOrEmpty(id)) {
            numKey = "";
        }
        else if (id.endsWith("*")) {
            numKey = id.substring(0, id.length() - 1);
        }
        if (numKey != null) {
            // Has auto-generated number, find first unused id
            int count = idCounter.getOrDefault(numKey, 0);
            do {
                count++;
                id = numKey+count;
            } while (getEntryById(id) != null);
            idCounter.put(numKey, count);
        }
        return id;
    }
    
    public synchronized TimerEntry addEntry(String id, long targetTime, String command, String channel, Set<Option> options) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                /**
                 * Creates an empty Parameters, except for indicating that this
                 * comes from a timer (for restricting some things that could
                 * cause issues such as endless loops of timers calling
                 * eachother).
                 * 
                 * Including all original parameters would be possible when
                 * executed in the same session, but storing timers through
                 * restarts with parameters wouldn't be as simple (especially
                 * objects like User), so to keep it consistent (and different
                 * kinds of executions don't potentially do different things)
                 * do it like this.
                 */
                Parameters parameters = Parameters.create("");
                parameters.put(TIMER_PARAMETERS_KEY, "true");
                action.performAction(command, channel, parameters, options);
                taskFinished(this);
            }
        };
        if (!isTargetInFuture(targetTime)) {
            task.run();
            return null;
        }
        else {
            TimerEntry entry = new TimerEntry(id, targetTime, command, channel, options, task);
            entries.add(entry);
            timer.schedule(task, new Date(targetTime));
            return entry;
        }
    }
    
    private synchronized boolean stopTimer(String id) {
        TimerEntry entry = getEntryById(id);
        if (entry != null) {
            entry.task.cancel();
            entries.remove(entry);
            return true;
        }
        return false;
    }
    
    private synchronized void taskFinished(TimerTask task) {
        Iterator<TimerEntry> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().task == task) {
                it.remove();
            }
        }
    }
    
    public synchronized int getNumTimers() {
        return entries.size();
    }
    
    /**
     * The the timer matched by the id exactly.
     * 
     * @param id
     * @return The TimerEntry or null if none could be found
     */
    private synchronized TimerEntry getEntryById(String id) {
        if (id == null) {
            return null;
        }
        for (TimerEntry entry : entries) {
            if (entry.id.equals(id)) {
                return entry;
            }
        }
        return null;
    }
    
    /**
     * Get all timers with ids matching the given id, based on the
     * {@link TimerEntry#matchesId(String)} method.
     *
     * @param id
     * @return 
     */
    private synchronized List<TimerEntry> getEntriesById(String id) {
        List<TimerEntry> filtered = new ArrayList<>();
        for (TimerEntry entry : entries) {
            if (id == null || entry.matchesId(id)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
    
    private synchronized String makeList(String id, boolean timeOrder) {
        if (entries.isEmpty()) {
            return "No active timers";
        }
        StringBuilder b = new StringBuilder();
        List<TimerEntry> filtered = getEntriesById(id);
        if (timeOrder) {
            filtered.sort((o1, o2) -> {
                return Long.compare(o1.targetTime, o2.targetTime);
            });
        }
        
        if (id != null && !id.isEmpty()) {
            b.append(String.format("Currently %d active timers matching '%s' (out of %d):",
                    filtered.size(), id, entries.size()));
        }
        else {
            b.append(String.format("Currently %d active timers:",
                    entries.size()));
        }
        b.append("\n");
        
        for (TimerEntry entry : filtered) {
            b.append(entry.toString()).append("\n");
        }
        return b.toString();
    }
    
    public static class TimerResult {
        
        public final String message;
        public final TimerEntry entry;
        
        public TimerResult(String message, TimerEntry entry) {
            this.entry = entry;
            this.message = message;
        }
        
    }
    
    public static class TimerEntry {
        
        private final TimerTask task;
        public final String command;
        public final String channel;
        public final String id;
        public final long targetTime;
        public final Set<Option> options;
        
        public TimerEntry(String id, long targetTime, String command, String channel, Set<Option> options, TimerTask task) {
            this.task = task;
            this.command = command;
            this.channel = channel;
            this.id = id;
            this.targetTime = targetTime;
            this.options = options;
        }
        
        /**
         * Matches the ids exactly, unless it ends with a "*", in which case it
         * only has to begin with the given id.
         * 
         * @param id
         * @return 
         */
        public boolean matchesId(String id) {
            return this.id.equals(id)
                    || (id.endsWith("*") && this.id.startsWith(id.substring(0, id.length() - 1)));
        }
        
        @Override
        public String toString() {
            LocalDateTime datetime = LocalDateTime.ofInstant(Instant.ofEpochMilli(targetTime), ZoneId.systemDefault());
            LocalDateTime midnight = LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS);
            return String.format("Timer %s runs in %s (%s%s, %s) [%s]",
                    id,
                    DateTime.duration(targetTime - System.currentTimeMillis()),
                    datetime.isBefore(midnight) ? DateTime.format(targetTime) : DateTime.formatFullDatetime(targetTime),
                    options.isEmpty() ? "" : ", -"+StringUtil.join(options, "", o -> ((Option)o).id),
                    channel,
                    StringUtil.shortenTo(command, 30));
        }
        
        public List<Object> toList() {
            List<Object> result = new ArrayList<>();
            result.add(id);
            result.add(targetTime);
            result.add(command);
            result.add(channel);
            result.add(StringUtil.join(options, "", o -> ((Option)o).id));
            return result;
        }
        
    }
    
    //==========================
    // Save/load
    //==========================
    
    private void saveSettings(Settings settings) {
        List<List> data = new ArrayList<>();
        for (TimerEntry entry : entries) {
            if (entry.options.contains(Option.KEEP_THROUGH_RESTART)) {
                data.add(entry.toList());
            }
        }
        settings.putList(SETTING_NAME, data);
    }
    
    public String loadFromSettings(Settings settings) {
        @SuppressWarnings("unchecked")
        List<List<Object>> data = (List<List<Object>>)settings.getList(SETTING_NAME);
        if (data.isEmpty()) {
            return null;
        }
        int scheduled = 0;
        for (List<Object> entry : data) {
            String id = (String) entry.get(0);
            long targetTime = (Long) entry.get(1);
            String command = (String) entry.get(2);
            String channel = (String) entry.get(3);
            Set<Option> options = getOptions((String) entry.get(4));
            TimerEntry addedTimer = addEntry(id, targetTime, command, channel, options);
            if (addedTimer != null) {
                scheduled++;
                action.log("Restored timer: "+addedTimer);
            }
            else {
                action.log("Executed restored timer: "+new TimerEntry(id, targetTime, command, channel, options, null));
            }
        }
        return String.format("%d timer commands run, %d scheduled",
                data.size() - scheduled,
                scheduled);
    }
    
    
    //==========================
    // Util
    //==========================
    
    private static Set<Option> getOptions(String input) {
        Set<Option> result = new HashSet<>();
        if (input != null) {
            for (Option option : Option.values()) {
                if (input.contains(option.id)) {
                    result.add(option);
                }
            }
        }
        return result;
    }
    
    private static boolean isTargetInFuture(long targetTime) {
        return targetTime - System.currentTimeMillis() > 0;
    }
    
    
    //==========================
    // Parsing
    //==========================
    
    //--------------------------
    // Duration
    //--------------------------
    
    private static final Pattern DURATION_PARSER = Pattern.compile("(?<num>[0-9]+)(?<unit>ms|s|m|h|d)?");
    
    private static long parseDuration(String text) {
        Matcher m = DURATION_PARSER.matcher(text);
        long ms = 0;
        TimeUnit unit = TimeUnit.SECONDS;
        while (m.find()) {
            long num = Long.parseLong(m.group("num"));
            String unitText = m.group("unit");
            if (unitText != null) {
                unit = getTimeUnitFromString(unitText);
            }
            ms += unit.toMillis(num);
            unit = nextTimeUnit(unit);
        }
        return System.currentTimeMillis() + ms;
    }
    
    /**
     * Supported time units ordered from long to short since something like
     * "10h30" should default the "30" to the next shorter unit ("30m").
     */
    private static final TimeUnit[] TIME_UNITS = new TimeUnit[]{
        TimeUnit.DAYS,
        TimeUnit.HOURS,
        TimeUnit.MINUTES,
        TimeUnit.SECONDS,
        TimeUnit.MILLISECONDS
    };
    
    private static TimeUnit nextTimeUnit(TimeUnit unit) {
        for (int i = 0; i < TIME_UNITS.length - 1; i++) {
            if (TIME_UNITS[i] == unit) {
                return TIME_UNITS[i + 1];
            }
        }
        return unit;
    }
    
    private static TimeUnit getTimeUnitFromString(String unit) {
        if (unit != null) {
            switch (unit) {
                case "ms":
                    return TimeUnit.MILLISECONDS;
                case "m":
                    return TimeUnit.MINUTES;
                case "h":
                    return TimeUnit.HOURS;
                case "d":
                    return TimeUnit.DAYS;
            }
        }
        return TimeUnit.SECONDS;
    }
    
    //--------------------------
    // Time
    //--------------------------
    
    /**
     * Not requiring two digits for hour.
     */
    private static final DateTimeFormatter LOCAL_TIME = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 1, 2, SignStyle.NORMAL)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .toFormatter();
    
    private static long parseDatetime(String input) {
        if (input == null) {
            return -1;
        }
        try {
            String[] split = input.split(" ", 2);

            LocalDate date = null;
            String timeText = split[0];
            if (split.length == 2) {
                date = LocalDate.parse(split[0], DateTimeFormatter.ISO_LOCAL_DATE);
                timeText = split[1];
            }

            LocalTime time = LocalTime.parse(timeText, LOCAL_TIME);
            LocalDateTime datetime = time.atDate(date != null ? date : LocalDate.now());
            if (datetime.isBefore(LocalDateTime.now())) {
                datetime = datetime.plusDays(1);
            }
            return datetime.toInstant(ZoneOffset.systemDefault().getRules().getOffset(datetime)).toEpochMilli();
        }
        catch (Exception ex) {
            return -1;
        }
    }
    
    //==========================
    // Testing
    //==========================
    public static void main(String[] args) {
        String input = "2022-08-01 21:17:10";
        String[] split = input.split(" ", 2);
        LocalDate date = null;
        String timeText = split[0];
        if (split.length == 2) {
            date = LocalDate.parse(split[0], DateTimeFormatter.ISO_DATE);
            timeText = split[1];
        }
        
        LocalTime time = LocalTime.parse(timeText, DateTimeFormatter.ISO_LOCAL_TIME);
        LocalDateTime datetime = time.atDate(date != null ? date : LocalDate.now());
        if (datetime.isBefore(LocalDateTime.now())) {
            datetime = datetime.plusDays(1);
        }
        System.out.println(datetime);
        System.out.println(datetime.toInstant(ZoneOffset.systemDefault().getRules().getOffset(datetime)));
        System.out.println(LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.DAYS));
    }
    
}
