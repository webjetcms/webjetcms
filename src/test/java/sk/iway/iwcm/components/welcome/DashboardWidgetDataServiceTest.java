package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;

/** Verifies dashboard input, authorization, period boundaries, and aggregate query semantics. */
class DashboardWidgetDataServiceTest {
    private final DashboardWidgetDataService service = new DashboardWidgetDataService(null, null, null);

    @Test
    void rejectsInvalidConfigurationBeforeAccessingData() {
        assertThrows(IllegalArgumentException.class, () -> service.load("unknown", 7, null, null, null, "example.test"));
        assertThrows(IllegalArgumentException.class, () -> service.load("approvals", 7, null, null, null, "example.test"));
        assertThrows(IllegalArgumentException.class, () -> service.load("logged-admins", 7, null, null, null, "example.test"));
        assertThrows(IllegalArgumentException.class, () -> service.load("sessions", 7, null, null, null, "example.test"));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("forms", 365, null, null));
        for (String type : List.of("traffic", "top-pages", "search-terms", "referrers", "errors", "publishing", "changed-pages")) assertThrows(IllegalArgumentException.class, () -> service.load(type, 7, null, null, null, "example.test"));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("forms", 7, " ", null));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("newsletter", 7, null, -1L));
    }

    @Test
    void requiresAdminAndTheActualModulePermission() {
        Identity user = mock(Identity.class);
        assertThrows(AccessDeniedException.class, () -> DashboardWidgetDataService.authorize("forms", user));
        when(user.isAdmin()).thenReturn(true);
        for (String type : DashboardWidgetDataService.TYPES) {
            assertThrows(AccessDeniedException.class, () -> DashboardWidgetDataService.authorize(type, user));
        }
        when(user.isEnabledItem("menuEmail")).thenReturn(true);
        DashboardWidgetDataService.authorize("newsletter", user);
        assertThrows(AccessDeniedException.class, () -> DashboardWidgetDataService.authorize("forms", user));
        when(user.isEnabledItem("cmp_form")).thenReturn(true);
        DashboardWidgetDataService.authorize("forms", user);
    }

    @Test
    void recentFormsIncludeSevenCalendarDatesThroughToday() {
        ZoneId zone = ZoneId.of("Europe/Bratislava");
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:34:56Z"), zone);
        var range = DashboardWidgetDataService.recentDays(7, clock);
        assertEquals(clock.millis(), range.until());
        assertEquals(LocalDate.of(2026, 3, 26).atStartOfDay(zone).toInstant().toEpochMilli(), range.from());
        long todaySubmission = Instant.parse("2026-04-01T09:00:00Z").toEpochMilli();
        assertTrue(todaySubmission >= range.from() && todaySubmission < range.until());
    }

    @Test
    void failedNewsletterRecipientsAreNotReportedAsSuccessfullySent() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement totals = mock(PreparedStatement.class);
        PreparedStatement clicks = mock(PreparedStatement.class);
        ResultSet totalRows = mock(ResultSet.class);
        ResultSet clickRows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(totals, clicks);
        when(totals.executeQuery()).thenReturn(totalRows);
        when(clicks.executeQuery()).thenReturn(clickRows);
        when(totalRows.getLong(1)).thenReturn(10L);
        when(totalRows.getLong(2)).thenReturn(7L);
        when(totalRows.getLong(3)).thenReturn(2L);
        when(totalRows.getLong(4)).thenReturn(3L);
        when(clickRows.getLong(1)).thenReturn(5L);
        Map<String, Object> item = new LinkedHashMap<>();
        service.campaignCounts(connection, 42, 7, item);
        assertEquals(Map.of("recipients", 10L, "sent", 7L, "failed", 2L, "opens", 3L, "clicks", 5L), item);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection, org.mockito.Mockito.times(2)).prepareStatement(sql.capture());
        assertTrue(sql.getAllValues().get(0).contains("sent_date IS NOT NULL AND (retry IS NULL OR (retry>=0 AND retry<>98))"));
        assertTrue(sql.getAllValues().get(1).contains("e.campain_id=? AND e.domain_id=?"));
        verify(totals).setLong(1, 42);
        verify(totals).setInt(2, 7);
        verify(clicks).setInt(2, 7);
        assertFalse(item.containsKey("unread"));
    }

    @Test
    void automaticCampaignPrefersLatestStartedThenEarliestScheduledThenLatestCompleted() {
        List<CampaingsEntity> allowed = java.util.stream.LongStream.rangeClosed(1, 5).mapToObj(id -> {
            CampaingsEntity campaign = new CampaingsEntity();
            campaign.setId(id);
            return campaign;
        }).toList();
        Map<Long, DashboardWidgetDataService.CampaignDelivery> delivery = new LinkedHashMap<>();
        delivery.put(1L, new DashboardWidgetDataService.CampaignDelivery(true, 100L, null, null));
        delivery.put(2L, new DashboardWidgetDataService.CampaignDelivery(true, 200L, null, null));
        delivery.put(3L, new DashboardWidgetDataService.CampaignDelivery(false, null, 400L, null));
        delivery.put(4L, new DashboardWidgetDataService.CampaignDelivery(false, 50L, null, 300L));
        delivery.put(5L, DashboardWidgetDataService.CampaignDelivery.EMPTY);
        assertEquals(2L, DashboardWidgetDataService.chooseCampaign(allowed, delivery));
        delivery.remove(2L);
        assertEquals(1L, DashboardWidgetDataService.chooseCampaign(allowed, delivery));
        delivery.remove(1L);
        assertEquals(3L, DashboardWidgetDataService.chooseCampaign(allowed, delivery));
        delivery.remove(3L);
        assertEquals(4L, DashboardWidgetDataService.chooseCampaign(allowed, delivery));
        delivery.remove(4L);
        assertEquals(null, DashboardWidgetDataService.chooseCampaign(allowed, delivery));
        delivery.put(5L, new DashboardWidgetDataService.CampaignDelivery(true, null, null, null));
        delivery.put(3L, new DashboardWidgetDataService.CampaignDelivery(false, null, 400L, null));
        assertEquals(5L, DashboardWidgetDataService.chooseCampaign(allowed, delivery));
    }

    @Test
    void currentlySendingNewsletterRecipientKeepsCampaignActiveAndIncomplete() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getLong(1)).thenReturn(42L);
        when(rows.getLong(2)).thenReturn(1L);
        when(rows.getLong(3)).thenReturn(0L);
        when(rows.getLong(6)).thenReturn(1L);
        when(rows.getTimestamp(4)).thenReturn(new Timestamp(1000));
        when(rows.getTimestamp(5)).thenReturn(new Timestamp(1000));
        var delivery = service.campaignDelivery(connection, 7, 2000).get(42L);
        assertTrue(delivery.active());
        assertEquals(null, delivery.completed());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("retry IS NULL OR retry<>98"));
        assertTrue(sql.getValue().contains("OR retry=98 THEN 1"));
        verify(statement).setInt(5, 7);
    }

}
