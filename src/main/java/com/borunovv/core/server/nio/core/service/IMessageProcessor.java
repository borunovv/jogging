package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.server.nio.core.protocol.IMessage;

public interface IMessageProcessor {
    void setMessageDispatcher(IMessageDispatcher messageDispatcher);

    void processInputMessage(IMessage msg);
    void processOutputMessage(IMessage msg);

    void onInputMessageReject(IMessage message);
    void onInputMessageError(IMessage message, Exception cause);
    void onInputMessageError(Exception cause);

    void onOutputMessageReject(IMessage message);
    void onOutputMessageError(IMessage message, Exception cause);
    void onOutputMessageError(Exception cause);
}
