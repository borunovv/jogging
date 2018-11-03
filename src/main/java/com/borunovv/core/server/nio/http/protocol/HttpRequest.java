package com.borunovv.core.server.nio.http.protocol;

import com.borunovv.core.util.Assert;
import com.borunovv.core.util.HttpMethod;
import com.borunovv.core.util.StringUtils;
import com.borunovv.core.util.UrlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequest {

    private static final Pattern URI_PATTERN = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+.*$");

    private static final String HDR_CONTENT_TYPE = "Content-Type";
    private static final String HDR_CONTENT_LENGTH = "Content-Length";

    private String rawHeader = "";
    private String method = "";
    private String uri = "";
    private Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private byte[] content = new byte[0];
    private Map<String, String> uriParams = new HashMap<String, String>();
    private String uriPath = "/";


    public HttpRequest(byte[] data, int length) throws NonCompleteHttpRequestException {
        parse(data, length);
    }

    public HttpMethod getMethod() {
        return HttpMethod.fromString(method);
    }

    public String getUri() {
        return uri;
    }

    public byte[] getContent() {
        return content;
    }

    public List<String> getHeader(String headerName) {
        return (headerName != null && headers.containsKey(headerName.toLowerCase())) ?
                headers.get(headerName.toLowerCase()) :
                new ArrayList<>(0);
    }

    public String getSingleHeader(String headerName) {
        List<String> values = getHeader(headerName);
        return values.isEmpty() ?
                null :
                values.get(0);
    }

    public boolean hasHeader(String headerName) {
        return getSingleHeader(headerName) != null;
    }

    public int getMarshalledSize() {
        return rawHeader.length()
                + 4 // '\r\n\r\n'
                + content.length;
    }

    public String getUriPath() {
        return StringUtils.ensureString(uriPath);
    }

    public Map<String, String> getUriParams() {
        return uriParams;
    }

    private void parse(byte[] data, int length) throws NonCompleteHttpRequestException {
        Assert.isTrue(data != null && data.length >= length, "Bad data to parse");

        int headerLength = findHeaderSeparator(data, length);
        if (headerLength <= 0) {
            throw new NonCompleteHttpRequestException("Can't find header separator.");
        }
        parseHeader(data, headerLength);
        parseContent(data, headerLength, length);
    }

    private static int findHeaderSeparator(byte[] data, int length) {
        // Ищем разделитель '\r\n\r\n'
        int offset = 0;
        int curIndex = 0;
        while (curIndex >= 0 && offset < length - 3) {
            curIndex = findNext(data, length, offset, (byte) '\r');
            if (data[curIndex + 1] == '\n'
                    && data[curIndex + 2] == '\r'
                    && data[curIndex + 3] == '\n') {
                return curIndex;
            }
            offset = curIndex + 1;
        }
        return -1;
    }

    private static int findNext(byte[] data, int dataLength, int fromIndex, byte what) {
        for (int i = fromIndex; i < dataLength; ++i) {
            if (data[i] == what) {
                return i;
            }
        }
        return -1;
    }

    private void parseHeader(byte[] data, int headerLength) throws NonCompleteHttpRequestException {
        headers.clear();

        rawHeader = StringUtils.toUtf8String(data, 0, headerLength);
        // Разделяем на кусочки.
        String[] lines = rawHeader.split("\\r\\n");
        // Первая строка содержит URI
        if (lines.length < 1) {
            throw new NonCompleteHttpRequestException("Not found first line with URI");
        }
        parseMethodAndUri(lines[0]);

        // Теперь заголовки..
        for (int i = 1; i < lines.length; ++i) {
            String name = parseHeaderName(lines[i]);
            String value = parseHeaderValue(lines[i]);
            String headerKeyInMap = name.toLowerCase();
            if (!headers.containsKey(headerKeyInMap)) {
                headers.put(headerKeyInMap, new ArrayList<String>(1));
            }
            headers.get(headerKeyInMap).add(value);
        }
    }

    // GET /uri/ HTTP/1.1
    private void parseMethodAndUri(String line) {
        Matcher matcher = URI_PATTERN.matcher(line);
        if (!matcher.matches() || matcher.groupCount() != 2) {
            throw new IllegalArgumentException("Can't parse Method and URI from line: '" + line + "'");
        }
        method = matcher.group(1);
        uri = matcher.group(2);

        uriParams = UrlUtils.parseUriParams(UrlUtils.getUriParamsPart(uri));
        uriPath = UrlUtils.getUrlPath(uri);
    }

    private static String parseHeaderName(String line) {
        return line.contains(":") ?
                line.substring(0, line.indexOf(":")).trim() :
                line;
    }

    private static String parseHeaderValue(String line) {
        return line.contains(":") ?
                line.substring(line.indexOf(":") + 1).trim() :
                "";
    }

    private void parseContent(byte[] data, int headerLength, int length) throws NonCompleteHttpRequestException {
        int contentOffset = headerLength + 4; // skip '\r\n\r\n'.
        int contentLengthByHeader = getContentLengthFromHeader();
        if (contentLengthByHeader > 0) {
            int remainingInBuffer = length - contentOffset;
            if (contentLengthByHeader > remainingInBuffer) {
                throw new NonCompleteHttpRequestException("Not all content in buffer");
            }
            content = Arrays.copyOfRange(data, contentOffset, contentOffset + contentLengthByHeader);
        }
    }

    public int getContentLengthFromHeader() {
        List<String> values = getHeader(HDR_CONTENT_LENGTH);
        if (values.isEmpty()) {
            return 0; // Считаем, что в запросе только заголовок.
        } else {
            return Integer.parseInt(values.get(0));
        }
    }

    public String getContentType() {
        return getSingleHeader(HDR_CONTENT_TYPE);
    }

    // Пробует парсить пакет, вернет длину валидного пакета в байтах.
    // Иначе вернет -1;
    public static int tryParse(byte[] data, int length) {
        try {
            return tryParseLocal(data, length);
        } catch (NonCompleteHttpRequestException e) {
            return 0;
        }
    }


    // Пробует парсить пакет, вернет длину валидного пакета в байтах.
    // Иначе вернет -1;
    private static int tryParseLocal(byte[] data, int length) throws NonCompleteHttpRequestException {
        Assert.isTrue(data != null, "data is null");
        Assert.isTrue(length > 0 && length <= data.length, "Invalid length");

        int headerLength = findHeaderSeparator(data, length);
        if (headerLength <= 0) {
            return -1;
        }

        String rawHeader = StringUtils.toUtf8String(data, 0, headerLength);
        // Разделяем на кусочки.
        String[] lines = rawHeader.split("\\r\\n");
        // Первая строка содержит URI
        if (lines.length < 1) {
            return -1;
        }

        int contentLengthByHeader = 0;
        // Теперь заголовки..
        for (int i = 1; i < lines.length; ++i) {
            String name = parseHeaderName(lines[i]);
            String value = parseHeaderValue(lines[i]);
            if (name.equalsIgnoreCase("Content-Length")) {
                contentLengthByHeader = Integer.parseInt(value);
                break;
            }
        }

        int expectedRequestSize = headerLength + 4 + contentLengthByHeader; // skip '\r\n\r\n'.
        return expectedRequestSize <= length ?
                expectedRequestSize :
                -1;
    }

    @Override
    public String toString() {
        return "HttpRequest{" + StringUtils.ensureString(method) + " " + StringUtils.ensureString(uri) + "}";
    }
}
