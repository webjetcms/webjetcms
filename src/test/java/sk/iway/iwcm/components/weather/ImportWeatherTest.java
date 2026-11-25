package sk.iway.iwcm.components.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jakarta.xml.bind.UnmarshalException;

/** Covers the XML boundary between the remote weather feed and JAXB. */
class ImportWeatherTest {

    private static final String FORECAST = """
        <weatherdata created="2026-11-25T12:00:00Z">
            <product class="pointData"/>
        </weatherdata>
        """;

    /** Checks that the parser still binds elements and attributes from a forecast. */
    @Test
    void readsWeatherProduct() throws Exception {
        var forecast = ImportWeather.parseWeather(FORECAST);
        assertEquals("pointData", forecast.getProduct().get(0).getClazz());
    }

    /** A UTF-8 byte-order mark can remain at the start of the decoded feed. */
    @Test
    void readsWeatherProductWithLeadingBom() throws Exception {
        var forecast = ImportWeather.parseWeather("\uFEFF" + FORECAST);
        assertEquals("pointData", forecast.getProduct().get(0).getClazz());
    }

    /** Even an empty document type declaration must be rejected. */
    @Test
    void rejectsDoctype() {
        assertThrows(UnmarshalException.class, () -> ImportWeather.parseWeather("<!DOCTYPE weatherdata>" + FORECAST));
    }
}
