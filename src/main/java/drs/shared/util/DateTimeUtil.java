package drs.shared.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralised date/time helpers so the application uses a single
 * formatter pattern everywhere.
  
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private DateTimeUtil() {
        // Not instantiable
    }

    /*   * @return current local date-time
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /*   * Format a LocalDateTime in the canonical "yyyy-MM-dd HH:mm:ss" form,
     * suitable for serialisation in audit logs and similar.
         * @param dt the date-time (nullable)
     * @return formatted string, or "-" when input is null
     */
    public static String format(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(FMT);
    }

    /*   * Format a LocalDateTime in a friendlier display form.
         * @param dt the date-time (nullable)
     * @return formatted string, or "-" when input is null
     */
    public static String display(LocalDateTime dt) {
        return dt == null ? "-" : dt.format(DISPLAY);
    }

    /*   * Render a "n minutes / hours / days ago" relative time string.
         * @param past prior moment (nullable)
     * @return relative phrase or "-" when input is null
     */
    public static String relative(LocalDateTime past) {
        if (past == null) {
            return "-";
        }
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(past, now).toMinutes();
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
