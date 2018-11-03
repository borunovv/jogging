package com.borunovv.core.server.nio.core.session;

public interface ISessionContext {
    void put(String key, Object value);
    Object putIfAbsent(String key, Object value);
    Object get(String key);
}
