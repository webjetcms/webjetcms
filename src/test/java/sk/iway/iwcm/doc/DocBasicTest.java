package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sk.iway.iwcm.test.BaseWebjetTest;

/**
 * Tests for DocBasic date/time setter methods.
 *
 * Known bug: setEventDate(long) loses the time component because setEventDateString()
 * unconditionally overwrites this.eventDateDate with a date-only (midnight) value,
 * so the subsequent call to setEventTimeString() uses Tools.formatTime() on a midnight date.
 */
@Execution(ExecutionMode.SAME_THREAD)
class DocBasicTest extends BaseWebjetTest {

    private DocDetails doc;

    // Fixed test timestamp: 06.05.2026 14:30:00
    private long timestamp;
    private static final String EXPECTED_DATE = "06.05.2026";
    private static final String EXPECTED_TIME = "14:30";

    @BeforeEach
    void setUp() {
        doc = new DocDetails();
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.MAY, 6, 14, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        timestamp = cal.getTimeInMillis();
    }

    // --- setEventDate ---

    @Test
    @DisplayName("setEventDate must preserve the date component in eventDateDate")
    void testSetEventDatePreservesDate() {
        doc.setEventDate(timestamp);
        assertEquals(EXPECTED_DATE, doc.getEventDateString(),
            "setEventDate must correctly set the date part");
    }

    @Test
    @DisplayName("setEventDate must preserve the time component in eventDateDate - BUG: setEventDateString overwrites eventDateDate with midnight")
    void testSetEventDatePreservesTime() {
        doc.setEventDate(timestamp);
        // This assertion fails with the current bug:
        // setEventDateString() sets eventDateDate to midnight (00:00),
        // then setEventTimeString() is called with Tools.formatTime(midnight) = "00:00",
        // so getEventTimeString() returns "00:00" instead of "14:30".
        assertEquals(EXPECTED_TIME, doc.getEventTimeString(),
            "setEventDate must preserve the time component - setEventDateString must not overwrite eventDateDate");
    }

    // --- setPublishStart (comparison: no bug expected) ---

    @Test
    @DisplayName("setPublishStart must correctly set the date string")
    void testSetPublishStartPreservesDate() {
        doc.setPublishStart(timestamp);
        assertEquals(EXPECTED_DATE, doc.getPublishStartString(),
            "setPublishStart must correctly set the date part");
    }

    @Test
    @DisplayName("setPublishStart must correctly set the time string")
    void testSetPublishStartPreservesTime() {
        doc.setPublishStart(timestamp);
        assertEquals(EXPECTED_TIME, doc.getPublishStartTimeString(),
            "setPublishStart must correctly preserve the time component");
    }

    // --- setPublishEnd (comparison: no bug expected) ---

    @Test
    @DisplayName("setPublishEnd must correctly set the date string")
    void testSetPublishEndPreservesDate() {
        doc.setPublishEnd(timestamp);
        assertEquals(EXPECTED_DATE, doc.getPublishEndString(),
            "setPublishEnd must correctly set the date part");
    }

    @Test
    @DisplayName("setPublishEnd must correctly set the time string")
    void testSetPublishEndPreservesTime() {
        doc.setPublishEnd(timestamp);
        assertEquals(EXPECTED_TIME, doc.getPublishEndTimeString(),
            "setPublishEnd must correctly preserve the time component");
    }
}
