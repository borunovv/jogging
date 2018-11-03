package com.borunovv.jogging.weather.provider.common;

import java.util.Date;

public class ForecastTask {
    public final Date date;
    public final String location;

    public ForecastTask(Date date, String location) {
        this.date = date;
        this.location = location;
    }
}
