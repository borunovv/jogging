package com.borunovv.jogging.weather.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity(name="forecasts")
public class Forecast implements Serializable {

    public enum Status {PENDING, ERROR, DONE}

    @Id
    @GeneratedValue
    private long id;

    @Column(name = "`date`")
    private Date date;

    @Column(name = "location")
    private String location;

    @Column(name = "state")
    @Enumerated(EnumType.ORDINAL)
    private Status status;

    @Column(name = "temperature")
    private int temperature;

    @Column(name = "humidity")
    private int humidity;

    @Column(name = "precipitation")
    private String precipitation;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public String getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(String precipitation) {
        this.precipitation = precipitation;
    }

    public Weather getWeather() {
        return new Weather(temperature, humidity, precipitation);
    }

    @Override
    public String toString() {
        return "Forecast{" +
                "id=" + id +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", status=" + status +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", precipitation='" + precipitation + '\'' +
                '}';
    }
}
