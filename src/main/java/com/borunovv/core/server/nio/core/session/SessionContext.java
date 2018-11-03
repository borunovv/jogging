package com.borunovv.core.server.nio.core.session;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Потокобезопасное Key-value хранилище, ассоциированное с сессией.
 * Используется более высокоуровневыми игровыми сервисами
 * для хранения ассоциированных данных.
 */
@ThreadSafe
public class SessionContext implements ISessionContext {

    private ConcurrentHashMap<String, Object> dataMap = new ConcurrentHashMap<String, Object>();

    @Override
    public void put(String key, Object value) {
        dataMap.put(key, value);
    }

    @Override
    public Object get(String key) {
        return dataMap.get(key);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return dataMap.putIfAbsent(key, value);
    }
}
