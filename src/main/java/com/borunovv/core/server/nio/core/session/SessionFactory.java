package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.service.IMessageDispatcher;

public class SessionFactory extends AbstractSessionFactory {

    public SessionFactory(IMessageDispatcher messageDispatcher,
                          IMessageProtocol messageProtocol,
                          boolean sequentialConsistent) {
        super(messageDispatcher, messageProtocol, sequentialConsistent);
    }

    @Override
    protected ISession createNewSession(IMessageDispatcher messageDispatcher,
                                        IMessageProtocol messageProtocol) {
        return new Session(messageDispatcher, messageProtocol, sequentialConsistent);
    }
}
