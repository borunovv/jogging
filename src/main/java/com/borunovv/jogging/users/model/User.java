package com.borunovv.jogging.users.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "user_profiles")
public class User implements Serializable {
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "created")
    private Date createTime = new Date();

    @Column(name = "login")
    private String login;

    @Column(name = "pass_hash")
    private String passHash;

    @Column(name = "rights")
    @Enumerated(value = EnumType.ORDINAL)
    private Rights rights;

    public User() {
    }

    public User(String login, String passHash, Rights rights) {
        this.login = login;
        this.passHash = passHash;
        this.rights = rights;
    }

    public long getId() {
        return id;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public String getLogin() {
        return login;
    }

    public String getPassHash() {
        return passHash;
    }

    public Rights getRights() {
        return rights;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }

    public void setRights(Rights rights) {
        this.rights = rights;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return login != null ? login.equals(user.login) : user.login == null;

    }

    @Override
    public int hashCode() {
        return login != null ? login.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User {" +
                "id=" + id +
                ", createTime=" + createTime +
                ", login='" + login + '\'' +
                ", passHash='" + passHash + '\'' +
                ", rights=" + rights +
                '}';
    }
}
