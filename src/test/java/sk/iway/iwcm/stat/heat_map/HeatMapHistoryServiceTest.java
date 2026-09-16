package sk.iway.iwcm.stat.heat_map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocHistory;
import sk.iway.iwcm.doc.DocHistoryRepository;
import sk.iway.iwcm.doc.MultigroupMappingDB;
import sk.iway.iwcm.stat.heat_map.HeatMapHistoryService.HistoryMatch;

/** Verifies historical heat-map selection across publication and approval transitions. */
class HeatMapHistoryServiceTest {

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

    /** A version predating the range remains the correct baseline even after actual is cleared. */
    @Test
    void selectsOlderPublishedBaselineAndIgnoresDrafts() {
        DocHistory baseline = published(1, 100);
        DocHistory draft = published(2, 250);
        draft.setApprovedBy(-1);

        HistoryMatch match = HeatMapHistoryService.selectHistory(List.of(baseline, draft), DOC_ID, 200, 300);

        assertEquals(1, match.history().getHistoryId());
        assertEquals(100, match.effectiveFrom());
        assertFalse(match.multipleVersions());
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
        assertEquals(250, afterApproval.effectiveFrom());
    }

    /** Completed scheduling respects both the publication date and a possibly later approval. */
    @Test
    void respectsScheduledPublicationAndLateApproval() {
        DocHistory scheduled = published(1, 100);
        scheduled.setPublishAfterStart(true);
        scheduled.setPublishStartDate(new Date(300));
        scheduled.setApproveDate(new Date(200));

        assertNull(HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 299));
        assertEquals(300, HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 300).effectiveFrom());

        scheduled.setApproveDate(new Date(350));
        assertNull(HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 349));
        assertEquals(350, HeatMapHistoryService.selectHistory(List.of(scheduled), DOC_ID, 0, 350).effectiveFrom());
    }

    /** A perex start date alone must not be mistaken for a scheduled publication date. */
    @Test
    void ignoresPublishStartWithoutSchedulingFlag() {
        DocHistory history = published(1, 100);
        history.setPublishStartDate(new Date(1000));

        assertEquals(100, HeatMapHistoryService.selectHistory(List.of(history), DOC_ID, 0, 200).effectiveFrom());
    }

    /** Approval and actual flags must not allow queued or canceled schedules into the preview. */
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

        assertNull(HeatMapHistoryService.selectHistory(List.of(pending, canceled), DOC_ID, 0, 500));
    }

    /** Sync status two denotes a canceled schedule only when the revision was scheduled. */
    @Test
    void acceptsOrdinarySyncedRevisionWithStatusTwo() {
        DocHistory history = published(1, 100);
        history.setSyncStatus(2);

        assertEquals(1, HeatMapHistoryService.selectHistory(List.of(history), DOC_ID, 0, 500).history().getHistoryId());
    }

    /** Rejections, incomplete approvals, and delete requests are not renderable content versions. */
    @Test
    void excludesRejectedPendingDeleteAndUnownedVersions() {
        DocHistory rejected = published(1, 100);
        rejected.setDisapprovedBy(7);
        DocHistory awaiting = published(2, 100);
        awaiting.setAwaitingApprove(",7,");
        DocHistory deletion = published(3, 100);
        deletion.setIsDelete(true);
        DocHistory foreign = published(4, 100);
        foreign.setDocId(456);
        DocHistory unknown = published(5, 100);
        unknown.setApprovedBy(null);

        assertNull(HeatMapHistoryService.selectHistory(List.of(rejected, awaiting, deletion, foreign, unknown), DOC_ID, 0, 500));
    }

    /** A disabled latest revision must not resurrect an older available revision. */
    @Test
    void retainsLatestDisabledRevision() {
        DocHistory older = published(1, 100);
        DocHistory disabled = published(2, 200);
        disabled.setAvailable(false);

        HistoryMatch match = HeatMapHistoryService.selectHistory(List.of(older, disabled), DOC_ID, 0, 300);

        assertEquals(2, match.history().getHistoryId());
        assertFalse(match.available());
    }

    /** Redirects and content remapping must not render a different page beneath an old click map. */
    @Test
    void marksRedirectedOrRemappedLatestRevisionUnavailable() {
        DocHistory older = published(1, 100);
        DocHistory redirected = published(2, 200);
        redirected.setExternalLink("https://external.example/new-page");
        HistoryMatch redirect = HeatMapHistoryService.selectHistory(List.of(older, redirected), DOC_ID, 0, 300);
        assertEquals(2, redirect.history().getHistoryId());
        assertFalse(redirect.available());

        DocHistory remapped = published(3, 250);
        remapped.setData("<p>!REMAP_PAGE(456)!</p>");
        HistoryMatch remap = HeatMapHistoryService.selectHistory(List.of(older, remapped), DOC_ID, 0, 300);
        assertEquals(3, remap.history().getHistoryId());
        assertFalse(remap.available());
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

        assertTrue(HeatMapHistoryService.selectHistory(history, DOC_ID, 150, 300).multipleVersions());
        assertFalse(HeatMapHistoryService.selectHistory(history, DOC_ID, 200, 300).multipleVersions());
        assertFalse(HeatMapHistoryService.selectHistory(history, DOC_ID, 250, 300).multipleVersions());
    }

    /** Equal publication times resolve deterministically without implying an extra layout interval. */
    @Test
    void breaksPublicationTimeTiesByHistoryId() {
        HistoryMatch match = HeatMapHistoryService.selectHistory(List.of(published(2, 100), published(1, 100)), DOC_ID, 0, 100);

        assertEquals(2, match.history().getHistoryId());
        assertFalse(match.multipleVersions());
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
            assertNull(HeatMapHistoryService.selectHistory(List.of(baseline, ambiguous), DOC_ID, 0, 300));
            assertEquals(5, HeatMapHistoryService.selectHistory(List.of(ambiguous, published(5, 250)), DOC_ID, 0, 300)
                    .history().getHistoryId());
        }
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
        doReturn(List.of(history)).when(service).loadHistory(42, 500);
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

            HeatMapHistoryService.PreviewSelection selection = service.resolve(request, DOC_ID, "period");

            assertEquals("history", selection.source());
            assertEquals(7, selection.historyId());
            assertSame(historicalContent, selection.document());
            assertEquals(DOC_ID, selection.document().getDocId());
            assertEquals(5, selection.document().getGroupId());
            assertEquals("/slave-page.html", selection.document().getVirtualPath());
            verify(docs).getDoc(-1, 7, false);
            verify(access).requireDocument(request, DOC_ID);
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

            HeatMapHistoryService.PreviewSelection selection = service.resolve(request, DOC_ID, "period");

            assertEquals("current", selection.source());
            assertTrue(selection.historicalUnavailable());
            assertNull(selection.historyId());
            assertSame(current, selection.document());
        }
    }
}
