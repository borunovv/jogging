package com.borunovv.core.hibernate;

import com.borunovv.core.hibernate.exception.DataNotFoundException;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

import java.util.Arrays;
import java.util.List;

public class DaoSupport extends HibernateDaoSupport {

    @Override
    protected HibernateTemplate createHibernateTemplate(SessionFactory sessionFactory) {
        HibernateTemplate template = super.createHibernateTemplate(sessionFactory);
        return template;
    }

    protected <T> List<T> findByCriteria(Class<T> cl, Criterion... criterion) {
        return findByCriteria(cl, 0, 0, (Order[]) null, criterion);
    }

    protected <T> List<T> findByCriteria(Class<T> cl, Order order, Criterion... criterion) {
        return findByCriteria(cl, 0, 0, order, criterion);
    }

    protected <T> List<T> findByCriteria(Class<T> cl, long firstResult, long maxResults,
                                         Criterion... criterion) {
        return findByCriteria(cl, firstResult, maxResults, (Order) null, criterion);
    }

    protected <T> List<T> findByCriteria(Class<T> cl, long firstResult, long maxResults,
                                         Order order, Criterion... criterion) {
        return findByCriteria(cl, firstResult, maxResults,
                order != null ? (new Order[]{order}) : null, criterion);
    }


    @SuppressWarnings({"unchecked"})
    protected <T> List<T> findByCriteria(final Class<T> cl, final long firstResult,
                                         final long maxResults, final Order[] order,
                                         final Criterion... criterion) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Criteria crit = session.createCriteria(cl);
            for (Criterion c : criterion) {
                crit.add(c);
            }
            if (order != null) {
                for (Order o : order) {
                    crit.addOrder(o);
                }
            }
            if (firstResult > 0) crit.setFirstResult((int) firstResult);
            if (maxResults > 0) crit.setMaxResults((int) maxResults);
            List<T> res = crit.list();
            checkNullByCriteria(res, crit);
            return res;
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <T> T getByCriteria(final Class<T> cl, final Criterion... criterion) {
        return (T) getHibernateTemplate().executeWithNativeSession((HibernateCallback) session -> {
            Criteria crit = session.createCriteria(cl);
            for (Criterion c : criterion) {
                crit.add(c);
            }
            Object res = crit.uniqueResult();
            checkNullByCriteria(res, crit);
            return res;
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <T> List<T> findByCriteria(CommonCriteria criteria) {
        return findByCriteria(criteria.getOffset(), criteria.getCount(), (DetachedCriteria) criteria);
    }

    @SuppressWarnings({"unchecked"})
    protected <T> List<T> findByCriteria(DetachedCriteria criteria) {
        if (criteria instanceof CommonCriteria)
            return findByCriteria((CommonCriteria) criteria);
        else
            return findByCriteria(-1, -1, criteria);
    }

    @SuppressWarnings({"unchecked"})
    protected <T> List<T> findByCriteria(final long firstResult, final long maxResults,
                                         final DetachedCriteria criteria) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Criteria executableCriteria = criteria.getExecutableCriteria(session);
            if (firstResult > 0) executableCriteria.setFirstResult((int) firstResult);
            if (maxResults > 0) executableCriteria.setMaxResults((int) maxResults);
            List<T> res = executableCriteria.list();
            checkNullByCriteria(res, executableCriteria);
            return res;
        });
    }

    protected void checkNullByCriteria(Object res, Criteria criteria) throws DataNotFoundException {
        if (res == null) {
            throw new DataNotFoundException(getClass().getName() + ":  data not found by criteria.");
        }

    }

    protected void checkNullByQuery(Object res, String query, Object[] args) throws DataNotFoundException {
        if (res == null) {
            throw new DataNotFoundException(getClass().getName() + ":  data not found by query: \n"
                    + query + "\nArgs: " + Arrays.toString(args));
        }
    }
}