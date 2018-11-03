package com.borunovv.core.server.nio.core.service;

import com.borunovv.core.testing.AbstractTest;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.assertEquals;

public class ConcurrentMessageProcessorTest extends AbstractTest {

    private static final Random random = new Random(System.currentTimeMillis());

    @Test
    public void testQueueTask() throws Exception {
        final AtomicLong processedMessages = new AtomicLong(0);
        ConcurrentMessageProcessor<String> processor = new ConcurrentMessageProcessor<String>(
                10, 100, 10000,
                new Handler(processedMessages));

        int msgCount = 1000;
        processor.start();

        for (int i = 0; i < msgCount; ++i) {
            processor.queue("Message #" + i);
        }

        while (processor.getQueueSize() > 0) {
            Thread.yield();
        }

        processor.stop();

        System.out.println("Processed Tasks: " + processedMessages.get());
        System.out.println("Tasks in queue: " + processor.getQueueSize());
        assertEquals(msgCount, processedMessages.get() + processor.getQueueSize());
    }

    private class Handler implements IMessageHandler<String> {
        private AtomicLong processedMessages;

        public Handler(AtomicLong processedMessages) {
            this.processedMessages = processedMessages;
        }

        @Override
        public void handle(String message) {
            try {
                Thread.sleep(random.nextInt(50));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(Thread.currentThread().getName() + ": Processed message: " + message);
            processedMessages.incrementAndGet();
        }

        @Override
        public void onReject(String message) {
        }

        @Override
        public void onError(String message, Exception cause) {
        }

        @Override
        public void onError(Exception cause) {
        }
    }
}
