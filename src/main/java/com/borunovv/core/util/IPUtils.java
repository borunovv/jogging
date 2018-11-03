package com.borunovv.core.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public final class IPUtils {

    public static String getLocalHostIP() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    public static long toLong(String ipV4Address) {
        long result = 0;
        if (ipV4Address.contains(".")) {
            String[] ipAddressInArray = ipV4Address.split("\\.");
            if (ipAddressInArray.length == 4) {
                for (int i = 3; i >= 0; i--) {
                    long part = Long.parseLong(ipAddressInArray[3 - i]);
                    result |= part << (i * 8);
                }
            }
        }
        return result;
    }

    public static String toString(SocketAddress addr) {
        if (addr instanceof InetSocketAddress) {
            return ((InetSocketAddress) addr).getHostString();
        } else {
            return addr.toString();
        }
    }
}
