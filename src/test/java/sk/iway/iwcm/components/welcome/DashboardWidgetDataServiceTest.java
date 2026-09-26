package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.stat.SessionClusterService;
import sk.iway.iwcm.stat.StatNewDB;

/** Verifies dashboard input, authorization, period boundaries, and aggregate query semantics. */
class DashboardWidgetDataServiceTest {
    private final DashboardWidgetDataService service = new DashboardWidgetDataService(null, null, null, null);

    @Test
    void rejectsInvalidConfigurationBeforeAccessingData() {
        assertThrows(IllegalArgumentException.class, () -> service.load("unknown", 7, "sessions", null, null, null, "example.test", "session"));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("traffic", 365, "sessions", null, null));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("traffic", 7, "COUNT(*)", null, null));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("forms", 7, "sessions", " ", null));
        assertThrows(IllegalArgumentException.class, () -> DashboardWidgetDataService.validate("newsletter", 7, "sessions", null, -1L));
    }

    @Test
    void requiresAdminAndTheActualModulePermission() {
        Identity user = mock(Identity.class);
        assertThrows(AccessDeniedException.class, () -> DashboardWidgetDataService.authorize("sessions", user));
        when(user.isAdmin()).thenReturn(true);
        DashboardWidgetDataService.authorize("sessions", user);
        for (String type : DashboardWidgetDataService.TYPES) {
            if (!type.equals("sessions")) assertThrows(AccessDeniedException.class, () -> DashboardWidgetDataService.authorize(type, user));
        }
        when(user.isEnabledItem("menuEmail")).thenReturn(true);
        DashboardWidgetDataService.authorize("newsletter", user);
        assertThrows(AccessDeniedException.class, () -> DashboardWidgetDataService.authorize("forms", user));
        when(user.isEnabledItem("cmp_form")).thenReturn(true);
        DashboardWidgetDataService.authorize("forms", user);
    }

    @Test
    void disabledStatisticsAreUnavailableRatherThanAnEmptySuccess() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("cmp_stat")).thenReturn(true);
        try (var constants = mockStatic(Constants.class)) {
            constants.when(() -> Constants.getString("statMode")).thenReturn("none");
            assertThrows(DashboardWidgetDataService.UnavailableException.class, () -> DashboardWidgetDataService.authorize("traffic", user));
        }
    }

    @Test
    void comparisonUsesCompletedCalendarDaysAcrossDaylightSavingTime() {
        ZoneId zone = ZoneId.of("Europe/Bratislava");
        Clock clock = Clock.fixed(Instant.parse("2026-04-01T12:00:00Z"), zone);
        var range = DashboardWidgetDataService.completedDays(7, clock);
        assertEquals(LocalDate.of(2026, 3, 25).atStartOfDay(zone).toInstant().toEpochMilli(), range.from());
        assertEquals(LocalDate.of(2026, 4, 1).atStartOfDay(zone).toInstant().toEpochMilli(), range.until());
        assertEquals(167, Duration.ofMillis(range.until() - range.from()).toHours());
        assertEquals(LocalDate.of(2026, 3, 18).atStartOfDay(zone).toInstant().toEpochMilli(), range.previous().from());
        assertEquals(range.from(), range.previous().until());
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
        assertEquals(DashboardWidgetDataService.completedDays(7, clock).until(), LocalDate.of(2026, 4, 1).atStartOfDay(zone).toInstant().toEpochMilli());
    }

    @Test
    void emptyAccessScopeCannotBecomeAnUnrestrictedQuery() {
        var empty = new DashboardWidgetDataService.Scope(List.of(), List.of());
        assertEquals("(s.group_id IN (-1) OR s.doc_id IN (-1))", empty.sql("s"));
        var onePage = new DashboardWidgetDataService.Scope(List.of(), List.of(42));
        assertEquals("(s.group_id IN (-1) OR s.doc_id IN (42))", onePage.sql("s"));
        var currentDomainPage = new DashboardWidgetDataService.Scope(List.of(), List.of(42), List.of(10, 11));
        assertEquals("s.group_id IN (10,11) AND (s.group_id IN (-1) OR s.doc_id IN (42))", currentDomainPage.statisticsSql("s"));
    }

    @Test
    void uniqueVisitorsAreCountedAcrossTheUnionOfPartitions() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getLong(1)).thenReturn(9L);
        when(rows.getLong(2)).thenReturn(4L);
        when(rows.getLong(3)).thenReturn(2L);
        var range = new DashboardWidgetDataService.Range(1000, 2000);
        try (var stat = mockStatic(StatNewDB.class)) {
            stat.when(() -> StatNewDB.getTableSuffix("stat_views", 1000, 1999)).thenReturn(new String[] {"_202603", "_202604"});
            stat.when(StatNewDB::getWhiteListedUAQuery).thenReturn(" AND s.browser_id>0");
            assertEquals(Map.of("views", 9L, "sessions", 4L, "uniqueUsers", 2L), service.trafficTotals(connection,
                new DashboardWidgetDataService.Scope(List.of(10), List.of(42)), range));
        }
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith("SELECT COUNT(doc_id), COUNT(DISTINCT session_id), COUNT(DISTINCT browser_id) FROM ("));
        assertTrue(sql.getValue().contains(" UNION ALL "));
        assertEquals(2, sql.getValue().split("s.group_id IN \\(10\\) AND ", -1).length - 1);
        verify(statement).setTimestamp(1, new Timestamp(1000));
        verify(statement).setTimestamp(2, new Timestamp(2000));
        verify(statement).setTimestamp(3, new Timestamp(1000));
        verify(statement).setTimestamp(4, new Timestamp(2000));
        verify(statement).setQueryTimeout(15);
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
    void sessionResponseUsesPlainValuesCompatibleWithTheSpringJsonMapper() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.getUserId()).thenReturn(42);
        try (var sessions = mockStatic(SessionClusterService.class)) {
            sessions.when(() -> SessionClusterService.getSessionInfo("current", 42)).thenReturn("{\"currentSessionId\":\"current\",\"userSessions\":[{\"userSessions\":[{\"sessionId\":\"current\"}]}]}");
            var result = service.load("sessions", 7, "sessions", null, null, user, null, "current");
            assertEquals(1L, result.get("total"));
            assertTrue(result.get("currentSessions") instanceof Map);
            String json = new tools.jackson.databind.json.JsonMapper().writeValueAsString(result);
            assertTrue(json.contains("\"currentSessionId\":\"current\""));
        }
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
    void accessibleFormsUseOneScopedProjectionInsteadOfPerFormEntityEnrichment() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rows = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getString(1)).thenReturn("contact");
        assertEquals(List.of("contact"), service.allowedFormNames(connection, new DashboardWidgetDataService.Scope(List.of(10), List.of(42)), 7));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertTrue(sql.getValue().contains("d.group_id IN (10) OR d.doc_id IN (42)"));
        assertTrue(sql.getValue().contains("MAX(create_date) AS latest_created FROM forms WHERE domain_id=?"));
        assertTrue(sql.getValue().contains("GROUP BY form_name HAVING SUM(CASE WHEN create_date IS NULL THEN 1 ELSE 0 END)>0"));
        assertFalse(sql.getValue().contains("NOT EXISTS"));
        verify(statement).setInt(1, 7);
        verify(statement).setInt(2, 7);
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

    @Test
    void weeklyErrorsKeepTheCollectorsCalendarYearAcrossNewYear() {
        ZoneId zone = ZoneId.of("Europe/Bratislava");
        var range = new DashboardWidgetDataService.Range(LocalDate.of(2025, 12, 29).atStartOfDay(zone).toInstant().toEpochMilli(),
            LocalDate.of(2026, 1, 5).atStartOfDay(zone).toInstant().toEpochMilli(), zone);
        var weeks = DashboardWidgetDataService.errorWeeks(range, false);
        assertTrue(weeks.stream().anyMatch(week -> week.year() == 2025));
        assertTrue(weeks.stream().anyMatch(week -> week.year() == 2026));
        assertTrue(weeks.contains(new DashboardWidgetDataService.ErrorWeek(2025, 1)));
        assertTrue(weeks.contains(new DashboardWidgetDataService.ErrorWeek(2026, 1)));
        var exception = assertThrows(DashboardWidgetDataService.UnavailableException.class, () -> DashboardWidgetDataService.errorWeeks(range, true));
        assertEquals("period-unavailable", exception.getMessage());
    }

    @Test
    void partialErrorWeekStopsAtNowWithoutQueryingNextMonthsFuturePartition() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-30T12:00:00Z"), ZoneId.of("Europe/Bratislava"));
        var range = DashboardWidgetDataService.errorRange(DashboardWidgetDataService.completedDays(7, clock), clock);
        assertEquals(clock.millis(), range.until());
        assertEquals(LocalDate.of(2026, 9, 21).atStartOfDay(clock.getZone()).toInstant().toEpochMilli(), range.from());
    }
}
