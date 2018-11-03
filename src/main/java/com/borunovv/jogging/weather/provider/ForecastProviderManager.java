package com.borunovv.jogging.weather.provider;

import com.borunovv.core.service.AbstractService;
import com.borunovv.jogging.weather.provider.common.IForecastProvider;
import com.borunovv.jogging.weather.provider.common.IForecastTaskSupplier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Service
public class ForecastProviderManager extends AbstractService implements IForecastProvider {

    private final List<IForecastProvider> providers = new ArrayList<>();

    @PostConstruct
    private void init() {
        providers.add(yahooForecastProvider);
        // Here wee can add more providers in future.
    }


    @Override
    public void setTaskSupplier(IForecastTaskSupplier supplier) {
        for (IForecastProvider provider : providers) {
            provider.setTaskSupplier(supplier);
        }
    }

    @Inject
    private YahooForecastProvider yahooForecastProvider;
}
