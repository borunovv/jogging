package com.borunovv.core.util;

import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class TimeUtilsTest {

    @Test
    public void parse_and_format_YYYYMMDD_HHMMSS_GMT0() throws Exception {
        Date d = TimeUtils.parseDateTime_YYYYMMDD_HHMMSS_GMT0("2016-02-23 15:16:17");
        assertEquals("2016-02-23 15:16:17", TimeUtils.formatDateTime_YYYYMMDD_HHMMSS_GMT0(d));
    }

    @Test
    public void parseDateTime_YYYYMMDD_HHMMSS_Z() throws Exception {
        Date d = TimeUtils.parseDateTime_YYYYMMDD_HHMMSS_GMT0("2016-02-23 15:16:17 (GMT+02:00)");
        assertEquals(-120, d.getTimezoneOffset()); // -120 = +2h
        String formatted = TimeUtils.formatDateTime_YYYYMMDD_HHMMSS_Z(d);
        assertTrue(formatted.contains("(") && formatted.contains(")"));
    }

    @Test
    public void formatDateTime_YYYYMMDD_HHMMSS_Z_GMT0() throws Exception {
        Date d = TimeUtils.parseDateTime_YYYYMMDD_HHMMSS_GMT0("2016-02-23 15:16:17 (GMT+00:00)");
        assertEquals("2016-02-23 15:16:17 (GMT+00:00)", TimeUtils.formatDateTime_YYYYMMDD_HHMMSS_Z_GMT0(d));
    }

    @Test
    public void parseDate_YYYY_MM_DD_GMT0() throws Exception {
        Date d = TimeUtils.parseDateTime_YYYYMMDD_GMT0("2017-03-15");
        assertEquals("2017-03-15 00:00:00", TimeUtils.formatDateTime_YYYYMMDD_HHMMSS_GMT0(d));
    }
}