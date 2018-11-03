package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.server.nio.core.session.ClientClosedException;
import com.borunovv.core.server.nio.core.session.ISession;
import com.borunovv.core.server.nio.core.session.ISessionFactory;
import com.borunovv.core.service.IConsumer;
import com.borunovv.core.util.IOUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class NIOReadWriteThread extends AbstractNIOThread implements IConsumer<SocketChannel> {

    private static final long HEART_BIT_DELAY_MS = 5;
    private static final int MAX_NEW_CLIENTS_QUEUE_SIZE = 1000;

    private Selector rwSelector;
    private ISessionFactory sessionFactory;
    private ConcurrentLinkedQueue<SocketChannel> newClients = new ConcurrentLinkedQueue<>();
    private long lastHeartBitTime = 0;
    private AtomicInteger subStateInIteration = new AtomicInteger(0);

    public NIOReadWriteThread(ISessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void consume(SocketChannel client) {
        Assert.notNull(rwSelector, "Illegal state: rwSelector is not initialized. Current state: " + getState());
        if (newClients.size() < MAX_NEW_CLIENTS_QUEUE_SIZE) {
            newClients.add(client);
        } else {
            try {
                logger.error("NIO RW Thread: rejected new client because of queue overflow, IP: "
                        + client.getRemoteAddress());
            } catch (Exception e) {
                logger.error("NIO RW Thread: rejected new client because of queue overflow."
                        + " IP: undefined (logger error below).", e);
            } finally {
                IOUtils.close(client);
            }
        }
    }

    @Override
    protected void onThreadStart() {
        try {
            rwSelector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize R/W NIO thread", e);
        }
    }

    @Override
    protected void onThreadStop() {
        closeAllSessions(rwSelector);
        IOUtils.close(rwSelector);
        rwSelector = null;

        try {
            logger.trace("NIO RWThread stopped.");
        } catch (Exception ignore) {
        }
    }

    @Override
    protected void onThreadIteration() {
        subStateInIteration.set(0);
        registerNewClients();

        subStateInIteration.set(1);
        processReadyClients();

        subStateInIteration.set(2);
        broadcastHeartBitIfNeed();
    }

    private void registerNewClients() {
        long start = System.currentTimeMillis();
        SocketChannel client;
        while ((client = newClients.poll()) != null) {
            registerClientInSelector(client);
            if (System.currentTimeMillis() - start > HEART_BIT_DELAY_MS) {
                break;
            }
        }
    }

    private void registerClientInSelector(SocketChannel client) {
        ISession session = sessionFactory.createSession();
        int selectionKeyFlags = session.init(client);

        try {
            client.register(rwSelector, selectionKeyFlags, session); // session as attachment
        } catch (ClosedChannelException e) {
            throw new RuntimeException("Failed to register client in R/W selector", e);
        }
    }

    private void processReadyClients() {
        List<SelectionKey> keys = selectReadyClients();
        for (SelectionKey key : keys) {
            if (isStopRequested()) {
                break;
            }
            doTransferData(key);
        }
    }

    private List<SelectionKey> selectReadyClients() {
        try {
            int count = rwSelector.select(HEART_BIT_DELAY_MS);
            return count > 0 ?
                    getValidKeysOnly(rwSelector.selectedKeys()) :
                    Collections.<SelectionKey>emptyList();
        } catch (IOException e) {
            throw new RuntimeException("RW NIO thread: failed to select next portion of clients", e);
        }
    }

    private void doTransferData(SelectionKey key) {
        ISession session = getSession(key);
        SocketChannel client = (SocketChannel) key.channel();

        try {
            if (key.isReadable() && key.isValid()) {
                session.onCanRead(key, client);
            }
            if (key.isWritable() && key.isValid()) {
                session.onCanWrite(key, client);
            }
        } catch (ClientClosedException e) {
            logger.trace("NIO RW Thread: Client disconnected (" + session.getClientRemoteAddress() + ")");
            closeClient(key, "Closed by client (while R/W): " + e.getMessage());
        } catch (Exception e) {
            long logId = logger.error("NIO RW Thread: Client error. Force to close connection ("
                    + session.getClientRemoteAddress() + ")", e);
            closeClient(key, "Closed by server (error: " + logId + "): " + e.getMessage());
        }
    }

    private List<SelectionKey> getValidKeysOnly(Set<SelectionKey> keys) {
        List<SelectionKey> result = new ArrayList<>(keys.size());
        Iterator<SelectionKey> iterator = keys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if (key.isValid()) {
                result.add(key);
            }
        }
        return result;
    }

    private void broadcastHeartBitIfNeed() {
        if (System.currentTimeMillis() - lastHeartBitTime < HEART_BIT_DELAY_MS) {
            return;
        }

        try {
            Set<SelectionKey> allKeys = getAllSelectionKeys(rwSelector);
            List<ISession> validSessions = new ArrayList<>(allKeys.size());
            for (SelectionKey key : allKeys) {
                ISession session = getSession(key);
                if (key.isValid()) {
                    try {
                        session.onHeartBit(key);
                        validSessions.add(session);
                    } catch (Exception e) {
                        logger.error("NIO RW Thread: Error while heart bitting ("
                                + session.getClientRemoteAddress() + ")", e);
                    }
                }
            }
        } finally {
            lastHeartBitTime = System.currentTimeMillis();
        }
    }

    private ISession getSession(SelectionKey key) {
        return (ISession) key.attachment();
    }

    private Set<SelectionKey> getAllSelectionKeys(Selector rwSelector) {
        Set<SelectionKey> allKeys = null;
        while (allKeys == null) {
            try {
                allKeys = new HashSet<>(rwSelector.keys());
            } catch (ConcurrentModificationException ignore) {
            }
        }
        return allKeys;
    }

    private void closeAllSessions(Selector rwSelector) {
        Set<SelectionKey> allKeys = getAllSelectionKeys(rwSelector);
        for (SelectionKey key : allKeys) {
            closeClient(key, "Closed by server");
        }

        SocketChannel client;
        while ((client = newClients.poll()) != null) {
            IOUtils.close(client);
        }
    }

    private void closeClient(SelectionKey key, String reason) {
        ISession session = getSession(key);
        if (session != null) {
            session.close(key, reason);
        } else {
            key.cancel();
            try {
                key.channel().close();
                logger.trace("NIO RW Thread: Client closed (with null session). Reason: " + reason);
            } catch (IOException ignore) {
            }
        }
        key.attach(null);
    }
}
