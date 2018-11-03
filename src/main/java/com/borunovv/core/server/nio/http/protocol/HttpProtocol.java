package com.borunovv.core.server.nio.http.protocol;

import com.borunovv.core.server.nio.core.protocol.IMessage;
import com.borunovv.core.server.nio.core.protocol.IMessageProtocol;
import com.borunovv.core.server.nio.core.session.ISession;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.StringUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class HttpProtocol implements IMessageProtocol {

    private int maxPacketSize;
    private int commonPacketSize;
    private int inactivityTimeoutSeconds;

    public HttpProtocol(int maxPacketSize,
                        int commonPacketSize,
                        int inactivityTimeoutSeconds) {
        this.maxPacketSize = maxPacketSize;
        this.commonPacketSize = commonPacketSize;
        this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
    }

    @Override
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    @Override
    public int getCommonPacketSize() {
        return commonPacketSize;
    }

    @Override
    public int getInactivityTimeoutSeconds() {
        return inactivityTimeoutSeconds;
    }

    @Override
    public int getMeaningRequestInactivityTimeoutSeconds() {
        return inactivityTimeoutSeconds;
    }

    @Override
    public int checkPacket(ByteBuffer buffer) {
        ByteBuffer buff = buffer.duplicate();
        buff.flip();
        return HttpRequest.tryParse(buff.array(), buff.limit());
    }

    @Override
    public byte[] marshall(IMessage msg) {
        HttpMessage httpMsg = (HttpMessage) msg;
        Assert.isTrue(httpMsg.hasResponse(), "Invalid response. Nothing to write to client!");
        return httpMsg.getResponse().marshall();
    }

    @Override
    public IMessage unmarshall(ISession session, byte[] data, int length) {
        try {
            return new HttpMessage(session, new HttpRequest(data, length), null);
        } catch (NonCompleteHttpRequestException e) {
            throw new RuntimeException("Unexpected state. Bad HTTP request: \n'"
                    + StringUtils.toUtf8String(Arrays.copyOf(data, length)) + "'", e);
        }
    }

    @Override
    public String toString() {
        return "Protocol: HttpProtocol\n" +
                "  maxPacketSize: " + maxPacketSize + "\n" +
                "  max client inactivity timeout: " + inactivityTimeoutSeconds + " sec.";
    }
}