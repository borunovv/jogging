package com.borunovv.core.util;

public enum HttpMethod {
    GET, POST, PUT, DELETE, HEAD;

    public static HttpMethod fromString(String str) {
        for (HttpMethod m : HttpMethod.values()) {
            if (m.toString().equalsIgnoreCase(str)) {
                return m;
            }
        }
        throw new RuntimeException("Invalid http method name: '" + str + "'");
    }
}