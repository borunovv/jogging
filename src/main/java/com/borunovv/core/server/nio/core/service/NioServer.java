package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.server.nio.core.session.ISessionFactory;


public class NioServer {
    private NIOAcceptThread acceptThread;
    private NIOReadWriteThread rwThread;


    public NioServer(int port, int acceptQueueSize, ISessionFactory sessionFactory) {
        rwThread = new NIOReadWriteThread(sessionFactory);
        acceptThread = new NIOAcceptThread(port, acceptQueueSize, rwThread);
    }

    public void start() {
        if (isRunning()) {
            throw new IllegalStateException("NIOServer already running!");
        }

        try {
            boolean bothAreStarted = acceptThread.start() && rwThread.start();
            if (!bothAreStarted) {
                throw new RuntimeException("Failed to start both accept and read/write threads (see logs).");
            }
        } catch (Exception e) {
            stop();
            throw new RuntimeException("Failed to start NioServer:\n" + this, e);
        }
    }

    public void stop() {
        stopAsync();
        while (isRunning()) {
            Thread.yield();
        }
    }

    public void stopAsync() {
        acceptThread.stop();
        rwThread.stop();
    }

    public boolean isRunning() {
        return !isStopped();
    }

    public boolean isStopped() {
        return acceptThread.isStopped() && rwThread.isStopped();
    }
}
