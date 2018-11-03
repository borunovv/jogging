package com.borunovv.jogging.web.controllers;

import com.borunovv.core.util.CryptUtils;
import com.borunovv.jogging.users.dao.UserDao;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.web.model.ErrorResponse;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.junit.Test;

import javax.inject.Inject;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

public class UserControllerTest extends AbstractControllerTest {

    @Test
    public void expectedPOST() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        String json = sendPut("/user/update", new UserController.UpdateRequest(session, "John.Smith", "new_password"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Expected POST request. Actual: PUT", response.getMsg());
    }

    @Test
    public void userCanUpdateSelfAccount() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        String json = sendPost("/user/update", new UserController.UpdateRequest(session, "John.Smith", "new_password"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals(CryptUtils.md5("new_password"), userDao.tryFind("John.Smith").getPassHash());
    }

    @Test
    public void userCanDeleteSelfAccount() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        String json = sendPost("/user/delete", new UserController.DeleteRequest(session, "John.Smith"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals(0, userDao.countAll());
    }

    @Test
    public void userNotPermittedToUpdateAnotherAccount() throws Exception {
        // ARRANGE
        registerNewUser("Karl.Marks", "marks1234");
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        // Try update another user's password.
        String json = sendPost("/user/update", new UserController.UpdateRequest(session, "Karl.Marks", "new_password"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Test
    public void userNotPermittedToDeleteAnotherAccount() throws Exception {
        // ARRANGE
        registerNewUser("Karl.Marks", "marks1234");
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        String json = sendPost("/user/delete", new UserController.DeleteRequest(session, "Karl.Marks"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Test
    public void userNotPermittedToReadAnotherAccounts() throws Exception {
        // ARRANGE
        registerNewUser("Karl.Marks", "marks1234");
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        String json = sendPost("/user/list", new UserController.ListRequest(session, 0, 10));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }


    @Test
    public void managerCanUpdateSelfAccount() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Manager", "initial_password", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/update", new UserController.UpdateRequest(session, "John.Manager", "new_password"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals(CryptUtils.md5("new_password"), userDao.tryFind("John.Manager").getPassHash());
    }

    @Test
    public void managerCanDeleteSelfAccount() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Manager", "password", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/delete", new UserController.DeleteRequest(session, "John.Manager"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertNull(userDao.tryFind("John.Manager"));
    }

    @Test
    public void managerCanReadOnlyUserAccounts() throws Exception {
        // ARRANGE
        registerNewUser("Elvis.User", "elvis_pass", Rights.USER);
        registerNewUser("Madonna.User", "madonna_pass", Rights.USER);
        registerNewUser("Jim.Manager", "jim_pass", Rights.MANAGER);
        registerNewUser("Mark.Admin", "mark_pass", Rights.ADMIN);
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/list", new UserController.ListRequest(session, 0, 10));
        UserController.ListResponse response = toModel(json, UserController.ListResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals(2, response.logins.size());
        assertTrue(response.logins.contains("Elvis.User"));
        assertTrue(response.logins.contains("Madonna.User"));
    }

    @Test
    public void managerCanCreateUserAccount() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/create", new UserController.UpdateRequest(session, "Elvis.User", "elvis_pass"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals(CryptUtils.md5("elvis_pass"), userDao.tryFind("Elvis.User").getPassHash());
    }

    @Test
    public void managerCanUpdateUserAccount() throws Exception {
        // ARRANGE
        registerNewUser("Elvis.User", "elvis_pass", Rights.USER);
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/update", new UserController.UpdateRequest(session, "Elvis.User", "new_pass"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals(CryptUtils.md5("new_pass"), userDao.tryFind("Elvis.User").getPassHash());
    }

    @Test
    public void managerCanDeleteUserAccount() throws Exception {
        // ARRANGE
        registerNewUser("Elvis.User", "elvis_pass", Rights.USER);
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/delete", new UserController.DeleteRequest(session, "Elvis.User"));
        SuccessResponse response = toModel(json, SuccessResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertNull(userDao.tryFind("Elvis.User"));
    }

    @Test
    public void managerNotPermittedToUpdateManagerAccount() throws Exception {
        // ARRANGE
        registerNewUser("Elvis.Manager", "elvis_pass", Rights.MANAGER);
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/update", new UserController.UpdateRequest(session, "Elvis.Manager", "new_passw"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Test
    public void managerNotPermittedToUpdateAdminAccount() throws Exception {
        // ARRANGE
        registerNewUser("Elvis.Admin", "elvis_pass", Rights.ADMIN);
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/update", new UserController.UpdateRequest(session, "Elvis.Admin", "new_passw"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Inject
    private UserDao userDao;
}