package com.borunovv.core.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractServiceWithOwnThread extends AbstractService {

    private volatile boolean stopRequested = false;
    private AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    private void init() {
        onInit();
    }

    @PreDestroy
    private void preDestroy() {
        onDestroy();
        stop();
    }

    protected void onInit() {
    }

    protected void onDestroy() {
    }

    protected abstract void doThreadIteration() throws InterruptedException;

    protected void onThreadError(Exception e) {
        logger.error("Error while in service worker thread iteration.", e);
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            try {
                new Thread(() -> {
                    try {
                        while (!stopRequested) {
                            try {
                                doThreadIteration();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            } catch (Exception e) {
                                try {
                                    onThreadError(e);
                                } catch (Exception e2) {
                                    logger.error("Error while error handling, wtf?", e2);
                                }
                            }
                        }
                    } finally {
                        running.set(false);
                    }

                }).start();
            } catch (Exception e) {
                running.set(false);
                throw e;
            }
        }
    }

    public void stop() {
        if (running.get()) {
            stopRequested = true;
            while (isRunning()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }


    protected void sleep(long timeoutMs) {
        long start = System.currentTimeMillis();
        while (!stopRequested && System.currentTimeMillis() - start < timeoutMs) {
            try {
                Thread.sleep(Math.min(timeoutMs, 50));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    protected boolean isStopRequested() {
        return stopRequested;
    }
}
