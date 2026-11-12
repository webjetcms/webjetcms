package sk.iway.iwcm.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.components.news.FieldEnum;
import sk.iway.iwcm.components.news.NewsQuery;
import sk.iway.iwcm.components.news.criteria.DatabaseCriteria;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.stat.StatNewDB;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.users.UsersDB;

/**
 * Opt-in isolation proof on the explicitly authorized local working database.
 * The only content inserts are uncommitted technical rows, always rolled back.
 */
@EnabledIfEnvironmentVariable(named = "WEBJET_P01_CORE", matches = "true")
class BranchCoreReadPocTest extends BaseWebjetTest
{
    /** Real pooled/JPA/cache and streaming reads must never see a pending reservation. */
    @Test
    void coreReadersHideTechnicalRows() throws Exception
    {
        DBPool.getInstance();
        boolean original = Constants.getBoolean("branchesEnabled");
        boolean originalExternalDirectories = Constants.getBoolean("enableStaticFilesExternalDir");
        Constants.setBoolean("enableStaticFilesExternalDir", true);
        Constants.setBoolean("branchesEnabled", true);
        EntityManager manager = null;
        try (Connection writer = DBPool.getConnection())
        {
            assertNotNull(writer);
            String catalog = writer.getCatalog();
            assertTrue("webjetcms_autotest_web".equals(catalog) || "webjet_p01".equals(catalog),
                "Only the authorized working profile or disposable P01 database may be used");
            String url = writer.getMetaData().getURL();
            assertTrue(url.startsWith("jdbc:mariadb://host.docker.internal/")
                || url.startsWith("jdbc:mariadb://127.0.0.1:33316/"));
            // Refuse before INSERT: MyISAM rows would be visible and survive rollback.
            requireInnoDb(writer, "documents");
            requireInnoDb(writer, "groups");
            writer.setAutoCommit(false);
            try
            {
                int docId = insert(writer, "INSERT INTO documents (title,data,data_asc,navbar) "
                    + "VALUES ('autotest P01 technical row','','','autotest P01 technical row')");
                int groupId = insert(writer, "INSERT INTO `groups` (group_name) VALUES ('autotest P01 technical group')");

                try (Connection reader = DBPool.getConnectionReadUncommited())
                {
                    assertEquals(Connection.TRANSACTION_READ_COMMITTED, reader.getTransactionIsolation());
                    assertHidden(reader, "documents", "doc_id", docId);
                    assertHidden(reader, "groups", "group_id", groupId);
                    try (PreparedStatement statement = StatNewDB.prepareStatement(reader, "SELECT doc_id FROM documents WHERE doc_id=?"))
                    {
                        statement.setInt(1, docId);
                        try (ResultSet result = statement.executeQuery()) { assertFalse(result.next()); }
                    }
                    // Exercise an existing statistics table through its streaming helper.
                    try (PreparedStatement statement = StatNewDB.prepareStatement(reader, "SELECT doc_id FROM stat_views LIMIT 1");
                        ResultSet result = statement.executeQuery())
                    {
                        result.next();
                    }
                }

                ComplexQuery streaming = new ComplexQuery().setSql("SELECT doc_id FROM documents WHERE doc_id=?").setParams(docId);
                streaming.setStreamingResultSet(true);
                assertTrue(streaming.list(rs -> rs.getInt("doc_id")).isEmpty());
                assertEquals(0, new NewsQuery().addCriteria(DatabaseCriteria.equal(FieldEnum.DOC_ID, docId)).getNewsCount());
                assertNull(DocDB.getInstance().getBasicDocDetails(docId, false));
                assertNull(DocDB.getInstance().getDoc(docId, -1, false));
                assertNull(GroupsDB.getInstance(true).getGroup(groupId));
                assertNotNull(UsersDB.getUser(1));

                DBPool.jpaInitialize();
                manager = DBPool.getEntityManagerFactory("iwcm").createEntityManager();
                String isolation = String.valueOf(manager.createNativeQuery("SELECT @@session.tx_isolation").getSingleResult());
                assertTrue(java.util.Set.of("READ-COMMITTED", "REPEATABLE-READ", "SERIALIZABLE")
                    .contains(isolation.toUpperCase(java.util.Locale.ROOT).replace('_', '-')),
                    "The direct JPA datasource must provide at least READ_COMMITTED");
                DocDetailsRepository pages = new JpaRepositoryFactory(manager).getRepository(DocDetailsRepository.class);
                assertTrue(pages.findById((long) docId).isEmpty());
                assertEquals(0L, ((Number) manager.createNativeQuery("SELECT COUNT(*) FROM `groups` WHERE group_id=?1")
                    .setParameter(1, groupId).getSingleResult()).longValue());

                System.out.println("P01 core READ_COMMITTED: pending document/group hidden from pooled JDBC, "
                    + "DocDB/GroupsDB caches, NewsQuery, both streaming helpers and EclipseLink repository; users/statistics read OK");
            }
            finally
            {
                writer.rollback();
            }
        }
        finally
        {
            if (manager != null) manager.close();
            DBPool.jpaDestroy();
            Constants.setBoolean("branchesEnabled", original);
            Constants.setBoolean("enableStaticFilesExternalDir", originalExternalDirectories);
        }
    }

    private int insert(Connection connection, String sql) throws Exception
    {
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS))
        {
            assertEquals(1, statement.executeUpdate());
            try (ResultSet result = statement.getGeneratedKeys()) { assertTrue(result.next()); return result.getInt(1); }
        }
    }

    private void requireInnoDb(Connection connection, String table) throws Exception
    {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT engine FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?"))
        {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery())
            {
                assertTrue(result.next());
                assertEquals("InnoDB", result.getString(1), "P01 refuses nontransactional " + table + " before writing");
            }
        }
    }

    private void assertHidden(Connection connection, String table, String key, int id) throws Exception
    {
        try (PreparedStatement statement = connection.prepareStatement("SELECT " + key + " FROM `" + table + "` WHERE " + key + "=?"))
        {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) { assertFalse(result.next()); }
        }
    }
}
