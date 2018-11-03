package com.borunovv.core.util;

import com.google.gson.Gson;

public class JsonUtils {

    public static String toJson(Object obj) {
        return new Gson().toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return new Gson().fromJson(json, clazz);
    }
}
