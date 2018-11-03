package com.borunovv.core.hibernate;

import com.borunovv.core.hibernate.exception.DuplicateEntryException;
import com.borunovv.core.util.TypeUtils;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Transactional
public abstract class HibernateAbstractDao<T> extends DaoSupport {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Inject
    public void initSessionFactory(SessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
    }

    private Class<T> persistentClass;

    @SuppressWarnings({"unchecked"})
    public HibernateAbstractDao() {
        persistentClass = (Class<T>) TypeUtils.getGenericParameterClass(
                this.getClass(), HibernateAbstractDao.class, 0);
    }

    public Class<T> getPersistentClass() {
        return persistentClass;
    }

    public String getTableName() {
        Table table = persistentClass.getAnnotation(Table.class);
        return table != null ?
                table.name() :
                "not_table_found_for_entity_" + persistentClass.getSimpleName();
    }

    public T get(long id) {
        return get(getPersistentClass(), id);
    }

    public <T> T get(Class<T> aClass, long id) {
        CommonCriteria criteria = getCommonCriteria(aClass);
        criteria.add(Restrictions.eq("id", id));
        return getByCriteria(aClass, criteria);
    }

    public <T> T get(Class<T> aClass, Serializable id) {
        return getHibernateTemplate().get(aClass, id);
    }

    public List<T> findAll() {
        return findByCriteria();
    }

    public List<T> findAll(long offset, long count) {
        return findByCriteria(offset, count);
    }

    public T save(T entity) {
        return save(entity, true);
    }

    public T save(T entity, boolean updatable) {
        try {
            if (updatable) {
                getHibernateTemplate().saveOrUpdate(entity);
            } else {
                getHibernateTemplate().save(entity);
            }
            return entity;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEntryException(entity + " already exists.", e);
        }
    }

    protected <V> V saveEntity(V entity) {
        try {
            getHibernateTemplate().saveOrUpdate(entity);
            return entity;
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEntryException(entity + " already exists.", e);
        }
    }

    public void delete(T entity) {
        getHibernateTemplate().delete(entity);
    }

    protected <V> V deleteEntity(V entity) {
        getHibernateTemplate().delete(entity);
        return entity;
    }

    @SuppressWarnings({"unchecked"})
    protected int deleteByQuery(final String query) {
        return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Integer>() {
            public Integer doInHibernate(Session session) throws HibernateException {
                Query queryObject = session.createQuery(query);
                return queryObject.executeUpdate();
            }
        });
    }

    protected void deleteByQuery(final String query, Object... args) {
        getHibernateTemplate().bulkUpdate(query, args);
    }

    public void executeNativeQuery(final String query) {
        getHibernateTemplate().executeWithNativeSession(new HibernateCallback<Void>() {
            @Override
            public Void doInHibernate(Session session) throws HibernateException {
                SQLQuery sqlQuery = session.createSQLQuery(query);
                sqlQuery.executeUpdate();
                return null;
            }
        });
    }

    public void deleteAll() {
        deleteByQuery("delete from " + persistentClass.getSimpleName());
    }

    protected List<T> findByCriteria(final Criterion... criterion) {
        return findByCriteria(getPersistentClass(), criterion);
    }

    protected List<T> findByCriteria(long firstResult, long maxResults, Criterion... criterions) {
        return findByCriteria(firstResult, maxResults, (Order) null, criterions);
    }

    protected List<T> findByCriteria(long firstResult, long maxResults, Order order,
                                     Criterion... criterion) {
        return findByCriteria(getPersistentClass(), firstResult, maxResults, order, criterion);
    }

    protected List<T> findAllByQuery(String query, Object... args) {
        return findAllByQuery(getPersistentClass(), query, args);
    }

    @SuppressWarnings({"unchecked"})
    protected <V> List<V> findAllByQuery(Class<V> cl, final String queryString, final Object... values) {
        return getHibernateTemplate().executeWithNativeSession((HibernateCallback<List<V>>) session -> {
            Query queryObject = session.createQuery(queryString);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            return queryObject.list();
        });
    }

    @SuppressWarnings({"unchecked"})
    protected List<T> findByQuery(final String queryString, final long offset, final long count, final Object... values) throws DataAccessException {
        return findByQuery(getPersistentClass(), queryString, offset, count, values);
    }

    @SuppressWarnings({"unchecked"})
    protected <V> List<V> findByQuery(final Class<V> cl, final String queryString, final long offset, final long count, final Object... values) throws DataAccessException {
        return getHibernateTemplate().executeWithNativeSession((HibernateCallback<List<V>>) session -> {
            Query queryObject = session.createQuery(queryString);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            if (count > 0)
                queryObject = queryObject.setFirstResult((int) offset).setMaxResults((int) count);

            return queryObject.list();
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <V> V getByNativeQuery(final String queryString, final Object... values) throws DataAccessException {
        return getHibernateTemplate().executeWithNativeSession(new HibernateCallback<V>() {
            public V doInHibernate(Session session) throws HibernateException {
                Query queryObject = session.createSQLQuery(queryString);
                if (values != null) {
                    for (int i = 0; i < values.length; i++) {
                        queryObject.setParameter(i, values[i]);
                    }
                }
                return (V) queryObject.uniqueResult();
            }
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <V> List<V> findByNativeQuery(final Class<V> cl, final String queryString,
                                            final long offset, final long count, final Object... values) throws DataAccessException {
        return getHibernateTemplate().executeWithNativeSession((HibernateCallback<List<V>>) session -> {
            Query queryObject = session.createSQLQuery(queryString).addEntity(cl);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            if (count > 0)
                queryObject = queryObject.setFirstResult((int) offset).setMaxResults((int) count);

            return queryObject.list();
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <V> List<V> findAllByNativeQuery(final Class<V> cl, final String queryString,
                                               final Object... values) throws DataAccessException {
        return getHibernateTemplate().executeWithNativeSession((HibernateCallback<List<V>>) session -> {
            Query queryObject = session.createSQLQuery(queryString).addEntity(cl);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }

            return queryObject.list();
        });
    }


    @SuppressWarnings({"unchecked"})
    protected <V> V getByCriteria(Class<V> cl, final DetachedCriteria criteria) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Criteria executableCriteria = criteria.getExecutableCriteria(session);
            V res = (V) executableCriteria.uniqueResult();
            checkNullByCriteria(res, executableCriteria);
            return res;
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <V> V getByCriteria(final DetachedCriteria criteria) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Criteria executableCriteria = criteria.getExecutableCriteria(session);
            V res = (V) executableCriteria.uniqueResult();
            checkNullByCriteria(res, executableCriteria);
            return res;
        });
    }

    @SuppressWarnings({"unchecked"})
    protected <V> V getByQuery(final Class<V> cl, final String query, final Object... values) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Query queryObject = session.createQuery(query);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            V res = (V) queryObject.uniqueResult();
            checkNullByQuery(res, query, values);
            return res;
        });
    }

    protected T getByQuery(String query, Object... values) {
        return getByQuery(getPersistentClass(), query, values);
    }

    @SuppressWarnings({"unchecked"})
    protected long count(final DetachedCriteria criteria) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Criteria executableCriteria = criteria.getExecutableCriteria(session);
            executableCriteria.setProjection(Projections.rowCount());
            return (Long) executableCriteria.uniqueResult();
        });
    }

    @SuppressWarnings("unchecked")
    protected long count(final Class clazz, final Criterion... criterion) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Criteria c = session.createCriteria(clazz);
            for (Criterion cr : criterion) {
                c.add(cr);
            }
            c.setProjection(Projections.rowCount());
            return (Long) c.uniqueResult();
        });
    }

    @SuppressWarnings("unchecked")
    protected long count(final String query, final Object... values) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Query queryObject = session.createQuery(query);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            return (Long) queryObject.uniqueResult();
        });
    }

    @SuppressWarnings("unchecked")
    protected long selectLongScalar(final String query, final Object... values) {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            Query queryObject = session.createQuery(query);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            return (Long) queryObject.uniqueResult();
        });
    }

    @SuppressWarnings({"unchecked"})
    protected long countByNativeQuery(final String queryString, final Object... values) throws DataAccessException {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            SQLQuery queryObject = session.createSQLQuery(queryString);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            queryObject.addScalar("result", StandardBasicTypes.BIG_INTEGER);
            return ((BigInteger) queryObject.uniqueResult()).longValue();
        });
    }

    @SuppressWarnings({"unchecked"})
    protected int updateByNativeQuery(final String queryString, final Object... values) throws DataAccessException {
        return getHibernateTemplate().executeWithNativeSession(session -> {
            SQLQuery queryObject = session.createSQLQuery(queryString);
            if (values != null) {
                for (int i = 0; i < values.length; i++) {
                    queryObject.setParameter(i, values[i]);
                }
            }
            queryObject.addScalar("result", StandardBasicTypes.BIG_INTEGER);
            return queryObject.executeUpdate();
        });
    }

    public void clearCache() {
        Cache cache = getHibernateTemplate().getSessionFactory().getCache();
        cache.evictEntityRegion(getPersistentClass());
    }

    public long countAll() {
        return count();
    }

    protected long count(final Criterion... criterion) {
        return count(getPersistentClass(), criterion);
    }

    protected T getByCriteria(final Criterion... criterion) {
        return getByCriteria(getPersistentClass(), criterion);
    }

    public void flush() {
        getHibernateTemplate().flush();
    }


    protected DetachedCriteria getCriteria(Class cl) {
        return DetachedCriteria.forClass(cl);
    }

    protected DetachedCriteria getCriteria() {
        return DetachedCriteria.forClass(getPersistentClass());
    }

    protected CommonCriteria getCommonCriteria(Class cl) {
        return CommonCriteria.forClass(cl);
    }

    protected CommonCriteria getCommonCriteria() {
        return getCommonCriteria(getPersistentClass());
    }

    protected Order orderBy(String property, boolean ascendant) {
        return ascendant ? Order.asc(property) : Order.desc(property);
    }
}