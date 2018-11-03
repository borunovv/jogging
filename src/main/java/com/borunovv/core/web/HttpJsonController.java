package com.borunovv.core.web;

import com.borunovv.core.server.nio.http.protocol.ContentType;
import com.borunovv.core.server.nio.http.protocol.HttpRequest;
import com.borunovv.core.server.nio.http.protocol.HttpResponse;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.JsonUtils;
import com.borunovv.core.util.StringUtils;
import com.google.gson.JsonParseException;

public abstract class HttpJsonController implements IHttpController {

    protected abstract Object handleRequest(String requestBodyJson, HttpRequest request);

    @Override
    public void handleRequest(HttpRequest request, HttpResponse response) {
        String contentType = request.getContentType();
        Assert.isTrue(ContentType.JSON.equalsIgnoreCase(contentType),
                String.format("Expected request Content-Type: %s. Actual: %s", ContentType.JSON, contentType));

        String json = StringUtils.toUtf8String(request.getContent());
        Assert.isTrue(!StringUtils.isNullOrEmpty(json), "Empty json");

        Object responseModel = handleRequest(json, request);

        Assert.isTrue(responseModel != null, "Failed to make response model");
        response.writeJson(JsonUtils.toJson(responseModel));
    }

    protected <T> T toModel(String json, Class<T> clazz) {
        try {
            return JsonUtils.fromJson(json, clazz);
        } catch (JsonParseException e) {
            throw new RuntimeException("Json parse error. Expected model '" + clazz.getSimpleName() + "'");
        }
    }
}
