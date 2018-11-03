package com.borunovv.core.server.nio.http.protocol;

import com.borunovv.core.server.nio.core.protocol.AbstractDeliverable;
import com.borunovv.core.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse extends AbstractDeliverable {

    private int status;
    private Map<String, String> headers = new HashMap<String, String>();
    private byte[] content;

    public HttpResponse(int status) {
        this.status = status;
        disableCachingOnClientSide();
    }

    public HttpResponse(int status, byte[] content) {
        this(status);
        this.content = content;
    }

    public HttpResponse(int status, Map<String, String> headers, byte[] content) {
        this(status, content);
        this.headers = headers != null ?
                headers :
                new HashMap<>();
    }

    public HttpResponse setStatus(int status) {
        this.status = status;
        return this;
    }

    public HttpResponse setContent(byte[] content, String contentType) {
        this.content = content;
        setHeader("Content-Type", contentType);
        return this;
    }

    public HttpResponse setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public void setRedirect(String url) {
        setStatus(302);
        setHeader("Location", url);
    }

    public void writeHtml(String html) {
        setContent(StringUtils.uft8StringToBytes(html), ContentType.HTML);
    }

    public void writePlainText(String text) {
        setContent(StringUtils.uft8StringToBytes(text), ContentType.TEXT);
    }

    public void writeJson(String json) {
        setContent(StringUtils.uft8StringToBytes(json), ContentType.JSON);
    }

    public byte[] marshall() {
        String header = "HTTP/1.1 " + status + " " + getStatusText(status) + "\r\n";
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header += entry.getKey() + ": " + entry.getValue() + "\r\n";
            }
        }
        header += "Content-Length: " + (content != null ? content.length : 0) + "\r\n";

        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        try {
            bas.write(StringUtils.uft8StringToBytes(header));
        } catch (IOException e) {
            throw new RuntimeException("Can't write header to byte array stream, header: " + header, e);
        }
        bas.write('\r');
        bas.write('\n');
        if (content != null) {
            try {
                bas.write(content);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write content to byte stream", e);
            }
        }
        return bas.toByteArray();
    }

    private void disableCachingOnClientSide() {
        if (headers == null) {
            headers = new LinkedHashMap<>();
        }

        setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        setHeader("Pragma", "no-cache");
        setHeader("Expires", "0");
    }

    private static String getStatusText(int status) {
        switch (status) {
            case 200:
                return "OK";
            case 101:
                return "Switching Protocols";
            case 301:
                return "Moved Permanently";
            case 302:
                return "Found";
            case 404:
                return "Not Found";
            case 500:
                return "Server Error";
            default:
                return "";
        }
    }

    @Override
    public String toString() {
        return "HttpResponse{status: " + status
                + ", content size: " + (content != null ? content.length : 0) + "}";
    }
}
