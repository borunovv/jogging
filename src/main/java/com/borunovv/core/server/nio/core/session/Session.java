package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.service.IMessageDispatcher;

import java.nio.channels.SocketChannel;

public class Session extends AbstractSession {

    public Session(IMessageDispatcher messageDispatcher,
                   IMessageProtocol messageProtocol,
                   boolean sequentialConsistent) {
        super(messageDispatcher, messageProtocol, sequentialConsistent);
    }

    @Override
    protected void onClientConnected(SocketChannel client) {
        logger.trace("Session: New client connected: " + getClientRemoteAddress());
    }

    @Override
    protected void onClientDisconnected(SocketChannel client) {
        logger.trace("Session: Client disconnected: " + getClientRemoteAddress());
    }

    @Override
    protected boolean onMessageCome(IMessage msg) {
        logger.trace("Session: message come from client: " + getClientRemoteAddress() + ", " + msg);
        return true;
    }

    @Override
    protected void onMessageSent(IMessage msg) {
        logger.trace("Session: message sent to client: " + getClientRemoteAddress() + ", " + msg);
    }

    @Override
    protected void onMessageSentFailed(IMessage msg, Throwable cause) {
        logger.error("Session: message sent failed: " + getClientRemoteAddress() + ", " + msg, cause);
    }

    @Override
    protected void onError(String errorMessage, Exception cause) {
        logger.error("Session: (" + getClientRemoteAddress() + ") Error: " + errorMessage, cause);
    }
}
