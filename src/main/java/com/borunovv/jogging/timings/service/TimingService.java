package com.borunovv.jogging.timings.service;

import com.borunovv.core.hibernate.exception.DataNotFoundException;
import com.borunovv.core.service.AbstractService;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.StringUtils;
import com.borunovv.core.util.TimeUtils;
import com.borunovv.jogging.config.Constants;
import com.borunovv.jogging.timings.dao.TimingDao;
import com.borunovv.jogging.timings.filter.FilterCompiler;
import com.borunovv.jogging.timings.model.Timing;
import com.borunovv.jogging.timings.model.TimingWithWeather;
import com.borunovv.jogging.weather.model.Weather;
import com.borunovv.jogging.weather.service.WeatherService;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TimingService extends AbstractService {

    private final List<ITimingServiceEventsListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(ITimingServiceEventsListener listener) {
        Assert.notNull(listener, "Listener is null");
        listeners.add(listener);
    }

    public long addTiming(long userId, Date date, String location, long distance, long time) {
        validate(userId, date, location, distance, time);

        Timing t = new Timing();
        t.setLocation(location);
        t.setDate(date);
        t.setDistanceMeters((int) distance);
        t.setTimeMinutes((int) time);

        dao.save(t);
        notifyListenersTimingCteated(t);

        return t.getId();
    }

    public Timing ensureExists(long timingId) {
        try {
            return dao.get(timingId);
        } catch (DataNotFoundException e) {
            throw new RuntimeException("Not found timing #" + timingId, e);
        }
    }

    public void update(Timing timing) {
        validate(timing.getUserId(), timing.getDate(), timing.getLocation(),
                timing.getDistanceMeters(), timing.getTimeMinutes());

        int totalMinutesForDate = dao.getTotalTimeForDate(timing.getUserId(), timing.getDate(), timing.getId());
        Assert.isTrue(totalMinutesForDate + timing.getTimeMinutes() <= Constants.MINUTES_IN_DAY,
                "Day limit exceeded (maximum 24h of jogging per day please)");

        dao.save(timing);
        notifyListenersTimingUpdated(timing);
    }

    public void delete(Timing timing) {
        dao.delete(timing);
    }

    public List<TimingWithWeather> findAllWithWeather(long userId, String filter, long offset, long count) {
        List<TimingWithWeather> result = new ArrayList<>();
        // TODO
        if (count > 0) {
            String sqlWhereClause = filterCompiler.compileFilterToSQL(filter);
            List<Timing> timings = dao.find(userId, sqlWhereClause, offset, count);
            for (Timing timing : timings) {
                Weather weather = weatherService.getWeather(timing.getDate(), timing.getLocation());
                result.add(new TimingWithWeather(
                        timing.getId(),
                        TimeUtils.formatDate_YYYYMMDD_GMT0(timing.getDate()),
                        timing.getLocation(),
                        timing.getDistanceMeters(),
                        timing.getTimeMinutes(),
                        weather));
            }
        }
        return result;
    }

    private void validate(long userId, Date date, String location, long distance, long time) {
        Assert.isTrue(userId > 0, "Bad userId: " + userId);
        Assert.notNull(date, "Bad date: null");
        Assert.isTrue(new DateTime(date).compareTo(DateTime.now().plusDays(1)) < 0,
                "Bad date: dates from the future not allowed");
        Assert.isTrue(new DateTime(date).compareTo(DateTime.now().plusMonths(-1)) > 0,
                "Bad date: dates older than 1 month ago not allowed");
        Assert.isTrue(!StringUtils.isNullOrEmpty(location), "Bad location: empty or null");

        Assert.isTrue(distance > 0 && distance <= Constants.MAX_DISTANCE_HUMAN_CAN_WALK_A_DAY,
                "Expected distance (in meters 1.." + Constants.MAX_DISTANCE_HUMAN_CAN_WALK_A_DAY + ")");

        Assert.isTrue(time > 0 && time <= Constants.MINUTES_IN_DAY,
                "Expected time (in minutes 0.." + Constants.MINUTES_IN_DAY + ")");
    }

    private void notifyListenersTimingCteated(Timing timing) {
        for (ITimingServiceEventsListener listener : listeners) {
            listener.onTimingCreated(timing);
        }
    }

    private void notifyListenersTimingUpdated(Timing timing) {
        for (ITimingServiceEventsListener listener : listeners) {
            listener.onTimingUpdated(timing);
        }
    }

    public interface ITimingServiceEventsListener {
        void onTimingCreated(Timing timing);

        void onTimingUpdated(Timing timing);
    }

    @Inject
    private TimingDao dao;
    @Inject
    private WeatherService weatherService;
    @Inject
    private FilterCompiler filterCompiler;
}
