package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.log.Loggable;
import com.borunovv.core.server.nio.core.protocol.IDeliveryCallback;
import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.session.ISession;
import org.springframework.util.Assert;

public abstract class AbstractMessageProcessor extends Loggable implements IMessageProcessor {

    private static final int MESSAGE_RESEND_START_TIMEOUT_MS = 5;
    private static final double MESSAGE_RESEND_MULTIPLIER = 2.0;
    private static final int MESSAGE_RESEND_ATTEMPTS_COUNT = 17;
    private static final long MESSAGE_RESEND_MAX_COOLDOWN_MS = 10000; // 10 sec

    private static final ExponentialTimeout cooldownTimeoutCalculator = new ExponentialTimeout(
            MESSAGE_RESEND_START_TIMEOUT_MS,
            MESSAGE_RESEND_MULTIPLIER,
            MESSAGE_RESEND_ATTEMPTS_COUNT,
            MESSAGE_RESEND_MAX_COOLDOWN_MS);


    private IMessageDispatcher messageDispatcher;

    protected IMessageDispatcher getDispatcher() {
        Assert.notNull(messageDispatcher,
                "Message dispatcher not set! Forgot to call setMessageDispatcher() at server startup ?");
        return messageDispatcher;
    }

    @Override
    public void setMessageDispatcher(IMessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void processOutputMessage(IMessage msg) {
        ISession session = msg.getSession();

        if (session.isClosed()) {
            // Сессия уже закрыта, юзер отключен. Сообщение уже никому не нужно. Забиваем на него.
            logger.trace("AbstractMessageProcessor: dropped message (session is closed: "
                    + session.getClientRemoteAddress() + "), message: " + msg);

            // Уведомим отправителя, что не удалось отправить (асинхронно).
            notifySenderMessageNotDelivered(msg, "Session closed");
            return;
        }

        int sendAttemptNumber = msg.incrementDeliveryAttemptsCount(); // 1..
        if (!session.queueMessageToClient(msg)) {
            // Сессия не позволила в нее записать (ее очередь забита).
            // Попробуем переслать сообщение попозже..
            long cooldownTimeMs = cooldownTimeoutCalculator.getTimeout(sendAttemptNumber);
            if (cooldownTimeMs > 0) {
                // Идем на очередной штрафной круг
                getDispatcher().queueOutputMessage(msg, cooldownTimeMs);
                logger.warn("AbstractMessageProcessor: Cooldown: " + cooldownTimeMs + " [" + session.getClientRemoteAddress() + "]");
            } else {
                msg.setEndDeliveryTime(System.currentTimeMillis());
                // Мы превысили кол-во попыток переслать сообщение.
                // Похоже, клиент помер или игнорит нас.
                logger.error("AbstractMessageProcessor: Send retry giving up. "
                        + "Rejected message (session will be forced to close: "
                        + session.getClientRemoteAddress() + "), message: " + msg);
                // Принудительно рвем связь, ибо нефиг трепать нам нервы.
                session.close("Error: closed by server. Send retry giving up.\nMessage: " + msg
                        + "\n\nWait time: " + msg.getDeliveryTime()
                        + " ms. Session queue size: " + session.getOutputQueueSize());
                notifySenderMessageNotDelivered(msg, "Delivery retry giving up. Session forced to close.");
            }
        }
    }

    private void notifySenderMessageNotDelivered(final IMessage msg, final String errorMsg) {
        for (final IDeliveryCallback callback : msg.getDeliveryCallbacks()) {
            getDispatcher().queueRunnableTask(() -> callback.onSentFailed(new Exception(errorMsg)));
        }
    }

    @Override
    public void onInputMessageReject(IMessage message) {
        logger.error("MessageDispatcher: rejected input message (queue is full): "
                + message.getSession().getClientRemoteAddress() + ", message: " + message);
    }

    @Override
    public void onInputMessageError(IMessage message, Exception cause) {
        logger.error("MessageDispatcher: input message error: "
                + message.getSession().getClientRemoteAddress() + ", message: " + message, cause);
    }

    @Override
    public void onInputMessageError(Exception cause) {
        logger.error("MessageDispatcher: input message error.", cause);
    }


    @Override
    public void onOutputMessageReject(IMessage message) {
        logger.error("MessageDispatcher: rejected output message (queue if full): "
                + message.getSession().getClientRemoteAddress() + ", message: " + message);
    }

    @Override
    public void onOutputMessageError(IMessage message, Exception cause) {
        logger.error("MessageDispatcher: output message error: "
                + message.getSession().getClientRemoteAddress() + ", message: " + message, cause);
    }

    @Override
    public void onOutputMessageError(Exception cause) {
        logger.error("MessageDispatcher: output message error.", cause);
    }
}
