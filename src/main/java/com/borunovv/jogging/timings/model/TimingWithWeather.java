package com.borunovv.jogging.timings.model;

import com.borunovv.jogging.weather.model.Weather;

import javax.annotation.Nullable;

public class TimingWithWeather {
    public long id;
    public String date;
    public String location;
    public Integer distanceMeters;
    public Integer timeMinutes;
    public Weather weather;

    public TimingWithWeather(long id, String date, String location,
                             int distanceMeters, int timeMinutes,
                             @Nullable Weather weather) {
        this.id = id;
        this.date = date;
        this.location = location;
        this.distanceMeters = distanceMeters;
        this.timeMinutes = timeMinutes;
        this.weather = weather;
    }
}
