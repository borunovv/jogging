package com.borunovv.jogging.timings.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

@Entity(name = "timings")
public class Timing implements Serializable {
    @Id
    @GeneratedValue
    private long id;

    @Column(name = "user_id")
    private long userId;

    @Column(name = "`date`")
    private Date date = new Date();

    @Column(name = "location")
    private String location;

    @Column(name = "distance_meters")
    private int distanceMeters;

    @Column(name = "time_minutes")
    private int timeMinutes;

    public Timing() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(int distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public int getTimeMinutes() {
        return timeMinutes;
    }

    public void setTimeMinutes(int timeMinutes) {
        this.timeMinutes = timeMinutes;
    }

    @Override
    public String toString() {
        return "Timing{" +
                "id=" + id +
                ", userId=" + userId +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", distanceMeters=" + distanceMeters +
                ", timeMinutes=" + timeMinutes +
                '}';
    }
}
