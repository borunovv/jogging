package com.borunovv.jogging.weather.service;

import com.borunovv.core.service.AbstractServiceWithOwnThread;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.StringUtils;
import com.borunovv.core.util.TimeUtils;
import com.borunovv.jogging.timings.model.Timing;
import com.borunovv.jogging.timings.service.TimingService;
import com.borunovv.jogging.weather.dao.ForecastDao;
import com.borunovv.jogging.weather.model.Forecast;
import com.borunovv.jogging.weather.model.Weather;
import com.borunovv.jogging.weather.provider.ForecastProviderManager;
import com.borunovv.jogging.weather.provider.common.ForecastTask;
import com.borunovv.jogging.weather.provider.common.IForecastTaskSupplier;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class WeatherService extends AbstractServiceWithOwnThread
        implements TimingService.ITimingServiceEventsListener, IForecastTaskSupplier {

    private enum State {LOADING_CACHE, NORMAL_WORKING}

    private final ConcurrentHashMap<String, Weather> cache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<ForecastTask> forecastRequestQueue = new ConcurrentLinkedQueue<>();

    private volatile State state = State.LOADING_CACHE;

    @Override
    protected void onInit() {
        timingService.subscribe(this);
        forecastProviderManager.setTaskSupplier(this);
        start();
    }

    @Override
    public ForecastTask poll() {
        return forecastRequestQueue.poll();
    }


    @Override
    protected void doThreadIteration() throws InterruptedException {
        if (state == State.LOADING_CACHE) {
            loadCache();
            state = State.NORMAL_WORKING;
        }
        sleep(1000);
    }

    private void loadCache() {
        List<Forecast> lastMonth = dao.findAllFromDate(DateTime.now().minusMonths(1).toDate());
        for (Forecast forecast : lastMonth) {
            if (isStopRequested()) break;
            if (forecast.getStatus() == Forecast.Status.DONE) {
                cache.put(toKey(forecast.getDate(), forecast.getLocation()), forecast.getWeather());
            } else {
                forecastRequestQueue.add(new ForecastTask(forecast.getDate(), forecast.getLocation()));
            }
        }
    }

    public Weather getWeather(Date date, String location) {
        return cache.get(toKey(date, location));
    }

    private String toKey(Date date, String location) {
        return TimeUtils.formatDate_YYYYMMDD_GMT0(date) + location.trim().toLowerCase().replace(" ", "");
    }

    @Override
    public void onTimingCreated(Timing timing) {
        scheduleForecastRequestIfNeed(timing.getDate(), timing.getLocation());
    }

    @Override
    public void onTimingUpdated(Timing timing) {
        scheduleForecastRequestIfNeed(timing.getDate(), timing.getLocation());
    }

    private void scheduleForecastRequestIfNeed(Date date, String location) {
        if (getWeather(date, location) == null) {
            scheduleForecastRequest(date, location);
        }
    }

    private void scheduleForecastRequest(Date date, String location) {
        Assert.notNull(date, "Bad date: null");
        Assert.isTrue(!StringUtils.isNullOrEmpty(location), "Bad location: null or empty");

        if (!dao.exists(date, location)) {
            Forecast forecast = new Forecast();
            forecast.setStatus(Forecast.Status.PENDING);
            forecast.setDate(date);
            forecast.setLocation(location);
            dao.ensureExists(forecast);
            forecastRequestQueue.add(new ForecastTask(date, location));
        }
    }

    @Override
    public void onForecast(ForecastTask task, Weather weather) {
        Date date = task.date;
        String location = task.location;

        boolean isError = (weather == null);
        Forecast forecast = new Forecast();
        forecast.setStatus(isError ? Forecast.Status.ERROR : Forecast.Status.DONE);
        forecast.setDate(date);
        forecast.setLocation(location);
        if (weather != null) {
            forecast.setTemperature(weather.temperature);
            forecast.setHumidity(weather.humidity);
            forecast.setPrecipitation(weather.precipitation);
        }
        dao.ensureExists(forecast);

        if (weather != null) {
            cache.put(toKey(date, location), weather);
        }
    }

    @Inject
    private TimingService timingService;
    @Inject
    private ForecastDao dao;
    @Inject
    private ForecastProviderManager forecastProviderManager;
}
