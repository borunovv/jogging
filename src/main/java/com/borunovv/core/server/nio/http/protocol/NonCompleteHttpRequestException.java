package com.borunovv.core.server.nio.http.protocol;

public class NonCompleteHttpRequestException extends Exception {
    NonCompleteHttpRequestException(String message) {
        super(message);
    }
}
