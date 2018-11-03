package com.borunovv.jogging.weather.provider.common;

import com.borunovv.jogging.weather.model.Weather;

public interface IForecastTaskSupplier {
    ForecastTask poll();

    void onForecast(ForecastTask task, Weather weather);
}
