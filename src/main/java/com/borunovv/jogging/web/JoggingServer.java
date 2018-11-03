package com.borunovv.jogging.web;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.server.nio.http.protocol.HttpResponse;
import com.borunovv.core.util.JsonUtils;
import com.borunovv.core.web.AbstractHttpControllerServer;
import com.borunovv.jogging.config.ServerConfig;
import com.borunovv.jogging.web.model.ErrorResponse;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

@Service
public class JoggingServer extends AbstractHttpControllerServer {

    public void start() {
        super.start(config.getHttpPort(),
                config.getHttpLogin(),
                config.getHttpPassword(),
                controllers);
    }

    public void startAsync() {
        super.startAsync(config.getHttpPort(),
                config.getHttpLogin(),
                config.getHttpPassword(),
                controllers);
    }

    public boolean isRunning() {
        return super.isRunning();
    }

    public void stop() {
        super.stop();
    }

    @Override
    protected void onError(HttpRequest request, HttpResponse response, String message, Exception cause) {
        long logId = logger.error("HttpController error. Request: "
                        + request
                        + (cause != null ? ". Error: " + cause.getMessage() + "." : ""),
                cause);
        response.writeJson(JsonUtils.toJson(new ErrorResponse(message, logId)));
    }

    @Inject
    private JoggingControllerFactory controllers;
    @Inject
    private ServerConfig config;
}
