package com.borunovv.core.server.nio.core.protocol;

import java.util.Collection;
import java.util.List;

public interface IDeliverable {
    void addDeliveryCallback(IDeliveryCallback callback);
    void addDeliveryCallbacks(Collection<? extends IDeliveryCallback> callbacks);
    List<IDeliveryCallback> getDeliveryCallbacks();

    void setStartDeliveryTime(long time);
    void setEndDeliveryTime(long messageSentTime);
    long getStartDeliveryTime();
    long getEndDeliveryTime();
    long getDeliveryTime();
    void setDeliverySizeInBytes(int sizeInBytes);
    int getDeliverySizeInBytes();

    // Для retry-стратегии.
    int incrementDeliveryAttemptsCount();
}
