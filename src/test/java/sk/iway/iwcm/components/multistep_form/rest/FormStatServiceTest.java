package sk.iway.iwcm.components.multistep_form.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.audit.jpa.AuditLogEntity;

/**
 * Verifies that statistics timelines preserve events across daylight-saving changes.
 */
@ResourceLock(Resources.TIME_ZONE)
class FormStatServiceTest {

    /**
     * Verifies that the spring transition does not hide events on the following day.
     */
    @Test
    void keepsEventsAfterSpringDaylightSavingChange() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Bratislava"));
        try {
            Prop prop = mock(Prop.class);
            when(prop.getText(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
            AuditLogEntity event = new AuditLogEntity();
            event.setCreateDate(Timestamp.valueOf("2026-03-29 15:30:00"));
            event.setSubId2(FormStatService.AUDIT_SUBID_ERROR_STEP_GET);

            FormStatService service = new FormStatService(null, null, null, null, null);
            JSONArray values = service.getTimelineErrorData(1, prop, List.of(event),
                Timestamp.valueOf("2026-03-28 00:00:00"), Timestamp.valueOf("2026-03-29 23:59:59"))
                .getJSONArray("components.multistep_form.system_errors.error_get_step");

            assertEquals(2, values.length());
            assertEquals(Timestamp.valueOf("2026-03-29 12:00:00").getTime(), values.getJSONObject(1).getLong("dayDate"));
            assertEquals(1, values.getJSONObject(1).getInt("count"));
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }
}
