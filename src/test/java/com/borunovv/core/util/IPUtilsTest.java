package com.borunovv.core.util;

import org.junit.Test;

import java.net.InetSocketAddress;

import static junit.framework.TestCase.assertEquals;

public class IPUtilsTest {

    @Test
    public void toLong() throws Exception {
        assertEquals(3232235521L, IPUtils.toLong("192.168.0.1"));
        assertEquals(1701209960L, IPUtils.toLong("101.102.103.104"));
        assertEquals(1L, IPUtils.toLong("0.0.0.1"));
        assertEquals(16909060L, IPUtils.toLong("1.2.3.4"));

        assertEquals(0L, IPUtils.toLong(""));
        assertEquals(0L, IPUtils.toLong("bad"));
        assertEquals(0L, IPUtils.toLong("1.2"));
        assertEquals(0L, IPUtils.toLong("1.2.3.4.5"));
    }

    @Test
    public void testToString() throws Exception {
        InetSocketAddress addr = new InetSocketAddress("192.168.0.1", 1234);
        assertEquals("192.168.0.1", IPUtils.toString(addr));
    }
}