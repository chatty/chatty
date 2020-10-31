
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
        COMPACT, VERBOSE, LEADING_ZERO_VALUES, NO_ZERO_VALUES, NO_SPACES,
        LAST_ONE_EXACT
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
    
    public static String duration(long seconds, int upperLimit, int max,
            int lowerLimit, int min, Formatting... options) {
        
        List<Formatting> options2 = Arrays.asList(options);
        boolean leadingZeroValues = options2.contains(Formatting.LEADING_ZERO_VALUES);
        boolean noZeroValues = options2.contains(Formatting.NO_ZERO_VALUES);
        boolean verbose = options2.contains(Formatting.VERBOSE);
        boolean lastOneExact = options2.contains(Formatting.LAST_ONE_EXACT);
        String sep = " ";
        if (options2.contains(Formatting.NO_SPACES)) {
            sep = "";
        }
        
        boolean negative = false;
        if (seconds < 0) {
            seconds = -seconds;
            negative = true;
        }
        double[] times = getTimes(seconds, TIME_DEF, upperLimit);
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
                // Substract for rounding down with one digit precision
                double exact = times[i] - 0.05;
                b.append(String.format(Locale.ENGLISH, "%.1f", exact));
            } else {
                b.append(time);
            }
            b.append(timeName);
            
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
    }
    
}
