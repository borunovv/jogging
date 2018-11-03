package com.borunovv.jogging.weather.model;

public class Weather {
    public int temperature;
    public int humidity;
    public String precipitation;

    public Weather(int temperature, int humidity, String precipitation) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.precipitation = precipitation;
    }

    @Override
    public String toString() {
        return "Weather{" +
                "temperature=" + temperature +
                ", humidity=" + humidity +
                ", precipitation='" + precipitation + '\'' +
                '}';
    }
}
