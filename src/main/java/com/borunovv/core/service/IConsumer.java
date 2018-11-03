package com.borunovv.core.service;

public interface IConsumer<T> {
    void consume(T obj);
}
