package com.borunovv.core.web;

import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.server.nio.http.protocol.HttpResponse;

public interface IHttpController {
    void handleRequest(HttpRequest request, HttpResponse response);
}
