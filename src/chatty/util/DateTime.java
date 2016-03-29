
package chatty.util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Stuff to do with dates/time.
 * 
 * @author tduva
 */
public class DateTime {
    
    private static final SimpleDateFormat FULL_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm");
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
            if (time == 1 && verbose) {
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
    
    public static final void main(String[] args) {
//        System.out.println("'"+dur(HOUR*2+1, Formatting.COMPACT, 0, -2, 2, 2, 2)+"'");
//        System.out.println("'"+duration(1000*MINUTE*1+1000, Formatting.COMPACT, N, 0, 0, 0, 2)+"'");
        //System.out.println(agoSingleVerbose(System.currentTimeMillis() ));
        //System.out.println(ago(System.currentTimeMillis() - 1000*60*60*25));
//        System.out.println(duration(1000*(HOUR*2), 0, 0, 0, 1, Formatting.LAST_ONE_EXACT));
        System.out.println(agoUptimeCompact(System.currentTimeMillis() - 1000*(MINUTE*110)));
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
        System.out.println(TimeUnit.HOURS.toMillis(1));
    }
}
