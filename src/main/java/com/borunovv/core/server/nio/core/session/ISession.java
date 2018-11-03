package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.service.IMessageDispatcher;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface ISession {

    // Вернет флаги для селектора
    int init(SocketChannel client);
    // Закрывает асинхронно (не сразу, а на heartbit-e)
    void close(String reason);
    // Закрывает синхронно (сразу)
    void close(SelectionKey key, String reason);
    boolean isClosed();

    void onPacketStart(SelectionKey key);
    void onPacketFinish(SelectionKey key, byte[] data, int length);
    void onPacketSent(SelectionKey key, Object customDataAssociatedWithPacket, int packetSize);
    void onPacketSentFailed(SelectionKey key, Object customDataAssociatedWithPacket, Throwable cause);
    void onBytesTransferred(long inputBytesCount, long outputBytesCount);

    void onCanRead(SelectionKey key, SocketChannel client) throws IOException;
    void onCanWrite(SelectionKey key, SocketChannel client) throws IOException;
    void onHeartBit(SelectionKey selectionKey);

    SocketAddress getClientRemoteAddress();

    // Вернет время, прошедшее с последней активности.
    long getInactivityTimeMs();

    boolean queueMessageToClient(IMessage msg);
    IMessageProtocol getProtocol();
    IMessageDispatcher getMessageDispatcher();

    ISessionContext getContext();

    // Для статистики и тротлинга
    int getOutputQueueSize();    // Размер очереди сессии на отправку.
    int getMaxOutputQueueSize(); // Максимальный допустимый размер очереди на отправку.
    float getLoadFactor(); // Для тротлинга, процент заполненности очереди на отправку, [0..1].

    int getInputQueueSize();    // Размер очереди сессии на получение.
    int getMaxInputQueueSize(); // Максимальный допустимый размер очереди на получение.

    // Для ограничения частоты входящих запросов.[0..1], 0 - разрешена макс. скорость, 1 - запрет на чтение из клиента.
    void setSlowDownFactor(float factor);
    float getSlowDownFactor();
}
