package com.borunovv.jogging.web.controllers;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class UptimeControllerTest extends AbstractControllerTest {

    @Test
    public void getUptime() throws Exception {
        // ACT
        String json = sendGet("/uptime");

        // VERIFY
        UptimeController.Model model = toModel(json, UptimeController.Model.class);
        assertTrue(model.uptime.startsWith("00:00:"));
        assertFalse(model.gitSha.isEmpty());
    }
}