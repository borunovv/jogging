package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.service.IMessageDispatcher;

public abstract class AbstractSessionFactory implements ISessionFactory {

    private IMessageDispatcher messageDispatcher;
    private IMessageProtocol messageProtocol;
    protected final boolean sequentialConsistent;

    public AbstractSessionFactory(IMessageDispatcher messageDispatcher,
                                  IMessageProtocol messageProtocol,
                                  boolean sequentialConsistent) {
        this.messageDispatcher = messageDispatcher;
        this.messageProtocol = messageProtocol;
        this.sequentialConsistent = sequentialConsistent;
    }

    protected abstract ISession createNewSession(IMessageDispatcher messageDispatcher,
                                                 IMessageProtocol messageProtocol);


    @Override
    public ISession createSession() {
        return createNewSession(messageDispatcher, messageProtocol);
    }

    @Override
    public String toString() {
        return "Session factory: \n" +
                (messageProtocol != null ?
                        "  " + messageProtocol.toString().replaceAll("\\n", "\n  ") :
                        "") + "\n" +
                (messageDispatcher != null ?
                        "  " + messageDispatcher.toString().replaceAll("\\n", "\n  ") :
                        "");
    }
}
