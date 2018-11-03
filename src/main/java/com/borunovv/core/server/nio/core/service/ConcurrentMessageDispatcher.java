package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.log.Loggable;
import com.borunovv.core.server.nio.core.cooldown.CooldownAsyncNotifier;
import com.borunovv.core.server.nio.core.cooldown.CooldownListener;
import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.session.ISessionListener;
import com.borunovv.core.server.nio.core.session.SessionEvent;
import com.borunovv.core.util.Assert;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ConcurrentMessageDispatcher extends Loggable implements IMessageDispatcher, CooldownListener<IMessage> {

    private Config config;
    private ConcurrentMessageProcessor<IMessage> inputMessageProcessor;
    private ConcurrentMessageProcessor<IMessage> outputMessageProcessor;
    private ConcurrentMessageProcessor<Runnable> callbackTaskProcessor; // Для колбэков - нотификаций об отправке сообщений
    private CooldownAsyncNotifier<IMessage> cooldownMessageProcessor;   // Для повторных отправок.

    public ConcurrentMessageDispatcher(Config config) {
        this.config = config;
    }

    @Override
    public void queueInputMessage(IMessage msg) {
        if (inputMessageProcessor == null) {
            throw new IllegalStateException("Dispatcher not started.");
        }

        if (msg.getStartDeliveryTime() == 0) {
            msg.setStartDeliveryTime(System.currentTimeMillis());
        }

        boolean queued = inputMessageProcessor.queue(msg);

        if (!queued) {
            logger.error("Dispatcher: Failed to queue input message. " + msg);
        }
    }

    @Override
    public void queueOutputMessage(IMessage msg) {
        if (outputMessageProcessor == null) {
            throw new IllegalStateException("Dispatcher not started.");
        }
        if (msg.getStartDeliveryTime() == 0) {
            msg.setStartDeliveryTime(System.currentTimeMillis());
        }
        boolean queued = outputMessageProcessor.queue(msg);

        if (queued) {
            logger.trace("Dispatcher: queued message. " + msg);
        } else {
            logger.error("Dispatcher: failed to queue output message. " + msg);
        }
    }

    @Override
    public void queueOutputMessage(IMessage msg, long cooldownMilliseconds) {
        if (cooldownMessageProcessor.size() < config.getOutputQueueSize()) {
            cooldownMessageProcessor.put(msg, cooldownMilliseconds, this);
        } else {
            queueOutputMessage(msg);
        }
    }

    @Override
    public void onCooldownFinished(IMessage msg) {
        queueOutputMessage(msg);
    }

    @Override
    public void onCooldownError(Throwable error) {
        logger.error("Error inside cooldown notifier", error);
    }


    @Override
    public void queueRunnableTask(Runnable callbackRunner) {
        if (callbackTaskProcessor == null) {
            throw new IllegalStateException("Dispatcher not started.");
        }
        callbackTaskProcessor.queue(callbackRunner);
    }

    @Override
    public void queueSessionEvent(final SessionEvent sessionEvent) {
        Assert.isTrue(sessionEvent != null, "Bad session event: null");
        // Асинхронно уведомляем если надо
        if (config.getMessageProcessor() != null
                && config.getMessageProcessor() instanceof ISessionListener) {
            final ISessionListener listener = (ISessionListener) config.getMessageProcessor();
            queueRunnableTask(() -> listener.onSessionEvent(sessionEvent));
        }
    }

    @Override
    public void onMessageSent(IMessage msg) {
        msg.setEndDeliveryTime(System.currentTimeMillis());
    }

    @Override
    public void onMessageSentFailed(IMessage msg, Throwable cause) {
        logger.error("Dispatcher: failed to send message. " + msg, cause);
    }

    @Override
    public void start() {
        startCooldownMessageProcessor();
        startOutputMessageProcessor();
        startCallbackTaskProcessor();
        startInputMessageProcessor();

        logger.trace("Dispatcher: started (" + config.getInputWorkerThreads() + " worker threads)");
    }

    @Override
    public void stop() {
        if (inputMessageProcessor != null
                && outputMessageProcessor != null
                && callbackTaskProcessor != null
                && cooldownMessageProcessor != null) {

            cooldownMessageProcessor.stop();
            inputMessageProcessor.stop();
            outputMessageProcessor.stop();
            callbackTaskProcessor.stop();

            cooldownMessageProcessor = null;
            inputMessageProcessor = null;
            outputMessageProcessor = null;
            callbackTaskProcessor = null;

            logger.trace("MessageDispatcher: stopped.");
        }
    }

    private void startCooldownMessageProcessor() {
        cooldownMessageProcessor = new CooldownAsyncNotifier<>();
        cooldownMessageProcessor.start();
    }

    private void startInputMessageProcessor() {
        inputMessageProcessor = new ConcurrentMessageProcessor<>(
                config.getInputWorkerThreads(), // Кол-во рабочих потоков на выгребание сообщений
                config.getInputQueueSize(),     // Макс. размер очереди входящих сообщений.
                config.getInputQueueTimeoutSeconds() * 1000, // Макс. таймаут ожидания переполненной очереди.
                new InputMessageHandler());     // Обработчик входящих сообщений.
        inputMessageProcessor.start();
    }

    private void startOutputMessageProcessor() {
        outputMessageProcessor = new ConcurrentMessageProcessor<>(
                config.getOutputWorkerThreads(), // Кол-во рабочих потоков на отправку исходящих сообщений.
                config.getOutputQueueSize(),     // Макс. размер очереди исходящих сообщений.
                config.getOutputQueueTimeoutSeconds() * 1000, // Макс. таймаут ожидания переполненной очереди.
                new OutputMessageHandler());     // Обработчик исходящих сообщений.
        outputMessageProcessor.start();
    }

    private void startCallbackTaskProcessor() {
        callbackTaskProcessor = new ConcurrentMessageProcessor<>(
                config.getCallbackWorkerThreads(), // Кол-во рабочих потоков на обработку колбэков.
                config.getCallbackQueueSize(),     // Макс. размер очереди колбэков.
                config.getCallbackQueueTimeoutSeconds() * 1000, // Макс. таймаут ожидания переполненной очереди.
                new RunnableTaskHandler());        // Обработчик колбэков.
        callbackTaskProcessor.start();
    }

    @Override
    public String toString() {
        return config != null ?
                config.toString() :
                super.toString();
    }

    private class InputMessageHandler implements IMessageHandler<IMessage> {
        @Override
        @SuppressWarnings("unchecked")
        public void handle(IMessage message) {
            // Для статистики.
            message.setEndDeliveryTime(System.currentTimeMillis());

            // Отправляем сообщение на обработку.
            config.getMessageProcessor()
                    .processInputMessage(message);
        }

        @Override
        public void onReject(IMessage message) {
            message.setEndDeliveryTime(System.currentTimeMillis());
            config.getMessageProcessor().onInputMessageReject(message);
        }

        @Override
        public void onError(IMessage message, Exception cause) {
            message.setEndDeliveryTime(System.currentTimeMillis());
            config.getMessageProcessor().onInputMessageError(message, cause);
        }

        @Override
        public void onError(Exception cause) {
            config.getMessageProcessor().onInputMessageError(cause);
        }
    }

    private class OutputMessageHandler implements IMessageHandler<IMessage> {
        @Override
        @SuppressWarnings("unchecked")
        public void handle(IMessage message) {
            config.getMessageProcessor()
                    .processOutputMessage(message);
        }

        @Override
        public void onReject(IMessage message) {
            config.getMessageProcessor().onOutputMessageReject(message);
        }

        @Override
        public void onError(IMessage message, Exception cause) {
            config.getMessageProcessor().onOutputMessageError(message, cause);
        }

        @Override
        public void onError(Exception cause) {
            config.getMessageProcessor().onOutputMessageError(cause);
        }
    }

    private class RunnableTaskHandler implements IMessageHandler<Runnable> {
        @Override
        public void handle(Runnable task) {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Error while running message callback.", e);
            }
        }

        @Override
        public void onReject(Runnable task) {
            logger.error("Rejected running message callback.");
        }

        @Override
        public void onError(Runnable task, Exception cause) {
            logger.error("Error while running message callback.", cause);
        }

        @Override
        public void onError(Exception cause) {
            logger.error("Error while running message callback.", cause);
        }
    }

    public static class Config {
        // Кол-во рабочих потоков на выгребание и обработку входящих сообщений.
        private int inputWorkerThreads;

        // Макс. размер очереди входящих сообщений.
        private int inputQueueSize;

        // Макс. таймаут ожидания переполненной очереди входящих сообщений
        // (при попытке добавить в нее. Кинет исключение если не дождется).
        private int inputQueueTimeoutSeconds;


        // Кол-во рабочих потоков на обработку исходящих сообщений.
        private int outputWorkerThreads;

        // Макс. размер очереди исходящих сообщений.
        private int outputQueueSize;

        // Макс. таймаут ожидания переполненной очереди входящих сообщений
        // (при попытке добавить в нее. Кинет исключение если не дождется).
        private int outputQueueTimeoutSeconds;


        // Кол-во рабочих потоков на обработку колбэков об успешности/неуспешности отправки.
        private int callbackWorkerThreads;

        // Макс. размер очереди колбэков об успешности/неуспешности отправки.
        private int callbackQueueSize;

        // Макс. таймаут ожидания переполненной очереди колбэков об успешности/неуспешности отправки
        // (при попытке добавить в нее. Кинет исключение если не дождется).
        private int callbackQueueTimeoutSeconds;

        // Обработчик единичных сообщений.
        private IMessageProcessor messageProcessor;

        public Config(int inputWorkerThreads,
                      int inputQueueSize,
                      int inputQueueTimeoutSeconds,
                      int outputWorkerThreads,
                      int outputQueueSize,
                      int outputQueueTimeoutSeconds,
                      int callbackWorkerThreads,
                      int callbackQueueSize,
                      int callbackQueueTimeoutSeconds,
                      IMessageProcessor messageProcessor) {

            this.inputWorkerThreads = inputWorkerThreads;
            this.inputQueueSize = inputQueueSize;
            this.inputQueueTimeoutSeconds = inputQueueTimeoutSeconds;

            this.outputWorkerThreads = outputWorkerThreads;
            this.outputQueueSize = outputQueueSize;
            this.outputQueueTimeoutSeconds = outputQueueTimeoutSeconds;

            this.callbackWorkerThreads = callbackWorkerThreads;
            this.callbackQueueSize = callbackQueueSize;
            this.callbackQueueTimeoutSeconds = callbackQueueTimeoutSeconds;

            this.messageProcessor = messageProcessor;
        }

        public int getInputWorkerThreads() {
            return inputWorkerThreads;
        }

        public int getInputQueueSize() {
            return inputQueueSize;
        }

        public int getInputQueueTimeoutSeconds() {
            return inputQueueTimeoutSeconds;
        }

        public int getOutputWorkerThreads() {
            return outputWorkerThreads;
        }

        public int getOutputQueueSize() {
            return outputQueueSize;
        }

        public int getOutputQueueTimeoutSeconds() {
            return outputQueueTimeoutSeconds;
        }

        public int getCallbackWorkerThreads() {
            return callbackWorkerThreads;
        }

        public int getCallbackQueueSize() {
            return callbackQueueSize;
        }

        public int getCallbackQueueTimeoutSeconds() {
            return callbackQueueTimeoutSeconds;
        }

        public IMessageProcessor getMessageProcessor() {
            return messageProcessor;
        }

        @Override
        public String toString() {
            return "Message dispatcher config:\n"
                    + "  input worker threads: " + inputWorkerThreads + "\n"
                    + "  input queue size: " + inputQueueSize + "\n"
                    + "  input queue timeout: " + inputQueueTimeoutSeconds + " sec.\n"
                    + "  output worker threads: " + outputWorkerThreads + "\n"
                    + "  output queue size: " + outputQueueSize + "\n"
                    + "  output queue timeout: " + outputQueueTimeoutSeconds + " sec.\n"
                    + "  callback worker threads: " + callbackWorkerThreads + "\n"
                    + "  callback queue size: " + callbackQueueSize + "\n"
                    + "  callback queue timeout: " + callbackQueueTimeoutSeconds + " sec.";
        }
    }
}
