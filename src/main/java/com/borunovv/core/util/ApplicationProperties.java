package com.borunovv.core.util;

import java.util.HashMap;
import java.util.Map;

public final class ApplicationProperties {

    private Map<String, String> props;

    public ApplicationProperties(String propertiesFileContent) {
        props = parse(propertiesFileContent);
    }

    public String get(String key) {
        return get(key, "");
    }

    public String get(String key, String defaultValue) {
        return has(key) ?
                props.get(key) :
                defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        return has(key) ?
                Long.parseLong(get(key)) :
                defaultValue;
    }

    public boolean has(String key) {
        return props.containsKey(key);
    }

    private Map<String, String> parse(String content) {
        Map<String, String> props = new HashMap<>();
        String[] rawLines = content.split("\\n");
        for (String line : rawLines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                int delimIndex = trimmedLine.indexOf('=');
                if (delimIndex > 0) {
                    String key = trimmedLine.substring(0, delimIndex).trim();
                    String value = trimmedLine.substring(delimIndex + 1).trim();

                    if (!key.isEmpty()) {
                        props.put(key, value);
                    }
                }
            }
        }
        return props;
    }
}
