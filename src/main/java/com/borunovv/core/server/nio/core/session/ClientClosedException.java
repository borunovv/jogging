package com.borunovv.core.server.nio.core.session;

import java.io.IOException;

public class ClientClosedException extends IOException {

    public ClientClosedException(String message) {
        super(message);
    }

    public ClientClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
