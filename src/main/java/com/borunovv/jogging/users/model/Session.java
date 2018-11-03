package com.borunovv.jogging.users.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "sessions")
public class Session implements Serializable {
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "created")
    private Date createTime = new Date();

    @Column(name = "user_id")
    private long userId;

    @Column(name = "code")
    private String code;

    public Session() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String session) {
        this.code = session;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Session session1 = (Session) o;

        return code != null ? code.equals(session1.code) : session1.code == null;

    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", createTime=" + createTime +
                ", userId=" + userId +
                ", code='" + code + '\'' +
                ", code='" + getCode() + '\'' +
                '}';
    }
}
