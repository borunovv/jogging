package com.borunovv.jogging.config;

import com.borunovv.core.util.ApplicationProperties;
import com.borunovv.core.util.ResourceManager;

/**
 * Умеет парсить файл application.properties и давать оттуда значения по ключу.
 */
public class JoggingApplicationProperties {

    private static final String APPLICATION_PROPERTIES_FILE_NAME = "application.properties";
    private static final String HTTP_SERVER_PORT = "http.server.port";
    private static final String HTTP_SERVER_LOGIN = "http.server.login";
    private static final String HTTP_SERVER_PASSWORD = "http.server.password";

    private ApplicationProperties properties;

    public JoggingApplicationProperties() {
        load();
    }

    public int getHttpPort() {
        ensureExist(HTTP_SERVER_PORT);
        return (int) properties.getLong(HTTP_SERVER_PORT, 0);
    }

    private void ensureExist(String key) {
        if (!properties.has(key)) {
            throw new IllegalStateException("Expected key '" + key
                    + "' in '" + APPLICATION_PROPERTIES_FILE_NAME + "' file.");
        }
    }

    private void load() {
        if (properties == null) {
            String content = ResourceManager.getTextFileContent(APPLICATION_PROPERTIES_FILE_NAME);
            properties = new ApplicationProperties(content);
        }
    }

    public String getHttpLogin() {
        ensureExist(HTTP_SERVER_LOGIN);
        return properties.get(HTTP_SERVER_LOGIN);
    }

    public String getHttpPassword() {
        ensureExist(HTTP_SERVER_PASSWORD);
        return properties.get(HTTP_SERVER_PASSWORD);
    }
}
