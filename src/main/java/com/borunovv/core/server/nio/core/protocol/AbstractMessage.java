package com.borunovv.core.server.nio.core.protocol;

import com.borunovv.core.server.nio.core.session.ISession;

public abstract class AbstractMessage extends AbstractDeliverable implements IMessage {
    private ISession session;
    private boolean messageIsResponse;

    // Для сообщений-ответов тут будет прикрепляться соответствующее
    // сообщение-запрос (нужно для статистики по latency).
    private IMessage linkedMessage;


    public AbstractMessage(ISession session, boolean isResponse) {
        this.session = session;
        this.messageIsResponse = isResponse;
    }

    @Override
    public ISession getSession() {
        return session;
    }

    @Override
    public void setLinkedMessage(IMessage msg) {
        this.linkedMessage = msg;
    }

    @Override
    public IMessage getLinkedMessage() {
        return linkedMessage;
    }

    @Override
    public boolean isResponse() {
        return messageIsResponse;
    }
}
