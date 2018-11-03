package com.borunovv.core.server.nio.core.cooldown;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class CooldownQueue<T> {

    private DelayQueue<ItemWrapper<T>> queue = new DelayQueue<>();

    public void put(T item, long cooldownDelayMilliseconds) {
        queue.add(new ItemWrapper<>(item, cooldownDelayMilliseconds));
    }

    public T poll() {
        ItemWrapper<T> next = queue.poll();
        return next != null ?
                next.item :
                null;
    }

    public long getMinDelayToNextItem() {
        ItemWrapper<T> next = queue.peek();
        return next != null ?
                Math.max(0, next.getDelay(TimeUnit.MILLISECONDS)) :
                -1;
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
    }


    private static class ItemWrapper<Q> implements Delayed {
        public final Q item;
        private final long startTimeMs;

        ItemWrapper(Q item, long cooldownMillisecondsFromNow) {
            this.item = item;
            this.startTimeMs = System.currentTimeMillis() + cooldownMillisecondsFromNow;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(startTimeMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.startTimeMs, ((ItemWrapper<Q>) o).startTimeMs);
        }
    }
}
