package com.borunovv.core.log.db;

import com.borunovv.core.log.AsyncLoggerService;
import com.borunovv.core.log.LogLevel;
import com.borunovv.core.log.db.service.LogLevelService;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Service
@Lazy(false)
public class Log4jDBAppender extends AppenderSkeleton {

    private static ThreadLocal<Long> lastLogEntryUid = new ThreadLocal<>();


    @PostConstruct
    public void init() {
        Logger packageLogger = Logger.getLogger("com.borunovv");
        packageLogger.addAppender(this);
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        Level level = loggingEvent.getLevel();

        if (!LogLevelService.isLogLevelAllowed(level)) {
            return;
        }

        Throwable cause = loggingEvent.getThrowableInformation() != null ?
                loggingEvent.getThrowableInformation().getThrowable() :
                null;

        String msg = loggingEvent.getMessage().toString();

        long logEntryUid = asyncLoggerService.put(getNearestLogLevel(level), msg, cause);
        lastLogEntryUid.set(logEntryUid);
    }

    private LogLevel getNearestLogLevel(Level log4jLevel) {
        if (log4jLevel.isGreaterOrEqual(Level.ERROR)) {
            return LogLevel.ERROR;

        } else if (log4jLevel.isGreaterOrEqual(Level.WARN)) {
            return LogLevel.WARNING;

        } else if (log4jLevel.isGreaterOrEqual(Level.INFO)) {
            return LogLevel.INFO;
        }
        return LogLevel.DEBUG;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {
    }

    public static long getLastLogEntryUid() {
        Long id = lastLogEntryUid != null ?
                lastLogEntryUid.get() :
                null;

        return id != null ?
                id :
                0L;
    }

    @Inject
    private AsyncLoggerService asyncLoggerService;
}