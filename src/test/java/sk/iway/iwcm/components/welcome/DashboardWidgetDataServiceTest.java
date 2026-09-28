package sk.iway.iwcm.components.welcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.dmail.jpa.CampaingsEntity;
import sk.iway.iwcm.doc.DocBasic;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;

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
        for (String type : List.of("traffic", "top-pages", "search-terms", "referrers", "errors")) assertThrows(IllegalArgumentException.class, () -> service.load(type, 7, null, null, null, "example.test"));
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

    /** The nearest scheduled events remain visible even when they are more than ninety days away. */
    @Test
    void publishingIncludesDistantPublicationAndExpirationFromTheAuditsCachedSource() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        long later = System.currentTimeMillis() + Duration.ofDays(365 * 4L).toMillis();
        DocHistory publish = scheduledPage(11, later);
        DocDetails expire = mock(DocDetails.class);
        when(expire.getDocId()).thenReturn(12);
        when(expire.getTitle()).thenReturn("Scheduled expiration");
        when(expire.isDisableAfterEnd()).thenReturn(true);
        when(expire.getPublishEnd()).thenReturn(later + 1000);
        DocDB docs = mock(DocDB.class);
        when(docs.getPublicableDocs()).thenReturn(List.of(expire, publish));
        when(docs.getBasicDocDetails(11, false)).thenReturn(mock(DocDetails.class));
        when(docs.getBasicDocDetails(12, false)).thenReturn(expire);

        try (var docStatic = mockStatic(DocDB.class);
             var access = mockStatic(DashboardRecentPagesService.class)) {
            docStatic.when(DocDB::getInstance).thenReturn(docs);
            access.when(() -> DashboardRecentPagesService.isAccessible(any(DocDetails.class), eq(user), eq("current.example"))).thenReturn(true);
            for (int days : List.of(7, 30, 90)) {
                var result = service.load("publishing", days, null, null, user, "current.example");
                assertEquals(2L, result.get("total"));
                List<?> items = (List<?>) result.get("items");
                Map<?, ?> first = (Map<?, ?>) items.get(0);
                Map<?, ?> second = (Map<?, ?>) items.get(1);
                assertEquals("publish", first.get("kind"));
                assertEquals(later, first.get("date"));
                assertEquals("/admin/v9/webpages/web-pages-list/?docid=11", first.get("url"));
                assertEquals("expire", second.get("kind"));
                assertEquals(later + 1000, second.get("date"));
                assertTrue((long) result.get("from") < later);
                assertFalse(result.containsKey("to"));
            }
        }
    }

    /** Filtering and deduplication happen before the six nearest events are selected. */
    @Test
    void publishingKeepsCurrentAccessChecksAndSortsBeforeLimitingThePreview() {
        Identity user = mock(Identity.class);
        when(user.isAdmin()).thenReturn(true);
        when(user.isEnabledItem("menuWebpages")).thenReturn(true);
        long later = System.currentTimeMillis() + Duration.ofDays(365).toMillis();
        DocDB docs = mock(DocDB.class);
        List<DocBasic> scheduled = new ArrayList<>();
        List<DocDetails> allowed = new ArrayList<>();
        for (int id = 8; id > 0; id--) {
            scheduled.add(scheduledPage(id, later + id * 1000L));
            DocDetails current = mock(DocDetails.class);
            when(docs.getBasicDocDetails(id, false)).thenReturn(current);
            allowed.add(current);
        }
        scheduled.add(scheduled.get(0));
        scheduled.add(scheduledPage(9, 1));
        scheduled.add(scheduledPage(10, later));
        scheduled.add(scheduledPage(11, later));
        DocDetails past = mock(DocDetails.class);
        DocDetails denied = mock(DocDetails.class);
        when(docs.getBasicDocDetails(9, false)).thenReturn(past);
        when(docs.getBasicDocDetails(10, false)).thenReturn(denied);
        when(docs.getPublicableDocs()).thenReturn(scheduled);

        try (var docStatic = mockStatic(DocDB.class);
             var access = mockStatic(DashboardRecentPagesService.class)) {
            docStatic.when(DocDB::getInstance).thenReturn(docs);
            for (DocDetails current : allowed) {
                access.when(() -> DashboardRecentPagesService.isAccessible(current, user, "current.example")).thenReturn(true);
            }
            access.when(() -> DashboardRecentPagesService.isAccessible(past, user, "current.example")).thenReturn(true);
            var result = service.load("publishing", 30, null, null, user, "current.example");
            assertEquals(8L, result.get("total"));
            List<?> items = (List<?>) result.get("items");
            assertEquals(DashboardWidgetDataService.PREVIEW_SIZE, items.size());
            for (int index = 0; index < items.size(); index++) {
                assertEquals(later + (index + 1L) * 1000L, ((Map<?, ?>) items.get(index)).get("date"));
            }
            access.verify(() -> DashboardRecentPagesService.isAccessible(denied, user, "current.example"));
            access.verify(() -> DashboardRecentPagesService.isAccessible(null, user, "current.example"));
        }
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

    private static DocHistory scheduledPage(int id, long date) {
        DocHistory page = mock(DocHistory.class);
        when(page.getDocId()).thenReturn(id);
        when(page.getTitle()).thenReturn("Scheduled page " + id);
        when(page.getPublicable()).thenReturn(true);
        when(page.getPublishStart()).thenReturn(date);
        return page;
    }

    private static DocDetails currentPage(int groupId, String title) {
        DocDetails page = mock(DocDetails.class);
        when(page.getGroupId()).thenReturn(groupId);
        when(page.getTitle()).thenReturn(title);
        when(page.getFullPath()).thenReturn("/" + title);
        return page;
    }
}
