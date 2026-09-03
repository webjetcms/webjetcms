package sk.iway.iwcm.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BrowserIdentifierMigrationServiceTest {

    @ParameterizedTest
    @CsvSource({
        "Chrome 127.0, Chrome",
        "Googlebot 2.1, Googlebot",
        "Mobile Safari 17.4, Mobile Safari",
        "python-requests 2.31.0, python-requests",
        "1.0, Unknown",
        "Chrome, Chrome"
    })
    void shouldNormalizeBrowserIdentifier(String source, String expected) {
        assertEquals(expected, BrowserIdentifierMigrationService.normalizeBrowserIdentifier(source));
    }
}
