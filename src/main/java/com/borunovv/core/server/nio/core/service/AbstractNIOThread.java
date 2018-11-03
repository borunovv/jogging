package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.log.Loggable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

abstract class AbstractNIOThread extends Loggable {
    private static int THREAD_PRIORITY_AVG_BETWEEN_NORM_AND_MAX =
            (Thread.NORM_PRIORITY + Thread.MAX_PRIORITY) / 2;


    private AtomicReference<NIOThreadState> myState = new AtomicReference<>(NIOThreadState.STOPPED);
    private AtomicLong errorsInThreadIteration = new AtomicLong(0);
    private AtomicReference<Throwable> lastErrorInThreadIteration = new AtomicReference<>(null);
    private AtomicReference<Throwable> lastThreadError = new AtomicReference<>(null);

    // Стартует, не дожидаясь запуска потока. Сразу вернет управление.
    public void startAsync() {
        boolean success = myState.compareAndSet(NIOThreadState.STOPPED, NIOThreadState.STARTING);
        if (!success) {
            throw new IllegalStateException("NIO thread not in STOPPED state. Actual state is: " + getState());
        }

        startWorkerThread();
    }

    // Стартует и ждет запуска потока.
    public boolean start() {
        startAsync();
        while (getState() == NIOThreadState.STARTING) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                stop();
                Thread.currentThread().interrupt();
            }
        }
        return getState() == NIOThreadState.RUNNING;
    }

    public void stop() {
        if (getState() != NIOThreadState.STOPPED) {
            setState(NIOThreadState.STOPPING);
            while (getState() != NIOThreadState.STOPPED) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public NIOThreadState getState() {
        return myState.get();
    }

    public boolean isStopped() {
        return getState() == NIOThreadState.STOPPED;
    }

    public boolean isRunning() {
        return getState() == NIOThreadState.RUNNING;
    }

    protected void setState(NIOThreadState newState) {
        myState.set(newState);
    }

    protected boolean isStopRequested() {
        return getState() == NIOThreadState.STOPPING;
    }

    private void startWorkerThread() {
        Thread t = new Thread(() -> {
            try {
                onThreadStart();
                // Важно, чтобы было после onThreadStart - чтобы убедиться, что все проинициализировалось.
                setState(NIOThreadState.RUNNING);
                while (!isStopRequested()) {
                    try {
                        onThreadIteration();
                    } catch (Throwable e) {
                        errorsInThreadIteration.incrementAndGet();
                        lastErrorInThreadIteration.set(e);
                        try {
                            logger.error("NIO Thread: ERROR while onThreadIteration (ignored)", e);
                        } catch (Throwable fuck) {
                            System.err.println("NIO Thread: ERROR while onThreadIteration.");
                            fuck.printStackTrace(System.err);
                        }
                    }
                }
            } catch (Throwable e) {
                lastThreadError.set(e);
                onThreadError(e);
            } finally {
                try {
                    onThreadStop();
                } catch (Exception e) {
                    logger.error("NIO Thread: Error while stopping NIO Thread.", e);
                } finally {
                    setState(NIOThreadState.STOPPED);
                }
            }
        });
        t.setPriority(getThreadPriority());
        t.start();
    }

    // Шаблонный метод. Переопределяется в потомках.
    // Вернет приоритет потока.
    protected int getThreadPriority() {
        return THREAD_PRIORITY_AVG_BETWEEN_NORM_AND_MAX;
    }


    // Шаблонный метод.
    // Переопределяется в потомках.
    // Вызывается в отдельном потоке перед входом в цикл итераций: while(!stopRequested).
    protected abstract void onThreadStart();


    // Шаблонный метод.
    // Переопределяется в потомках.
    // Вызывается периодически в отдельном потоке внутри цикла итераций: while(!stopRequested).
    protected abstract void onThreadIteration();

    // Шаблонный метод.
    // Переопределяется в потомках.
    // Вызывается после выхода из цикла итераций: while(!stopRequested).
    protected abstract void onThreadStop();


    // Шаблонный метод.
    // Переопределяется в потомках. Имеет поведение по умолчанию.
    // Вызывается после выхода из цикла итераций в случае ошибки.
    protected void onThreadError(Throwable e) {
        logger.error("NIO Thread: Error.", e);
    }
}
