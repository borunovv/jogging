package com.borunovv.core.server.nio.http.service;

import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.service.*;
import com.borunovv.core.server.nio.core.session.SessionFactory;
import com.borunovv.core.server.nio.http.protocol.HttpMessage;
import com.borunovv.core.server.nio.http.protocol.HttpProtocol;
import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.server.nio.http.protocol.HttpResponse;
import com.borunovv.core.service.AbstractService;

public class HttpServer extends AbstractService {

    public static final int ACCEPT_QUEUE_SIZE = 100;
    private static final int MAX_PACKET_SIZE = 10 * 1024 * 1024; // 10 Mb
    private static final int COMMON_PACKET_SIZE = 10 * 1024;     // 10 Kb
    private static final int INACTIVITY_TIMEOUT = 30;

    private static final int INPUT_WORKER_THREADS = 4;
    private static final int INPUT_QUEUE_SIZE = 1000;
    private static final int OUTPUT_WORKER_THREADS = 2;
    private static final int OUTPUT_QUEUE_SIZE = 1000;
    private static final int CALLBACK_WORKER_THREADS = 1;
    private static final int CALLBACK_QUEUE_SIZE = 10;
    private static final int QUEUE_TIMEOUT_SECONDS = 30;

    private NioServer server;
    private ConcurrentMessageDispatcher messageDispatcher;

    private volatile boolean isStarted = false;
    private volatile int port;

    public void start(int port, IHttpController controller) {

        if (!isStarted) {
            logger.info("HttpServer starting ..");

            IMessageProtocol messageProtocol = new HttpProtocol(
                    MAX_PACKET_SIZE,
                    COMMON_PACKET_SIZE,
                    INACTIVITY_TIMEOUT);

            IMessageProcessor messageProcessor = createMessageProcessor(controller);

            messageDispatcher = new ConcurrentMessageDispatcher(getDispatcherConfig(messageProcessor));

            messageProcessor.setMessageDispatcher(messageDispatcher);

            messageDispatcher.start();

            server = new NioServer(
                    port,
                    ACCEPT_QUEUE_SIZE,
                    new SessionFactory(
                            messageDispatcher,
                            messageProtocol,
                            false));

            server.start();
            isStarted = true;
            this.port = port;

            logger.info("HttpServer started successfully (port #" + port + ")");
        } else {
            throw new IllegalStateException("Server already started!");
        }
    }

    public void stop() {
        if (isStarted) {
            logger.info("HttpServer stopping (port #" + port + ") ..");

            server.stop();
            messageDispatcher.stop();

            server = null;
            messageDispatcher = null;

            isStarted = false;

            logger.info("HttpServer stopped (port #" + port + ")");
            isStarted = false;
        }
    }

    public void stopAsync() {
        if (isRunning()) {
            new Thread(this::stop).start();
        }
    }

    private ConcurrentMessageDispatcher.Config getDispatcherConfig(IMessageProcessor messageProcessor) {
        return new ConcurrentMessageDispatcher.Config(
                INPUT_WORKER_THREADS,
                INPUT_QUEUE_SIZE,
                QUEUE_TIMEOUT_SECONDS,

                OUTPUT_WORKER_THREADS,
                OUTPUT_QUEUE_SIZE,
                QUEUE_TIMEOUT_SECONDS,

                CALLBACK_WORKER_THREADS,
                CALLBACK_QUEUE_SIZE,
                QUEUE_TIMEOUT_SECONDS,
                messageProcessor);
    }

    private IMessageProcessor createMessageProcessor(final IHttpController controller) {
        return new AbstractMessageProcessor() {
            @Override
            public void processInputMessage(IMessage msg) {
                HttpMessage httpMsg = (HttpMessage) msg;
                HttpResponse response = new HttpResponse(200);
                HttpMessage responseMsg = new HttpMessage(msg.getSession(), httpMsg.getRequest(), response);
                controller.handle(httpMsg.getRequest(), response);
                responseMsg.addDeliveryCallbacks(response.getDeliveryCallbacks());
                getDispatcher().queueOutputMessage(responseMsg);
            }
        };
    }

    public boolean isRunning() {
        return isStarted;
    }

    public interface IHttpController {
        void handle(HttpRequest request, HttpResponse response);
    }
}
