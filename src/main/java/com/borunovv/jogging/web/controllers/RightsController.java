package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.web.HttpController;
import com.borunovv.jogging.permissions.Action;
import com.borunovv.jogging.permissions.Subject;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.SessionService;
import com.borunovv.jogging.users.service.UserService;
import com.borunovv.jogging.web.model.AbstractRequest;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@HttpController(path = "/user/rights/")
public class RightsController extends AuthorizedController {

    @Override
    protected Object handleRequest(String requestJson, HttpRequest request, User user) {
        ensurePOST(request);

        String command = parseCommand(request.getUriPath());
        switch (command) {
            // Url: /user/rights/read
            case "read":
                return doRead(requestJson, user);
            // Url: /user/rights/update
            case "update":
                return doUpdate(requestJson, user);
            default:
                throw new RuntimeException("No API for path '" + request.getUriPath() + "'");
        }
    }

    private Object doRead(String requestJson, User caller) {
        ReadRequest request = toModel(requestJson, ReadRequest.class);
        User userToRead = userService.ensureUser(request.login);

        boolean actionOnSelfAccount = caller.equals(userToRead);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                userToRead.getRights();

        ensureHasPermission(caller, Action.READ, Subject.ACCOUNT_RIGHTS, ownerRights);

        return new ReadResponse(userToRead.getRights().toString());
    }


    private Object doUpdate(String requestJson, User caller) {
        UpdateRequest request = toModel(requestJson, UpdateRequest.class);
        User userToUpdate = userService.ensureUser(request.login);

        boolean actionOnSelfAccount = caller.equals(userToUpdate);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                userToUpdate.getRights();

        ensureHasPermission(caller, Action.UPDATE, Subject.ACCOUNT_RIGHTS, ownerRights);

        userService.updateRights(userToUpdate, Rights.fromString(request.rights));
        return SuccessResponse.INSTANCE;
    }

    static class ReadRequest extends AbstractRequest {
        public String login;

        public ReadRequest() {
        }

        public ReadRequest(String session, String login) {
            super(session);
            this.login = login;
        }
    }

    static class ReadResponse extends SuccessResponse {
        public String rights;

        public ReadResponse() {
        }

        public ReadResponse(String rights) {
            this.rights = rights;
        }

        public String getRights() {
            return rights;
        }
    }


    static class UpdateRequest extends AbstractRequest {
        public String login;
        public String rights;

        public UpdateRequest() {
        }

        public UpdateRequest(String session, String login, String rights) {
            super(session);
            this.login = login;
            this.rights = rights;
        }
    }

    @Inject
    private UserService userService;
    @Inject
    private SessionService sessionService;
}
