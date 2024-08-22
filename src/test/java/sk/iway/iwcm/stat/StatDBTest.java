package sk.iway.iwcm.stat;

import sk.iway.iwcm.test.BaseWebjetTest;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatDBTest extends BaseWebjetTest {

    @ParameterizedTest
    @CsvSource(value ={
        "https://example.com/path;jsessionid=12345?param=value, https://example.com/path?param=value",
        "/path;jsessionid=12345?param=value, /path?param=value",
        "https://example.com/path?param=value, https://example.com/path?param=value",
        "/path?param=value, /path?param=value",
        "/path/aaa, /path/aaa",
        "https://example.com/path;jsessionid=12345, https://example.com/path",
        "/path;jsessionid=12345, /path",
        "/path/;jsessionid=12345, /path/",
        "'', ''",
        ", ",
        "null, null",
    }, nullValues = {"null"}, emptyValue = "")
    void testRemoveJsessionId(String url, String expected) {
        String result = StatDB.removeJsessionId(url);
        assertEquals(expected, result);
    }

}
