package com.borunovv.core.util;

import java.util.Calendar;
import java.util.Date;

public final class CalendarUtils {

    public final static long MILLIS_IN_SECOND = 1000L;
    public final static long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    public final static long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    public final static long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;


    public static Calendar current() {
        return Calendar.getInstance();
    }

    public static Calendar instance(Date date) {
        Calendar instance = Calendar.getInstance();
        instance.setTime(date);
        return instance;
    }

    public static Calendar addDays(Calendar calendar, int count) {
        return addCalendarUnit(calendar, count, Calendar.DAY_OF_WEEK);
    }

    public static Calendar addWeeks(Calendar calendar, int count) {
        return addCalendarUnit(calendar, count, Calendar.WEEK_OF_YEAR);
    }

    public static Calendar addMonths(Calendar calendar, int count) {
        return addCalendarUnit(calendar, count, Calendar.MONTH);
    }

    public static Calendar addYears(Calendar calendar, int count) {
        return addCalendarUnit(calendar, count, Calendar.YEAR);
    }

    public static Calendar addSeconds(Calendar calendar, int count) {
        return addCalendarUnit(calendar, count, Calendar.SECOND);
    }

    public static Calendar get(int year, int month, int day) {
        return get(year, month, day, 0, 0, 0);
    }

    // Месяц 1..12
    public static Calendar get(int year, int month, int day, int hour, int minute, int second) {
        Calendar res = Calendar.getInstance();
        res.setTimeInMillis(0);
        res.set(year, month - 1, day, hour, minute, second);
        return res;
    }

    private static Calendar addCalendarUnit(Calendar calendar, int count, int field) {
        Calendar res = Calendar.getInstance();
        res.setTime(calendar.getTime());
        res.add(field, count);
        return res;
    }
}
