package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.editor.EditorDB;
import sk.iway.iwcm.editor.rest.DocHistoryDto;
import sk.iway.iwcm.editor.rest.DocHistoryRestController;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies history deletion permissions and protected states against the configured database. */
class DocHistoryDeleteTest extends BaseWebjetTest {
    private static final String TITLE = "autotest-history-delete-m9";
    private static final int GROUP_ID = 910091;

    /** Both permission levels preserve live and pending versions, enforce page access and audit successful deletions. */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deletesOnlyAuthorizedVersions(boolean cleanup) {
        DBPool.getInstance();
        DBPool.jpaInitialize();
        EntityManager manager = DBPool.getEntityManagerFactory("iwcm").createEntityManager();
        try {
            manager.getTransaction().begin();
            cleanFixtures(manager);
            DocHistoryRepository history = new JpaRepositoryFactory(manager).getRepository(DocHistoryRepository.class);
            DocDetailsRepository pages = new JpaRepositoryFactory(manager).getRepository(DocDetailsRepository.class);
            DocDetails page = new DocDetails();
            page.setTitle(TITLE);
            page.setNavbar(TITLE);
            page.setGroupId(GROUP_ID);
            page.setData("autotest live content");
            page.setDataAsc(page.getData());
            page.setDateCreated(System.currentTimeMillis());
            page = pages.saveAndFlush(page);
            int docId = page.getDocId();
            Date now = new Date();
            Date future = new Date(now.getTime() + 86400000L);

            DocHistory old = history.saveAndFlush(version(docId, false, null));
            DocHistory scheduled = history.saveAndFlush(version(docId, true, future));
            DocHistory cancelled = history.saveAndFlush(version(docId, false, future));
            DocHistory expired = history.saveAndFlush(version(docId, true, new Date(now.getTime() - 86400000L)));
            DocHistory missingDate = history.saveAndFlush(version(docId, true, null));
            DocHistory published = version(docId, true, future);
            published.setActual(true);
            history.saveAndFlush(published);
            DocHistory approval = version(docId, true, future);
            approval.setAwaitingApprove("7,9");
            history.saveAndFlush(approval);
            DocHistory approvalDraft = version(docId, false, null);
            approvalDraft.setAwaitingApprove("7");
            history.saveAndFlush(approvalDraft);
            DocHistory foreign = history.saveAndFlush(version(910092, false, null));
            // HistoryDB reads through a separate JDBC connection.
            manager.getTransaction().commit();
            manager.getTransaction().begin();

            Set<Long> expected = cleanup
                ? Set.of(old.getId(), scheduled.getId(), cancelled.getId(), expired.getId(), missingDate.getId())
                : Set.of(scheduled.getId());
            Identity user = mock(Identity.class);
            when(user.getUserId()).thenReturn(42);
            when(user.isEnabledItem("cmp_data_deleting")).thenReturn(cleanup);
            when(user.isEnabledItem("menuWebpages")).thenReturn(true);
            DocHistoryRestController controller = new DocHistoryRestController(history) {
                @Override
                public Identity getUser() {
                    return user;
                }
            };
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setParameter("docId", Integer.toString(docId));
            controller.setRequest(request);
            DocHistoryDto forged = new DocHistoryDto();
            forged.setId(old.getId());
            forged.setDocId(docId);
            forged.setDeleteAllowed(true);
            forged.setPublicable(true);
            forged.setHistoryActual(false);
            forged.setPublishStartDate(future);

            try (var editor = mockStatic(EditorDB.class); var audit = mockStatic(Adminlog.class);
                    var mappings = mockStatic(MultigroupMappingDB.class)) {
                // A cleanup permission never bypasses the persisted page's edit permissions.
                assertFalse(controller.deleteItem(forged, scheduled.getId()));
                List<DocHistoryDto> inaccessible = controller.getAllItems(Pageable.unpaged()).getContent();
                assertEquals(8, inaccessible.size());
                assertTrue(inaccessible.stream().noneMatch(DocHistoryDto::isDeleteAllowed));
                editor.when(() -> EditorDB.isPageEditable(user, docId)).thenReturn(true);
                assertEquals(expected, deletableIds(controller));
                assertFalse(controller.deleteItem(forged, foreign.getId()));
                assertFalse(controller.deleteItem(forged, -1L));
                assertFalse(controller.deleteItem(forged, published.getId()));
                assertFalse(controller.deleteItem(forged, approval.getId()));
                assertFalse(controller.deleteItem(forged, approvalDraft.getId()));
                audit.verifyNoInteractions();

                for (DocHistory candidate : List.of(old, scheduled, cancelled, expired, missingDate)) {
                    boolean allowed = expected.contains(candidate.getId());
                    assertEquals(allowed, controller.deleteItem(forged, candidate.getId()));
                    if (allowed) {
                        audit.verify(() -> Adminlog.add(Adminlog.TYPE_DATA_DELETING, 42,
                            "Deleted page history: docId=" + docId + ", historyId=" + candidate.getId(),
                            docId, candidate.getHistoryId()));
                    }
                }
                audit.verifyNoMoreInteractions();
                manager.clear();
                for (Long deletedId : expected) assertFalse(history.existsById(deletedId));
                for (DocHistory protectedVersion : List.of(published, approval, approvalDraft, foreign)) {
                    assertTrue(history.existsById(protectedVersion.getId()));
                }
                assertEquals("autotest live content", pages.findById(page.getId()).orElseThrow().getData());

                // A version that became current after selection must still be protected by the DELETE query.
                DocHistory changed = history.saveAndFlush(version(docId, true, future));
                manager.getTransaction().commit();
                manager.getTransaction().begin();
                assertTrue(deletableIds(controller).contains(changed.getId()));
                changed.setActual(true);
                history.saveAndFlush(changed);
                assertEquals(0, history.deleteDeletableById(changed.getId(), docId, cleanup, now));
                changed.setActual(false);
                changed.setAwaitingApprove("7");
                history.saveAndFlush(changed);
                assertEquals(0, history.deleteDeletableById(changed.getId(), docId, cleanup, now));
            }
        } finally {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.clear();
            manager.getTransaction().begin();
            cleanFixtures(manager);
            manager.getTransaction().commit();
            manager.close();
        }
    }

    private static Set<Long> deletableIds(DocHistoryRestController controller) {
        return controller.getAllItems(Pageable.unpaged()).stream()
            .filter(DocHistoryDto::isDeleteAllowed).map(DocHistoryDto::getId).collect(Collectors.toSet());
    }

    private static DocHistory version(int docId, boolean publicable, Date publishStart) {
        DocHistory version = new DocHistory();
        version.setDocId(docId);
        version.setGroupId(GROUP_ID);
        version.setTitle(TITLE);
        version.setNavbar(TITLE);
        version.setData("autotest archived content");
        version.setDataAsc(version.getData());
        version.setDateCreated(System.currentTimeMillis());
        version.setSaveDate(new Date());
        version.setActual(false);
        version.setPublicable(publicable);
        version.setPublishStartDate(publishStart);
        version.setApprovedBy(-1);
        version.setSyncStatus(0);
        return version;
    }

    private static void cleanFixtures(EntityManager manager) {
        manager.createNativeQuery("DELETE FROM documents_history WHERE title = ?1 AND group_id = ?2")
            .setParameter(1, TITLE).setParameter(2, GROUP_ID).executeUpdate();
        manager.createNativeQuery("DELETE FROM documents WHERE title = ?1 AND group_id = ?2")
            .setParameter(1, TITLE).setParameter(2, GROUP_ID).executeUpdate();
    }
}
