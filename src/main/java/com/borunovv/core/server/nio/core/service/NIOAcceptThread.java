package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.service.IConsumer;
import com.borunovv.core.util.IOUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class NIOAcceptThread extends AbstractNIOThread {

    private final int port;
    private final int acceptQueueSize;
    private IConsumer<? super SocketChannel> socketChannelConsumer;

    private ServerSocketChannel serverSocketChannel;
    private Selector acceptSelector;


    public NIOAcceptThread(int port,
                           int acceptQueueSize,
                           IConsumer<? super SocketChannel> socketChannelConsumer) {
        Assert.notNull(socketChannelConsumer, "socketChannelConsumer is null");
        Assert.isTrue(port > 0 && port < 65536, "Bad port: " + port);
        Assert.isTrue(acceptQueueSize > 0, "Bad acceptQueueSize: " + acceptQueueSize);

        this.port = port;
        this.acceptQueueSize = acceptQueueSize;
        this.socketChannelConsumer = socketChannelConsumer;
    }

    public int getPort() {
        return port;
    }

    protected void onThreadStart() {
        try {
            acceptSelector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            ServerSocket ss = serverSocketChannel.socket();
            ss.bind(new InetSocketAddress(port), acceptQueueSize);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Accept NIO thread", e);
        }
    }

    protected void onThreadIteration() {
        int count;
        try {
            count = acceptSelector.select(100);
        } catch (IOException e) {
            throw new RuntimeException("Failed to accept next portion of clients", e);
        }

        if (count > 0) {
            List<SelectionKey> keys = getValidAcceptableKeysOnly(acceptSelector.selectedKeys());
            for (SelectionKey key : keys) {
                if (isStopRequested()) {
                    break;
                }
                acceptClient(key);
            }
        }
    }

    protected void onThreadStop() {
        IOUtils.close(acceptSelector);
        IOUtils.close(serverSocketChannel);
        acceptSelector = null;
        serverSocketChannel = null;
        try {
            logger.trace("NIO AcceptThread stopped (port " + port + ")");
        } catch (Exception ignore) {
        }
    }

    protected void onThreadError(Throwable e) {
        try {
            logger.error("ERROR: In NIO Accept thread (port " + port + ")", e);
        } catch (Exception e2) {
            System.err.println("ERROR: In NIO Accept thread (port " + port + ")");
            e.printStackTrace(System.err);
        }
    }


    private List<SelectionKey> getValidAcceptableKeysOnly(Set<SelectionKey> allKeys) {
        List<SelectionKey> result = new ArrayList<>(allKeys.size());

        Iterator<SelectionKey> iterator = allKeys.iterator();
        while (iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if (key.isValid() && key.isAcceptable()) {
                result.add(key);
            }
        }

        return result;
    }

    private void acceptClient(SelectionKey key) {
        SocketChannel client = null;
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            client = serverChannel.accept();
            //String clientIpAddress = NIOUtils.getRemoteIpAddress(client);
            client.configureBlocking(false);
            client.socket().setKeepAlive(true);
            client.socket().setTcpNoDelay(true);
            socketChannelConsumer.consume(client);
        } catch (Exception e) {
            IOUtils.close(client);
            try {
                logger.error("NIOAcceptThread (port " + port
                        + "): error while accepting client, force to close connection: " + client, e);
            } catch (Exception ignore) {
            }
        }
    }
}
