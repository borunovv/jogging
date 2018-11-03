package com.borunovv.core.log.db.service;

import com.borunovv.core.log.LogLevel;
import org.apache.log4j.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class LogLevelService {

    private static volatile LogLevel logLevel = LogLevel.WARNING;

    @PostConstruct
    public void init() {
        logLevel = LogLevel.valueOf(defaultLogLevel);
    }

    public static boolean isLogLevelAllowed(LogLevel logLevel) {
        return getLogLevel().accepts(logLevel);
    }

    public static boolean isLogLevelAllowed(Level log4jLevel) {
        switch (getLogLevel()) {
            case DEBUG:
                return true;
            case INFO:
                return log4jLevel.isGreaterOrEqual(Level.INFO);
            case WARNING:
                return log4jLevel.isGreaterOrEqual(Level.WARN);
            case ERROR:
                return log4jLevel.isGreaterOrEqual(Level.ERROR);
            default:
                return false;
        }
    }

    public static LogLevel getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(LogLevel newLevel) {
        logLevel = newLevel;
    }

    @Value("${default.db.log.level}")
    private int defaultLogLevel;
}
