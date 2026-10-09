package sk.iway.iwcm.users.devices;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import sk.iway.iwcm.JsonTools;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

/** Verifies the shared dashboard and REST JSON representation of device notices. */
class DeviceEntityTest {
    /** Both serializers omit recognition secrets and expose device dates as epoch milliseconds. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void exposesOnlySafeDeviceFieldsWithMillisecondDates(boolean rest) throws Exception {
        long now = 1791201600123L;
        DeviceEntity device = new DeviceEntity();
        device.setId(42L);
        device.setUserId(7);
        device.setTokenHash("a".repeat(64));
        device.setCreateDate(Instant.ofEpochMilli(now));
        device.setLastSeen(Instant.ofEpochMilli(now + 100));
        device.setBrowserName("Firefox");
        device.setBrowserVersion("131");
        device.setOperatingSystem("Windows 11");
        device.setIpAddress("192.0.2.1");
        device.setConfirmedAt(Instant.ofEpochMilli(now + 200));
        device.setReportedAt(Instant.ofEpochMilli(now + 300));

        String json = rest ? JsonMapper.builder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DateTimeFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS)
            .build().writeValueAsString(device) : JsonTools.objectToJSON(device);
        var data = new ObjectMapper().readTree(json);
        Set<String> fields = new HashSet<>();
        data.fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id", "createDate", "lastSeen", "expiresAt", "browserName", "browserVersion",
            "operatingSystem", "ipAddress", "location", "confirmedAt", "reportedAt"), fields);
        assertTrue(data.path("id").isIntegralNumber());
        assertEquals(42L, data.path("id").longValue());
        assertEquals(now, data.path("createDate").longValue());
        assertEquals(now + 100, data.path("lastSeen").longValue());
        assertEquals(now + Duration.ofDays(7).toMillis(), data.path("expiresAt").longValue());
        assertEquals(now + 200, data.path("confirmedAt").longValue());
        assertEquals(now + 300, data.path("reportedAt").longValue());
        assertEquals("Firefox", data.path("browserName").textValue());
    }
}
