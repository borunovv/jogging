package com.borunovv.core.util;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class UrlUtilsTest {
    @Test
    public void getUrlPath() throws Exception {
        assertEquals("/aaa/bbb", UrlUtils.getUrlPath("http://domain.com/aaa/bbb"));
        assertEquals("/aaa/bbb", UrlUtils.getUrlPath("/aaa/bbb"));
        assertEquals("", UrlUtils.getUrlPath(""));
        assertEquals("/", UrlUtils.getUrlPath("/"));

        assertEquals("/aaa/bbb", UrlUtils.getUrlPath("/aaa/././bbb"));
        assertEquals("/aaa/bbb/", UrlUtils.getUrlPath("/aaa/././bbb/"));

        assertEquals("/aaa/bbb", UrlUtils.getUrlPath("/aaa/././bbb?a=1"));
        assertEquals("/aaa/bbb/", UrlUtils.getUrlPath("/aaa/././bbb/?a=1"));
    }
}