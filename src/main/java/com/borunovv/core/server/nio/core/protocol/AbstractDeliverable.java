package com.borunovv.core.server.nio.core.protocol;

import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractDeliverable implements IDeliverable {

    private List<IDeliveryCallback> deliveryCallbacks = new CopyOnWriteArrayList<>();

    private AtomicInteger processAttemptsCount = new AtomicInteger(0);
    // Время попадания в очередь на отправку/или момент прихода сообщений из сокета.
    private volatile long startDeliveryTime = 0;
    // Время фактической доставки клиенту / или доставки в сервис от клиента.
    private volatile long endDeliveryTime = 0;
    // Кол-во байт, переданной или полученной из сети при получении/передаче данного сообщения.
    // Нужно для учета сетевого трафика.
    private int deliverySizeInBytes = 0;

    @Override
    public void addDeliveryCallback(IDeliveryCallback deliveryCallback) {
        Assert.notNull(deliveryCallback, "deliveryCallback is null");
        deliveryCallbacks.add(deliveryCallback);
    }

    @Override
    public void addDeliveryCallbacks(Collection<? extends IDeliveryCallback> callbacks) {
        Assert.notNull(callbacks, "deliveryCallback is null");
        deliveryCallbacks.addAll(callbacks);
    }

    @Override
    public List<IDeliveryCallback> getDeliveryCallbacks() {
        return deliveryCallbacks;
    }

    @Override
    public void setEndDeliveryTime(long endDeliveryTime) {
        this.endDeliveryTime = endDeliveryTime;
    }

    @Override
    public void setStartDeliveryTime(long time) {
        this.startDeliveryTime = time;
    }

    @Override
    public long getStartDeliveryTime() {
        return this.startDeliveryTime;
    }

    @Override
    public long getEndDeliveryTime() {
        return this.endDeliveryTime;
    }

    @Override
    public int incrementDeliveryAttemptsCount() {
        return processAttemptsCount.incrementAndGet();
    }

    @Override
    public long getDeliveryTime() {
        if (startDeliveryTime > 0
                && endDeliveryTime >= startDeliveryTime) {
            return endDeliveryTime - startDeliveryTime;
        } else {
            return -1;
        }
    }

    @Override
    public void setDeliverySizeInBytes(int sizeInBytes) {
        deliverySizeInBytes = sizeInBytes;
    }

    @Override
    public int getDeliverySizeInBytes() {
        return deliverySizeInBytes;
    }
}
