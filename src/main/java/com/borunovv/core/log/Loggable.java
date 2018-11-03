package com.borunovv.core.log;

import com.borunovv.core.log.db.Log4jDBAppender;
import com.borunovv.core.log.db.service.LogLevelService;
import com.borunovv.core.util.Assert;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Loggable {
    protected MyLogger logger = new MyLogger(LoggerFactory.getLogger(this.getClass()));

    public static <T> MyLogger createLogger(Class<T> clazz) {
        return new MyLogger(LoggerFactory.getLogger(clazz));
    }

    public static class MyLogger {
        private Logger log4jLogger;

        private MyLogger(Logger log4jLogger) {
            Assert.isTrue(log4jLogger != null, "log4jLogger is null");
            this.log4jLogger = log4jLogger;
        }

        public String getName() {
            return log4jLogger.getName();
        }

        public void trace(String msg) {
            log4jLogger.trace(msg);
        }

        public void trace(String format, Object arg) {
            log4jLogger.trace(format, arg);
        }

        public void trace(String format, Object[] argArray) {
            log4jLogger.trace(format, argArray);
        }

        public void trace(String msg, Throwable t) {
            log4jLogger.trace(msg, t);
        }


        public void debug(String msg) {
            log4jLogger.debug(msg);
        }

        public void debug(String format, Object arg) {
            log4jLogger.debug(format, arg);
        }

        public void debug(String format, Object[] argArray) {
            log4jLogger.debug(format, argArray);
        }

        public void debug(String msg, Throwable t) {
            log4jLogger.debug(msg, t);
        }


        public void info(String msg) {
            log4jLogger.info(msg);
        }

        public void info(String format, Object arg) {
            log4jLogger.info(format, arg);
        }

        public void info(String format, Object[] argArray) {
            log4jLogger.info(format, argArray);
        }

        public void info(String msg, Throwable t) {
            log4jLogger.info(msg, t);
        }


        public void warn(String msg) {
            log4jLogger.warn(msg);
        }

        public void warn(String format, Object arg) {
            log4jLogger.warn(format, arg);
        }

        public void warn(String format, Object[] argArray) {
            log4jLogger.warn(format, argArray);
        }

        public void warn(String msg, Throwable t) {
            log4jLogger.warn(msg, t);
        }

        public long error(String msg) {
            log4jLogger.error(msg);
            return getLastDBErrorId(Level.ERROR);
        }

        public long error(String format, Object arg) {
            log4jLogger.error(format, arg);
            return getLastDBErrorId(Level.ERROR);
        }

        public long error(String format, Object[] argArray) {
            log4jLogger.error(format, argArray);
            return getLastDBErrorId(Level.ERROR);
        }

        public long error(String msg, Throwable t) {
            log4jLogger.error(msg, t);
            return getLastDBErrorId(Level.ERROR);
        }

        private long getLastDBErrorId(Level level) {
            if (LogLevelService.isLogLevelAllowed(level)) {
                return Log4jDBAppender.getLastLogEntryUid();
            } else {
                return 0;
            }
        }
    }
}
