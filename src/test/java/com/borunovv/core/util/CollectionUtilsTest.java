package com.borunovv.core.util;

import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class CollectionUtilsTest {

    @Test
    public void mapToString() throws Exception {
        Map<String, Object> map = new TreeMap<>();
        map.put("key1", 123L);
        map.put("key2", "Hello");

        String result = CollectionUtils.mapToString(map);
        System.out.println(result);
        assertEquals("{key1=123;key2=Hello;}", result);
    }

    @Test
    public void emptyMapToString() throws Exception {
        Map<String, Object> map = new HashMap<>();
        String result = CollectionUtils.mapToString(map);
        assertEquals("{}", result);
    }

    @Test
    public void nullMapToString() throws Exception {
        String result = CollectionUtils.mapToString(null);
        assertEquals("null", result);
    }

    @Test
    public void commaSeparatedList() throws Exception {
        assertEquals("1,2,3,4,5", CollectionUtils.toCommaSeparatedList(Arrays.asList(1,2,3,4,5)));
        assertEquals("hello,world", CollectionUtils.toCommaSeparatedList(Arrays.asList("hello", "world")));
        assertEquals("hello", CollectionUtils.toCommaSeparatedList(Arrays.asList("hello")));
        assertEquals("", CollectionUtils.toCommaSeparatedList(Collections.emptyList()));
        assertEquals("1,2,4,5", CollectionUtils.toCommaSeparatedList(Arrays.asList(1,2,null,4,5)));
    }

    @Test
    public void separatedList() throws Exception {
        assertEquals("1##2##3##4##5", CollectionUtils.toSeparatedList(Arrays.asList(1,2,3,4,5), "##"));
        assertEquals("12345", CollectionUtils.toSeparatedList(Arrays.asList(1,2,3,4,5), ""));
    }
}