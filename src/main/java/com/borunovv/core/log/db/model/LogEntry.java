package com.borunovv.core.log.db.model;

import com.borunovv.core.log.LogLevel;
import org.apache.commons.lang.exception.ExceptionUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "logs")
public class LogEntry implements Serializable {

    private static final int MAX_TEXT_SIZE = 32 * 1024; // 32 Kb символов

    @Id
    @GeneratedValue
    private long id;

    @Column(name = "create_time")
    private Date createTime;

    @Column(name = "level")
    @Enumerated(value = EnumType.ORDINAL)
    private LogLevel level;

    @Column(name = "message")
    private String message;

    @Column(name = "exception")
    private String exception;

    @Transient
    private long uid;

    public static LogEntry getInstance() {
        LogEntry entry = new LogEntry();
        entry.setCreateTime(new Date());
        return entry;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public long getUid() {
        return uid;
    }
    public void setUid(long uid) {
        this.uid = uid;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = truncateTo(message, MAX_TEXT_SIZE);
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = truncateTo(exception, MAX_TEXT_SIZE);
    }

    public void setException(Throwable e) {
        if (e != null) {
            setException(ExceptionUtils.getStackTrace(e));
        } else {
            setException("");
        }
    }

    private String truncateTo(String str, int maxTextSize) {
        if (str == null) {
            return str;
        }
        if (str.length() > maxTextSize) {
            return str.substring(0, MAX_TEXT_SIZE);
        } else {
            return str;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LogEntry)) return false;

        LogEntry entry = (LogEntry) o;

        if (id != entry.id) return false;
        if (level != entry.level) return false;
        if (createTime != null ? !createTime.equals(entry.createTime) : entry.createTime != null) return false;
        if (exception != null ? !exception.equals(entry.exception) : entry.exception != null) return false;
        if (message != null ? !message.equals(entry.message) : entry.message != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (createTime != null ? createTime.hashCode() : 0);
        result = 31 * result + level.hashCode();
        result = 31 * result + (message != null ? message.hashCode() : 0);
        result = 31 * result + (exception != null ? exception.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogEntry");
        sb.append("{id=").append(id);
        sb.append(", createTime=").append(createTime);
        sb.append(", level=").append(level);
        sb.append(", message='").append(message).append('\'');
        sb.append(", exception='").append(exception).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
