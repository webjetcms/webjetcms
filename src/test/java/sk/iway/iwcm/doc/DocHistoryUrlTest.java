package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.editor.DocNoteDB;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies pending-version selection and URL-only updates against the configured database. */
class DocHistoryUrlTest extends BaseWebjetTest {
    /** Scheduled and approval versions remain pending, while published and superseded snapshots remain unchanged. */
    @Test
    void updatesOnlyPendingUrlsWithoutChangingPublicationState() {
        DBPool.getInstance();
        DBPool.jpaInitialize();
        EntityManager manager = DBPool.getEntityManagerFactory("iwcm").createEntityManager();
        try {
            manager.getTransaction().begin();
            cleanFixtures(manager);
            DocDetailsRepository pages = new JpaRepositoryFactory(manager).getRepository(DocDetailsRepository.class);
            DocHistoryRepository history = new JpaRepositoryFactory(manager).getRepository(DocHistoryRepository.class);
            DocDetails page = new DocDetails();
            page.setTitle("autotest-pending-url");
            page.setNavbar(page.getTitle());
            page.setData("autotest live content");
            page.setDataAsc(page.getData());
            page.setDateCreated(System.currentTimeMillis());
            page.setGroupId(910081);
            page.setAvailable(false);
            page = pages.saveAndFlush(page);

            DocHistory published = version(page, 910081);
            published.setActual(true);
            published.setApprovedBy(1);
            history.saveAndFlush(published);
            DocHistory firstSchedule = version(page, 910081);
            firstSchedule.setPublicable(true);
            firstSchedule.setPublishStartDate(new Date(2000000000000L));
            history.saveAndFlush(firstSchedule);
            DocHistory secondSchedule = version(page, 910081);
            secondSchedule.setPublicable(true);
            secondSchedule.setPublishStartDate(new Date(2100000000000L));
            history.saveAndFlush(secondSchedule);
            DocHistory approval = version(page, 910081);
            approval.setAwaitingApprove("7,9");
            history.saveAndFlush(approval);
            DocHistory oldDraft = history.saveAndFlush(version(page, 910081));
            DocHistory deleted = version(page, 910081);
            deleted.setPublicable(true);
            deleted.setIsDelete(true);
            history.saveAndFlush(deleted);
            DocHistory foreign = version(page, 910082);
            foreign.setPublicable(true);
            history.saveAndFlush(foreign);
            DocHistory draft = history.saveAndFlush(version(page, 910081));

            Set<Long> expected = Set.of(firstSchedule.getId(), secondSchedule.getId(), approval.getId(), draft.getId());
            assertEquals(expected, history.findPendingByGroupId(910081).stream().map(DocHistory::getId).collect(Collectors.toSet()));
            for (Long id : expected) history.updateVirtualPath(id, "/autotest-new/page.html");

            // Read through the same persistence context used to refresh the publication queue.
            DocHistory scheduled = history.findById(firstSchedule.getId()).orElseThrow();
            assertEquals("/autotest-new/page.html", scheduled.getVirtualPath());
            assertEquals(firstSchedule.getPublishStartDate(), scheduled.getPublishStartDate());
            assertEquals(firstSchedule.getData(), scheduled.getData());
            assertEquals(Boolean.TRUE, scheduled.getPublicable());
            assertEquals(Boolean.FALSE, scheduled.getActual());
            assertFalse(scheduled.isAvailable());
            assertEquals(-1, scheduled.getApprovedBy());
            assertEquals(1, scheduled.getSyncStatus());
            assertEquals("7,9", history.findById(approval.getId()).orElseThrow().getAwaitingApprove());
            assertEquals("/autotest-new/page.html", history.findById(secondSchedule.getId()).orElseThrow().getVirtualPath());
            for (DocHistory unchanged : new DocHistory[] { published, oldDraft, deleted, foreign }) {
                assertEquals("/autotest-old/page.html", history.findById(unchanged.getId()).orElseThrow().getVirtualPath());
            }
            assertFalse(pages.findById(page.getId()).orElseThrow().isAvailable());

            DocHistory rejected = version(page, 910081);
            rejected.setDisapprovedBy(7);
            history.saveAndFlush(rejected);
            assertFalse(history.findPendingByGroupId(910081).stream().anyMatch(item -> item.getId().equals(rejected.getId())));

            publishScheduledVersion(history, pages, firstSchedule.getId());
            manager.clear();
            DocDetails publishedPage = pages.findById(page.getId()).orElseThrow();
            assertEquals("/autotest-new/page.html", publishedPage.getVirtualPath(), "Scheduled publication must not restore the old folder URL");
            assertEquals(firstSchedule.getData(), publishedPage.getData());
            assertTrue(publishedPage.isAvailable());
            assertFalse(history.findById(firstSchedule.getId()).orElseThrow().getPublicable());
            assertTrue(history.findById(secondSchedule.getId()).orElseThrow().getPublicable());
        } finally {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.clear();
            manager.getTransaction().begin();
            cleanFixtures(manager);
            manager.getTransaction().commit();
            manager.close();
        }
    }

    private static void cleanFixtures(EntityManager manager) {
        // Some supported database tables do not roll back inserts; remove only this test's fixtures explicitly.
        manager.createNativeQuery("DELETE FROM documents_history WHERE title = 'autotest-pending-url' AND group_id IN (910081, 910082)").executeUpdate();
        manager.createNativeQuery("DELETE FROM documents WHERE title = 'autotest-pending-url' AND group_id = 910081").executeUpdate();
    }

    private static void publishScheduledVersion(DocHistoryRepository history, DocDetailsRepository pages, Long historyId) {
        DocPublishService publisher = new DocPublishService();
        ReflectionTestUtils.setField(publisher, "dhr", history);
        ReflectionTestUtils.setField(publisher, "ddr", pages);
        DocDB documents = mock(DocDB.class);
        when(documents.getDocLink(anyInt(), any(), isNull())).thenReturn("/autotest-new/page.html");
        try (var initialization = mockStatic(InitServlet.class); var groupAccess = mockStatic(GroupsDB.class);
             var docAccess = mockStatic(DocDB.class); var multigroup = mockStatic(MultigroupMappingDB.class);
             var notes = mockStatic(DocNoteDB.class); var tools = mockStatic(Tools.class, CALLS_REAL_METHODS);
             var constants = mockStatic(Constants.class, CALLS_REAL_METHODS)) {
            initialization.when(InitServlet::isWebjetInitialized).thenReturn(true);
            groupAccess.when(GroupsDB::getInstance).thenReturn(mock(GroupsDB.class));
            docAccess.when(DocDB::getInstance).thenReturn(documents);
            docAccess.when(() -> DocDB.getRootGroupL(anyInt(), any(), anyInt())).thenReturn(new int[] { 0, 0, 0 });
            notes.when(DocNoteDB::getInstance).thenReturn(mock(DocNoteDB.class));
            constants.when(() -> Constants.getBoolean("webpagesNotifyAutorOnPublish")).thenReturn(false);
            tools.when(Tools::getNow).thenReturn(2000000001000L);

            publisher.refreshPagesToPublish();
            DocHistory queued = publisher.getPublicableDocs().stream()
                .filter(item -> item instanceof DocHistory && item.getId().equals(historyId))
                .map(DocHistory.class::cast).findFirst().orElseThrow();
            assertEquals("/autotest-new/page.html", queued.getVirtualPath(), "The refreshed publication queue must contain the new URL");
            // Only publish this test's version; other scheduled pages in the shared test database are untouched.
            ReflectionTestUtils.setField(publisher, "publicableDocs", List.of(queued));
            DocDetailsRepository failingPages = mock(DocDetailsRepository.class, org.mockito.AdditionalAnswers.delegatesTo(pages));
            doThrow(new IllegalStateException("Injected page save failure")).when(failingPages).saveAndFlush(any(DocDetails.class));
            ReflectionTestUtils.setField(publisher, "ddr", failingPages);
            assertThrows(IllegalStateException.class, () -> publisher.copyDHistory(queued, documents));
            assertTrue(history.findById(historyId).orElseThrow().getPublicable(), "A failed page save must remain scheduled for retry");
            ReflectionTestUtils.setField(publisher, "ddr", pages);

            publisher.checkWebpagesToPublish(documents);
            verify(documents).updateInternalCaches(queued.getDocId());
        }
    }

    private static DocHistory version(DocDetails page, int groupId) {
        DocHistory version = new DocHistory();
        version.setDocId(page.getDocId());
        version.setGroupId(groupId);
        version.setTitle(page.getTitle());
        version.setNavbar(page.getNavbar());
        version.setData("autotest pending content");
        version.setDataAsc(version.getData());
        version.setDateCreated(System.currentTimeMillis());
        version.setSaveDate(new Date());
        version.setVirtualPath("/autotest-old/page.html");
        version.setAvailable(false);
        version.setActual(false);
        version.setPublicable(false);
        version.setApprovedBy(-1);
        version.setSyncStatus(0);
        return version;
    }
}
