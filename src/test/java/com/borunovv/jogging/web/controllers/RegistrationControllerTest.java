package com.borunovv.jogging.web.controllers;

import com.borunovv.core.util.CryptUtils;
import com.borunovv.jogging.users.dao.UserDao;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.UserService;
import com.borunovv.jogging.web.model.ErrorResponse;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.junit.Test;

import javax.inject.Inject;

import static junit.framework.TestCase.assertEquals;

public class RegistrationControllerTest extends AbstractControllerTest {

    @Test
    public void registersNewUser() throws Exception {
        // PRECONDITION
        assertEquals(0, userDao.countAll());

        // ACT
        String json = sendPost("/register", new RegistrationController.Request("John.Smith", "J0hNn_23#42%="));

        // VERIFY
        SuccessResponse response = toModel(json, SuccessResponse.class);
        assertEquals("success", response.getStatus());

        assertEquals(1, userDao.countAll());
        User user = userDao.findAll().get(0);
        assertEquals("John.Smith", user.getLogin());
        assertEquals(CryptUtils.md5("J0hNn_23#42%="), user.getPassHash());
        assertEquals(Rights.USER, user.getRights());
    }

    @Test
    public void failsToRegisterOnDuplicateUser() throws Exception {
        // PRECONDITION
        assertEquals(0, userDao.countAll());

        // ARRANGE
        userService.registerNewUser("John.Smith", "J0hNn_23#42%=");

        // ACT
        String json = sendPost("/register", new RegistrationController.Request("John.Smith", "J0hNn_23#42%="));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals("error", response.getStatus());
        assertEquals(1, userDao.countAll());
        assertEquals("USER already exists.", response.getMsg());
    }

    @Test
    public void registerTwoUsersWithSamePassword() throws Exception {
        // PRECONDITION
        assertEquals(0, userDao.countAll());

        // ARRANGE
        userService.registerNewUser("John.Smith", "password");

        // ACT
        String json = sendPost("/register", new RegistrationController.Request("Karl.Marks", "password"));

        // VERIFY
        SuccessResponse response = toModel(json, SuccessResponse.class);
        assertEquals("success", response.getStatus());
        assertEquals(2, userDao.countAll());
    }

    @Test
    public void failsOnShortLogin() throws Exception {
        // ACT
        String json = sendPost("/register", new RegistrationController.Request("cat", "J0hNn_23#42%="));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals(0, userDao.countAll());
        assertEquals("error", response.getStatus());
        assertEquals("Bad param 'login'. Expected string of 4-30 characters.", response.getMsg());
    }

    @Test
    public void failsOnTooLongLogin() throws Exception {
        // ACT
        String json = sendPost("/register", new RegistrationController.Request(
                "too_long_login_123456789012345678901234567890abc", "J0hNn_23#42%="));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals("error", response.getStatus());
        assertEquals("Bad param 'login'. Expected string of 4-30 characters.", response.getMsg());
    }

    @Test
    public void failsOnBadSymbolInLogin() throws Exception {
        // ACT
        String json = sendPost("/register", new RegistrationController.Request(
                "has_bad_symbols_&!?*", "J0hNn_23#42%="));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals(0, userDao.countAll());
        assertEquals("error", response.getStatus());
        assertEquals("Bad param 'login'. Wrong symbol: '&'. Allowed symbols are: "
                        + "[ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.@].",
                response.getMsg());
    }

    @Test
    public void failsOnShortPassword() throws Exception {
        // ACT
        String json = sendPost("/register", new RegistrationController.Request("user", "short"));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals(0, userDao.countAll());
        assertEquals("error", response.getStatus());
        assertEquals("Bad param 'pass'. Expected string of 8-30 characters.", response.getMsg());
    }

    @Test
    public void failsOnTooLongPassword() throws Exception {
        // ACT
        String json = sendPost("/register", new RegistrationController.Request(
                "user",
                "too_long_password_1234567890qwertyuiop1234567890qwertyuiop"));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals(0, userDao.countAll());
        assertEquals("error", response.getStatus());
        assertEquals("Bad param 'pass'. Expected string of 8-30 characters.", response.getMsg());
    }

    @Test
    public void failsOnBadSymbolInPassword() throws Exception {
        // ACT
        String json = sendPost("/register", new RegistrationController.Request(
                "user",
                "bad_symbol_`"));

        // VERIFY
        ErrorResponse response = toModel(json, ErrorResponse.class);
        assertEquals(0, userDao.countAll());
        assertEquals("error", response.getStatus());
        assertEquals("Bad param 'pass'. Wrong symbol: '`'. " +
                        "Allowed symbols are: [ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_.!@#$%^&*()+=].",
                response.getMsg());
    }

    @Inject
    private UserDao userDao;
    @Inject
    private UserService userService;
}