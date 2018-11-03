package com.borunovv.jogging.web.controllers;

import com.borunovv.core.util.CryptUtils;
import com.borunovv.jogging.users.dao.SessionDao;
import com.borunovv.jogging.users.dao.UserDao;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.Session;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.UserService;
import org.junit.Test;

import javax.inject.Inject;

import static junit.framework.TestCase.*;

public class LogInControllerTest extends AbstractControllerTest {

    @Test
    public void logInUser() throws Exception {
        // PRECONDITION
        assertEquals(0, userDao.countAll());
        assertEquals(0, sessionDao.countAll());

        // ARRANGE
        userService.registerNewUser("John.Smith", "J0hNn_23#42%=");

        // ACT
        String json = sendPost("/login", new LogInController.Request("John.Smith", "J0hNn_23#42%="));

        // VERIFY
        LogInController.Response response = toModel(json, LogInController.Response.class);
        assertNotNull(response);
        assertEquals("success", response.getStatus());

        assertEquals(1, userDao.countAll());
        assertEquals(1, sessionDao.countAll());

        User user = userDao.findAll().get(0);
        assertEquals("John.Smith", user.getLogin());
        assertEquals(CryptUtils.md5("J0hNn_23#42%="), user.getPassHash());
        assertEquals(Rights.USER, user.getRights());

        Session session = sessionDao.tryFindByUser(user.getId());
        assertNotNull(session);
        assertEquals(32, response.getSession().length());
        assertEquals(response.getSession(), session.getCode());
    }

    @Test
    public void logInUserSeveralTimes() throws Exception {
        // PRECONDITION
        assertEquals(0, userDao.countAll());
        assertEquals(0, sessionDao.countAll());

        // ARRANGE
        userService.registerNewUser("John.Smith", "J0hNn_23#42%=");

        // ACT
        String json1 = sendPost("/login", new LogInController.Request("John.Smith", "J0hNn_23#42%="));
        LogInController.Response response1 = toModel(json1, LogInController.Response.class);
        String session1 = response1.getSession();

        String json2 = sendPost("/login", new LogInController.Request("John.Smith", "J0hNn_23#42%="));
        LogInController.Response response2 = toModel(json2, LogInController.Response.class);
        String session2 = response2.getSession();

        assertFalse(session1.equals(session2));
        assertEquals(1, userDao.countAll());
        assertEquals(1, sessionDao.countAll());

        Session session = sessionDao.tryFindByCode(session2);
        assertNotNull(session);
        assertEquals(session2, session.getCode());
    }

    @Inject
    private UserDao userDao;
    @Inject
    private SessionDao sessionDao;
    @Inject
    private UserService userService;
}