package com.borunovv.core.log;

import com.borunovv.core.log.db.dao.LoggerDao;
import com.borunovv.core.log.db.model.LogEntry;
import com.borunovv.core.log.db.service.LogLevelService;
import com.borunovv.core.service.AbstractProducer;
import com.borunovv.core.service.IConsumer;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AsyncLoggerService extends AbstractProducer<List<LogEntry>> {

    private static final int MAX_QUEUE_SIZE = 5000;
    private static final long MAX_WAIT_QUEUE_TIME_SEC = 30;
    private static final int MAX_BATCH_SIZE = 100;
    private static final long WAIT_FULL_BATCH_TIME_MS = 10 * 1000;
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private volatile boolean isStopRequested = false;
    private long lastSaveTime = 0;
    private volatile int maxQueueSize;
    private ConcurrentLinkedQueue<LogEntry> queue = new ConcurrentLinkedQueue<>();
    private AtomicLong counter = new AtomicLong(0);

    @PostConstruct
    public void init() {
        startIfNeed();
    }

    @PreDestroy
    public void preDestroy() {
        stop();
    }

    public long put(LogLevel level, String msg, Throwable error) {
        if (!LogLevelService.isLogLevelAllowed(level)) {
            return 0;
        }

        boolean canPutToQueue = startIfNeed();

        if (! canPutToQueue) {
            LogEntry entry  = makeLogEntry(level, msg, error);
            loggerDao.save(entry);
            return entry.getUid();
        }

        long start = System.currentTimeMillis();
        while (queue.size() > MAX_QUEUE_SIZE) {
            if (!sleepOneMillisecond()) {
                return 0;
            }

            if (System.currentTimeMillis() - start > MAX_WAIT_QUEUE_TIME_SEC * 1000) {
                System.err.println(
                        "[ERROR] AsyncLoggerService can't put entry to queue (the queue is full) Entry is:\n"
                                + "Entry: [" + level + "] " + msg
                                + (error != null ? "\n" + ExceptionUtils.getStackTrace(error) : ""));
                return 0;
            }
        }

        return addLogEntryToQueue(level, msg, error);
    }

    private boolean sleepOneMillisecond() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }


    private LogEntry makeLogEntry(LogLevel level, String msg, Throwable error) {
        LogEntry entry = LogEntry.getInstance();
        entry.setLevel(level);

        long count = counter.incrementAndGet();
        long uid = getLogEntryUID(entry, count);
        entry.setUid(uid);
        entry.setMessage(msg + "\n\nThread [" + Thread.currentThread().getName() + "]\nUID: " + uid);

        if (error != null) {
            entry.setException(ExceptionUtils.getStackTrace(error));
        }

        return entry;
    }

    private long addLogEntryToQueue(LogLevel level, String msg, Throwable error) {
        LogEntry entry = makeLogEntry(level, msg, error);
        queue.add(entry);
        return entry.getUid();
    }

    private long getLogEntryUID(LogEntry entry, long globalEntryIndexFromProgramStart) {
        long shiftLeft = 10;
        long mask = (1L << shiftLeft) - 1;
        return (entry.getCreateTime().getTime() << shiftLeft)
                | (globalEntryIndexFromProgramStart & mask);
    }

    public void stop() {
        isStopRequested = true;
        while (isStarted.get()) {
            if (!sleepOneMillisecond()) {
                break;
            }
        }
    }

    private boolean startIfNeed() {
        if (isStarted.compareAndSet(false, true)) {
            if (isStopRequested) {
                isStarted.set(false);
                return false;
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (!isStopRequested) {
                            doIteration();
                            try {
                                Thread.sleep(queue.size() > 0 ? 1 : 10);
                            } catch (InterruptedException e) {
                                break;
                            }
                        }

                        while (!queue.isEmpty()) {
                            lastSaveTime = 0;
                            doIteration();
                        }
                    } finally {
                        isStarted.set(false);
                    }

                }
            }).start();
        }
        return isStarted.get();
    }


    private void doIteration() {
        try {
            if (!isCanSaveNextPortion() || queue.isEmpty()) {
                return;
            }

            maxQueueSize = Math.max(maxQueueSize, queue.size());
            List<LogEntry> batch = new ArrayList<>(MAX_BATCH_SIZE);

            for (int i = 0; i < MAX_BATCH_SIZE; ++i) {
                LogEntry entry = queue.poll();
                if (entry == null) {
                    break;
                }
                batch.add(entry);
            }

            loggerDao.saveBatch(batch);
            notifyConsumers(new ArrayList<>(batch));

            batch.clear();

            lastSaveTime = System.currentTimeMillis();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    protected void onConsumerNotifyError(IConsumer<? super List<LogEntry>> consumer, List<LogEntry> batch, Exception e) {
        put(LogLevel.ERROR, "Failed to notify consumer '" + consumer.getClass().getSimpleName() + "' about batch sent", e);
    }

    private boolean isCanSaveNextPortion() {
        return (System.currentTimeMillis() - lastSaveTime >= WAIT_FULL_BATCH_TIME_MS)
                || queue.size() >= MAX_BATCH_SIZE;
    }

    @Inject
    private LoggerDao loggerDao;
}
