package com.borunovv.jogging.users.dao;

import com.borunovv.core.hibernate.CommonCriteria;
import com.borunovv.core.hibernate.HibernateAbstractDao;
import com.borunovv.core.util.Assert;
import com.borunovv.jogging.users.model.Session;
import com.borunovv.jogging.users.model.User;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SessionDao extends HibernateAbstractDao<Session>{

    public Session tryFindByUser(long userId) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.eq("userId", userId));
        List<Session> list = findByCriteria(criteria);

        Assert.isTrue(list.size() < 2,
                "Invariant violation: There are several sessions for same user in DB, userId: " + userId);

        return list.isEmpty() ?
                null :
                list.get(0);
    }

    public Session tryFindByCode(String code) {
        CommonCriteria criteria = getCommonCriteria();
        criteria.add(Restrictions.eq("code", code));
        List<Session> list = findByCriteria(criteria);

        Assert.isTrue(list.size() < 2,
                "Invariant violation: There are several sessions with same code in DB: " + code);

        return list.isEmpty() ?
                null :
                list.get(0);
    }

    public void clearSessionsFor(User user) {
        deleteByQuery("DELETE FROM sessions WHERE user_id=?", user.getId());
    }

    public void clearExpiredSessions(DateTime olderThanTime) {
        deleteByQuery("DELETE FROM sessions WHERE created < ?", olderThanTime.toDate());
    }
}
