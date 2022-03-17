
package chatty.util;

import chatty.Room;
import chatty.util.api.StreamInfo;
import chatty.util.api.TwitchApi;
import chatty.util.commands.CustomCommand;
import chatty.util.commands.Parameters;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a formatted timestamp string based on SimpleDateFormat. Can also set
 * a custom timezone, include the stream uptime and customize the AM/PM string.
 * 
 * <p>
 * For stream uptime output the TwitchApi has to be set if the
 * {@link make(long, Room)} method is to be used. Only already cached StreamInfo
 * is used, it is not requested for this.
 *
 * @author tduva
 */
public class Timestamp {
    
    private static final Logger LOGGER = Logger.getLogger(Timestamp.class.getName());

    /**
     * Pattern to find the Uptime definition.
     */
    private static final Pattern UPTIME = Pattern.compile("\\{(?:([^a-z]+),)?uptime(:[a-zA-Z0-9]+)?(?:,([^a-z]+))?\\}");

    /**
     * With non-capturing groups, for use in the Settings Dialog. Also for use
     * before date formatting, so it includes the single quotes.
     */
    public static final Pattern UPTIME_NO_CAPTURE = Pattern.compile("'\\{(?:[^a-z]+,)?uptime(?::[a-zA-Z0-9]+)?(?:,[^a-z]+)?\\}'");

    /**
     * Used to get a StreamInfo when none is provided but a Room is available.
     */
    private static TwitchApi api;

    /**
     * For regular date/time formatting.
     */
    private final SimpleDateFormat format;
    
    /**
     * Whether this Timestamp contains one or more stream uptime format strings.
     */
    private final boolean hasUptime;
    
    /**
     * Optional CustomCommand to format the timestamp further (may be null).
     */
    private final CustomCommand command;

    /**
     * The timestamp input is a SimpleDateFormat format string, but may also
     * contain additional formatting:
     * 
     * <li>Everything after "###" is interpreted as a Custom Command (so this
     * doesn't have conform to SimpleDateFormat formatting)
     * <li>am/pm customization and uptime output (this has to be quoted inside
     * the SimpleDateFormat formatting)
     *
     * @param timestampInput If "off" nothing is output, must not be null
     * @param timezone If non-empty and not "local", apply a custom timezone,
     * must not be null
     */
    public Timestamp(String timestampInput, String timezone) {
        String customCommandInput = null;
        String[] split = timestampInput.split("###", 2);
        if (split.length == 2) {
            timestampInput = split[0];
            customCommandInput = split[1];
        }
        SimpleDateFormat parsedTimestamp = null;
        if (!timestampInput.equals("off")) {
            try {
                SimpleDateFormat sdf = DateTime.createSdfAmPm(timestampInput);
                if (!timezone.isEmpty() && !timezone.equalsIgnoreCase("local")) {
                    sdf.setTimeZone(TimeZone.getTimeZone(timezone));
                }
                parsedTimestamp = sdf;
            }
            catch (IllegalArgumentException ex) {
                LOGGER.warning("Invalid timestamp: " + timestampInput);
            }
        }
        format = parsedTimestamp;
        hasUptime = UPTIME.matcher(timestampInput).find();

        CustomCommand parsedCommand = null;
        if (customCommandInput != null) {
            parsedCommand = CustomCommand.parse(customCommandInput);
            if (parsedCommand.hasError()) {
                LOGGER.warning("Timestamp command error: " + parsedCommand.getError());
                parsedCommand = null;
            }
        }
        command = parsedCommand;
    }
    
    /**
     * Sets the TwitchApi reference for all Timestamp objects.
     * 
     * @param api 
     */
    public static void setTwitchApi(TwitchApi api) {
        Timestamp.api = api;
    }

    /**
     * Returns true only if the timestamp was valid and not "off".
     * 
     * @return 
     */
    public boolean isEnabled() {
        return format != null;
    }

    /**
     * Create the timestamp string based on the formatting specified when
     * creating this Timestamp. The StreamInfo for stream uptime output (if
     * necessary) is retrieved from the TwitchApi previously set via
     * {@link setTwitchApi(TwitchApi)}, using the stream of the Room.
     *
     * @param time The time to based the timestamp on, -1 for current time
     * @param room The Room the timestamp belongs to, may be null
     * @return The timestamp String, or null if isEnabled() is false or the
     * optional CustomCommand returned null
     */
    public String make(long time, Room room) {
        if (format != null && hasUptime && room != null && api != null) {
            StreamInfo info = api.getCachedStreamInfo(room.getStream());
            return make2(time, info);
        }
        return make2(time, null);
    }

    /**
     * Create the timestamp string based on the formatting specified when
     * creating this Timestamp. The stream uptime output (if necessary) is based
     * on the provided StreamInfo object (the TwitchApi is not called).
     * 
     * @param time The time to based the timestamp on, -1 for current time
     * @param info The StreamInfo to get the stream uptime from, may be null
     * @return The timestamp String, or null if isEnabled() is false or the
     * optional CustomCommand returned null
     */
    public String make2(long time, StreamInfo info) {
        if (format == null) {
            return null;
        }
        String result = time > 0 ? DateTime.format(time, format) : DateTime.currentTime(format);

        // Uptime
        if (hasUptime) {
            Matcher m = UPTIME.matcher(result);
            StringBuffer b = new StringBuffer();
            while (m.find()) {
                handleUptime(b, m, time, info);
            }
            m.appendTail(b);
            result = b.toString();
        }

        // Format using Custom Command
        if (command != null) {
            Parameters param = Parameters.create(result);
            return command.replace(param);
        }
        return result;
    }

    private static void handleUptime(StringBuffer b, Matcher m, long time, StreamInfo info) {
        boolean uptimeAdded = false;
        if (info != null
                && (time > 0 || (info.isValid() && info.getOnline()))) {
            String prefix = m.group(1) != null ? m.group(1) : "";
            String suffix = m.group(3) != null ? m.group(3) : "";
            String options = m.group(2);
            int lower = DateTime.S;
            int upper = DateTime.H;
            int min = 1;
            List<DateTime.Formatting> options2 = new ArrayList<>();
            boolean picnic = false;
            boolean picnicOptional = false;
            if (options != null) {
                if (options.contains("s")) {
                    lower = DateTime.N;
                    min++;
                }
                if (options.contains("H")) {
                    min++;
                }
                if (options.contains("c")) {
                    options2.add(DateTime.Formatting.CLOCK_STYLE);
                    options2.add(DateTime.Formatting.DOUBLE_DIGITS_EXCEPT_FIRST);
                    if (min < 2) {
                        min++;
                    }
                }
                if (options.contains("00")) {
                    options2.remove(DateTime.Formatting.DOUBLE_DIGITS_EXCEPT_FIRST);
                    options2.add(DateTime.Formatting.DOUBLE_DIGITS);
                }
                else if (options.contains("0")) {
                    options2.add(DateTime.Formatting.DOUBLE_DIGITS_EXCEPT_FIRST);
                }
                if (options.contains("t")) {
                    options2.add(DateTime.Formatting.NO_SPACES);
                }
                picnic = options.contains("p");
                if (options.contains("P")) {
                    picnic = true;
                    picnicOptional = true;
                }
            }
            long streamStarted = info.getHistoryStreamStart(time, picnic);
            if (picnicOptional) {
                if (info.getHistoryStreamStart(time, false) == streamStarted) {
                    /**
                     * Turn off if "P" option is used and Picnic time is not
                     * different from regular.
                     */
                    streamStarted = -1;
                }
            }
            if (streamStarted != -1) {
                long duration = getDuration(time, streamStarted);
                String uptime = DateTime.duration(duration, upper, 0, lower, min, options2);
//                        uptime = DateTime.agoUptimeCompact2(adjustTime(time, info.getHistoryStreamStart(time, false)), true);
//                        switch (m.group(2)) {
//                            case "uptime":
//                                uptime = DateTime.agoUptimeCompact2(adjustTime(time, info.getHistoryStreamStart(time, false)), true);
//                                break;
//                            case "uptime2":
//                                uptime = DateTime.agoUptimeCompact2(adjustTime(time, info.getHistoryStreamStart(time, false)), false);
//                                break;
//                            case "uptime3":
//                                uptime = DateTime.agoUptimeCompact2(adjustTime(time, info.getHistoryStreamStart(time, true)), true);
//                                break;
//                            case "uptime4":
//                                uptime = DateTime.agoUptimeCompact2(adjustTime(time, info.getHistoryStreamStart(time, true)), false);
//                                break;
//                        }
                m.appendReplacement(b, prefix + uptime + suffix);
                uptimeAdded = true;
            }
        }
        if (!uptimeAdded) {
            m.appendReplacement(b, "");
        }
    }

    private static long getDuration(long time, long started) {
        if (time > 0) {
            return time - started;
        }
        return System.currentTimeMillis() - started;
    }

}
