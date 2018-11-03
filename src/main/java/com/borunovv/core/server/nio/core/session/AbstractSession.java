package com.borunovv.core.server.nio.core.session;

import com.borunovv.core.log.Loggable;
import com.borunovv.core.server.nio.core.protocol.IDeliveryCallback;
import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.service.IMessageDispatcher;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.IOUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSession extends Loggable implements ISession {

    private static final int MAX_INPUT_MESSAGE_QUEUE_SIZE = 2000;
    private static final long MAX_INPUT_MSG_PROCESS_TIME_BEFORE_IGNORE_SEQUENTIAL = 5000;
    private static final long BYTE_RATE_MEASURE_PERIOD_MS = 5000;

    private SocketAddress clientRemoteAddress = null;
    private long lastClientActivityTime = 0;
    private long lastPacketStartReadTime = 0;
    private volatile boolean closeRequested = false;
    private volatile boolean sessionClosed = false;
    private volatile String closeReason = null;

    private SessionReader sessionReader = new SessionReader(this);
    private SessionWriter sessionWriter = new SessionWriter(this);

    private final SessionContext context = new SessionContext();

    private final ConcurrentLinkedQueue<IMessage> inputMessages = new ConcurrentLinkedQueue<>();
    private long lastTimeSequentialRequestStartedToProcess = 0;
    private volatile int maxInputQueueSize;

    private volatile long totalBytesRead = 0;
    private volatile long totalBytesWritten = 0;

    private volatile long lastByteRateMeasureStartTime = 0;
    private volatile long lastByteRateMeasureBytesReadValue = 0;
    private volatile long lastByteRateMeasureBytesWrittenValue = 0;

    private volatile int lastInputByteRate = 0;
    private volatile int lastOutputByteRate = 0;

    private volatile int maxInputByteRate = 0;
    private volatile int maxOutputByteRate = 0;

    private IMessageDispatcher messageDispatcher;
    private IMessageProtocol messageProtocol;
    private final boolean sequentialConsistent;

    private enum SequentialState {READY, WAITING_RESPONSE}
    private final AtomicReference<SequentialState> sequentialState = new AtomicReference<>(SequentialState.READY);


    protected AbstractSession(IMessageDispatcher messageDispatcher,
                              IMessageProtocol messageProtocol,
                              boolean sequentialConsistent) {

        Assert.isTrue(messageDispatcher != null, "messageDispatcher is null");
        Assert.isTrue(messageProtocol != null, "messageProtocol is null");

        this.messageDispatcher = messageDispatcher;
        this.messageProtocol = messageProtocol;
        this.sequentialConsistent = sequentialConsistent;
    }

    protected abstract void onClientConnected(SocketChannel client);

    protected abstract void onClientDisconnected(SocketChannel client);

    protected abstract boolean onMessageCome(IMessage msg);

    protected abstract void onMessageSent(IMessage msg);

    protected abstract void onMessageSentFailed(IMessage msg, Throwable cause);

    protected abstract void onError(String errorMsg, Exception cause);

    @Override
    public int init(SocketChannel client) {
        clientRemoteAddress = client.socket().getRemoteSocketAddress();
        updateLastClientActivityTime();
        onClientConnected(client);
        messageDispatcher.queueSessionEvent(SessionEvent.makeClientConnected(this));
        return getSelectionKeyFlags();
    }

    @Override
    public void close(String reason) {
        closeRequested = true;
        closeReason = reason;
    }

    @Override
    public void close(SelectionKey key, String reason) {
        sessionClosed = true;
        onClientDisconnected((SocketChannel) key.channel());
        messageDispatcher.queueSessionEvent(SessionEvent.makeClientDisconnected(this, reason));

        key.cancel();
        IOUtils.close(key.channel());
        logger.trace("Session: Client closed: " + getClientRemoteAddress() + ". Reason: " + reason);

        closeRequested = true;
    }

    @Override
    public boolean isClosed() {
        return sessionClosed || closeRequested;
    }

    @Override
    public SocketAddress getClientRemoteAddress() {
        return clientRemoteAddress;
    }

    @Override
    public IMessageDispatcher getMessageDispatcher() {
        return messageDispatcher;
    }

    @Override
    public IMessageProtocol getProtocol() {
        return messageProtocol;
    }

    @Override
    public void onPacketStart(SelectionKey selectionKey) {
        lastPacketStartReadTime = System.currentTimeMillis();
        logger.trace("Session: Start read request from client: " + getClientRemoteAddress());
    }

    @Override
    public void onPacketFinish(SelectionKey key, byte[] data, int len) {
        long currentTime = System.currentTimeMillis();
        logger.trace("Session: Finish read request from client: " + getClientRemoteAddress()
                + ", " + len + " bytes. Time reading: " + (currentTime - lastPacketStartReadTime) + " ms");

        try {
            // Парсим.
            IMessage msg = getProtocol().unmarshall(this, data, len);
            msg.setDeliverySizeInBytes(len);
            msg.setEndDeliveryTime(currentTime);
            msg.setStartDeliveryTime(lastPacketStartReadTime);

            if (onMessageCome(msg)) {
                queueInputMessage(msg);
            }
        } catch (Exception e) {
            notifyError("Failed to parse packet. Data:\n" + Arrays.toString(Arrays.copyOf(data, len)), e);
        }
    }

    private void queueInputMessage(IMessage msg) {
        if (!sequentialConsistent) {
            messageDispatcher.queueInputMessage(msg);
            return;
        }

        if (sequentialState.compareAndSet(SequentialState.READY, SequentialState.WAITING_RESPONSE)) {
            // Захватили состояние. Можно слать запрос в диспетчер.
            if (inputMessages.isEmpty()) {
                startProcessInputMessage(msg);
            } else {
                inputMessages.add(msg);
                maxInputQueueSize = Math.max(maxInputQueueSize, inputMessages.size());
                startProcessInputMessage(inputMessages.poll());
            }
        } else {
            inputMessages.add(msg);
            maxInputQueueSize = Math.max(maxInputQueueSize, inputMessages.size());
            if (inputMessages.size() > MAX_INPUT_MESSAGE_QUEUE_SIZE / 5) {
                setSlowDownFactor((float) inputMessages.size() / MAX_INPUT_MESSAGE_QUEUE_SIZE);
            }

            boolean waitingRequestTooLong = System.currentTimeMillis() - lastTimeSequentialRequestStartedToProcess >
                    MAX_INPUT_MSG_PROCESS_TIME_BEFORE_IGNORE_SEQUENTIAL;

            if (waitingRequestTooLong) {
                startProcessInputMessage(inputMessages.poll());
                logger.warn("Session: WARNING!!! Sequential: ignored because of process msg timeout");
            }
        }
    }

    private void startProcessInputMessage(IMessage msg) {
        if (msg != null) {
            messageDispatcher.queueInputMessage(msg);
            lastTimeSequentialRequestStartedToProcess = System.currentTimeMillis();
        }
    }

    @Override
    public void onPacketSent(SelectionKey selectionKey,
                             Object customDataAssociatedWithPacket,
                             int packetSize) {
        try {
            // Это может быть пинг!
            IMessage msg = (IMessage) customDataAssociatedWithPacket;
            msg.setDeliverySizeInBytes(packetSize);
            onMessageSent(msg);
            messageDispatcher.onMessageSent(msg);
        } finally {
            // Вызываем колбэк завершения отправки пакета.
            notifyMessageSentSuccessIfNeed((IMessage) customDataAssociatedWithPacket);
        }
    }

    @Override
    public void onPacketSentFailed(SelectionKey selectionKey,
                                   Object customDataAssociatedWithPacket,
                                   Throwable cause) {
        logger.trace("Session: Failed to sent packet to client: "
                + selectionKey.channel() + ": " + customDataAssociatedWithPacket, cause);

        try {
            if (customDataAssociatedWithPacket != null) {
                ((IMessage) customDataAssociatedWithPacket).setEndDeliveryTime(
                        System.currentTimeMillis());
            }

            onMessageSentFailed((IMessage) customDataAssociatedWithPacket, cause);
            messageDispatcher.onMessageSentFailed((IMessage) customDataAssociatedWithPacket, cause);
        } finally {
            // Вызываем колбэк неудачной отправки пакета.
            notifyMessageSentFailedIfNeed((IMessage) customDataAssociatedWithPacket, cause);
        }
    }

    @Override
    public void onBytesTransferred(long inputBytesCount, long outputBytesCount) {
        if (inputBytesCount > 0 || outputBytesCount > 0) {
            updateLastClientActivityTime();
        }

        // Non-atomic but we can live with it.
        totalBytesRead += inputBytesCount;
        totalBytesWritten += outputBytesCount;

        long curTime = System.currentTimeMillis();
        if (curTime - lastByteRateMeasureStartTime >= BYTE_RATE_MEASURE_PERIOD_MS) {
            long totalBytesReadDelta = totalBytesRead - lastByteRateMeasureBytesReadValue;
            long totalBytesWrittenDelta = totalBytesWritten - lastByteRateMeasureBytesWrittenValue;

            lastInputByteRate = (int) (totalBytesReadDelta * 1000 / (curTime - lastByteRateMeasureStartTime));
            lastOutputByteRate = (int) (totalBytesWrittenDelta * 1000 / (curTime - lastByteRateMeasureStartTime));

            // Non-atomic but we can live with it.
            maxInputByteRate = Math.max(maxInputByteRate, lastInputByteRate);
            maxOutputByteRate = Math.max(maxOutputByteRate, lastOutputByteRate);

            lastByteRateMeasureBytesReadValue = totalBytesRead;
            lastByteRateMeasureBytesWrittenValue = totalBytesWritten;

            lastByteRateMeasureStartTime = curTime;
        }
    }

    @Override
    public void onHeartBit(SelectionKey selectionKey) {
        if (closeRequested) {
            close(selectionKey, closeReason);
        } else if (checkClientInactivity(selectionKey)) {
            sessionReader.onHeartBit(selectionKey);
            selectionKey.interestOps(getSelectionKeyFlags());
        }

        if (sequentialConsistent && !inputMessages.isEmpty()) {
            if (sequentialState.compareAndSet(SequentialState.READY, SequentialState.WAITING_RESPONSE)) {
                IMessage msg = inputMessages.poll();
                if (msg != null) {
                    startProcessInputMessage(msg);
                } else {
                    sequentialState.set(SequentialState.READY);
                }
            } else {
                boolean waitingResponseTooLong =
                        System.currentTimeMillis() - lastTimeSequentialRequestStartedToProcess >
                                MAX_INPUT_MSG_PROCESS_TIME_BEFORE_IGNORE_SEQUENTIAL;

                if (waitingResponseTooLong) {
                    startProcessInputMessage(inputMessages.poll());
                    logger.warn("Session: Sequential problem: wait too long for response");
                }
            }
        }
    }

    @Override
    public void onCanRead(SelectionKey key, SocketChannel client) throws IOException {
        sessionReader.onCanRead(key, client);
    }

    @Override
    public void onCanWrite(SelectionKey key, SocketChannel client) throws IOException {
        sessionWriter.onCanWrite(key, client);
    }

    @Override
    public boolean queueMessageToClient(IMessage msg) {
        boolean isSuccess = false;
        if (sessionWriter.canQueuePacket()) {
            byte[] rawData = getProtocol().marshall(msg);
            isSuccess = sessionWriter.queuePacket(ByteBuffer.wrap(rawData), msg);
            if (isSuccess) {
                logger.trace("Session: Queued packet to client. Size: " + rawData.length);
            }

        }

        if (isSuccess && sequentialConsistent && msg.isResponse()) {
            IMessage nextMsg = inputMessages.poll();
            if (nextMsg != null) {
                startProcessInputMessage(nextMsg);
            } else {
                sequentialState.set(SequentialState.READY);
            }
        }

        return isSuccess;
    }

    @Override
    public long getInactivityTimeMs() {
        return lastClientActivityTime > 0 ?
                System.currentTimeMillis() - lastClientActivityTime :
                0;
    }

    @Override
    public ISessionContext getContext() {
        return context;
    }

    @Override
    public int getOutputQueueSize() {
        return sessionWriter.getMsgQueueSize();
    }

    @Override
    public int getMaxOutputQueueSize() {
        return sessionWriter.getMaxMsgQueueSize();
    }

    @Override
    public int getInputQueueSize() {
        return inputMessages.size();
    }
    public int getMaxInputQueueSize() {
        return maxInputQueueSize;
    }

    @Override
    public float getLoadFactor() {
        return sessionWriter.getLoadFactor();
    }

    @Override
    public void setSlowDownFactor(float factor) {
        sessionReader.setSlowDawnFactor(factor);
    }

    @Override
    public float getSlowDownFactor() {
        return sessionReader.getSlowDownFactor();
    }

    protected void notifyError(String errorMsg, Exception e) {
        try {
            onError(errorMsg, e);
        } catch (Exception e1) {
            logger.error("Session: (" + getClientRemoteAddress() + ") Failed to notify about error ("
                    + errorMsg + ")", e1);
        }
    }

    private int getSelectionKeyFlags() {
        return sessionReader.getSelectionKeyFlags()
                | sessionWriter.getSelectionKeyFlags();
    }

    private void notifyMessageSentSuccessIfNeed(final IMessage message) {
        boolean hasCallbacks = message != null
                && message.getDeliveryCallbacks() != null;

        if (!hasCallbacks) {
            return;
        }

        for (final IDeliveryCallback callback : message.getDeliveryCallbacks()) {
            try {
                messageDispatcher.queueRunnableTask(() -> callback.onSentSuccess());
            } catch (Exception e) {
                notifyError("Error while put notification about packet sent success to queue (using callback)", e);
            }
        }
    }

    private void notifyMessageSentFailedIfNeed(final IMessage message, final Throwable cause) {
        boolean hasCallbacks = message != null
                && message.getDeliveryCallbacks() != null;

        if (!hasCallbacks) {
            return;
        }

        for (final IDeliveryCallback callback : message.getDeliveryCallbacks()) {
            try {
                messageDispatcher.queueRunnableTask(() -> callback.onSentFailed(cause));
            } catch (Exception e) {
                notifyError("Error while put notification about packet sent failed (using callback)", e);
            }
        }
    }

    private void updateLastClientActivityTime() {
        lastClientActivityTime = System.currentTimeMillis();
    }

    private boolean isClientInaсtivityTimeout() {
        long inactivityTimeoutMs = getProtocol().getInactivityTimeoutSeconds() * 1000;
        return getInactivityTimeMs() >= inactivityTimeoutMs;
    }

    private boolean checkClientInactivity(SelectionKey selectionKey) {
        if (isClientInaсtivityTimeout()) {
            logger.trace("Session: Client inactivity timeout, will be closed: " + selectionKey.channel());
            close(selectionKey, "Inactivity timeout");
            return false;
        }
        return true;
    }
}
