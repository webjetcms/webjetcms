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

/** Verifies safe HTML and unchanged URLs in real page/history storage and both readers. */
class PageTitlePersistenceTest extends BaseWebjetTest {
    private static final String UNSAFE = "autotest-title-storage <img src=x onerror=alert('WJ14-TITLE')>";
    private static final String SAFE = "autotest-title-storage <img src=\"x\" />";
    private static final String UNSAFE_FILE_NAME = "/autotest <b onclick=alert(1)>folder</b>";
    private static final String SAFE_FILE_NAME = "/autotest <b>folder</b>";
    private static final String VIRTUAL_PATH = "/autotest-url/?q=%C5%BElta+modra&sort=a%26b%3Dc#section";
    private static final String EXTERNAL_LINK = "https://example.invalid/search?q=a&lang=sk&token=a+b%2Fc==#results";
    private static final String PEREX_IMAGE = "/images/autotest.jpg?width=320&height=200&crop=1#preview";

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
            ReflectionTestUtils.setField(page, "fileName", UNSAFE_FILE_NAME);
            setUrls(page);
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
            ReflectionTestUtils.setField(version, "fileName", UNSAFE_FILE_NAME);
            setUrls(version);
            historyId = history.saveAndFlush(version).getId();
            manager.getTransaction().commit();

            assertStoredTitles(manager, "documents", "doc_id", pageId, SAFE);
            assertStoredTitles(manager, "documents_history", "history_id", historyId, SAFE);
            evict(manager, pageId, historyId);
            assertTitles(pages.findById(pageId).orElseThrow());
            assertTitles(history.findById(historyId).orElseThrow());

            // Recreate pre-fix rows without passing through either converter or setters.
            manager.getTransaction().begin();
            manager.createNativeQuery("UPDATE documents SET title = ?1, navbar = ?1, file_name = ?3 WHERE doc_id = ?2")
                .setParameter(1, UNSAFE).setParameter(2, pageId).setParameter(3, UNSAFE_FILE_NAME).executeUpdate();
            manager.createNativeQuery("UPDATE documents_history SET title = ?1, navbar = ?1, file_name = ?3 WHERE history_id = ?2")
                .setParameter(1, UNSAFE).setParameter(2, historyId).setParameter(3, UNSAFE_FILE_NAME).executeUpdate();
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
                    assertEquals(SAFE, publicPage.getTitle());
                    assertEquals(SAFE, publicPage.getNavbar());
                    // JDBC's default column filter escapes markup before the entity setter.
                    assertEquals("/autotest &lt;b onclick&#61;alert(1)&gt;folder&lt;/b&gt;", publicPage.getFileName());
                    assertUrls(publicPage);
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
        assertEquals(SAFE_FILE_NAME, doc.getFileName());
        assertUrls(doc);
    }

    private static void assertStoredTitles(EntityManager manager, String table, String idColumn, Long id, String expected) {
        Object[] row = (Object[]) manager.createNativeQuery("SELECT title, navbar, file_name, virtual_path, external_link, perex_image FROM " + table + " WHERE " + idColumn + " = ?1")
            .setParameter(1, id).getSingleResult();
        assertEquals(expected, row[0]);
        assertEquals(expected, row[1]);
        assertEquals(SAFE.equals(expected) ? SAFE_FILE_NAME : UNSAFE_FILE_NAME, row[2]);
        assertEquals(VIRTUAL_PATH, row[3]);
        assertEquals(EXTERNAL_LINK, row[4]);
        assertEquals(PEREX_IMAGE, row[5]);
    }

    private static void setUrls(DocBasic doc) {
        doc.setVirtualPath(VIRTUAL_PATH);
        doc.setExternalLink(EXTERNAL_LINK);
        doc.setPerexImage(PEREX_IMAGE);
    }

    private static void assertUrls(DocBasic doc) {
        assertEquals(VIRTUAL_PATH, doc.getVirtualPath());
        assertEquals(EXTERNAL_LINK, doc.getExternalLink());
        assertEquals(PEREX_IMAGE, doc.getPerexImage());
    }

    private static void evict(EntityManager manager, Long pageId, Long historyId) {
        manager.clear();
        if (pageId != null) manager.getEntityManagerFactory().getCache().evict(DocDetails.class, pageId.intValue());
        if (historyId != null) manager.getEntityManagerFactory().getCache().evict(DocHistory.class, historyId);
    }
}
