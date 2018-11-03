package com.borunovv.jogging.web.controllers;

import com.borunovv.jogging.users.dao.SessionDao;
import com.borunovv.jogging.users.dao.UserDao;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.web.model.ErrorResponse;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.junit.Test;

import javax.inject.Inject;

import static junit.framework.TestCase.assertEquals;

public class RightsControllerTest extends AbstractControllerTest {

    @Test
    public void expectedPOST() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Smith", "initial_password");

        // ACT
        String json = sendPut("/user/rights/update", new RightsController.UpdateRequest(session, "John.Smith", "manager"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Expected POST request. Actual: PUT", response.getMsg());
    }

    @Test
    public void userCanReadSelfRights() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.User", "john_pass", Rights.USER);

        // ACT
        String json = sendPost("/user/rights/read", new RightsController.ReadRequest(session, "John.User"));
        RightsController.ReadResponse response = toModel(json, RightsController.ReadResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals("user", response.getRights());
    }

    @Test
    public void userNotPermittedToReadAnotherRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.User", "marks1234");
        String session = registerAndLogIn("John.User", "john_pass", Rights.USER);

        // ACT
        String json = sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.User"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Test
    public void userNotPermittedToUpdateAnotherRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.User", "marks1234", Rights.USER);
        registerNewUser("Karl.Manager", "marks1234", Rights.MANAGER);
        registerNewUser("Karl.Admin", "marks1234", Rights.ADMIN);
        String session = registerAndLogIn("John.User", "john_pass", Rights.USER);

        // ACT
        ErrorResponse responseUser = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.User", "manager")),
                ErrorResponse.class);

        ErrorResponse responseManager = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.Manager", "user")),
                ErrorResponse.class);

        ErrorResponse responseAdmin = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.Admin", "manager")),
                ErrorResponse.class);


        // VERIFY
        assertEquals("error", responseUser.getStatus());
        assertEquals("Not enough rights.", responseUser.getMsg());

        assertEquals("error", responseManager.getStatus());
        assertEquals("Not enough rights.", responseManager.getMsg());

        assertEquals("error", responseAdmin.getStatus());
        assertEquals("Not enough rights.", responseAdmin.getMsg());
    }

    @Test
    public void managerCanReadSelfRights() throws Exception {
        // ARRANGE
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/rights/read", new RightsController.ReadRequest(session, "John.Manager"));
        RightsController.ReadResponse response = toModel(json, RightsController.ReadResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals("manager", response.getRights());
    }

    @Test
    public void managerCanReadUserRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.User", "marks1234");
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.User"));
        RightsController.ReadResponse response = toModel(json, RightsController.ReadResponse.class);

        // VERIFY
        assertEquals("success", response.getStatus());
        assertEquals("user", response.getRights());
    }

    @Test
    public void managerNotPermittedToReadAnotherManagerRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.Manager", "marks1234", Rights.MANAGER);
        String session = registerAndLogIn("John.User", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.Manager"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Test
    public void managerNotPermittedToReadAdminRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.Admin", "marks1234", Rights.ADMIN);
        String session = registerAndLogIn("John.User", "john_pass", Rights.MANAGER);

        // ACT
        String json = sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.Admin"));
        ErrorResponse response = toModel(json, ErrorResponse.class);

        // VERIFY
        assertEquals("error", response.getStatus());
        assertEquals("Not enough rights.", response.getMsg());
    }

    @Test
    public void managerNotPermittedToUpdateAnotherRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.User", "marks1234", Rights.USER);
        registerNewUser("Karl.Manager", "marks1234", Rights.MANAGER);
        registerNewUser("Karl.Admin", "marks1234", Rights.ADMIN);
        String session = registerAndLogIn("John.Manager", "john_pass", Rights.MANAGER);

        // ACT
        ErrorResponse responseUser = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.User", "manager")),
                ErrorResponse.class);

        ErrorResponse responseManager = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.Manager", "user")),
                ErrorResponse.class);

        ErrorResponse responseAdmin = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.Admin", "user")),
                ErrorResponse.class);


        // VERIFY
        assertEquals("error", responseUser.getStatus());
        assertEquals("Not enough rights.", responseUser.getMsg());

        assertEquals("error", responseManager.getStatus());
        assertEquals("Not enough rights.", responseManager.getMsg());

        assertEquals("error", responseAdmin.getStatus());
        assertEquals("Not enough rights.", responseAdmin.getMsg());
    }

    @Test
    public void adminCanUpdateAnyRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.User", "marks1234", Rights.USER);
        registerNewUser("Karl.Manager", "marks1234", Rights.MANAGER);
        registerNewUser("Karl.Admin", "marks1234", Rights.ADMIN);
        String session = registerAndLogIn("John.Admin", "john_pass", Rights.ADMIN);

        // ACT
        SuccessResponse responseUser = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.User", "manager")),
                SuccessResponse.class);

        SuccessResponse responseManager = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.Manager", "user")),
                SuccessResponse.class);

        SuccessResponse responseAdmin = toModel(
                sendPost("/user/rights/update", new RightsController.UpdateRequest(session, "Karl.Admin", "user")),
                SuccessResponse.class);


        // VERIFY
        assertEquals("success", responseUser.getStatus());
        assertEquals(Rights.MANAGER, userDao.tryFind("Karl.User").getRights());

        assertEquals("success", responseManager.getStatus());
        assertEquals(Rights.USER, userDao.tryFind("Karl.Manager").getRights());

        assertEquals("success", responseAdmin.getStatus());
        assertEquals(Rights.USER, userDao.tryFind("Karl.Admin").getRights());
    }

    @Test
    public void adminCanReadAnyRights() throws Exception {
        // ARRANGE
        registerNewUser("Karl.User", "marks1234", Rights.USER);
        registerNewUser("Karl.Manager", "marks1234", Rights.MANAGER);
        registerNewUser("Karl.Admin", "marks1234", Rights.ADMIN);
        String session = registerAndLogIn("John.Admin", "john_pass", Rights.ADMIN);

        // ACT
        RightsController.ReadResponse responseUser = toModel(
                sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.User")),
                RightsController.ReadResponse.class);

        RightsController.ReadResponse responseManager = toModel(
                sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.Manager")),
                RightsController.ReadResponse.class);

        RightsController.ReadResponse responseAdmin = toModel(
                sendPost("/user/rights/read", new RightsController.ReadRequest(session, "Karl.Admin")),
                RightsController.ReadResponse.class);


        // VERIFY
        assertEquals("success", responseUser.getStatus());
        assertEquals("user", responseUser.getRights());

        assertEquals("success", responseManager.getStatus());
        assertEquals("manager", responseManager.getRights());

        assertEquals("success", responseAdmin.getStatus());
        assertEquals("admin", responseAdmin.getRights());
    }


    @Inject
    private UserDao userDao;
    @Inject
    private SessionDao sessionDao;

}