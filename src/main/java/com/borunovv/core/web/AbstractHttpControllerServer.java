package com.borunovv.core.web;

import com.borunovv.core.log.AsyncLoggerService;
import com.borunovv.core.server.nio.core.protocol.IDeliveryCallback;
import com.borunovv.core.server.nio.http.protocol.BasicAuth;
import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.server.nio.http.protocol.HttpResponse;
import com.borunovv.core.server.nio.http.service.HttpServer;
import com.borunovv.core.service.AbstractService;
import org.springframework.beans.factory.DisposableBean;

import javax.inject.Inject;

public abstract class AbstractHttpControllerServer extends AbstractService
        implements DisposableBean, HttpServer.IHttpController {

    private HttpServer httpServer;
    private String adminBasicAuthLogin;
    private String adminBasicAuthPassword;
    private IHttpControllerFactory controllerFactory;

    @Override
    public void destroy() throws Exception {
        stop();
    }

    protected void start(int port,
                         String adminBasicAuthLogin,
                         String adminBasicAuthPassword,
                         IHttpControllerFactory controllerFactory) {
        stop();
        this.adminBasicAuthLogin = adminBasicAuthLogin;
        this.adminBasicAuthPassword = adminBasicAuthPassword;
        this.controllerFactory = controllerFactory;
        httpServer = new HttpServer();
        httpServer.start(port, this);
        logger.info("Started HTTP controller (port #" + port + ")");
        waitServerStopped();
    }

    protected void startAsync(final int port,
                              final String adminBasicAuthLogin,
                              final String adminBasicAuthPassword,
                              final IHttpControllerFactory controllerFactory) {
        new Thread(new Runnable() { // <- Не превращать в лямбду! Спринг матерится (баг у него там, http://stackoverflow.com/questions/38914731/which-version-of-hibernate-is-suited-for-spring-4-3-2)
            @Override
            public void run() {
                start(port, adminBasicAuthLogin, adminBasicAuthPassword, controllerFactory);
            }
        }).start();
    }

    private void waitServerStopped() {
        while (isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void stop() {
        if (httpServer != null) {
            httpServer.stop();
            httpServer = null;
            logger.trace("HTTP controller stopped.");
        }
    }

    protected boolean isRunning() {
        return httpServer != null && httpServer.isRunning();
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response) {
        String path = request.getUriPath();
        if (path.equals("/__stop")) {
            handleStop(request, response);
        } else {
            handleRequest(request, response);
        }
    }

    protected void handleRequest(HttpRequest request, HttpResponse response) {
        String path = request.getUriPath();

        IHttpController controller = controllerFactory.findController(path);
        if (controller != null) {
            try {
                controller.handleRequest(request, response);
            } catch (Exception e) {
                onError(request, response, e.getMessage(), e);
            }
        } else {
            onError(request, response, "No handler for path: '" + path + "'", null);
        }
    }

    protected abstract void onError(HttpRequest request, HttpResponse response, String message, Exception cause);

    private void handleStop(HttpRequest request, final HttpResponse response) {
        if (!new BasicAuth(adminBasicAuthLogin, adminBasicAuthPassword).check(request, response)) {
            return;
        }

        response.writePlainText("Server stopped.");

        response.addDeliveryCallback(new IDeliveryCallback() {
            @Override
            public void onSentSuccess() {
                stopHttpServer();
            }

            @Override
            public void onSentFailed(Throwable cause) {
                stopHttpServer();
            }

            private void stopHttpServer() {
                logger.trace("HTTP controller: stopping server by '/__stop' request now..");
                httpServer.stopAsync();
            }
        });
    }

    @Inject
    private AsyncLoggerService asyncLogger;
}
