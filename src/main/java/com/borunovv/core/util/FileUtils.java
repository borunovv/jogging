package com.borunovv.core.util;

import java.io.*;

public final class FileUtils {

    public static void saveTextFile(String path, String content) throws IOException {
        try (FileWriter writer = new FileWriter(path)) {
            writer.append(content);
            writer.flush();
        }
    }

    public static void saveBinaryFile(String name, byte[] data) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(name);
            out.write(data);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
