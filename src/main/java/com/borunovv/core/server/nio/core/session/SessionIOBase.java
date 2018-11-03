package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.log.Loggable;
import com.borunovv.core.util.Assert;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class SessionIOBase extends Loggable {

    private static final int INITIAL_BUFFER_SIZE = 1024;

    protected ISession session;
    private ByteBuffer workBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);


    protected SessionIOBase(ISession session) {
        Assert.isTrue(session != null, "session is null");
        this.session = session;
    }

    public ByteBuffer getWorkBuffer() {
        return workBuffer;
    }

    // Вернет флажки для асинхронного I/O (типа OP_READ | OP_WRITE)
    // Чтобы система в след. раз для данной сесси уведомила нас о готовности читать/писать в клиента.
    public abstract int getSelectionKeyFlags();

    /**
     * Увеличивает размер рабочего буфера.
     * Вызывается серваком, когда пакет от клиента не помещается в рабочий буфер.
     */
    protected ByteBuffer enlargeWorkBuffer(int preferredSize) throws IOException {
        if (workBuffer.capacity() >= preferredSize) {
            // У нас и так размер больше, чем просят. Ничего не делаем.
            return workBuffer;
        }

        int newSize = Math.min(preferredSize, getMaxPacketSize());
        if (workBuffer.capacity() < newSize) {
            ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
            workBuffer.flip();
            newBuffer.put(workBuffer);
            workBuffer = newBuffer;
            return workBuffer;
        } else {
            throw new IOException(
                    "Maximal work buffer limit reached (too big packet). Max allowed size: "
                            + getMaxPacketSize() + " bytes");
        }
    }

    protected ByteBuffer resetWorkBufferSizeIfNeed() {
        if (workBuffer.capacity() > getCommonPacketSize()) {
            workBuffer = ByteBuffer.allocate(getCommonPacketSize());
        }
        return workBuffer;
    }

    protected void putAndPrepareForRead(byte[] data, int length) throws IOException {
        if (workBuffer.capacity() < length) {
            if (length > getMaxPacketSize()) {
                throw new IOException(
                        "Maximal work buffer limit reached (too big packet). Max allowed size: "
                                + getMaxPacketSize() + " bytes");
            }
            workBuffer = ByteBuffer.allocate(length);
        }

        workBuffer.clear();
        workBuffer.put(data, 0, length);
        workBuffer.flip();
    }

    private int getMaxPacketSize() {
        return session.getProtocol().getMaxPacketSize();
    }

    private int getCommonPacketSize() {
        return session.getProtocol().getCommonPacketSize();
    }
}
