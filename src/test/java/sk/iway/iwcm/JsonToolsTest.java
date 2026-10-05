package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Verifies Java time support and the existing JSON formats used by server-rendered pages. */
class JsonToolsTest {
    /** Instants use integer epoch milliseconds, including sub-millisecond precision and pre-epoch values. */
    @ParameterizedTest
    @CsvSource({
        "2026-10-05T12:00:00.123456789Z, 1791201600123",
        "1970-01-01T00:00:00Z, 0",
        "1969-12-31T23:59:59.999999999Z, -1"
    })
    void serializesNestedInstantAsEpochMilliseconds(String instant, long expected) throws Exception {
        String json = JsonTools.objectToJSON(Map.of("events", List.of(Map.of("createDate", Instant.parse(instant)))));

        var date = new ObjectMapper().readTree(json).path("events").get(0).path("createDate");
        assertTrue(date.isIntegralNumber());
        assertEquals(expected, date.longValue());
    }

    /** Legacy dates, ordinary JSON values and nulls keep their existing representation. */
    @Test
    void preservesExistingValueFormats() throws Exception {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("date", new Date(1791201600123L));
        values.put("text", "Autotest \"JSON\"");
        values.put("enabled", true);
        values.put("count", 42);
        values.put("missing", null);

        var data = new ObjectMapper().readTree(JsonTools.objectToJSON(values));
        assertEquals(1791201600123L, data.path("date").longValue());
        assertEquals("Autotest \"JSON\"", data.path("text").textValue());
        assertTrue(data.path("enabled").booleanValue());
        assertEquals(42, data.path("count").intValue());
        assertTrue(data.path("missing").isNull());
    }
}
