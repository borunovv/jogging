package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.HttpMethod;
import com.borunovv.core.util.JsonUtils;
import com.borunovv.core.web.HttpController;
import com.borunovv.core.web.HttpJsonController;
import com.borunovv.jogging.permissions.Action;
import com.borunovv.jogging.permissions.PermissionService;
import com.borunovv.jogging.permissions.Subject;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.SessionService;

import javax.inject.Inject;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AuthorizedController extends HttpJsonController {

    protected abstract Object handleRequest(String requestJson, HttpRequest request, User user);

    @Override
    protected Object handleRequest(String requestJson, HttpRequest request) {
        User user = checkAuthorization(requestJson);
        return handleRequest(requestJson, request, user);
    }

    protected void ensureHasPermission(User actor, Action action, Subject subject, Rights subjectOwner) {
        permissionService.ensureHasPermission(actor.getRights(), action, subject, subjectOwner);
    }

    protected boolean hasPermission(User actor, Action action, Subject subject, Rights subjectOwner) {
        return permissionService.hasPermission(actor.getRights(), action, subject, subjectOwner);
    }

    protected void ensurePOST(HttpRequest request) {
        Assert.isTrue(request.getMethod() == HttpMethod.POST,
                "Expected POST request. Actual: " + request.getMethod());
    }


    @SuppressWarnings("unchecked")
    private User checkAuthorization(String requestJson) {
        String session;
        try {
            Map<String, Object> params = JsonUtils.fromJson(requestJson, Map.class);
            Assert.isTrue(params.containsKey("session"), "Expected session");
            session = params.get("session").toString();
            Assert.isTrue(session.length() > 0, "Session is empty");
        } catch (Exception e) {
            throw new RuntimeException("Authorization failed. " + e.getMessage());
        }

        User user = sessionService.tryGetUserBySession(session);
        Assert.notNull(user, "Session not exists or expired. Please login first.");

        return user;
    }

    protected String parseCommand(String uriPath) {
        HttpController annotation = this.getClass().getAnnotation(HttpController.class);
        Assert.notNull(annotation, "Controller class must have annotation @HttpController: '" + this.getClass().getName() + "'");
        String path = annotation.path();

        Pattern pattern = Pattern.compile(Pattern.quote(path) + "([^/]*)(.*)");
        Matcher matcher = pattern.matcher(uriPath);
        if (matcher.find() && matcher.groupCount() >= 2) {
            if (matcher.groupCount() == 2 || matcher.group(2).equals("/")) {
                String cmd = matcher.group(1).toLowerCase().trim();
                return cmd;
            }
        }
        throw new RuntimeException("No API for path '" + uriPath + "'");
    }

    @Inject
    private SessionService sessionService;
    @Inject
    private PermissionService permissionService;
}
