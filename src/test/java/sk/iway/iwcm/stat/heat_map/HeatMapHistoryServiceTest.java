package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.HistoryMatch;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.VersionPeriod;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.ReportRange;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies historical heat-map selection across publication and approval transitions. */
class HeatMapHistoryServiceTest extends BaseWebjetTest {

    private static final int DOC_ID = 123;

    private DocHistory published(int id, long saved) {
        DocHistory history = new DocHistory();
        history.setHistoryId(id);
        history.setDocId(DOC_ID);
        history.setSaveDate(new Date(saved));
        history.setApprovedBy(0);
        history.setAvailable(true);
        history.setPublicable(false);
        history.setIsDelete(false);
        history.setActual(false);
        return history;
    }

    /** Only a published revision owned by the page can replace the baseline from before the range. */
    @Test
    void keepsPublishedBaselineWhenNewerRevisionsAreIneligible() {
        DocHistory baseline = published(1, 100);
        DocHistory draft = published(2, 250);
        draft.setApprovedBy(-1);
        DocHistory rejected = published(3, 250);
        rejected.setDisapprovedBy(7);
        DocHistory awaiting = published(4, 250);
        awaiting.setAwaitingApprove(",7,");
        DocHistory deletion = published(5, 250);
        deletion.setIsDelete(true);
        DocHistory foreign = published(6, 250);
        foreign.setDocId(456);

        HistoryMatch match = HeatMapHistoryService.selectHistory(
                List.of(baseline, draft, rejected, awaiting, deletion, foreign), DOC_ID, 200, 300);

        assertEquals(1, match.history().getHistoryId());
        assertEquals(List.of(new VersionPeriod(1, 100, new ReportRange(200, 301))), match.versions());
    }

    /** Approval, rather than the earlier save, determines when an approved draft became visible. */
    @Test
    void respectsLateApprovalAndOrdersByPublicationTime() {
        DocHistory newerSave = published(2, 200);
        DocHistory lateApproval = published(1, 100);
        lateApproval.setApprovedBy(7);
        lateApproval.setApproveDate(new Date(250));

        assertEquals(2, HeatMapHistoryService.selectHistory(List.of(lateApproval, newerSave), DOC_ID, 0, 240)
                .history().getHistoryId());
        HistoryMatch afterApproval = HeatMapHistoryService.selectHistory(List.of(lateApproval, newerSave), DOC_ID, 0, 250);
        assertEquals(1, afterApproval.history().getHistoryId());
        assertEquals(250, afterApproval.versions().get(0).effectiveFrom());
    }

    /** Completed scheduling respects both the publication date and a possibly later approval. */
    @Test
    void respectsScheduledPublicationAndLateApproval() {
        DocHistory scheduled = published(1, 100);
        scheduled.setPublishStartDate(new Date(300));
        assertEquals(100, HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 200).versions().get(0).effectiveFrom());

        scheduled.setPublishAfterStart(true);
        scheduled.setApproveDate(new Date(200));

        assertNull(HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 299).history());
        assertEquals(300, HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 300).versions().get(0).effectiveFrom());

        scheduled.setApproveDate(new Date(350));
        assertNull(HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 349).history());
        assertEquals(350, HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 350).versions().get(0).effectiveFrom());
    }

    /** A synced revision remains visible while later scheduled revisions are pending or canceled. */
    @Test
    void excludesPendingAndCanceledSchedules() {
        DocHistory pending = published(1, 100);
        pending.setApprovedBy(7);
        pending.setActual(true);
        pending.setPublicable(true);
        pending.setPublishAfterStart(true);
        pending.setPublishStartDate(new Date(200));

        DocHistory canceled = published(2, 150);
        canceled.setPublishAfterStart(true);
        canceled.setPublishStartDate(new Date(250));
        canceled.setSyncStatus(2);

        DocHistory synced = published(3, 50);
        synced.setSyncStatus(2);
        assertSame(synced, HeatMapHistoryService.selectHistory(List.of(synced, pending, canceled), DOC_ID, 0, 500).history());
    }

    /** Disabled, redirected and remapped revisions each prevent a preview of the older content. */
    @Test
    void keepsUnavailableLatestRevision() {
        DocHistory older = published(1, 100);
        DocHistory disabled = published(2, 200);
        disabled.setAvailable(false);
        DocHistory redirected = published(3, 200);
        redirected.setExternalLink("https://external.example/new-page");
        DocHistory remapped = published(4, 200);
        remapped.setData("<p>!REMAP_PAGE(456)!</p>");

        for (DocHistory latest : List.of(disabled, redirected, remapped)) {
            HistoryMatch match = HeatMapHistoryService.selectHistory(List.of(older, latest), DOC_ID, 0, 300);
            assertSame(latest, match.history());
            assertFalse(match.available(), "History ID " + latest.getHistoryId());
        }
    }

    /** Expiration deactivates a revision only when its automatic deactivation flag is set. */
    @Test
    void evaluatesDeactivationAtTheRangeEnd() {
        DocHistory history = published(1, 100);
        history.setPublishEndDate(new Date(300));
        assertTrue(HeatMapHistoryService.selectHistory(List.of(history), DOC_ID, 0, 400).available());

        history.setDisableAfterEnd(true);
        assertTrue(HeatMapHistoryService.selectHistory(List.of(history), DOC_ID, 0, 299).available());
        assertFalse(HeatMapHistoryService.selectHistory(List.of(history), DOC_ID, 0, 300).available());
    }

    /** Only revisions whose validity overlaps the reporting range contribute to the warning. */
    @Test
    void countsOverlappingVersionsAndHandlesTheRangeStart() {
        DocHistory older = published(1, 100);
        DocHistory newer = published(2, 200);
        List<DocHistory> history = List.of(older, newer);

        assertEquals(2, HeatMapHistoryService.selectHistory(history, DOC_ID, 150, 300).versions().size());
        assertEquals(1, HeatMapHistoryService.selectHistory(history, DOC_ID, 200, 300).versions().size());
        assertEquals(1, HeatMapHistoryService.selectHistory(history, DOC_ID, 250, 300).versions().size());
    }

    /** The baseline and latest intervals are clipped, and ambiguity and expiry leave gaps. */
    @Test
    void limitsVersionChoicesToTheirPublishedIntervals() {
        DocHistory ambiguous = published(3, 200);
        ambiguous.setApprovedBy(null);
        DocHistory expired = published(5, 300);
        expired.setDisableAfterEnd(true);
        expired.setPublishEndDate(new Date(350));
        var history = List.of(published(1, 50), published(2, 100), ambiguous, published(4, 300),
                expired, published(6, 400), published(7, 600));
        var match = HeatMapHistoryService.selectHistory(history, DOC_ID, 150, 499);
        assertEquals(List.of(new VersionPeriod(6, 400, new ReportRange(400, 500)),
                new VersionPeriod(5, 300, new ReportRange(300, 350)),
                new VersionPeriod(2, 100, new ReportRange(150, 200))), match.versions());
    }

    /** A newer incomplete historical row must not silently resurrect an earlier accepted version. */
    @Test
    void declinesAmbiguousNewerHistory() {
        DocHistory baseline = published(1, 100);
        DocHistory unknownApproval = published(2, 200);
        unknownApproval.setApprovedBy(null);
        DocHistory missingStart = published(3, 200);
        missingStart.setPublishAfterStart(true);
        DocHistory missingEnd = published(4, 200);
        missingEnd.setDisableAfterEnd(true);

        for (DocHistory ambiguous : List.of(unknownApproval, missingStart, missingEnd)) {
            HistoryMatch match = HeatMapHistoryService.selectHistory(List.of(baseline, ambiguous), DOC_ID, 0, 300);
            assertNull(match.history());
            assertEquals(List.of(new VersionPeriod(1, 100, new ReportRange(100, 200))), match.versions());
            assertEquals(5, HeatMapHistoryService.selectHistory(List.of(ambiguous, published(5, 250)), DOC_ID, 0, 300)
                    .history().getHistoryId());
        }
    }

    /** An ambiguous revision at the same publication time takes precedence only when its history ID is newer. */
    @Test
    void resolvesAmbiguousPublicationTimeTiesByHistoryId() {
        DocHistory ambiguous = published(2, 100);
        ambiguous.setApprovedBy(null);

        assertNull(HeatMapHistoryService.selectHistory(List.of(ambiguous, published(1, 100)), DOC_ID, 0, 300).history());
        assertEquals(3, HeatMapHistoryService.selectHistory(List.of(ambiguous, published(3, 100)), DOC_ID, 0, 300)
                .history().getHistoryId());
    }

    /** Shared historical content is loaded without cache and retains the authorized slave page's identity. */
    @Test
    void loadsVerifiedMasterHistoryThroughDocDbForSlavePreview() {
        HeatMapAccess access = mock(HeatMapAccess.class);
        DocHistoryRepository repository = mock(DocHistoryRepository.class);
        HeatMapHistoryService service = spy(new HeatMapHistoryService(repository, access));
        MockHttpServletRequest request = new MockHttpServletRequest();
        DocDetails current = new DocDetails();
        current.setDocId(DOC_ID);
        current.setGroupId(5);
        current.setVirtualPath("/slave-page.html");
        when(access.requireDocument(request, DOC_ID)).thenReturn(current);
        when(access.dateRange("period")).thenReturn(new Date[] { new Date(0), new Date(500) });
        DocHistory history = published(7, 100);
        history.setDocId(42);
        DocHistory later = published(8, 300);
        later.setDocId(42);
        doReturn(List.of(history, later)).when(service).loadHistory(42, 500);
        when(repository.findById(7L)).thenReturn(Optional.of(history));
        DocDetails historicalContent = new DocDetails();
        historicalContent.setDocId(42);
        historicalContent.setAvailable(true);
        historicalContent.setData("<p>Published shared content</p>");
        DocDB docs = mock(DocDB.class);
        when(docs.getDoc(-1, 7, false)).thenReturn(historicalContent);

        try (MockedStatic<MultigroupMappingDB> mappings = mockStatic(MultigroupMappingDB.class);
                MockedStatic<DocDB> lookup = mockStatic(DocDB.class)) {
            mappings.when(() -> MultigroupMappingDB.getMasterDocId(DOC_ID, true)).thenReturn(42);
            lookup.when(DocDB::getInstance).thenReturn(docs);

            HeatMapHistoryService.PreviewSelection selection = service.resolve(request, DOC_ID, "period", 7);

            assertEquals("history", selection.source());
            assertEquals(7, selection.historyId());
            assertEquals(new ReportRange(100, 300), selection.clickRange());
            assertSame(historicalContent, selection.document());
            assertEquals(DOC_ID, selection.document().getDocId());
            assertEquals(5, selection.document().getGroupId());
            assertEquals("/slave-page.html", selection.document().getVirtualPath());
            verify(docs).getDoc(-1, 7, false);
            verify(access).requireDocument(request, DOC_ID);
            assertEquals(selection.clickRange(), service.versionRange(DOC_ID, access.dateRange("period"), 7));
            assertThrows(ResponseStatusException.class, () -> service.resolve(request, DOC_ID, "period", 9));
            verify(service).loadHistory(42, 500);
        } finally {
            HeatMapHistoryService.historyDeleted(42);
        }
    }

    /** A mismatched history owner is rejected even after metadata selection and uses an explicit current fallback. */
    @Test
    void rejectsLoadedHistoryOwnedByAnotherPage() {
        HeatMapAccess access = mock(HeatMapAccess.class);
        DocHistoryRepository repository = mock(DocHistoryRepository.class);
        HeatMapHistoryService service = spy(new HeatMapHistoryService(repository, access));
        MockHttpServletRequest request = new MockHttpServletRequest();
        DocDetails current = new DocDetails();
        current.setDocId(DOC_ID);
        current.setAvailable(true);
        when(access.requireDocument(request, DOC_ID)).thenReturn(current);
        when(access.dateRange("period")).thenReturn(new Date[] { new Date(0), new Date(500) });
        DocHistory history = published(7, 100);
        doReturn(List.of(history)).when(service).loadHistory(DOC_ID, 500);
        when(repository.findById(7L)).thenReturn(Optional.of(history));
        DocDetails foreignContent = new DocDetails();
        foreignContent.setDocId(456);
        DocDB docs = mock(DocDB.class);
        when(docs.getDoc(-1, 7, false)).thenReturn(foreignContent);
        when(docs.getDoc(DOC_ID, -1, false)).thenReturn(current);

        try (MockedStatic<MultigroupMappingDB> mappings = mockStatic(MultigroupMappingDB.class);
                MockedStatic<DocDB> lookup = mockStatic(DocDB.class)) {
            mappings.when(() -> MultigroupMappingDB.getMasterDocId(DOC_ID, true)).thenReturn(DOC_ID);
            lookup.when(DocDB::getInstance).thenReturn(docs);

            HeatMapHistoryService.PreviewSelection selection = service.resolve(request, DOC_ID, "period", null);

            assertEquals("current", selection.source());
            assertTrue(selection.historicalUnavailable());
            assertNull(selection.historyId());
            assertSame(current, selection.document());
        }
    }
}
