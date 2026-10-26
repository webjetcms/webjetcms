package sk.iway.iwcm.doc;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.test.BaseWebjetTest;

/** Verifies real page/history storage and both readers against the configured database. */
class PageTitlePersistenceTest extends BaseWebjetTest {
    private static final String UNSAFE = "autotest-title-storage <img src=x onerror=alert('WJ14-TITLE')>";
    private static final String SAFE = "autotest-title-storage <img src=\"x\" />";

    /** Native SQL proves writes are sanitized; legacy rows remain safe through JPA and public JDBC hydration. */
    @Test
    void sanitizesStoredAndLegacyTitles() throws Exception {
        DBPool.getInstance();
        DBPool.jpaInitialize();
        EntityManager manager = DBPool.getEntityManagerFactory("iwcm").createEntityManager();
        Long pageId = null;
        Long historyId = null;
        try {
            manager.getTransaction().begin();
            DocDetailsRepository pages = new JpaRepositoryFactory(manager).getRepository(DocDetailsRepository.class);
            DocHistoryRepository history = new JpaRepositoryFactory(manager).getRepository(DocHistoryRepository.class);
            DocDetails page = new DocDetails();
            page.setGroupId(910093);
            page.setAvailable(false);
            page.setData("autotest title persistence content");
            page.setDataAsc(page.getData());
            page.setDateCreated(System.currentTimeMillis());
            // Bypass setters to verify the JPA write boundary itself.
            ReflectionTestUtils.setField(page, "title", UNSAFE);
            ReflectionTestUtils.setField(page, "navbar", UNSAFE);
            page = pages.saveAndFlush(page);
            pageId = page.getId();
            DocHistory version = new DocHistory();
            version.setDocId(page.getDocId());
            version.setGroupId(page.getGroupId());
            version.setSaveDate(new Date());
            version.setDateCreated(System.currentTimeMillis());
            version.setActual(false);
            version.setPublicable(false);
            version.setIsDelete(false);
            version.setApprovedBy(0);
            version.setData(page.getData());
            version.setDataAsc(page.getDataAsc());
            ReflectionTestUtils.setField(version, "title", UNSAFE);
            ReflectionTestUtils.setField(version, "navbar", UNSAFE);
            historyId = history.saveAndFlush(version).getId();
            manager.getTransaction().commit();

            assertStoredTitles(manager, "documents", "doc_id", pageId, SAFE);
            assertStoredTitles(manager, "documents_history", "history_id", historyId, SAFE);
            evict(manager, pageId, historyId);
            assertTitles(pages.findById(pageId).orElseThrow());
            assertTitles(history.findById(historyId).orElseThrow());

            // Recreate pre-fix rows without passing through either converter or setters.
            manager.getTransaction().begin();
            manager.createNativeQuery("UPDATE documents SET title = ?1, navbar = ?1 WHERE doc_id = ?2")
                .setParameter(1, UNSAFE).setParameter(2, pageId).executeUpdate();
            manager.createNativeQuery("UPDATE documents_history SET title = ?1, navbar = ?1 WHERE history_id = ?2")
                .setParameter(1, UNSAFE).setParameter(2, historyId).executeUpdate();
            manager.getTransaction().commit();
            assertStoredTitles(manager, "documents", "doc_id", pageId, UNSAFE);
            evict(manager, pageId, historyId);
            assertTitles(pages.findById(pageId).orElseThrow());
            assertTitles(history.findById(historyId).orElseThrow());

            try (Connection connection = DBPool.getConnection();
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM documents WHERE doc_id = ?")) {
                statement.setLong(1, pageId);
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    DocDetails publicPage = new DocDetails();
                    DocDB.getDocDetails(rows, publicPage, false, true);
                    assertTitles(publicPage);
                }
            }
        } finally {
            if (manager.getTransaction().isActive()) manager.getTransaction().rollback();
            manager.clear();
            manager.getTransaction().begin();
            if (historyId != null) manager.createNativeQuery("DELETE FROM documents_history WHERE history_id = ?1").setParameter(1, historyId).executeUpdate();
            if (pageId != null) manager.createNativeQuery("DELETE FROM documents WHERE doc_id = ?1").setParameter(1, pageId).executeUpdate();
            manager.getTransaction().commit();
            evict(manager, pageId, historyId);
            manager.close();
        }
    }

    private static void assertTitles(DocBasic doc) {
        assertEquals(SAFE, doc.getTitle());
        assertEquals(SAFE, doc.getNavbar());
    }

    private static void assertStoredTitles(EntityManager manager, String table, String idColumn, Long id, String expected) {
        Object[] row = (Object[]) manager.createNativeQuery("SELECT title, navbar FROM " + table + " WHERE " + idColumn + " = ?1")
            .setParameter(1, id).getSingleResult();
        assertEquals(expected, row[0]);
        assertEquals(expected, row[1]);
    }

    private static void evict(EntityManager manager, Long pageId, Long historyId) {
        manager.clear();
        if (pageId != null) manager.getEntityManagerFactory().getCache().evict(DocDetails.class, pageId.intValue());
        if (historyId != null) manager.getEntityManagerFactory().getCache().evict(DocHistory.class, historyId);
    }
}
