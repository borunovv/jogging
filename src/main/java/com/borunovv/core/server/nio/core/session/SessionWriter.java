package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.util.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Конечный автомат по асинхронноой записи в сокет.
 */
public class SessionWriter extends SessionIOBase {

    private static final int MAX_QUEUE_SIZE = 3000;

    private enum State {Ready, Writing}

    private State state = State.Ready;
    private final ConcurrentLinkedQueue<BufferWithCustomData> queue = new ConcurrentLinkedQueue<>();
    private volatile int maxQueueSize = 0;

    public SessionWriter(ISession session) {
        super(session);
    }

    /**
     * Вызывается асинхронно.
     * Вернет true, если удачно поставлен в очередь.
     */
    public boolean queuePacket(ByteBuffer packet, Object customData) {
        // Тут неатомарная проверка, но не страшно если очередь чуть переполнится
        // (максимум на кол-во элементов, равному кол-ву конкурирующих потоков).
        // Зато не надо дорого лочить.
        if (canQueuePacket()) {
            queue.add(new BufferWithCustomData(packet, customData));
            maxQueueSize = Math.max(maxQueueSize, queue.size());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Вернет true если можно _попробовать_ положить пакет в очередь.
     * Теоритически нас могут опередить из другого потока.
     */
    public boolean canQueuePacket() {
        return queue.size() < MAX_QUEUE_SIZE;
    }

    /**
     * Вызывается серваком при возможности писать в клиент.
     */
    public void onCanWrite(SelectionKey key, SocketChannel client) throws IOException {
        try {
            switch (state) {
                case Ready:
                    BufferWithCustomData nextPacket = queue.peek();

                    if (nextPacket != null) {
                        Assert.isTrue(nextPacket.buffer != null, "nextPacket.getBuffer() is null!");
                        Assert.isTrue(nextPacket.buffer.hasArray(), "nextPacket.getBuffer().hasArray == false!");

                        // Есть очередной пакет данных для записи, копируем в рабочий буфер..
                        putAndPrepareForRead(nextPacket.buffer.array(), nextPacket.buffer.limit());
                        // И сразу же пытаемся писать.
                        if (doWrite(key, client)) {
                            // Целиком все записать не получилось - меняем состояние.
                            state = State.Writing;
                        } else {
                            // Пакет отправлен, удаляем его из очереди.
                            queue.poll();
                        }
                    }
                    break;
                case Writing:
                    if (!doWrite(key, client)) {
                        // Пакет отправлен, удаляем его из очереди.
                        queue.poll();
                        state = State.Ready;
                    }
                    break;
            }
        } catch (Exception e) {
            onPacketSendFailed(key, e);
            // Пакет не отправлен, но все-равно удаляем его из очереди.
            queue.poll();
            throw new IOException("Failed to write data into channel.", e);
        }
    }

    // Для статистики и для расчета загруженности клиента (для тротлинга / замедления).
    public int getMsgQueueSize() {
        return queue.size();
    }
    public int getMaxMsgQueueSize() {
        return maxQueueSize;
    }

    // Вернет процент заполненности оцереди пакетов.
    // Используется для замедления коммуникации (тротлинг).
    public float getLoadFactor() {
        // Внимание, из-за оптимизации многопоточности,
        // у нас очередь может иногда слегка выходить за MAX_QUEUE_SIZE
        // (максимум на кол-во элементов, равному кол-ву конкурирующих потоков).
        // См. комменты внутри метода queuePacket()
        // Поэтому, чтобы получить процент заполненности очереди, не превосходящий 1,
        // надо обрезать по верхней границе.
        float percent = ((float) getMsgQueueSize()) / MAX_QUEUE_SIZE;
        percent = Math.max(0.0f, percent);
        percent = Math.min(1.0f, percent);
        return percent;
    }

    /**
     * Вернет true если надо продолжать запись.
     */
    private boolean doWrite(SelectionKey key, SocketChannel client) throws IOException {
        ByteBuffer buffer = getWorkBuffer();
        Assert.notNull(buffer, "workBuffer is null");

        if (buffer.remaining() > 0) {
            int written = client.write(buffer);
            if (written > 0) {
                session.onBytesTransferred(0, written);
            }
            if (!buffer.hasRemaining()) {
                int packetSize = buffer.position();
                buffer = resetWorkBufferSizeIfNeed();
                buffer.clear();
                onPacketSent(key, packetSize);
                return false;
            }
        }

        return buffer.hasRemaining();
    }

    private void onPacketSent(SelectionKey key, int packetSize) {
        BufferWithCustomData justSentPacket = queue.peek();
        Assert.isTrue(justSentPacket != null, "Expecting non null packet..hmm..(" + session.getClientRemoteAddress() + ")");

        session.onPacketSent(key, justSentPacket.customData, packetSize);
    }

    private void onPacketSendFailed(SelectionKey key, Throwable cause) {
        BufferWithCustomData currentPacket = queue.peek();
        if (currentPacket == null) {
            throw new RuntimeException("Expecting non null packet..hmm..(" + session.getClientRemoteAddress()
                    + "). The packet write error is below: ", cause);
        }

        session.onPacketSentFailed(key, currentPacket.customData, cause);
    }

    /**
     * Вызывается серваком часто (heart beating).
     */
    @Override
    public int getSelectionKeyFlags() {
        switch (state) {
            case Ready:
                // Мы ничего не пишем ..
                return hasSomethingToWrite() ?
                        SelectionKey.OP_WRITE : // ..но в очереди что-то есть! Начинаем писать!
                        0; // ..а если очередь пуста...ждем дальше.
            case Writing:
                return SelectionKey.OP_WRITE;
        }
        return 0;
    }

    private boolean hasSomethingToWrite() {
        return !queue.isEmpty(); // nextPacketToSend.get() != null;
    }


    private class BufferWithCustomData {
        final ByteBuffer buffer;
        final Object customData;

        BufferWithCustomData(ByteBuffer buffer, Object customData) {
            this.buffer = buffer;
            this.customData = customData;
        }
    }
}
