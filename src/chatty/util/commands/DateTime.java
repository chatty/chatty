
package chatty.util.commands;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Output date/time, with optional custom format, timezone and locale.
 * 
 * @author tduva
 */
class DateTime implements Item {

    private static final Map<String, DateTimeFormatter> predefinedFormats = new HashMap<>();
    
    static {
        predefinedFormats.put("date", DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        predefinedFormats.put("date2", DateTimeFormatter.ofPattern("yyyy-MM-dd z 'GMT'xx"));
        predefinedFormats.put("datetime", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        predefinedFormats.put("datetime2", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z 'GMT'xx"));
        predefinedFormats.put("time", DateTimeFormatter.ofPattern("HH:mm:ss"));
        predefinedFormats.put("time2", DateTimeFormatter.ofPattern("HH:mm:ss z 'GMT'xx"));
        predefinedFormats.put("time_short", DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        predefinedFormats.put("time_medium", DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
        predefinedFormats.put("time_long", DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG));
        predefinedFormats.put("time_full", DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL));
        predefinedFormats.put("date_short", DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
        predefinedFormats.put("date_medium", DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        predefinedFormats.put("date_long", DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
        predefinedFormats.put("date_full", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
        predefinedFormats.put("datetime_short", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
        predefinedFormats.put("datetime_medium", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
        predefinedFormats.put("datetime_long", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
        predefinedFormats.put("datetime_full", DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL));
    }
    
    private final Item format;
    private final Item zone;
    private final Item locale;
    private final Item timestamp;
    private final boolean isRequired;
    
    public DateTime(Item format, Item zone, Item locale, Item timestamp, boolean isRequired) {
        this.format = format;
        this.zone = zone;
        this.locale = locale;
        this.timestamp = timestamp;
        this.isRequired = isRequired;
    }

    @Override
    public String replace(Parameters parameters) {
        //--------------------
        // Prepare parameters
        //--------------------
        String formatString = "";
        if (format != null) {
            formatString = format.replace(parameters);
        }
        
        String zoneString = "";
        if (zone != null) {
            zoneString = zone.replace(parameters);
        }
        
        String localeString = "";
        if (locale != null) {
            localeString = locale.replace(parameters);
        }
        
        String timestampString = "";
        if (timestamp != null) {
            timestampString = timestamp.replace(parameters);
        }
        
        if (!Item.checkReq(isRequired, formatString, zoneString, localeString, timestampString)) {
            return null;
        }
        
        zoneString = zoneString.trim();
        localeString = localeString.trim();
        timestampString = timestampString.trim();
        
        //-----------
        // Formatter
        //-----------
        DateTimeFormatter formatter = getPredefined(formatString);
        if (formatter == null) {
            if (!formatString.isEmpty()) {
                try {
                    formatter = DateTimeFormatter.ofPattern(formatString);
                } catch (Exception ex) {
                    return "Invalid time format (" + ex.getLocalizedMessage() + ")";
                }
            } else {
                formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
            }
        }
        
        //----------
        // Timezone
        //----------
        ZoneId zoneId = ZoneId.systemDefault();
        if (!zoneString.isEmpty()) {
            try {
                // Try parsing it directly first
                zoneId = ZoneId.of(zoneString);
            } catch (Exception ex) {
                try {
                    // Try searching in available zone ids
                    zoneId = ZoneId.of(findZoneId(zoneString));
                } catch (Exception ex2) {
                    return "Invalid timezone";
                }
            }
        }
        
        //--------
        // Locale
        //--------
        if (!localeString.isEmpty()) {
            formatter = formatter.withLocale(Locale.forLanguageTag(localeString));
        }
        
        //-----------
        // Timestamp
        //-----------
        long parsedTimestamp = -1;
        if (!timestampString.isEmpty()) {
            try {
                parsedTimestamp = Long.parseLong(timestampString);
            }
            catch (NumberFormatException ex) {
                return "Invalid timestamp";
            }
        }
        
        //--------
        // Output
        //--------
        // This could still throw an error (although not sure how likely)
        try {
            if (parsedTimestamp != -1) {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(parsedTimestamp), zoneId).format(formatter);
            }
            return ZonedDateTime.now(zoneId).format(formatter);
        } catch (Exception ex) {
            return "Time format error";
        }
    }
    
    /**
     * Find a zone id that contains the given search. There could be more than
     * one that matches, but only one of them is returned. If the search is
     * equal to a zone id, that one is returned, otherwise the last matching one
     * is (although they could be unsorted anyway).
     * 
     * @param search
     * @return 
     */
    private String findZoneId(String search) {
        search = search.replace(" ", "_").toLowerCase(Locale.ENGLISH);
        String someMatch = null;
        for (String zoneId : ZoneId.getAvailableZoneIds()) {
            if (zoneId.equalsIgnoreCase(search)) {
                return zoneId;
            }
            if (zoneId.toLowerCase(Locale.ENGLISH).contains(search)) {
                someMatch = zoneId;
            }
        }
        return someMatch;
    }
    
    private DateTimeFormatter getPredefined(String input) {
        for (String key : predefinedFormats.keySet()) {
            if (input.equals(key)) {
                return predefinedFormats.get(key);
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Time (" + format + "," + zone + "," + locale + ")";
    }

    @Override
    public Set<String> getIdentifiersWithPrefix(String prefix) {
        return Item.getIdentifiersWithPrefix(prefix, format, zone, locale);
    }
    
    @Override
    public Set<String> getRequiredIdentifiers() {
        return Item.getRequiredIdentifiers(isRequired, format, zone, locale);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DateTime other = (DateTime) obj;
        if (!Objects.equals(this.format, other.format)) {
            return false;
        }
        if (!Objects.equals(this.zone, other.zone)) {
            return false;
        }
        if (!Objects.equals(this.locale, other.locale)) {
            return false;
        }
        if (this.isRequired != other.isRequired) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 71 * hash + Objects.hashCode(this.format);
        hash = 71 * hash + Objects.hashCode(this.zone);
        hash = 71 * hash + Objects.hashCode(this.locale);
        hash = 71 * hash + (this.isRequired ? 1 : 0);
        return hash;
    }
    
}
