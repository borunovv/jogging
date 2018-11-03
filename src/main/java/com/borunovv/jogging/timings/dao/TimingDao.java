package com.borunovv.jogging.timings.dao;

import com.borunovv.core.hibernate.HibernateAbstractDao;
import com.borunovv.core.util.StringUtils;
import com.borunovv.jogging.timings.model.Timing;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class TimingDao extends HibernateAbstractDao<Timing> {

    public int getTotalTimeForDate(long userId, Date date, long excludeTimingId) {
        int totalMinutes = getByNativeQuery("SELECT SUM(time_minutes) FROM timings WHERE user_id=? AND id <> ?",
                userId, excludeTimingId);
        return totalMinutes ;
    }

    public List<Timing> find(long userId, String sqlWhereClause, long offset, long count) {
        return findByQuery(Timing.class, "FROM Timing WHERE user_id=? AND "
                + (StringUtils.isNullOrEmpty(sqlWhereClause) ? "1" : "(" + sqlWhereClause + ")"),
                offset, count, userId);
    }
}
