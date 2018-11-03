package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.HttpMethod;
import com.borunovv.core.web.HttpController;
import com.borunovv.core.web.HttpJsonController;
import com.borunovv.jogging.users.service.UserService;
import com.borunovv.jogging.web.model.SuccessResponse;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@HttpController(path = "/login")
public class LogInController extends HttpJsonController {

    @Override
    protected Object handleRequest(String requestJson, HttpRequest request) {
        Assert.isTrue(request.getMethod() == HttpMethod.POST, "Expected POST method");
        Request req = toModel(requestJson, Request.class);
        String session = service.loginUser(req.login, req.pass);
        return new Response(session);
    }

    public static class Request {
        public String login;
        public String pass;

        public Request() {
        }

        public Request(String login, String pass) {
            this.login = login;
            this.pass = pass;
        }
    }

    public static class Response extends SuccessResponse {
        public String session;

        public Response() {
        }

        public Response(String session) {
            this.session = session;
        }

        public String getSession() {
            return session;
        }
    }

    @Inject
    private UserService service;
}
