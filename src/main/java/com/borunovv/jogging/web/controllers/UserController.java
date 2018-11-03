package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.util.Assert;
import com.borunovv.core.web.HttpController;
import com.borunovv.jogging.permissions.Action;
import com.borunovv.jogging.permissions.Subject;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.SessionService;
import com.borunovv.jogging.users.service.UserService;
import com.borunovv.jogging.web.model.AbstractRequest;
import com.borunovv.jogging.web.model.RequestWithPagination;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
@HttpController(path = "/user/")
public class UserController extends AuthorizedController {

    @Override
    protected Object handleRequest(String requestJson, HttpRequest request, User user) {
        ensurePOST(request);

        String command = parseCommand(request.getUriPath());
        switch (command) {
            // Url: /user/create
            case "create":
                return doCreate(requestJson, user);

            // Url: /user/update
            case "update":
                return doUpdate(requestJson, user);

            // Url: /user/delete
            case "delete":
                return doDelete(requestJson, user);

            // Url: /user/list
            case "list":
                return doList(requestJson, user);

            default:
                throw new RuntimeException("No API for path '" + request.getUriPath() + "'");
        }
    }

    private Object doCreate(String requestJson, User caller) {
        ensureHasPermission(caller, Action.CREATE, Subject.ACCOUNT, Rights.USER);

        CreateRequest request = toModel(requestJson, CreateRequest.class);
        userService.registerNewUser(request.login, request.pass);
        return SuccessResponse.INSTANCE;
    }

    private Object doUpdate(String requestJson, User caller) {
        UpdateRequest request = toModel(requestJson, UpdateRequest.class);
        User userToUpdate = userService.ensureUser(request.login);
        boolean actionOnSelfAccount = caller.equals(userToUpdate);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                userToUpdate.getRights();

        ensureHasPermission(caller, Action.UPDATE, Subject.ACCOUNT, ownerRights);

        userService.updatePassword(userToUpdate, request.pass);
        return SuccessResponse.INSTANCE;
    }

    private Object doDelete(String requestJson, User caller) {
        DeleteRequest request = toModel(requestJson, DeleteRequest.class);
        User userToDelete = userService.ensureUser(request.login);
        boolean actionOnSelfAccount = caller.equals(userToDelete);
        Rights ownerRights = actionOnSelfAccount ?
                Rights.Self :
                userToDelete.getRights();

        ensureHasPermission(caller, Action.DELETE, Subject.ACCOUNT, ownerRights);

        userService.delete(userToDelete);
        return SuccessResponse.INSTANCE;
    }

    private Object doList(String requestJson, User caller) {
        List<Rights> allowedRightsToRead = new ArrayList<>();
        for (Rights rights : Rights.values()) {
            if (hasPermission(caller, Action.READ, Subject.ACCOUNT, rights)) {
                allowedRightsToRead.add(rights);
            }
        }
        Assert.isTrue(!allowedRightsToRead.isEmpty(), "Not enough rights.");

        ListRequest request = toModel(requestJson, ListRequest.class);
        long offset = request.getOffset();
        long count = request.getCount();
        List<String> logins = new ArrayList<>();
        if (count > 0) {
            List<User> users = userService.findAll(offset, count, allowedRightsToRead);
            for (User user : users) {
                logins.add(user.getLogin());
            }
        }

        return new ListResponse(logins);
    }

    static class CreateRequest extends AbstractRequest {
        public String login;
        public String pass;

        public CreateRequest() {
        }

        public CreateRequest(String login, String pass) {
            this.login = login;
            this.pass = pass;
        }
    }

    static class UpdateRequest extends AbstractRequest {
        public String login;
        public String pass;

        public UpdateRequest() {
        }

        public UpdateRequest(String session, String login, String pass) {
            super(session);
            this.login = login;
            this.pass = pass;
        }
    }

    static class DeleteRequest extends AbstractRequest {
        public String login;

        public DeleteRequest() {
        }

        public DeleteRequest(String session, String login) {
            super(session);
            this.login = login;
        }
    }

    static class ListRequest extends RequestWithPagination {
        public ListRequest(String session, long offset, long count) {
            setSession(session);
            setOffset(offset);
            setCount(count);
        }
    }

    static class ListItem {
        public String login;
    }

    static class ListResponse extends SuccessResponse {
        public List<String> logins;

        public ListResponse() {
        }

        public ListResponse(List<String> logins) {
            this.logins = logins;
        }
    }

    @Inject
    private UserService userService;
    @Inject
    private SessionService sessionService;
}
