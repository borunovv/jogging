package com.borunovv.jogging.users.service;

import com.borunovv.core.hibernate.exception.DuplicateEntryException;
import com.borunovv.core.service.AbstractServiceWithOwnThread;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.CryptUtils;
import com.borunovv.jogging.users.dao.SessionDao;
import com.borunovv.jogging.users.dao.UserDao;
import com.borunovv.jogging.users.model.Session;
import com.borunovv.jogging.users.model.User;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Random;

@Service
public class SessionService extends AbstractServiceWithOwnThread {

    private static final int SESSION_LIFETIME_MINUTES = 60;

    private static final Random RANDOM = new Random(System.currentTimeMillis());


    @Override
    protected void onInit() {
        super.onInit();
        start();
    }

    @Override
    protected void doThreadIteration() throws InterruptedException {
        clearExpiredSessions();
        sleep(1000 * 60);
    }

    protected void clearExpiredSessions() {
        sessionDao.clearExpiredSessions(DateTime.now().plusMinutes(0 - SESSION_LIFETIME_MINUTES));
    }

    public String startNewSession(User user) {
        clearSessionsFor(user);

        Session session = new Session();
        for (int attempt = 0; attempt < 5; ++attempt) {
            try {
                session.setCode(generateRandomSessionCode(user, attempt));
                session.setUserId(user.getId());
                sessionDao.save(session);
            } catch (DuplicateEntryException ignore) {
            }
        }
        Assert.isTrue(session.getId() > 0, "Failed to generate unique session.");
        return session.getCode();
    }

    public void clearSessionsFor(User user) {
        sessionDao.clearSessionsFor(user);
    }

    public User tryGetUserBySession(String sessionCode) {
        Session session = sessionDao.tryFindByCode(sessionCode);
        return session != null ?
                userDao.tryFind(session.getUserId()) :
                null;
    }

    private String generateRandomSessionCode(User user, int attempt) {
        byte[] rnd = new byte[32];
        RANDOM.nextBytes(rnd);
        return CryptUtils.md5(user.getLogin() + CryptUtils.md5(rnd) + attempt);
    }

    @Inject
    private SessionDao sessionDao;
    @Inject
    private UserDao userDao;
}
