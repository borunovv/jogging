package com.borunovv.core.util;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class TimeUtils {

    public static final long MILLISECONDS_IN_DAY = (1000L * 60 * 60 * 24);

    /**
     * "YYYY-MM-DD hh:mm:ss" -> Date
     */
    public static Date parseDateTime_YYYYMMDD_HHMMSS_GMT0(String time) {
        try {
            return DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_GMT_0.get().parse(time);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse GMT0 time '" + time + "'", e);
        }
    }

    // Date -> "YYYY-MM-DD hh:mm:ss"
    // In GMT0 !
    public static String formatDateTime_YYYYMMDD_HHMMSS_GMT0(Date date) {
        return DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_GMT_0.get().format(date);
    }

    /**
     * "YYYY-MM-DD hh:mm:ss (GMT +00:00)" -> Date
     */
    public static Date parseDateTime_YYYYMMDD_HHMMSS_Z(String time) {
        try {
            return DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_GMT_0_WITH_ZONE.get().parse(time);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse GMT0 time '" + time + "'", e);
        }
    }

    /**
     * "YYYY-MM-DD (GMT +00:00)" -> Date with time 00:00:00
     */
    public static Date parseDateTime_YYYYMMDD_GMT0(String dateStr) {
        try {
            Date date = DATE_FORMAT_YYYY_MM_DD_GMT_0.get().parse(dateStr);
            return new Date((date.getTime() / MILLISECONDS_IN_DAY) * MILLISECONDS_IN_DAY);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse GMT0 date '" + dateStr + "'", e);
        }
    }

    // Date -> "YYYY-MM-DD hh:mm:ss (GMT +00:00)"
    public static String formatDateTime_YYYYMMDD_HHMMSS_Z_GMT0(Date date) {
        return DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_GMT_0_WITH_ZONE.get().format(date);
    }

    public static String formatDate_YYYYMMDD_Z_GMT0(Date date) {
        return DATE_FORMAT_YYYY_MM_DD_GMT_0_WITH_ZONE.get().format(date);
    }

    public static String formatDate_YYYYMMDD_GMT0(Date date) {
        return DATE_FORMAT_YYYY_MM_DD_GMT_0.get().format(date);
    }

    // Date -> "YYYY-MM-DD hh:mm:ss (GMT +NN:00)" - with default zone (server time).
    public static String formatDateTime_YYYYMMDD_HHMMSS_Z(Date date) {
        return DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_WITH_ZONE.get().format(date);
    }

    // Конвертит секунды в формат вида: "2 days(s), 12:04:45"
    // Удобно для отображения времени какого-то процесса.
    public static String secondsToDDHHMMSS(long seconds) {
        long days = seconds / (24 * 60 * 60);
        seconds -= days * (24 * 60 * 60);
        long hours = seconds / (60 * 60);
        seconds -= hours * (60 * 60);
        long minutes = seconds / 60;
        seconds -= minutes * 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" day(s), ");
        }
        sb.append(hours < 10 ? "0" + hours : "" + hours).append(":");
        sb.append(minutes < 10 ? "0" + minutes : "" + minutes).append(":");
        sb.append(seconds < 10 ? "0" + seconds : "" + seconds);

        return sb.toString();
    }

    // Return date difference (now - timeInFuture) in seconds
    public static long getDeltaSecondsFromNowTo(Date timeInFuture) {
        return getDeltaSecondsFromNowTo(new DateTime(timeInFuture));
    }

    // Return date difference (now - timeInFuture) in seconds
    public static long getDeltaSecondsFromNowTo(DateTime timeInFuture) {
        return getDeltaSeconds(DateTime.now(), timeInFuture);
    }

    // Return date difference = (to - from) in seconds
    public static long getDeltaSeconds(DateTime from, DateTime to) {
        Seconds seconds = Seconds.secondsBetween(from, to);
        return seconds.getSeconds();
    }

    public static long getUnixTimestamp() {
        return System.currentTimeMillis() / 1000L;
    }

    private static final String YYYY_MM_DD = "yyyy-MM-dd";
    private static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    private static final String YYYY_MM_DD_HH_MM_SS_Z = "yyyy-MM-dd HH:mm:ss (z)";
    private static final String YYYY_MM_DD_Z = "yyyy-MM-dd (z)";
    private static final String GMT_0 = "GMT+0:00";

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_YYYY_MM_DD_GMT_0
            = makeThreadSafeFormatter(YYYY_MM_DD, GMT_0);

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_GMT_0
            = makeThreadSafeFormatter(YYYY_MM_DD_HH_MM_SS, GMT_0);

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_GMT_0_WITH_ZONE
            = makeThreadSafeFormatter(YYYY_MM_DD_HH_MM_SS_Z, GMT_0);

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_YYYY_MM_DD_GMT_0_WITH_ZONE
            = makeThreadSafeFormatter(YYYY_MM_DD_Z, GMT_0);

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT_YYYY_MM_DD_HH_MM_SS_WITH_ZONE
            = makeThreadSafeFormatter(YYYY_MM_DD_HH_MM_SS_Z, null);


    private static ThreadLocal<SimpleDateFormat> makeThreadSafeFormatter(final String format,
                                                                         final String nullableTimeZone) {
        return new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                SimpleDateFormat result = new SimpleDateFormat(format);
                if (nullableTimeZone != null) {
                    result.setTimeZone(TimeZone.getTimeZone(nullableTimeZone));
                }
                return result;
            }
        };
    }
}