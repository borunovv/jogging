package com.borunovv.core.server.nio.core.cooldown;

public interface CooldownListener<T> {
    void onCooldownFinished(T item);
    void onCooldownError(Throwable error);
}
