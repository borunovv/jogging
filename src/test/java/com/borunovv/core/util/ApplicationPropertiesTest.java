package com.borunovv.core.util;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ApplicationPropertiesTest {

    private static final String CONTENT = ""
            + "#comment bla\n"
            + "\r\n"
            + "key_without_value\n"
            + " \tkey_without_value2   bla\n"
            + "host=\n"
            + " port=123\n"
            + "my.name = Vasia Pupkin\n"
            + "\tmy.town=NY";

    private ApplicationProperties properties;

    @Before
    public void onSetUp() {
        properties = new ApplicationProperties(CONTENT);
    }

    @Test
    public void getString() throws Exception {
        assertEquals("", properties.get("not_exist"));
        assertEquals("", properties.get("host"));
        assertEquals("123", properties.get("port"));
        assertEquals("Vasia Pupkin", properties.get("my.name"));
        assertEquals("NY", properties.get("my.town"));
    }

    @Test
    public void getStringWithDefault() throws Exception {
        assertEquals("bla", properties.get("not_exist", "bla"));
    }

    @Test
    public void getLong() throws Exception {
        assertEquals(123L, properties.getLong("port", 0L));
    }

    @Test
    public void getNotExistingLongReturnsDefaultValue() throws Exception {
        assertEquals(555L, properties.getLong("not_exist", 555L));
    }

    @Test(expected = NumberFormatException.class)
    public void getStringAsLongCausesException() throws Exception {
        properties.getLong("host", 0L);
    }

    @Test
    public void has() throws Exception {
        assertFalse(properties.has("not_existing_key"));
        assertFalse(properties.has("comment bla"));
        assertFalse(properties.has("key_without_value"));
        assertFalse(properties.has("key_without_value2   bla"));

        assertTrue(properties.has("host"));
        assertTrue(properties.has("port"));
        assertTrue(properties.has("my.name"));
        assertTrue(properties.has("my.town"));
    }
}