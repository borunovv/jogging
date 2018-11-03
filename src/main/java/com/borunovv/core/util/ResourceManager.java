package com.borunovv.core.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ResourceManager {

    /**
     * Вернет файл из директории ресурсов.
     *
     * @param relativeResourcesFilePath can be like 'subdir/file.ext'.
     * @return binary file from resources directory as stream or null on error.
     * @throws IllegalArgumentException Note: the returned stream is already buffered.
     */
    public static InputStream getFile(String relativeResourcesFilePath) throws IllegalArgumentException {
        InputStream resourceStream =
                ResourceManager.class.getClassLoader().getResourceAsStream(
                        relativeResourcesFilePath);

        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource file not found: '"
                    + relativeResourcesFilePath + "'.");
        }

        return new BufferedInputStream(resourceStream);
    }

    /**
     * Вернет содержимое текстового файла в UTF-8 из ресурсов.
     */
    public static String getTextFileContent(String relativeResourcesFilePath) {
        return StringUtils.toUtf8String(getFileContent(relativeResourcesFilePath));
    }

    /**
     * Вернет содержимое файла.
     */
    public static byte[] getFileContent(String relativeResourcesFilePath) {
        try {
            return IOUtils.inputStreamToByteArray(getFile(relativeResourcesFilePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to obtain resource file content '"
                    + relativeResourcesFilePath + "'", e);
        }
    }
}