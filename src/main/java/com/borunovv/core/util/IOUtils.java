package com.borunovv.core.util;

import java.io.*;

public final class IOUtils {

    private static final int DEFAULT_TEMP_BUFFER_SIZE = 8192;

    /**
     * Convert InputStream to String.
     */
    public static String inputStreamToString(InputStream inputStream) throws IOException {
        return StringUtils.toUtf8String(
                inputStreamToByteArray(inputStream));
    }

    /**
     * Convert InputStream to byte array input stream.
     * Note: auto close the stream.
     */
    public static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        byte[] temp = new byte[DEFAULT_TEMP_BUFFER_SIZE];
        ByteArrayOutputStream out = new ByteArrayOutputStream(
                inputStream.available() > 0 ?
                        inputStream.available() :
                        16);
        int l;
        while ((l = inputStream.read(temp)) != -1) {
            out.write(temp, 0, l);
        }

        close(inputStream);
        return out.toByteArray();
    }

    /**
     * Convert String to InputStream.
     */
    public static InputStream stringToInputStream(String str) {
        return new ByteArrayInputStream(
                StringUtils.uft8StringToBytes(str));
    }


    /**
     * Close stream.
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Can't close stream or channel: " + closeable, e);
        }
    }

    /**
     * Copy one stream to another.
     * Note: do not close any of streams.
     * You should care about it yourself !
     */
    public static void copy(InputStream input, OutputStream output) throws IllegalArgumentException, IOException {
        if (input == null || output == null) {
            throw new IllegalArgumentException("Bad input or output stream: null");
        }

        byte[] buf = new byte[DEFAULT_TEMP_BUFFER_SIZE];
        while (true) {
            int length = input.read(buf);
            if (length < 0) {
                break;
            }
            output.write(buf, 0, length);
        }
    }
}
