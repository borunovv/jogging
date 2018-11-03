package com.borunovv.jogging.users.dao;

import com.borunovv.core.hibernate.CommonCriteria;
import com.borunovv.core.hibernate.HibernateAbstractDao;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserDao extends HibernateAbstractDao<User> {

    public User tryFind(String login, String passwordHash) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.eq("login", login));
        criteria.add(Restrictions.eq("passHash", passwordHash));
        List<User> users = findByCriteria(criteria);
        if (users.size() == 1) {
            return users.get(0);
        } else if (users.isEmpty()) {
            return null;
        } else {
            // Must not enter here but just in case
            throw new RuntimeException("Invariant violation: there are several users with same login and password in DB: '" + login + "'");
        }
    }

    public User tryFind(String login) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.eq("login", login));
        List<User> users = findByCriteria(criteria);
        if (users.size() == 1) {
            return users.get(0);
        } else if (users.isEmpty()) {
            return null;
        } else {
            // Must not enter here but just in case
            throw new RuntimeException("Invariant violation: there are several users with same login in DB: '" + login + "'");
        }
    }

    public User tryFind(long userId) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.eq("id", userId));
        List<User> users = findByCriteria(criteria);

        return users.isEmpty() ?
                null :
                users.get(0);
    }

    public List<User> findAll(long offset, long count, List<Rights> rightsList) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.in("rights", rightsList));
        criteria.setOffset(offset);
        criteria.setCount(count);
        criteria.addOrder(Order.asc("id"));

        return findByCriteria(criteria);
    }
}
