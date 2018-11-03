package com.borunovv.jogging.weather.provider;

import com.borunovv.core.service.AbstractServiceWithOwnThread;
import com.borunovv.jogging.config.Constants;
import com.borunovv.jogging.weather.model.Weather;
import com.borunovv.jogging.weather.provider.common.ForecastTask;
import com.borunovv.jogging.weather.provider.common.IForecastProvider;
import com.borunovv.jogging.weather.provider.common.IForecastTaskSupplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class YahooForecastProvider extends AbstractServiceWithOwnThread implements IForecastProvider {

    private volatile IForecastTaskSupplier supplier;
    private AtomicLong quotaRefreshTime = new AtomicLong(System.currentTimeMillis() + Constants.MILLIS_IN_DAY);
    private AtomicInteger quota = new AtomicInteger(0);

    @Override
    protected void onInit() {
        quota.set(quotaRequestsPerDay);
        start();
    }

    @Override
    public void setTaskSupplier(IForecastTaskSupplier supplier) {
        this.supplier = supplier;
    }

    @Override
    protected void doThreadIteration() throws InterruptedException {
        IForecastTaskSupplier supplier = this.supplier;
        if (supplier == null) {
            sleep(500);
            return;
        }

        // Refresh quota every 1 day (24h)
        long nextQuotaRefreshTime = quotaRefreshTime.get();
        if (System.currentTimeMillis() >= nextQuotaRefreshTime) {
            if (quotaRefreshTime.compareAndSet(nextQuotaRefreshTime, System.currentTimeMillis() + Constants.MILLIS_IN_DAY)) {
                quota.set(quotaRequestsPerDay);
                logger.info("YahooForecastProvider: quota refreshed: " + quotaRequestsPerDay);
            }
        }

        if (quota.decrementAndGet() > 0) {
            ForecastTask task = supplier.poll();
            if (task != null) {
                processTask(task, supplier);
                sleep(10);
            } else {
                quota.incrementAndGet();
                sleep(100);
            }
        } else {
            quota.incrementAndGet();
            sleep(1000);
        }
    }

    private void processTask(ForecastTask task, IForecastTaskSupplier supplier) {
        try {
            Weather weather = getWeather(task.date, task.location);
            supplier.onForecast(task, weather);
        } catch (Exception e) {
            logger.error("YahooForecastProvider: error", e);
            try {
                supplier.onForecast(task, null);
            } catch (Exception e2) {
                logger.error("YahooForecastProvider: error in supplier", e2);
            }
        }
    }

    private Weather getWeather(Date date, String location) {
        // TODO ask Yahoo!
        return new Weather(10, 15, "TODO: ask yahoo!");
    }

    @Value("${forecast.yahoo.quota.requests.per.day}")
    private int quotaRequestsPerDay;
}
