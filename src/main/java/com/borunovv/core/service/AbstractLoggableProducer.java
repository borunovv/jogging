package com.borunovv.core.service;

import com.borunovv.core.log.Loggable;

public abstract class AbstractLoggableProducer<T> extends Loggable implements IProducer<T> {

    private final AbstractProducer<T> inner = new AbstractProducer<T>() {
        @Override
        protected void onConsumerNotifyError(IConsumer<? super T> consumer, T resource, Exception e) {
            AbstractLoggableProducer.this.logger.error(
                    "Failed to notify consumer '" + consumer.getClass().getSimpleName() + "', e");
        }
    };

    @Override
    public void subscribe(IConsumer<? super T> consumer) {
        inner.subscribe(consumer);
    }

    protected void notifyConsumers(T resource) {
        inner.notifyConsumers(resource);
    }
}
