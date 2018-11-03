package com.borunovv.core.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

public class JsonTest {

    @Test
    public void test() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key1", 1L);
        map.put("key2", "two");
        map.put("key3", 3.14);

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        map.put("list", list);

        Map<String, Object> map2 = new HashMap<String, Object>();
        map2.put("key1_1", 5L);
        map2.put("key1_2", "aaa");
        map2.put("key1_3", 666);

        list.add(map2);

        String json = JsonUtils.toJson(map);
        System.out.println(json);

        Map<String, Object> map3 = (Map<String, Object>) JsonUtils.fromJson(json, Map.class);

        for (Map.Entry<String, Object> entry : map3.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " (" + entry.getValue().getClass().getSimpleName() + ")");
        }
    }

    @Test
       public void testMap() {
        Map<String, Object> a = JsonUtils.fromJson("{\"a\":\"1\"}", Map.class);
        System.out.println(a);
        assertEquals("1", a.get("a"));

        String json = JsonUtils.toJson(a);
        System.out.println("JSON: " + json);

        assertEquals("{\"a\":\"1\"}", json);
    }

    @Test
    public void testList() {
        List<Map<String, Object>> a = JsonUtils.fromJson("[{\"a\":\"1\"},{\"b\":\"2\"}]", List.class);

        System.out.println(a);
        assertEquals("1", a.get(0).get("a"));
        assertEquals("2", a.get(1).get("b"));

        String json = JsonUtils.toJson(a);
        System.out.println("JSON: " + json);

        assertEquals("[{\"a\":\"1\"},{\"b\":\"2\"}]", json);
    }
}
