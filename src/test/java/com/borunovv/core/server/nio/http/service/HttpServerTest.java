package com.borunovv.core.server.nio.http.service;

import com.borunovv.core.server.nio.core.protocol.IDeliveryCallback;
import com.borunovv.core.testing.AbstractTest;
import com.borunovv.core.util.HttpMethod;
import com.borunovv.core.util.UrlReader;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class HttpServerTest extends AbstractTest {

    @Test
    public void testStart() throws Exception {
        final HttpServer server = new HttpServer();
        server.start(8099, (request, response) -> {
            try {
                String responseContent = "Method: " + request.getMethod()
                        + ", URI: " + request.getUri()
                        + ", Content length: " + request.getContent().length;

                response.setContent(
                        responseContent.getBytes("UTF-8"),
                        "text/plain; charset=UTF-8");

                if (request.getUri().equals("/__stop")) {
                    response.addDeliveryCallback(new IDeliveryCallback() {
                        @Override
                        public void onSentSuccess() {
                            stopServer();
                        }

                        @Override
                        public void onSentFailed(Throwable cause) {
                            System.out.println("!!! Response failed to send to client! Cause:\n");
                            cause.printStackTrace();

                            stopServer();
                        }

                        private void stopServer() {
                            // В отдельном потоке (асинхронно), т.к. иначе будет дедлок.
                            // Т.к. обработка происходит в диспетчере, который ждет завершения раб. лошадок,
                            // а мы как раз в такой лошадке)
                            System.out.println("Forced to stop server after request.");
                            server.stopAsync();
                        }
                    });
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });

        // GET - запрос
        String response;
        response = UrlReader.getAsString("localhost:8099/abcd/?a=1");
        assertEquals("Method: GET, URI: /abcd/?a=1, Content length: 0", response);

        // POST - запрос
        UrlReader.Request req = new UrlReader.Request(HttpMethod.POST, "localhost:8099/post/?a=1&b=2");
        req.setExtraHeader("MyHeader", "MyValue");
        req.setContent(getPostData(1024 * 1024 * 9), "text/plain");
        response = UrlReader.send(req).getBodyAsString();
        assertEquals("Method: POST, URI: /post/?a=1&b=2, Content length: " + (1024 * 1024 * 9), response);

        // GET - запрос на остановку
        response = UrlReader.getAsString("localhost:8099/__stop");
        assertEquals("Method: GET, URI: /__stop, Content length: 0", response);

        long startWait = System.currentTimeMillis();
        while (server.isRunning() && System.currentTimeMillis() - startWait < 10 * 1000) {
            Thread.sleep(100);
        }

        if (server.isRunning()) {
            server.stop();
            fail("Server stop wait timeout! 10 sec. Forced to stop.");
        }
    }

    private byte[] getPostData(int size) {
        byte[] data = new byte[size];
        Arrays.fill(data, (byte) 0);
        return data;
    }
}
