
package chatty.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stuff to do with dates/time.
 * 
 * @author tduva
 */
public class DateTime {
    
    private static final SimpleDateFormat FULL_DATETIME = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ZZ");
    private static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat SDF2 = new SimpleDateFormat("HH:mm");
    public static final int MINUTE = 60;
    public static final int HOUR = MINUTE * 60;
    public static final int DAY = HOUR * 24;
    
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
    
    public static String ago(long time) {
        long timePassed = System.currentTimeMillis() - time;
        return ago2(timePassed);
        
    }
    
    public static String ago2(long timePassed) {
        long seconds = timePassed / 1000;
        if (seconds < MINUTE*10) {
            return "just now";
        }
        if (seconds < HOUR) {
            return "recently";
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+" "+(hours == 1 ? "hour" : "hours")+" ago";
        }
        int days = (int)seconds / DAY;
        return days+" "+(days == 1 ? "day" : "days")+" ago";
    }
    
    public static String ago4(long time) {
        long seconds = (System.currentTimeMillis() - time) / 1000;
        if (seconds < MINUTE) {
            return seconds+" "+(seconds == 1 ? "second" : "seconds");
        }
        if (seconds < HOUR) {
            int minutes = (int)seconds / MINUTE;
            return minutes+" "+(minutes == 1 ? "minute" : "minutes");
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+" "+(hours == 1 ? "hour" : "hours");
        }
        int days = (int)seconds / DAY;
        return days+" "+(days == 1 ? "day" : "days");
    }
    
    public static String ago4compact(long time) {
        long seconds = (System.currentTimeMillis() - time) / 1000;
        if (seconds < MINUTE) {
            return seconds+"s";
        }
        if (seconds < HOUR) {
            int minutes = (int)seconds / MINUTE;
            return minutes+"m";
        }
        if (seconds < DAY) {
            int hours = (int)seconds / HOUR;
            return hours+"h";
        }
        int days = (int)seconds / DAY;
        return days+"d";
    }
    
    public static String ago3(long time, boolean showSeconds) {
        long timePassed = System.currentTimeMillis() - time;
        long seconds = timePassed / 1000;
        
        return duration2(seconds, showSeconds);
    }
    
    public static String duration2(long seconds, boolean showSeconds) {
        long hours = seconds / HOUR;
        seconds = seconds % HOUR;
        
        long minutes = seconds / MINUTE;
        seconds = seconds % MINUTE;
        
        if (showSeconds)
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        return String.format("%02d:%02d", hours, minutes, seconds);
    }
    
    public static String ago5(long time) {
        return ago5(time, false);
    }
    
    public static String ago5(long time, boolean showSeconds) {
        long timePassed = System.currentTimeMillis() - time;
        long seconds = timePassed / 1000;
        
        return duration3(seconds, showSeconds);
    }
    
    public static String duration3(long seconds) {
        return duration3(seconds, false);
    }
    
    public static String duration3(long seconds, boolean showSeconds) {
        long hours = seconds / HOUR;
        seconds = seconds % HOUR;
        
        long minutes = seconds / MINUTE;
        seconds = seconds % MINUTE;
        
        if (hours == 0) {
            if (showSeconds) {
                return String.format("%dm %ds", minutes, seconds);
            }
            return String.format("%dm", minutes);
        }
        if (showSeconds) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }
        return String.format("%dh %dm", hours, minutes);
    }
    
    public static String duration(long time, boolean detailed) {
        return duration(time, detailed, true);
    }
    
    public static String duration(long time, boolean detailed, boolean milliseconds) {
        long seconds = time;
        if (milliseconds) {
            seconds = time / 1000;
        }
        if (seconds < MINUTE) {
            return seconds+"s";
        }
        if (seconds < HOUR) {
            int s = (int)seconds % MINUTE;
            if (detailed && s > 0) {
                return seconds / MINUTE+"m "+s+"s";
            }
            return seconds / MINUTE+"m";
        }
        if (seconds < DAY) {
            return seconds / HOUR+"h";
        }
        return seconds / DAY+"d";
    }
    
    public static String agoFull(long time) {
        return durationFull(System.currentTimeMillis() - time, true);
    }
    
    public static String durationFull(long time, boolean milliseconds) {
        long seconds = time;
        if (milliseconds) {
            seconds = time / 1000;
        }
        long days = seconds / DAY;
        seconds = seconds % DAY;
        
        long hours = seconds / HOUR;
        seconds = seconds % HOUR;
        
        long minutes = seconds / MINUTE;
        seconds = seconds % MINUTE;
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }
    
    public static final void main(String[] args) {
        System.out.println(durationFull(269467, false));
        System.out.println(formatFullDatetime((long)1427846400*1000));
        System.out.println(System.currentTimeMillis());
        try {
            long time = chatty.util.api.Util.parseTime("2015-04-01T00:00:00Z");
            System.out.println(time);
            System.out.println(formatFullDatetime(time));
            System.out.println(time / 1000);
        } catch (ParseException ex) {
            Logger.getLogger(DateTime.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
