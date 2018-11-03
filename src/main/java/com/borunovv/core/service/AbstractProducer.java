package com.borunovv.core.service;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractProducer<T> implements IProducer<T> {

    private final ConcurrentHashMap<IConsumer<? super T>, Boolean> consumers = new ConcurrentHashMap<>();

    @Override
    public void subscribe(IConsumer<? super T> consumer) {
        consumers.put(consumer, true);
    }

    public void notifyConsumers(T resource) {
        for (IConsumer<? super T> consumer : consumers.keySet()) {
            notifyConsumer(consumer, resource);
        }
    }

    private void notifyConsumer(IConsumer<? super T> consumer, T resource) {
        try {
            consumer.consume(resource);
        } catch (Exception e) {
            onConsumerNotifyError(consumer, resource, e);
        }
    }

    protected abstract void onConsumerNotifyError(IConsumer<? super T> consumer, T resource, Exception e);
}
