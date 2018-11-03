package com.borunovv.core.log;

import com.borunovv.core.util.Assert;

public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3),
    DISABLED(4);

    LogLevel(int level) {
        this.level = level;
    }

    public boolean accepts(LogLevel logLevel) {
        return logLevel.level >= this.level;
    }

    public static LogLevel valueOf(int level) {
        Assert.isTrue(level >= 0 && level < values().length, "Bad log level: " + level);
        return values()[level];
    }

    public int toInteger() {
        return ordinal();
    }

    private final int level;
}






