package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.server.nio.core.util.StoppableSleep;
import com.borunovv.core.util.Assert;
import org.apache.http.annotation.ThreadSafe;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ThreadSafe
public class ConcurrentMessageProcessor<T> {

    private int workerThreadsCount;
    private int taskQueueLimit;
    private long queueAddTaskWaitTimeoutMs;

    private volatile ExecutorService executor;
    private final ConcurrentLinkedQueue<T> taskQueue = new ConcurrentLinkedQueue<>();
    private final IMessageHandler<T> messageHandler;

    private volatile boolean stopRequested = false;


    public ConcurrentMessageProcessor(int workerThreadsCount,
                                      int taskQueueLimit,
                                      long queueAddTaskWaitTimeoutMs,
                                      IMessageHandler<T> messageHandler) {
        Assert.isTrue(workerThreadsCount > 0, "workerThreadsCount must be > 0");
        Assert.isTrue(taskQueueLimit > 0, "taskQueueLimit must be > 0");
        Assert.isTrue(queueAddTaskWaitTimeoutMs > 0, "queueAddTaskWaitTimeoutMs must be > 0");
        Assert.isTrue(messageHandler != null, "messageHandler is null");

        this.workerThreadsCount = workerThreadsCount;
        this.taskQueueLimit = taskQueueLimit;
        this.queueAddTaskWaitTimeoutMs = queueAddTaskWaitTimeoutMs;
        this.messageHandler = messageHandler;
    }

    public boolean queue(T task) {
        Assert.isTrue(task != null, "task is null");

        long startWait = System.currentTimeMillis();
        while (taskQueue.size() >= taskQueueLimit && !stopRequested) {
            stoppableWaitTimeout(10);
            if (System.currentTimeMillis() - startWait >= queueAddTaskWaitTimeoutMs) {
                messageHandler.onReject(task);
                return false;
            }
        }
        taskQueue.add(task);

        return true;
    }

    public void stop() {
        if (executor != null) {
            stopRequested = true;
            executor.shutdown();
            while (!executor.isTerminated()) {
                sleep(10);
            }
            stopRequested = false;
            executor = null;
        }
    }

    public void start() {
        stop();

        executor = Executors.newFixedThreadPool(workerThreadsCount);
        for (int i = 0; i < workerThreadsCount; ++i) {
            executor.submit(new Worker());
        }
    }

    public int getQueueSize() {
        return taskQueue.size();
    }


    private class Worker implements Runnable {
        private int timeoutToWait;

        @Override
        public void run() {
            while (!stopRequested) {
                T task = taskQueue.poll();
                if (task != null) {
                    // В очереди что-то есть, обработаем..
                    timeoutToWait = 0; // Сброс таймаута ожидания.
                    processTask(task);
                } else {
                    // Очередь пуста. Прогрессивный таймаут ожидания.
                    progressiveSleep();
                }
            }
        }

        private void progressiveSleep() {
            stoppableWaitTimeout(timeoutToWait);
            timeoutToWait = Math.min(100, timeoutToWait + 1);
        }

        private void processTask(T task) {
            try {
                messageHandler.handle(task);
            } catch (Exception e) {
                try {
                    messageHandler.onError(task, e);
                } catch (Exception e2) {
                    e2.printStackTrace(System.err);
                }
            }
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void stoppableWaitTimeout(int ms) {
        try {
            StoppableSleep.sleep(ms, () -> stopRequested);
        } catch (InterruptedException e) {
            messageHandler.onError(e);
            Thread.currentThread().interrupt();
        }
    }
}
