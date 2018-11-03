package com.borunovv.jogging.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServerConfig {

    public int getHttpPort() {
        return httpPort;
    }

    public String getHttpLogin() {
        return httpLogin;
    }

    public String getHttpPassword() {
        return httpPassword;
    }

    public boolean isDebug() {
        return !isProduction();
    }

    public boolean isProduction() {
        return productionMode;
    }

    @Value("${http.server.port}")
    private int httpPort;

    @Value("${http.server.login}")
    private String httpLogin;

    @Value("${http.server.password}")
    private String httpPassword;

    @Value("${production.mode}")
    private boolean productionMode;
}
