package com.borunovv.core.log.db.service;

import com.borunovv.core.log.db.dao.LoggerDao;
import com.borunovv.core.service.AbstractService;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AutoLogCleaner extends AbstractService implements DisposableBean, LoggerDao.IInterrupter {

    private long CLEAN_INTERVAL_MINUTES = 60;
    private int MAX_DAYS_TO_LIVE = 30;
    private long MAX_COUNT_TO_LIVE = 1000000;

    private volatile AtomicReference<State> currentState = new AtomicReference<State>(State.READY);
    private volatile AtomicLong lastCleanTime = new AtomicLong(System.currentTimeMillis());
    private volatile AtomicBoolean forceStartFlag = new AtomicBoolean(false);
    private volatile AtomicLong lastCleanDurationSec = new AtomicLong(-1);

    public AutoLogCleaner() {
    }

    public AutoLogCleaner(long intervalInMinutes, int maxDaysToLive, long maxRowCount) {
        this.CLEAN_INTERVAL_MINUTES = intervalInMinutes;
        this.MAX_DAYS_TO_LIVE = maxDaysToLive;
        this.MAX_COUNT_TO_LIVE = maxRowCount;
    }

    @PostConstruct
    public void create() {
        onCreate();
    }

    @Override
    public void destroy() throws Exception {
        onDestroy();
    }

    protected void onCreate() {
    }

    protected void onDestroy() {
        stop();
    }

    protected void start() {
        if (getState() != State.READY) {
            throw new IllegalStateException("Not in READY mode. Already running ?");
        }
        currentState.set(State.WAITING);
        new Thread(() -> {
            logger.trace("AutoLogCleaner: scheduler started. " + getShortStats());

            while (getState() != State.STOPPING) {
                try {
                    long currentTime = System.currentTimeMillis();
                    boolean timeToClean = (currentTime - lastCleanTime.get()) / 1000 / 60 >= CLEAN_INTERVAL_MINUTES;
                    if (timeToClean || forceStartFlag.get()) {
                        forceStartFlag.set(false);
                        doClean();
                        lastCleanDurationSec.set((System.currentTimeMillis() - currentTime) / 1000);
                        lastCleanTime.set(System.currentTimeMillis());
                        logger.debug("AutoLogCleaner: successful clean ("
                                + lastCleanDurationSec.get() + " sec).");
                    }
                } catch (Exception e) {
                    logger.error("AutoLogCleaner: Error in worker thread (ignored).", e);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            logger.trace("AutoLogCleaner: stopped");
            currentState.set(State.READY);
        }).start();
    }

    public void stop() {
        boolean isThreadRunning = getState() != State.READY;

        if (isThreadRunning) {
            currentState.set(State.STOPPING);
            while (getState() != State.READY) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
            currentState.set(State.READY);
        }
    }

    public void forceClean() {
        forceStartFlag.set(true);
    }

    public void doClean() {
        currentState.set(State.CLEANING);
        try {
            loggerDao.deleteOldByCount(MAX_COUNT_TO_LIVE, this);
            loggerDao.deleteOldByDays(MAX_DAYS_TO_LIVE, this);
        } catch (Exception e) {
            logger.error("AutoLogCleaner: Error in worker thread (ignored).", e);
        }
        currentState.set(State.WAITING);
    }

    @Override
    public boolean isNeedInterrupt() {
        return getState() == State.STOPPING;
    }

    private State getState() {
        return currentState.get();
    }

    public String getShortStats() {
        long delta = lastCleanDurationSec.get() >= 0 ?
                (System.currentTimeMillis() - lastCleanTime.get()) / 1000 / 60 :
                -1;
        String deltaStr = delta >= 0 ?
                "" + delta + " min ago" :
                "never";

        StringBuilder sb = new StringBuilder();
        sb.append("Last cleaned: ").append(deltaStr).append(" (duration: ").append(lastCleanDurationSec.get()).append(" sec)")
                .append(", State: ").append(currentState.get())
                .append(", Interval: ").append(CLEAN_INTERVAL_MINUTES).append(" min.")
                .append(", Max_rows: ").append(MAX_COUNT_TO_LIVE)
                .append(", Max_days: ").append(MAX_DAYS_TO_LIVE);
        return sb.toString();
    }

    public enum State {READY, WAITING, CLEANING, STOPPING}

    @Inject
    private LoggerDao loggerDao;
}
