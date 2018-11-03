package com.borunovv.core.server.nio.core.util;

import com.borunovv.core.util.Assert;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public final class NIOUtils {

    public static String getRemoteIpAddress(SocketChannel client) {
        try {
            return getIpAddress(client.getRemoteAddress());
        } catch (IOException e) {
            return "";
        }
    }

    public static String getIpAddress(SocketAddress address) {
        Assert.isTrue(address instanceof InetSocketAddress,
                "Expected InetSocketAddress, actual: " + address.getClass().getSimpleName());

        InetSocketAddress inetAddress = (InetSocketAddress) address;
        return inetAddress.getAddress().getHostAddress();
    }
}
