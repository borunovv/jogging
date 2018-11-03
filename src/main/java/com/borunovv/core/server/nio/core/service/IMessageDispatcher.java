package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.session.SessionEvent;

public interface IMessageDispatcher {

    void start();
    void stop();

    void queueInputMessage(IMessage msg);
    void queueOutputMessage(IMessage msg);
    void queueOutputMessage(IMessage msg, long delayMilliseconds);
    void queueRunnableTask(Runnable runnable);

    void queueSessionEvent(SessionEvent sessionEvent);
    void onMessageSent(IMessage msg);
    void onMessageSentFailed(IMessage msg, Throwable cause);
}
