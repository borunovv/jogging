package com.borunovv.core.server.nio.core.cooldown;

import java.util.concurrent.atomic.AtomicBoolean;


public class CooldownAsyncNotifier<T> {

    private CooldownQueue<Wrapper<T>> queue = new CooldownQueue<>();
    private AtomicBoolean started = new AtomicBoolean(false);
    private AtomicBoolean threadStopped = new AtomicBoolean(true);


    public void start() {
        if (started.compareAndSet(false, true)) {
            threadStopped.set(false);
            new Thread(() -> {
                try {
                    while (started.get()) {
                        doIteration();
                    }
                } finally {
                    threadStopped.set(true);
                }
            }).start();
        }
    }

    public void stop() {
        if (started.compareAndSet(true, false)) {
            while (!threadStopped.get()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void put(T item, long cooldownDelayMilliseconds, CooldownListener<T> listener) {
        if (!started.get()) {
            throw new IllegalStateException("Notifier not started. Use start() method before put()");
        }

        queue.put(new Wrapper<>(item, listener),
                cooldownDelayMilliseconds);
    }

    public int size() {
        return queue.size();
    }

    public void clear() {
        queue.clear();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    private void doIteration() {
        Wrapper<T> wrapper = null;
        try {
            wrapper = queue.poll();
            if (wrapper != null) {
                wrapper.listener.onCooldownFinished(wrapper.item);
            } else {
                waitNext();
            }
        } catch (Exception e) {
            try {
                if (wrapper != null) {
                    wrapper.listener.onCooldownError(e);
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    private void waitNext() {
        try {
            while (started.get() && queue.getMinDelayToNextItem() != 0) {
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }


    private static class Wrapper<Q> {
        final Q item;
        final CooldownListener<Q> listener;

        Wrapper(Q item, CooldownListener<Q> listener) {
            this.item = item;
            this.listener = listener;
        }
    }
}

