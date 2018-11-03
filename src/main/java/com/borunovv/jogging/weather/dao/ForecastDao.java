package com.borunovv.jogging.weather.dao;

import com.borunovv.core.hibernate.CommonCriteria;
import com.borunovv.core.hibernate.HibernateAbstractDao;
import com.borunovv.core.hibernate.exception.DuplicateEntryException;
import com.borunovv.core.util.StringUtils;
import com.borunovv.jogging.weather.model.Forecast;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class ForecastDao extends HibernateAbstractDao<Forecast> {

    public List<Forecast> findAllFromDate(Date startDate) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.ge("date", startDate));
        criteria.addOrder(Order.desc("date"));
        return findByCriteria(criteria);
    }

    public boolean exists(Date date, String location) {
        return tryFind(date, location) != null;
    }

    public Forecast tryFind(Date date, String location) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.eq("date", date));
        criteria.add(Restrictions.eq("location", location));
        criteria.setCount(1);
        List<Forecast> list = findByCriteria(criteria);
        return list.isEmpty() ?
                null :
                list.get(0);
    }

    public void ensureExists(Forecast forecast) {
        Forecast existing = tryFind(forecast.getDate(), forecast.getLocation());
        if (existing != null) {
            if (existing.getStatus() != forecast.getStatus()
                    || existing.getTemperature() != forecast.getTemperature()
                    || existing.getHumidity() != forecast.getHumidity()
                    || !StringUtils.ensureString(existing.getPrecipitation()).equalsIgnoreCase(forecast.getPrecipitation())) {
                existing.setStatus(forecast.getStatus());
                existing.setTemperature(forecast.getTemperature());
                existing.setHumidity(forecast.getHumidity());
                existing.setPrecipitation(forecast.getPrecipitation());
                save(existing);
            }
        } else {
            forecast.setId(0);
            try {
                save(forecast);
            } catch (DuplicateEntryException ignore) {
            }
        }
    }
}
