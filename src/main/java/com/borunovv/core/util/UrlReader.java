package com.borunovv.core.util;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class UrlReader {

    static {
        System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }

    private static final int MAX_REDIRECT_COUNT = 20;

    public static String getAsString(String url) throws IOException {
        return get(url).getBodyAsString();
    }

    public static Response send(HttpMethod method, String url) throws IOException {
        return send(new Request(method, url));
    }

    public static Response get(String url) throws IOException {
        return send(HttpMethod.GET, url);
    }

    public static Response post(String url, byte[] content, String contentType) throws IOException {
        return send(new Request(HttpMethod.POST, url, content, contentType));
    }

    public static Response put(String url, byte[] content, String contentType) throws IOException {
        return send(new Request(HttpMethod.PUT, url, content, contentType));
    }

    public static Response delete(String url) throws IOException {
        return send(HttpMethod.DELETE, url);
    }

    public static Response head(String url) throws IOException {
        return send(HttpMethod.HEAD, url);
    }

    public static Response send(Request request) throws IOException {
        Response response = sendWithoutRedirects(request);
        int redirectCount = 0;
        while (isRedirect(response)) {
            if (redirectCount > MAX_REDIRECT_COUNT) {
                throw new IOException("Max redirect count reached: " + MAX_REDIRECT_COUNT);
            }
            request.setUrl(getRedirectLocation(response));
            response = sendWithoutRedirects(request);
            redirectCount++;
        }
        return response;
    }

    private static boolean isRedirect(Response response) {
        int status = response.getStatus();
        return status == 302 || status == 301;
    }

    private static String getRedirectLocation(Response response) {
        return StringUtils.ensureString(response.getHeader("Location").getValue());
    }

    private static Response sendWithoutRedirects(Request request) throws IOException {
        if (StringUtils.isNullOrEmpty(request.getUrl())) {
            throw new IOException("Empty request (url): " + request.getUrl());
        }

        String fixedUrl = (request.getUrl().toLowerCase().startsWith("http://")
                || request.getUrl().toLowerCase().startsWith("https://")) ?
                request.getUrl() :
                "http://" + request.getUrl();

        HttpUriRequest httpRequest = createHttpUriRequestByMethod(request.getMethod(), fixedUrl);

        // Добавляем контент запроса.
        if (request.hasContent()) {
            Assert.isTrue(httpRequest instanceof HttpEntityEnclosingRequest,
                    "Request method: '" + request.method + "' do not support any content. But content is set.");
            HttpEntityEnclosingRequest requestWithEntity = (HttpEntityEnclosingRequest) httpRequest;
            requestWithEntity.setEntity(new ByteArrayEntity(request.getContent()));
            httpRequest.setHeader("Content-Type", request.getContentType());
        }

        // Дополнительные хидеры.
        for (String key : request.getExtraHeaders().keySet()) {
            httpRequest.removeHeaders(key);
            httpRequest.setHeader(key, request.getExtraHeaders().get(key));
        }

        Response response = new Response();

        HttpClient httpclient = getHttpClient();
        try {
            HttpResponse httpResponse = httpclient.execute(httpRequest);
            response.setStatus(httpResponse.getStatusLine().getStatusCode());

            // Copy response headers.
            for (Header h : httpResponse.getAllHeaders()) {
                response.addHeader(h.getName(), h);
            }
            // Copy body.
            response.setBody(readResponseBody(httpResponse));
        } catch (IOException ex) {
            // In case of an IOException the connection will be released
            // back to the connection manager automatically
            throw new IOException("Can't get url: " + request.getUrl(), ex);
        } catch (RuntimeException ex) {
            // In case of an unexpected exception you may want to abort
            // the HTTP request in order to shut down the underlying
            // connection and release it back to the connection manager.
            httpRequest.abort();
            throw new RuntimeException("Can't get url: " + request.getUrl(), ex);
        } finally {
            // When HttpClient instance is no longer needed,
            // shut down the connection manager to ensure
            // immediate deallocation of all system resources
            httpclient.getConnectionManager().shutdown();
        }

        if (request.getMethod() != HttpMethod.HEAD && response.getBody() == null) {
            throw new IOException("Can't get url: " + request.getUrl());
        }

        return response;
    }

    private static HttpUriRequest createHttpUriRequestByMethod(HttpMethod method, String url) {
        switch (method) {
            case HEAD:
                return new HttpHead(url);
            case PUT:
                return new HttpPut(url);
            case GET:
                return new HttpGet(url);
            case POST:
                return new HttpPost(url);
            case DELETE:
                return new HttpDelete(url);
            default:
                throw new IllegalArgumentException("Undefined method: " + method + ". Forgot to implement ?");
        }
    }

    private static byte[] readResponseBody(HttpResponse httpResponse) throws IOException {
        HttpEntity entity = httpResponse.getEntity();
        InputStream instream = entity != null ?
                entity.getContent() :
                null;
        if (instream == null) return null;

        try {
            long contentLength = entity.getContentLength();
            ByteArrayOutputStream bytes =
                    new ByteArrayOutputStream(
                            contentLength > 0 ?
                                    (int) contentLength :
                                    1024);

            int arrayInitialSize = contentLength > 0 ?
                    (int) contentLength :
                    1024 * 8;

            int readBytes = 0;
            byte[] tmp = new byte[arrayInitialSize];
            while ((readBytes = instream.read(tmp)) != -1) {
                bytes.write(tmp, 0, readBytes);
            }

            return bytes.toByteArray();
        } finally {
            IOUtils.close(instream);
        }
    }

    // HttpClient get around the checks of SSL certificate validity.
    // see http://stackoverflow.com/questions/24752485/httpclient-javax-net-ssl-sslpeerunverifiedexception-peer-not-authenticated-when
    private static HttpClient getHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null,
                    new TrustManager[]{new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(
                                X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(
                                X509Certificate[] certs, String authType) {
                        }
                    }}, new SecureRandom());

            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{"TLSv1", "TLSv1.1", "TLSv1.2"},
                    null,
                    new NoopHostnameVerifier());

            HttpClient httpClient = HttpClientBuilder.create().setSSLSocketFactory(socketFactory).build();
            return httpClient;

        } catch (Exception e) {
            e.printStackTrace();
            return HttpClientBuilder.create().build();
        }
    }

    public static class Request {
        private String url;
        private HttpMethod method = HttpMethod.GET;
        private Map<String, String> extraHeaders = new HashMap<String, String>();
        private byte[] content;
        private String contentType;

        public Request(String url) {
            this.url = url;
        }

        public Request(HttpMethod method, String url) {
            this.method = method;
            this.url = url;
        }

        public Request(String method, String url) {
            this.method = HttpMethod.fromString(method);
            this.url = url;
        }

        public Request(HttpMethod method, String url, byte[] content, String contentType) {
            this.method = method;
            this.url = url;
            this.content = content;
            this.contentType = contentType;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public void setExtraHeaders(Map<String, String> extraHeaders) {
            this.extraHeaders = extraHeaders;
        }

        public void setExtraHeader(String key, Object value) {
            if (!StringUtils.isNullOrEmpty(key) && value != null) {
                extraHeaders.put(key, value.toString());
            }
        }

        public Map<String, String> getExtraHeaders() {
            return extraHeaders;
        }

        public void setContent(byte[] content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }

        public byte[] getContent() {
            return content;
        }

        public boolean hasContent() {
            return content != null && content.length > 0;
        }

        public String getContentType() {
            return contentType;
        }

        public void setXFormPostContent(String urlEncodedUriParams) {
            try {
                setXFormPostContent(StringUtils.ensureString(urlEncodedUriParams).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Failed to get utf-8 bytes", e);
            }
        }

        public void setXFormPostContent(byte[] postContent) {
            method = HttpMethod.POST;
            setContent(postContent, CONTENT_TYPE_APPLICATION_X_WWW_FORM_URLENCODED);
        }

        private static final String CONTENT_TYPE_APPLICATION_X_WWW_FORM_URLENCODED =
                "application/x-www-form-urlencoded";
    }

    public static class Response {
        private int status = 200;
        private byte[] body;
        private Map<String, Header> headers = new HashMap<String, Header>();

        public Response() {
        }

        public Response(int status) {
            this.status = status;
        }

        public int getStatus() {
            return status;
        }

        public boolean isOK() {
            return getStatus() == 200;
        }

        public void setStatus(int code) {
            this.status = code;
        }

        public byte[] getBody() {
            return body;
        }

        public void setBody(byte[] body) {
            this.body = body;
        }

        public InputStream getBodyAsStream() {
            return body != null ?
                    new ByteArrayInputStream(body) :
                    null;
        }

        public String getBodyAsString() {
            return body != null ?
                    new String(body) :
                    null;
        }

        public Map<String, Header> getHeaders() {
            return headers;
        }

        public Header getHeader(String key) {
            return headers != null ?
                    headers.containsKey(key) ?
                            headers.get(key) :
                            headers.containsKey(key.toLowerCase()) ?
                                    headers.get(key.toLowerCase()) :
                                    headers.containsKey(key.toUpperCase()) ?
                                            headers.get(key.toUpperCase()) :
                                            null :
                    null;
        }

        public void setHeaders(Map<String, Header> headers) {
            this.headers = headers;
        }

        public void addHeader(String key, Header header) {
            headers.put(key, header);
        }

        public int getContentSize() {
            Header hdr = getHeader("Content-Size");
            return hdr != null ?
                    Integer.parseInt(hdr.getValue()) :
                    body != null ?
                            body.length :
                            0;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "status=" + status +
                    ", body size=" + (body != null ? body.length : 0) +
                    ", headers=" + CollectionUtils.mapToString(headers) +
                    ", bodyAsString='" + getBodyAsString() + '\'' +
                    '}';
        }
    }
}