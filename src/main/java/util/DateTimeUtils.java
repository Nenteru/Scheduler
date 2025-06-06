package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
// import java.util.Locale;

public class DateTimeUtils {

    private static final DateTimeFormatter SHORT_DATE_TIME = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    private static final DateTimeFormatter MEDIUM_DATE_TIME = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
    // private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss", new Locale("ru"));

    public static String formatShort(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(SHORT_DATE_TIME);
    }

    public static String formatMedium(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(MEDIUM_DATE_TIME);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
}
