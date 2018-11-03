package com.borunovv.jogging.web.controllers;

import com.borunovv.core.server.nio.http.protocol.ContentType;
import com.borunovv.core.util.*;
import com.borunovv.jogging.config.ServerConfig;
import com.borunovv.jogging.server.AbstractJoggingServerTest;
import com.borunovv.jogging.users.model.Rights;
import com.borunovv.jogging.users.model.User;
import com.borunovv.jogging.users.service.UserService;
import com.borunovv.jogging.web.JoggingServer;
import com.google.gson.JsonParseException;

import javax.inject.Inject;
import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

public abstract class AbstractControllerTest extends AbstractJoggingServerTest {

    protected String makeUrl(String path) {
        return "http://localhost:" + serverConfig.getHttpPort()
                + (path.startsWith("/") ? path : "/" + path);
    }

    protected String sendGet(String urlPath) throws IOException {
        return sendAsJson(HttpMethod.GET, urlPath, null);
    }

    protected String sendPost(String urlPath, Object model) throws IOException {
        return sendAsJson(HttpMethod.POST, urlPath, model);
    }

    protected String sendPut(String urlPath, Object model) throws IOException {
        return sendAsJson(HttpMethod.PUT, urlPath, model);
    }

    protected <T> T toModel(String json, Class<T> clazz) {
        try {
            return JsonUtils.fromJson(json, clazz);
        } catch (JsonParseException e) {
            throw new RuntimeException("Json parse error. Expected model '" + clazz.getSimpleName()
                    + "'\nActual json:\n" + json);
        }
    }

    protected String sendDelete(String urlPath, Object model) throws IOException {
        return sendAsJson(HttpMethod.DELETE, urlPath, model);
    }

    private String sendAsJson(HttpMethod method, String urlPath, Object model) throws IOException {
        String json = model != null ?
                JsonUtils.toJson(model) :
                "";

        UrlReader.Request request = new UrlReader.Request(
                method,
                makeUrl(urlPath),
                StringUtils.uft8StringToBytes(json),
                ContentType.JSON);

        UrlReader.Response response = UrlReader.send(request);

        String contentType = response.getHeader("Content-Type") != null ?
                response.getHeader("Content-Type").getValue() :
                "";
        Assert.isTrue(contentType.equalsIgnoreCase(ContentType.JSON),
                "Expected Content-Type: " + ContentType.JSON + ", Actual: " + contentType);

        return response.getBodyAsString();
    }

    protected void registerNewUser(String login, String password) {
        userService.registerNewUser(login, password);
    }

    protected void registerNewUser(String login, String password, Rights rights) {
        User user = userService.registerNewUser(login, password);
        if (rights != Rights.USER) {
            userService.updateRights(user, rights);
        }
    }

    protected String registerAndLogIn(String login, String password) throws IOException {
        registerNewUser(login, password);
        return logInUser(login, password);
    }

    protected String registerAndLogIn(String login, String password, Rights rights) throws IOException {
        registerNewUser(login, password, rights);
        return logInUser(login, password);
    }

    protected String logInUser(String login, String password) throws IOException {
        String json = sendPost("/login", new LogInController.Request(login, password));
        LogInController.Response response = toModel(json, LogInController.Response.class);
        assertEquals("success", response.getStatus());
        String session = response.getSession();
        assertTrue(session.length() > 0);
        return session;
    }

    @Inject
    protected ServerConfig serverConfig;
    @Inject
    protected JoggingServer joggingServer;
    @Inject
    private UserService userService;
}
