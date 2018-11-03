package com.borunovv.jogging.server;

import com.borunovv.core.testing.AbstractTest;
import com.borunovv.jogging.config.ServerConfig;
import com.borunovv.jogging.web.JoggingServer;

import javax.inject.Inject;

public abstract class AbstractJoggingServerTest extends AbstractTest {

    @Override
    protected FixtureInfo[] getFixtures() {
        return new FixtureInfo[]{
                new FixtureInfo("clear_all.yml")
        };
    }

    @Override
    protected void onSetUp() {
        super.onSetUp();
        joggingServer.startAsync();
        while (!joggingServer.isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Test server started (port #" + serverConfig.getHttpPort() + ")");
    }

    @Override
    protected void onTearDown() {
        joggingServer.stop();
        System.out.println("Test server stopped (port #" + serverConfig.getHttpPort() + ")");
        super.onTearDown();
    }


    @Inject
    protected ServerConfig serverConfig;
    @Inject
    protected JoggingServer joggingServer;
}
