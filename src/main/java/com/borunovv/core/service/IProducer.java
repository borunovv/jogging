package com.borunovv.core.service;

public interface IProducer<T> {
    void subscribe(IConsumer<? super T> consumer);
}
