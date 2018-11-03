package com.borunovv.core.server.nio.core.protocol;

import com.borunovv.core.server.nio.core.session.ISession;

public interface IMessage extends IDeliverable {
    ISession getSession();
    void setLinkedMessage(IMessage msg);
    IMessage getLinkedMessage();

    boolean isResponse();
}
