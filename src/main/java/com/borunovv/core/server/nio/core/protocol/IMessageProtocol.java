package com.borunovv.core.server.nio.core.protocol;

import com.borunovv.core.server.nio.core.session.ISession;

import java.nio.ByteBuffer;

public interface IMessageProtocol {

    int getMaxPacketSize();

    int getCommonPacketSize();

    int getInactivityTimeoutSeconds();

    int getMeaningRequestInactivityTimeoutSeconds();

    int checkPacket(ByteBuffer buffer);

    IMessage unmarshall(ISession session, byte[] data, int length);
    byte[] marshall(IMessage msg);
}
