package com.borunovv.core.server.nio.http.protocol;

import com.borunovv.core.server.nio.core.protocol.AbstractMessage;
import com.borunovv.core.server.nio.core.session.ISession;

public class HttpMessage extends AbstractMessage {

    private HttpRequest request;
    private HttpResponse response;

    public HttpMessage(ISession session, HttpRequest request ,HttpResponse response) {
        super(session, false);
        this.request = request;
        this.response = response;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }

    public boolean hasResponse() {
        return response != null;
    }

    public boolean hasRequest() {
        return request != null;
    }

    @Override
    public String toString() {
        return "HttpMessage{" + (request != null ? request : "[null request]")
                + (response!= null ? ", " + response : "") + "}";
    }
}
