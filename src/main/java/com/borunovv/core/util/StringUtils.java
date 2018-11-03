package com.borunovv.core.util;

import java.io.UnsupportedEncodingException;

public final class StringUtils {

    public static String toUtf8String(byte[] data) {
        return toUtf8String(data, 0, data.length);
    }

    public static String toUtf8String(byte[] data, int offset, int length) {
        try {
            return new String(data, offset, length, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to convert bytes to utf-8", e);
        }
    }

    public static byte[] uft8StringToBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Can't convert utf8 string to bytes", e);
        }
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static String ensureString(String source) {
        return source != null ?
                source :
                "";
    }

    public static String unquote(String value) {
        if (value == null || value.length() < 2) return value;
        if ((value.startsWith("'") && value.endsWith("'"))
                || (value.startsWith("\"") && value.endsWith("\"")))
            return value.substring(1, value.length() - 2);
        return value;
    }
}
