package com.borunovv.core.server.nio.core.protocol;

public interface IDeliveryCallback {
    void onSentSuccess();
    void onSentFailed(Throwable cause);
}
