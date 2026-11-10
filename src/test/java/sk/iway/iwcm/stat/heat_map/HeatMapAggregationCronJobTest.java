package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

/** Checks the cron arguments and the delivery grace period in the statistics timezone. */
class HeatMapAggregationCronJobTest {
    /**
     * Verifies that empty arguments select all months and that named and positional forms select the same month.
     */
    @Test
    void selectsTheRequestedMonthOrAllMonths() {
        YearMonth october = YearMonth.of(2025, 10);
        assertNull(HeatMapAggregationCronJob.parseTarget(new String[0]));
        assertEquals(october, HeatMapAggregationCronJob.parseTarget(new String[] {"year=2025", "month=10"}));
        assertEquals(october, HeatMapAggregationCronJob.parseTarget(new String[] {"2025_10"}));
        assertEquals(october, HeatMapAggregationCronJob.parseTarget(new String[] {"2025", "10"}));
        assertThrows(IllegalArgumentException.class, () -> HeatMapAggregationCronJob.parseTarget(new String[] {"2025_13"}));
    }

    /**
     * Checks the exact delivery cutoff for December in the statistics time zone.
     */
    @Test
    void waitsForTheFullDeliveryWindowAcrossNewYear() {
        ZoneId zone = ZoneId.of("Europe/Bratislava");
        Clock ready = Clock.fixed(Instant.parse("2026-01-01T23:00:00Z"), zone);
        YearMonth december = YearMonth.of(2025, 12);
        Clock early = Clock.offset(ready, Duration.ofMillis(-1));
        assertFalse(HeatMapAggregationCronJob.eligible(december, early));
        assertTrue(HeatMapAggregationCronJob.eligible(december, ready));
        assertFalse(HeatMapAggregationCronJob.eligible(december.plusMonths(1), ready));
    }
}
