
package chatty.util;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stuff to do with dates/time.
 * 
 * @author tduva
 */
public class DateTime {
    
    private static final SimpleDateFormat FULL_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat SDF3 = new SimpleDateFormat("HH:mm:ss/SSS");
    public static final long MINUTE = 60;
    public static final long HOUR = MINUTE * 60;
    public static final long DAY = HOUR * 24;
    public static final long YEAR = DAY * 365;
    
    /**
     * Update the timezone for the formatters. These may be initialized before
     * the default timezone is set (e.g. from this class being used for logging
     * or other stuff), so probably need to update them.
     *
     * @param tz The timezone to set
     */
    public static void setTimeZone(TimeZone tz) {
        FULL_DATETIME.setTimeZone(tz);
        SDF.setTimeZone(tz);
        SDF2.setTimeZone(tz);
        SDF3.setTimeZone(tz);
    }
    
    public static int currentHour12Hour() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR);
    }
    
    public static String currentTime(SimpleDateFormat sdf) {
        synchronized(sdf) {
            Calendar cal = Calendar.getInstance();
            return sdf.format(cal.getTime());
        }
    }
    
    public static String fullDateTime() {
        return currentTime(FULL_DATETIME);
    }
    
    public static String currentTime() {
        return currentTime(SDF);
    }
    
    public static String currentTimeExact() {
        return currentTime(SDF3);
    }
    
    public static String formatExact(long time) {
        return format(time, SDF3);
    }
    
    public static String currentTime(String format) {
        return currentTime(new SimpleDateFormat(format));
    }
    
    public static String format(long time, SimpleDateFormat sdf) {
        synchronized(sdf) {
            return sdf.format(new Date(time));
        }
    }
    
    public static String format(long time) {
        return format(time, SDF);
    }
    
    public static String formatFullDatetime(long time) {
        return format(time, FULL_DATETIME);
    }
    
    public static String format2(long time) {
        return format(time, SDF2);
    }
    
    public static String formatAccountAge(long time, Formatting... options) {
        if (System.currentTimeMillis() - time > DAY*1000) {
            return ago(time, 0, 2, DateTime.H, options);
        }
        return ago(time, 0, 1, 0, options);
    }
    
    public static String formatAccountAgeCompact(long time) {
        return formatAccountAgeCompact(time, false);
    }
    
    public static String formatAccountAgeCompact(long time, boolean moreCompact) {
        Formatting compact = moreCompact ? Formatting.COMPACT : Formatting.VERBOSE;
        if (System.currentTimeMillis() - time >= YEAR*1000) {
            return ago(time, 0, 1, 0, Formatting.LAST_ONE_EXACT, compact);
        }
        return ago(time, 0, 1, 0, compact);
    }
    
    public static String formatAccountAgeVerbose(long time) {
        if (System.currentTimeMillis() - time > DAY*1000) {
            return ago(time, 0, 2, DateTime.H, Formatting.VERBOSE);
        }
        return ago(time, 0, 1, 0, Formatting.VERBOSE);
    }

    public static String agoText(long time) {
        long seconds = (System.currentTimeMillis() - time) / 1000;
        if (seconds < MINUTE*10) {
            return "just now";
        }
        if (seconds < HOUR) {
            return "recently";
        }
        if (seconds < DAY) {
            long hours = seconds / HOUR;
            return hours+" "+(hours == 1 ? "hour" : "hours")+" ago";
        }
        if (seconds < YEAR) {
            long days = seconds / DAY;
            return days + " " + (days == 1 ? "day" : "days") + " ago";
        }
        long years = seconds / YEAR;
        return years+" "+(years == 1 ? "year" : "years")+" ago";
    }
    
    public static String agoClock(long time, boolean showSeconds) {
        long timePassed = System.currentTimeMillis() - time;
        long seconds = timePassed / 1000;
        
        return durationClock(seconds, showSeconds);
    }
    
    public static String durationClock(long seconds, boolean showSeconds) {
        long hours = seconds / HOUR;
        seconds = seconds % HOUR;
        
        long minutes = seconds / MINUTE;
        seconds = seconds % MINUTE;
        
        if (showSeconds)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", hours, minutes, seconds);
    }

    public static String agoSingleCompact(long time) {
        return DateTime.ago(time, 0, 1, 0);
    }
    
    public static String agoSingleCompactAboveMinute(long time) {
        if (System.currentTimeMillis() - time > 60*1000) {
            return DateTime.ago(time, 0, 1, 0);
        }
        return "now";
    }
    
    public static String agoSingleVerbose(long time) {
        return duration(System.currentTimeMillis() - time, 1, 0,
                Formatting.VERBOSE);
    }
    
    public static String agoUptimeCompact(long time) {
        long ago = System.currentTimeMillis() - time;
        if (ago < (1000*HOUR)) {
            return duration(ago, 0, 0, S);
        }
        return duration(ago, H, 0, M, Formatting.LAST_ONE_EXACT);
    }
    
    public static String agoUptimeCompact2(long time) {
        long seconds = (System.currentTimeMillis() - time)/1000;
        long hours = seconds/HOUR;
        long minutes = (seconds%HOUR) / MINUTE;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        }
        return String.format("%dm", minutes);
    }
    
    public static enum Formatting {
        /**
         * Short time names.
         */
        COMPACT,
        /**
         * Long time names.
         */
        VERBOSE,
        /**
         * Show zero values at the front, for example "0y 1d" instead of "1d".
         */
        LEADING_ZERO_VALUES,
        /**
         * Show no zero values at all, for example "1m" instead of "1m 0s".
         */
        NO_ZERO_VALUES,
        /**
         * No spaces in between outputs, for example "1m10s".
         */
        NO_SPACES,
        /**
         * The last output is a floating point value, for example "4h 1.5m".
         */
        LAST_ONE_EXACT,
        /**
         * Pad with 0 to force double-digits, for example "01h 05m".
         */
        DOUBLE_DIGITS,
        /**
         * Same as {@link DOUBLE_DIGITS}, except not for the first output, for
         * example "1h 05m".
         */
        DOUBLE_DIGITS_EXCEPT_FIRST,
        /**
         * Use a colon as separator and omit time names, for example "1:10".
         * Recommended together with {@link DOUBLE_DIGITS} or
         * {@link DOUBLE_DIGITS_EXCEPT_FIRST} so it's "1:09" instead of "1:9".
         */
        CLOCK_STYLE
    }
    
    private static final String[] TIMENAMES_COMPACT = {"y", "d", "h", "m", "s"};
    private static final String[] TIMENAMES_VERBOSE = {" years", " days", " hours", " minutes", " seconds"};
    
    public static final int S = 1;
    public static final int M = 2;
    public static final int H = 3;
    public static final int D = 4;
    public static final int N = 0;
    
    public static String ago(long time, Formatting... options) {
        return duration(System.currentTimeMillis() - time, options);
    }
    
    public static String ago(long milliseconds, int upperLimit, int max,
            int lowerLimit, Formatting... options) {
        return duration(System.currentTimeMillis() - milliseconds, upperLimit,
                max, lowerLimit, options);
    }

    public static String duration(long milliseconds, Formatting... options) {
        return duration(milliseconds, 0, 0, options);
    }
    
    public static String duration(long milliseconds, int max, int lowerLimit, Formatting... options) {
        return duration(milliseconds, 0, max, lowerLimit, options);
    }
    
    public static String duration(long milliseconds, int upperLimit, int max,
            int lowerLimit, Formatting... options) {
        return duration(milliseconds, upperLimit, max, lowerLimit, 1, options);
    }
    
    public static String duration(long milliseconds, int upperLimit, int max,
            int lowerLimit, int min, Formatting... options) {
        return duration(milliseconds, upperLimit, max, lowerLimit, min, Arrays.asList(options));
    }
    
    /**
     * Create a duration String.
     * 
     * @param milliseconds The number of milliseconds
     * @param upperLimit The highest-order output (inclusive), e.g. if this is H, then two days duration will be output as "48h" (rather than "2d")
     * @param max The maximum number of outputs, e.g. "2h 10m" is impossible when this is 1
     * @param lowerLimit The lowest-order output (exclusive), e.g. if this is S, then ten seconds duration will be output as "0m" (rather than "10s")
     * @param min The minimum number of outputs, e.g. 0 seconds duration may be output as 0h 0m 0s when this is 3 (if applicable within other contraints)
     * @param options The options in {@link Formatting}
     * @return 
     */
    public static String duration(long milliseconds, int upperLimit, int max,
            int lowerLimit, int min, List<Formatting> options) {
        boolean leadingZeroValues = options.contains(Formatting.LEADING_ZERO_VALUES);
        boolean noZeroValues = options.contains(Formatting.NO_ZERO_VALUES);
        boolean verbose = options.contains(Formatting.VERBOSE);
        boolean lastOneExact = options.contains(Formatting.LAST_ONE_EXACT);
        boolean doubleDigits = options.contains(Formatting.DOUBLE_DIGITS) || options.contains(Formatting.DOUBLE_DIGITS_EXCEPT_FIRST);
        boolean doubleDigitsExceptFirst = options.contains(Formatting.DOUBLE_DIGITS_EXCEPT_FIRST);
        boolean clockStyle = options.contains(Formatting.CLOCK_STYLE);
        String sep = " ";
        if (options.contains(Formatting.NO_SPACES)) {
            sep = "";
        }
        if (clockStyle) {
            sep = ":";
        }
        
        boolean negative = false;
        if (milliseconds < 0) {
            milliseconds = -milliseconds;
            negative = true;
        }
        double[] times = getTimes(milliseconds, TIME_DEF, upperLimit);
        String[] timeNames = verbose ? TIMENAMES_VERBOSE : TIMENAMES_COMPACT;

        StringBuilder b = new StringBuilder();
        int shown = 0;
        int shownNonzero = 0;
        for (int i=0;i<times.length;i++) {
            int left = times.length - i;
            int time = (int)times[i];
//            if (shown >= max && max > 0) {
//                break;
//            }
//            if (left <= lowerLimit && shown > 0) {
//                break;
//            }
            if (time == 0) {
                if ((left > lowerLimit+min) || shown >= min) {
                    if (noZeroValues) {
                        continue;
                    } else if (!leadingZeroValues && shownNonzero == 0) {
                        continue;
                    }
                }
            }

            if (shown > 0) {
                b.append(sep);
            } else if (negative) {
                b.append("-");
            }
            /**
             * Now considered shown
             */
            shown++;
            boolean lastOne = shown >= max && max > 0
                    || left-1 <= lowerLimit && shown > 0;
            String timeName = timeNames[i+timeNames.length-times.length];
            if (time == 1 && verbose && !(lastOne && lastOneExact)) {
                timeName = timeName.substring(0, timeName.length() - 1);
            }
            if (lastOne && lastOneExact) {
                /**
                 * Substract for rounding down with one digit precision
                 * 
                 * Examples:
                 * 10.0 -> 9.95 -> String.format %.1f -> 10.0
                 * 1.55 -> 1.5 -> 1.5 (instead of 1.6)
                 * 
                 * For the double digits check, it basicially rounds it to one
                 * digit precision as well, except without dividing it again.
                 */
                double exact = times[i] - 0.05;
                if (doubleDigits
                        && Math.round(exact * 10) < 100
                        && (shown > 1 || !doubleDigitsExceptFirst)) {
                    b.append("0");
                }
                b.append(String.format(Locale.ENGLISH, "%.1f", exact));
            } else {
                if (doubleDigits
                        && time < 10
                        && (shown > 1 || !doubleDigitsExceptFirst)) {
                    b.append("0");
                }
                b.append(time);
            }
            if (!clockStyle) {
                b.append(timeName);
            }
            
            if (time > 0) {
                shownNonzero++;
            }
            if (lastOne) {
                break;
            }
        }
        return b.toString();
    }
    
    private static final long[] TIME_DEF = {YEAR, DAY, HOUR, MINUTE, 1};
    
    public static double[] getTimes(long input, long[] timeDef, int upperLimit) {
        double seconds = (double)(input / 1000);
        if (upperLimit <= 0 || upperLimit > timeDef.length) {
            upperLimit = timeDef.length;
        }
        double[] result = new double[upperLimit];
        int offset = timeDef.length - upperLimit;
        for (int i=0;i<result.length;i++) {
            long def = timeDef[i+offset];
            result[i] = seconds / def;
            seconds = seconds % def;
        }
        return result;
    }
    
    private static final MonthDay APRIL_FIRST = MonthDay.of(Month.APRIL, 1);
    private static final ElapsedTime isAprilFirstET = new ElapsedTime();
    private static boolean isAprilFirst;
    
    public static boolean isAprilFirst() {
        if (Debugging.isEnabled("f2")) {
            return true;
        }
        if (isAprilFirstET.secondsElapsed(600)) {
            isAprilFirstET.set();
            isAprilFirst = MonthDay.now().equals(APRIL_FIRST);
        }
        return isAprilFirst;
    }
    
    /**
     * Parses the time returned from the Twitch API.
     * 
     * https://stackoverflow.com/a/2202300/2375667
     * 
     * Switched to java.time now because DatatypeConverter isn't visible by
     * default in Java 9 anymore.
     *
     * @param time The time string
     * @return The timestamp
     * @throws IllegalArgumentException if the time could not be parsed
     */
    public static long parseDatetime(String time) {
        OffsetDateTime odt = OffsetDateTime.parse(time);
        return odt.toInstant().toEpochMilli();
    }
    
    private static final Pattern AM_PM_CUSTOM = Pattern.compile("'a:(.*?)\\/(.*?)'");
    
    public static SimpleDateFormat createSdfAmPm(String format) {
        Matcher m = AM_PM_CUSTOM.matcher(format);
        if (m.find()) {
            String[] amPm = new String[]{m.group(1), m.group(2)};
            format = m.replaceAll("");
            
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            DateFormatSymbols symbols = sdf.getDateFormatSymbols();
            symbols.setAmPmStrings(amPm);
            sdf.setDateFormatSymbols(symbols);
            return sdf;
        }
        return new SimpleDateFormat(format);
    }
    
    public static String formatMonthsVerbose(int months) {
        if (months < 12) {
            return months+" months";
        }
        return months+" months, "+formatMonths(months);
    }
    
    public static String formatMonths(int months) {
        if (months < 12) {
            return months+" months";
        }
        int y = months / 12;
        int m = months % 12;
        if (m == 0) {
            return String.format("%d %s",
                y,
                y == 1 ? "year" : "years");
        }
        return String.format("%d %s %d %s",
                y,
                y == 1 ? "year" : "years",
                m,
                m == 1 ? "month" : "months");
    }
    
    //--------------------------
    // Parse Duration
    //--------------------------
    private static final Pattern DURATION_PARSER = Pattern.compile("(?<num>[0-9]+)(?<unit>ms|s|m|h|d)?");
    
    public static long parseDurationSeconds(String text) {
        if (text == null) {
            return -1;
        }
        return parseDuration(text) / 1000;
    }
    
    public static long parseDuration(String text) {
        if (text == null) {
            return -1;
        }
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
        return ms;
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
    
    public static final void main(String[] args) {
//        System.out.println("'"+dur(HOUR*2+1, Formatting.COMPACT, 0, -2, 2, 2, 2)+"'");
//        System.out.println("'"+duration(1000*MINUTE*1+1000, Formatting.COMPACT, N, 0, 0, 0, 2)+"'");
        //System.out.println(agoSingleVerbose(System.currentTimeMillis() ));
        //System.out.println(ago(System.currentTimeMillis() - 1000*60*60*25));
//        System.out.println(duration(1000*(HOUR*2), 0, 0, 0, 1, Formatting.LAST_ONE_EXACT));
//        System.out.println(agoUptimeCompact(System.currentTimeMillis() - 1000*(MINUTE*110)));
        int a = 1 << 4;
        int b = 1 << 5;
        int c = 1 << 1;
        int v = a | b;
        //System.out.println(v ^ c);
//        System.out.println("'"+duration(1000*1, 1, 2)+"'");
//        System.out.println((long)1000*DAY*3000+1);
//        System.out.println(durationFull(269467, false));
//        System.out.println(formatFullDatetime((long)1427846400*1000));
//        System.out.println(System.currentTimeMillis());
//        try {
//            long time = chatty.util.api.Util.parseTime("2015-04-01T00:00:00Z");
//            System.out.println(time);
//            System.out.println(formatFullDatetime(time));
//            System.out.println(time / 1000);
//        } catch (ParseException ex) {
//            Logger.getLogger(DateTime.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        System.out.println(TimeUnit.HOURS.toMillis(1));
//        
//        System.out.println(formatAccountAgeCompact(System.currentTimeMillis() - 2500*1000));
//        System.out.println(formatAccountAgeCompact(System.currentTimeMillis() - DAY*3*1000));
        System.out.println(formatAccountAgeCompact(System.currentTimeMillis() - YEAR*1*1000, true));
//        System.out.println(formatAccountAgeCompact(System.currentTimeMillis() - 12500*1000));
//        System.out.println(formatAccountAgeVerbose(System.currentTimeMillis() - 300*DAY*1000));
        
        System.out.println(duration(60*11170*1000, N, 0, N, 0, Formatting.NO_ZERO_VALUES));
    }
    
}
