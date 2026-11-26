package sk.iway.iwcm.components.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Tools;

/** Verifies how the weather component handles feeds without a second forecast day. */
class WeatherForecastTest {

    /** A rejected feed leaves all forecast views empty and can be retried on the next request. */
    @Test
    void rejectedFeedReturnsEmptyForecast() {
        var forecast = loadWithoutCaching("<!DOCTYPE weatherdata><weatherdata/>");

        assertTrue(forecast.getDayForecastBeanList().isEmpty());
        assertTrue(forecast.getHourForecastBeanList().isEmpty());
        assertTrue(forecast.getCurrentHourForecast().isEmpty());
    }

    /** A single forecast day is available to the template even without a cache expiry date. */
    @Test
    void singleDayIsAvailableWithoutCaching() {
        String xml = """
            <weatherdata>
                <product class="pointData">
                    <time from="%1$sT12:00:00" to="%1$sT18:00:00">
                        <location><symbol id="sun" number="1"/></location>
                    </time>
                </product>
            </weatherdata>
            """.formatted(LocalDate.now());
        var forecast = loadWithoutCaching(xml);

        assertEquals(1, forecast.getDayForecastBeanList().size());
        assertEquals("sun", forecast.getDayForecastBeanList().get(0).getSymbol());
        assertEquals(1, forecast.getHourForecastBeanList().size());
    }

    private WeatherForecast loadWithoutCaching(String xml) {
        Cache cache = mock(Cache.class);
        try (var caches = mockStatic(Cache.class); var tools = mockStatic(Tools.class, CALLS_REAL_METHODS)) {
            caches.when(Cache::getInstance).thenReturn(cache);
            tools.when(() -> Tools.downloadUrl(anyString())).thenReturn(xml);

            var forecast = new WeatherForecast("Bratislava", BigDecimal.valueOf(48), BigDecimal.valueOf(17));
            verify(cache, never()).setObjectByExpiry(anyString(), any(), anyLong(), anyBoolean());
            return forecast;
        }
    }
}
