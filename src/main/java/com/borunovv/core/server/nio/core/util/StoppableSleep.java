package com.borunovv.core.server.nio.core.util;

public class StoppableSleep {

    public static void sleep(int timeoutMs, IStopper stopper) throws InterruptedException {
        if (timeoutMs <= 10) {
            Thread.sleep(timeoutMs);
        } else {
            long start = System.currentTimeMillis();
            long remain = timeoutMs;
            while (remain > 0 && !stopper.isStopRequested()) {
                Thread.sleep(Math.min(remain, 10));
                remain = timeoutMs - (System.currentTimeMillis() - start);
            }
        }
    }

    public interface IStopper {
        boolean isStopRequested();
    }
}
