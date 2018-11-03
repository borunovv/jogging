package com.borunovv.core.log.db.dao;

import com.borunovv.core.hibernate.CommonCriteria;
import com.borunovv.core.hibernate.HibernateAbstractDao;
import com.borunovv.core.hibernate.exception.DataNotFoundException;
import com.borunovv.core.log.db.model.LogEntry;
import com.borunovv.core.util.CalendarUtils;
import com.borunovv.core.util.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class LoggerDao extends HibernateAbstractDao<LogEntry> {

    private static final long MAX_ROWS_PER_QUERY = 1000;

    public void saveBatch(final List<LogEntry> items) {
        if (items.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO logs (create_time, level, exception, message) VALUES ");
        for (int i = 0; i < items.size(); ++i) {
            sb.append("(?,?,?,?),");
        }
        sb.deleteCharAt(sb.length() - 1);
        final String sql = sb.toString();

        getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(Session session) throws HibernateException {
                SQLQuery sqlQuery = session.createSQLQuery(sql);
                int index = 0;
                for (LogEntry item : items) {
                    sqlQuery.setParameter(index * 4 + 0, item.getCreateTime());
                    sqlQuery.setParameter(index * 4 + 1, item.getLevel().ordinal());
                    sqlQuery.setParameter(index * 4 + 2, StringUtils.ensureString(item.getException()));
                    sqlQuery.setParameter(index * 4 + 3, StringUtils.ensureString(item.getMessage()));
                    index++;
                }

                sqlQuery.executeUpdate();
                return null;
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    public void deleteOldByDays(int maxDaysToLive, IInterrupter interrupter) {
        final Date dateToDeleteBefore = CalendarUtils.addDays(
                CalendarUtils.current(), -maxDaysToLive).getTime();

        final String query = "DELETE FROM logs WHERE create_time <= ?";
        deleteIncrementally(interrupter, query, dateToDeleteBefore);
    }

    @SuppressWarnings({"unchecked"})
    public void deleteOldByCount(long maxLogEntryCountToLive, IInterrupter interrupter) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.setProjection(Projections.max("id"));
        Long maxId = null;
        try {
            maxId = getByCriteria(Long.class, criteria);
        } catch (DataNotFoundException ignore) {
        }

        if (maxId != null) {
            final Long idToDeleteBefore = maxId - maxLogEntryCountToLive;

            if (idToDeleteBefore > 0) {
                final String query = "DELETE FROM logs WHERE id <= ?";
                deleteIncrementally(interrupter, query, idToDeleteBefore);
            }
        }
    }

    private void deleteIncrementally(IInterrupter interrupter, String query, final Object... params) {
        String limitedQuery = query + " LIMIT " + MAX_ROWS_PER_QUERY;
        while (true) {
            if (interrupter != null && interrupter.isNeedInterrupt()) {
                break;
            }

            if (executeDelete(limitedQuery, params) <= 0) {
                break;
            }
            Thread.yield();
        }
    }

    private long executeDelete(final String query, final Object... params) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            SQLQuery sqlQuery = session.createSQLQuery(query);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; ++i) {
                    sqlQuery.setParameter(i, params[i]);
                }
            }
            return (long) sqlQuery.executeUpdate();
        });
    }

    public interface IInterrupter {
        boolean isNeedInterrupt();
    }
}
