package com.borunovv.jogging.web.controllers;

import org.junit.Ignore;
import org.junit.Test;

public class InfiniteServerTest extends AbstractControllerTest {

    @Ignore
    @Test
    public void getUptime() throws Exception {
        System.out.println("Test url: http://localhost:9195/uptime");
        while (joggingServer.isRunning()) {
            Thread.sleep(10000);
            System.out.println("Server started in infinite loop (to stop: http://localhost:9195/__stop)");
        }
    }
}