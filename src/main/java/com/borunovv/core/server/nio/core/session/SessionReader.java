package com.borunovv.core.server.nio.core.session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Random;

public class SessionReader extends SessionIOBase {

    private static final long SLOW_DOWN_FACTOR_FADE_TIME_MS = 5000;

    public SessionReader(ISession session) {
        super(session);
    }
    private volatile boolean readingNow = false;

    private volatile float slowDownFactor = 0;
    private volatile long lastSlowDownFactorChangedTime = 0;
    private byte[] slowDownBuffer = new byte[1024]; // 1K

    private final Random random = new Random(System.currentTimeMillis());

    public void onCanRead(SelectionKey key, SocketChannel client) throws IOException {
        doRead(key, client);
    }

    public boolean isReadingNow() {
        return readingNow;
    }

    private void doRead(SelectionKey key, SocketChannel client) throws IOException {
        ByteBuffer buffer = getWorkBuffer();
        boolean bufferWasEmpty = (buffer.position() == 0);

        if (bufferWasEmpty && client == null) {
            return;
        }

        int len = 0;
        if (client != null) {
            try {
                // Для замедления чтения из клиента
                float curSlowDownFactor = getSlowDownFactor();
                if (curSlowDownFactor > 0.0f) {
                    int remaining = buffer.remaining();
                    int newSize = Math.max(0, (int) ((1.0 - curSlowDownFactor) * slowDownBuffer.length));
                    if (remaining - newSize >= 10) {
                        if (newSize > 0) {
                            ByteBuffer tmp = ByteBuffer.wrap(slowDownBuffer, 0, newSize);
                            len = client.read(tmp);
                            if (len > 0) {
                                buffer.put(slowDownBuffer, 0, len);
                            }
                        }
                    } else {
                        len = client.read(buffer);
                    }
                } else {
                    len = client.read(buffer);
                }

                if (len == -1) {
                    throw new ClientClosedException("Client closed (can't read)");
                }
            } catch (IOException e) {
                throw new ClientClosedException("Client I/O error", e);
            }
        } else {
            // Нас вызвали на heartbeat
            if (needSkipReading()) {
                return;
            }
        }

        boolean somethingRed = (len > 0);
        if (somethingRed) {
            session.onBytesTransferred(len, 0);
        }

        if (bufferWasEmpty && somethingRed) {
            readingNow = true;
            session.onPacketStart(key);
        }

        if (somethingRed || (!bufferWasEmpty && client == null)) {
            int correctPacketLen = getCorrectPacketLength(buffer);

            if (correctPacketLen > 0) {
                buffer.flip();  // prepare to read.
                session.onPacketFinish(key, buffer.array(), correctPacketLen);
                if (buffer.limit() == correctPacketLen) {
                    buffer = resetWorkBufferSizeIfNeed();
                    buffer.clear();
                    readingNow = false;
                } else {
                    buffer.position(correctPacketLen);
                    buffer.compact();

                    readingNow = true;
                    session.onPacketStart(key);
                }
            }
        }

        if (buffer.remaining() == 0) {
            enlargeWorkBuffer(buffer.capacity() * 2);
        }
    }

    @Override
    public int getSelectionKeyFlags() {
        return needSkipReading() ?
                0 :
                SelectionKey.OP_READ;
    }

    private boolean needSkipReading() {
        float skipProbability = getSlowDownFactor();
        return skipProbability > 0.0f && random.nextFloat() <= skipProbability;
    }

    private int getCorrectPacketLength(ByteBuffer buffer) {
        return session.getProtocol().checkPacket(buffer);
    }

    public void onHeartBit(SelectionKey key) {
        if (isReadingNow()) {
            try {
                doRead(key, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setSlowDawnFactor(float factor) {
        slowDownFactor = Math.max(0.0f, Math.min(factor, 1.0f));
        lastSlowDownFactorChangedTime = System.currentTimeMillis();
    }

    public float getSlowDownFactor() {
        return fadeSlowDownFactor();
    }

    private float fadeSlowDownFactor() {
        if (slowDownFactor == 0.0f) return 0.0f;

        long deltaTime = System.currentTimeMillis() - lastSlowDownFactorChangedTime;
        // [0 .. SLOW_DOWN_FACTOR_FADE_TIME_MS]
        long clampedDeltaTime = Math.max(0, Math.min(deltaTime, SLOW_DOWN_FACTOR_FADE_TIME_MS));
        // [0..1]
        float fadePercent = (float) ((double) clampedDeltaTime / SLOW_DOWN_FACTOR_FADE_TIME_MS);

        float curSlowDownFactor = slowDownFactor * (1.0f - fadePercent);
        if (curSlowDownFactor < 0.00001f) {
            slowDownFactor = 0.0f;
            curSlowDownFactor = 0.0f;
        }

        return curSlowDownFactor;
    }
}
