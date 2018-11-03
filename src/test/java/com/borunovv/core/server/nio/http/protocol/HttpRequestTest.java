package com.borunovv.core.server.nio.http.protocol;

import com.borunovv.core.testing.AbstractTest;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.HttpMethod;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class HttpRequestTest extends AbstractTest {

    @Test
    public void testParseWithoutContent() throws Exception {
        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Host", "api.opencalais.com");
        extraHeaders.put("Content-Type", "text/xml; charset=utf-8");

        byte[] allRequest = buildRequest("GET", "/the/uri?a=1&b=2", extraHeaders, null);

        HttpRequest req = new HttpRequest(allRequest, allRequest.length);

        assertEquals(HttpMethod.GET, req.getMethod());
        assertEquals("/the/uri?a=1&b=2", req.getUri());
        assertEquals(allRequest.length, req.getMarshalledSize());
        assertEquals(0, req.getContent().length);
    }


    @Test
    public void testParseWithContent() throws Exception {
        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Host", "api.opencalais.com");
        extraHeaders.put("Content-Type", "text/xml; charset=utf-8");
        byte[] content = "Test_Content".getBytes("UTF-8");

        byte[] allRequest = buildRequest("POST", "/the/uri?a=1&b=2", extraHeaders, content);
        HttpRequest req = new HttpRequest(allRequest, allRequest.length);

        assertEquals(HttpMethod.POST, req.getMethod());
        assertEquals("/the/uri?a=1&b=2", req.getUri());
        assertEquals(allRequest.length, req.getMarshalledSize());
        Assert.arraysAreEqual(content, req.getContent());
    }


    @Test(expected = NonCompleteHttpRequestException.class)
    public void testParseWithNotEnoughtContent() throws Exception {
        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Host", "api.opencalais.com");
        extraHeaders.put("Content-Type", "text/xml; charset=utf-8");
        byte[] content = "Test_Content".getBytes("UTF-8");

        byte[] allRequest = buildRequest("POST", "/the/uri?a=1&b=2", extraHeaders, content);
        HttpRequest req = new HttpRequest(allRequest, allRequest.length - 1);
    }

    @Test(expected = NonCompleteHttpRequestException.class)
    public void testParseWithNotEnoughtHeader() throws Exception {
        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("Host", "api.opencalais.com");
        extraHeaders.put("Content-Type", "text/xml; charset=utf-8");
        byte[] content = "Test_Content".getBytes("UTF-8");

        byte[] allRequest = buildRequest("POST", "/the/uri?a=1&b=2", extraHeaders, content);
        HttpRequest req = new HttpRequest(allRequest, 10);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testBadMethod() throws Exception {
        byte[] allRequest = buildRequest("", "/the/uri?a=1&b=2", null, null);
        HttpRequest req = new HttpRequest(allRequest, allRequest.length);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testBadUri() throws Exception {
        byte[] allRequest = buildRequest("GET", "", null, null);
        HttpRequest req = new HttpRequest(allRequest, allRequest.length);
    }


    private byte[] buildRequest(String method,
                                String uri,
                                Map<String, String> headers,
                                byte[] content) throws IOException {
        String header = method + " " + uri + " HTTP/1.1\r\n";
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                header += entry.getKey() + ": " + entry.getValue() + "\r\n";
            }
        }
        header += (content != null ? "Content-Length: " + content.length + "\r\n" : "");

        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        bas.write(header.getBytes("UTF-8"));
        bas.write('\r');
        bas.write('\n');
        if (content != null) {
            bas.write(content);
        }
        return bas.toByteArray();
    }
}
